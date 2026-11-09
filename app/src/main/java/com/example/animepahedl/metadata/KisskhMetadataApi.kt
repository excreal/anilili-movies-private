package com.example.animepahedl.metadata

import com.example.animepahedl.*

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Public KissKH data contract. Country/type are always Korean TV, including local search/sections. */
internal class KisskhMetadataApi(cookieHeader: ((String) -> String)? = null) : SiteMetadataApi {
    override val site = MetadataSite.KISSKH
    private val http = MetadataHttp(setOf("kisskh.do", "www.kisskh.do"), cookieHeader)
    private val origin = "https://kisskh.do/".toHttpUrl()
    private val taxonomyLock = Mutex()
    private var taxonomyCache: Pair<Long, MetadataTaxonomy>? = null
    private val indexLock = Mutex()
    private val indexRequests = Semaphore(4)
    private val detailLock = Mutex()
    private val detailCache = linkedMapOf<Long, Pair<Long, MetadataDetails>>()
    private data class Index(val loaded: Long, val rows: List<JsonObject>)
    private val indexes = linkedMapOf<String, Index>()

    override suspend fun browse(query: MetadataBrowseQuery): MetadataPage {
        require(query.page > 0) { "Page must be positive." }
        val incoming = query.url?.let(http::checked)
        require(incoming == null || incoming.encodedPath in setOf("/", "/Explore", "/api/DramaList/List")) { "Unsupported KissKH listing URL." }
        val filters = linkedMapOf<String, String>().apply {
            incoming?.queryParameterNames?.forEach { key -> put(key, incoming.queryParameter(key).orEmpty()) }
            putAll(query.filters)
        }
        val supported = setOf("type", "country", "sub", "status", "order", "page", "pageSize", "_search", "_section", "section")
        require(filters.keys.all { it in supported }) { "Unsupported KissKH filter: ${(filters.keys - supported).joinToString()}." }
        require(filters["type"] in setOf(null, "1") && filters["country"] in setOf(null, "2")) { "KissKH is limited to Korean TV dramas." }
        require(filters["page"] == null || filters["page"]?.toIntOrNull() != null) { "Invalid KissKH page." }
        require(filters["pageSize"] == null || filters["pageSize"]?.toIntOrNull() != null) { "Invalid KissKH page size." }
        val page = filters["page"]?.toIntOrNull() ?: query.page
        val size = filters["pageSize"]?.toIntOrNull() ?: 48
        require(page in 1..10000 && size in 1..500) { "Invalid KissKH pagination." }
        val sub = filters["sub"] ?: "0"
        val status = filters["status"] ?: "0"
        val order = filters["order"] ?: "1"
        require(sub in setOf("0", "1", "2", "3", "4", "5", "10") && status in setOf("0", "1", "2", "3") && order in setOf("1", "2", "3")) { "Unsupported KissKH filter value." }
        val term = query.query.trim().ifBlank { filters["_search"].orEmpty().trim() }
        val section = filters["section"] ?: filters["_section"]
        val endpoint = section?.let { sections[it] ?: throw IllegalArgumentException("Unsupported KissKH section.") }
        val payloads = mutableListOf<MetadataPayload>()
        val rows: List<JsonObject>
        val total: Long
        if (term.isNotBlank() || section != null) {
            // /Search ignores country; a full Korean-only index prevents accidental foreign results.
            val korean = koreanIndex(sub, status, order)
            var selected = korean
            if (endpoint != null) {
                val response = http.get(origin.resolve("api/DramaList/$endpoint")!!.toString())
                val data = response.json()
                require(data.isJsonArray) { "KissKH section returned an unexpected response." }
                val allowed = korean.associateBy { it.kString("id") }
                selected = data.asJsonArray.mapNotNull { element ->
                    element.takeIf(JsonElement::isJsonObject)?.asJsonObject?.takeIf { it.kString("id") in allowed }
                }
                // Retain only rows permitted by this API's Korean-TV policy.
                payloads += metadataPayload("section-$section", response.url, JsonArray().apply { selected.forEach(::add) })
            }
            if (term.isNotBlank()) selected = selected.filter { it.kString("title").contains(term, ignoreCase = true) }
            total = selected.size.toLong()
            rows = selected.drop(((page.toLong() - 1) * size).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).take(size)
        } else {
            val response = http.get(listUrl(page, size, sub, status, order).toString())
            val envelope = response.json().asJsonObject
            rows = envelope.kObjects("data")
            total = envelope.kString("totalCount").toLongOrNull() ?: rows.size.toLong()
            payloads += response.payload("korean-tv-list")
        }
        val next = if (page.toLong() * size < total) listUrl(page + 1, size, sub, status, order).newBuilder().apply {
            if (term.isNotBlank()) addQueryParameter("_search", term)
            if (section != null) addQueryParameter("_section", section)
        }.build().toString() else null
        val items = rows.map(::record)
        // Keep the common listing call to its one page request. Explore's filter options live in
        // separately versioned JS chunks; fetch them only when a caller explicitly asks taxonomy().
        payloads += metadataPayload("taxonomy-status", "https://kisskh.do/Explore", JsonObject().apply {
            addProperty("status", "not-requested")
            addProperty("reason", "Call taxonomy() to load the separately published web filter options.")
        })
        val filterGroups = emptyMap<String, List<MetadataTerm>>()
        return MetadataPage(items, next, total,
            sections = if (section != null) listOf(MetadataSection(section, items)) else emptyList(),
            filters = filterGroups, payloads = payloads)
    }

    override suspend fun details(url: String): MetadataDetails {
        val target = http.checked(url)
        val id = target.queryParameter("id")?.toLongOrNull()?.takeIf { it > 0 }
            ?: throw IllegalArgumentException("A KissKH drama URL must contain its series ID.")
        return detailLock.withLock {
            val now = System.nanoTime()
            detailCache[id]?.takeIf { now - it.first < java.util.concurrent.TimeUnit.MINUTES.toNanos(2) }?.let { return@withLock it.second }
            val response = http.get(origin.resolve("api/DramaList/Drama/$id?isq=false")!!.toString())
            val row = response.json().asJsonObject
            require(row.kString("country") in setOf("South Korea", "Korea") && row.kString("type") == "TVSeries") { "KissKH is limited to Korean TV dramas." }
            val base = record(row).copy(payloads = listOf(response.payload("drama-detail")))
            val episodes = row.kObjects("episodes").sortedBy { it.kString("number").toBigDecimalOrNull() }
                .map { episodeRecord(it, base) }
            val result = MetadataDetails(base, episodes, payloads = base.payloads)
            detailCache[id] = now to result
            while (detailCache.size > 32) detailCache.remove(detailCache.keys.first())
            result
        }
    }

    override suspend fun episode(url: String): MetadataEpisode {
        val target = http.checked(url)
        val show = details(url)
        val id = target.queryParameter("ep") ?: show.episodes.firstOrNull()?.id
            ?: throw IllegalArgumentException("This drama has no episode metadata.")
        val episode = show.episodes.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("The episode does not belong to this Korean drama.")
        val attributes = episode.attributes.toMutableMap()
        val payloads = episode.payloads.toMutableList()
        // These independent JSON descriptor requests should not serialize a playable episode
        // behind a slow caption endpoint. Neither request downloads media/caption bodies.
        val descriptors = coroutineScope {
            val subtitles = async {
                try {
                    val descriptorUrl = origin.resolve("api/Sub/$id")!!.newBuilder()
                        .addQueryParameter("kkey", kisskhRequestKey(id, "VgV52sWhwvBSf8BsM3BRY9weWiiCbtGp")).build()
                    val response = http.get(descriptorUrl.toString())
                    val tracks = response.json()
                    require(tracks.isJsonArray) { "KissKH returned invalid subtitle descriptors." }
                    val mapped = tracks.asJsonArray.mapNotNull { element ->
                        val track = element.takeIf(JsonElement::isJsonObject)?.asJsonObject ?: return@mapNotNull null
                        val src = track.kString("src").takeIf(String::isNotBlank) ?: return@mapNotNull null
                        val parsed = runCatching { src.toHttpUrl() }.getOrNull()?.takeIf {
                            it.isHttps && it.username.isEmpty() && it.password.isEmpty()
                        } ?: return@mapNotNull null
                        MetadataSubtitle(parsed.toString(), track.kString("label"), track.kString("land").ifBlank { null },
                            parsed.encodedPath.substringAfterLast('.', "").ifBlank { null }, track.kString("default").equals("true", true), track.kAttributes())
                    }
                    mapped to response.payload("subtitle-descriptors")
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
            }
            val video = async {
                try {
                    val descriptorUrl = origin.resolve("api/DramaList/Episode/$id.png")!!.newBuilder()
                        .addQueryParameter("err", "false").addQueryParameter("ts", "null").addQueryParameter("time", "null")
                        .addQueryParameter("kkey", kisskhRequestKey(id)).build()
                    val response = http.get(descriptorUrl.toString())
                    val descriptor = response.json().asJsonObject
                    descriptor to response.payload("episode-video-descriptor")
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
            }
            subtitles.await() to video.await()
        }
        val (subtitleResult, videoResult) = descriptors
        val subtitles = subtitleResult?.first.orEmpty()
        subtitleResult?.second?.let(payloads::add)
        attributes["subtitleMetadataStatus"] = if (subtitleResult == null) "unavailable" else "available"
        videoResult?.let { (descriptor, payload) ->
            val videoAttributes = descriptor.kAttributes().filterKeys { it !in setOf("Video", "Video_tmp") }
                .mapKeys { "video.${it.key}" }.toMutableMap()
            listOf("Video", "Video_tmp").forEach { key ->
                kisskhPlayableVideoUrl(descriptor.kString(key))?.let { videoAttributes["video.$key"] = it.toString() }
            }
            attributes.putAll(videoAttributes)
            payloads += payload
        }
        attributes["videoMetadataStatus"] = if (videoResult == null) "unavailable" else "available"
        return episode.copy(subtitles = subtitles, attributes = attributes, payloads = payloads)
    }

    override suspend fun taxonomy(): MetadataTaxonomy = taxonomyLock.withLock {
        taxonomyCache?.takeIf { System.currentTimeMillis() - it.first < 1_800_000 }?.let { return@withLock it.second }
        val home = http.get(origin.toString()).document()
        val runtimeUrl = home.select("script[src]").map { it.absUrl("src") }
            .firstOrNull { it.contains("/runtime.") && it.endsWith(".js") }
            ?: throw IllegalStateException("KissKH did not advertise its web runtime.")
        val runtime = http.get(runtimeUrl).text
        val hash = Regex("""780:["]([a-f0-9]+)["]""").find(runtime)?.groupValues?.get(1)
            ?: throw IllegalStateException("KissKH changed its Explore module; taxonomy needs refreshing.")
        val response = http.get(origin.resolve("780.$hash.js")!!.toString())
        val after = response.text.substringAfter("this.chips=", "")
        require(after.startsWith("[[") && "]]" in after) { "KissKH did not return readable filter options." }
        val literal = after.substringBefore("]]") + "]]"
        // Read only the literal chips array. No remote JavaScript is evaluated.
        val json = literal.replace(Regex("""([,{])\s*(name|value|state)\s*:"""), "$1\"$2\":")
            .replace(Regex("""(:\s*)!1"""), "$1false").replace(Regex("""(:\s*)!0"""), "$1true")
        val arrays = com.google.gson.JsonParser.parseString(json).asJsonArray
        require(arrays.size() >= 5) { "KissKH omitted filter groups." }
        val names = listOf("type", "sub", "country", "status", "order")
        val groups = linkedMapOf<String, List<MetadataTerm>>()
        arrays.forEachIndexed { index, group ->
            val name = names.getOrNull(index) ?: "webGroup$index"
            groups[name] = group.asJsonArray.mapNotNull { option ->
                val row = option.takeIf(JsonElement::isJsonObject)?.asJsonObject ?: return@mapNotNull null
                MetadataTerm(row.kString("name"), row.kString("value"), attributes = row.kAttributes())
            }
        }
        groups["type"] = groups.getValue("type").map { it.copy(attributes = it.attributes + ("allowedByAppPolicy" to (it.id == "1").toString())) }
        groups["country"] = groups.getValue("country").map { it.copy(attributes = it.attributes + ("allowedByAppPolicy" to (it.id == "2").toString())) }
        groups["section"] = sections.keys.map { MetadataTerm(it, id = it) }
        val result = MetadataTaxonomy(groups, listOf(metadataPayload("web-explore-options", response.url, arrays)))
        taxonomyCache = System.currentTimeMillis() to result
        result
    }

    private suspend fun koreanIndex(sub: String, status: String, order: String): List<JsonObject> = indexLock.withLock {
        val key = "$sub:$status:$order"
        indexes[key]?.takeIf { System.currentTimeMillis() - it.loaded < 1_800_000 }?.let { return@withLock it.rows }
        val first = http.get(listUrl(1, 500, sub, status, order).toString()).json().asJsonObject
        val total = first.kString("totalCount").toLongOrNull() ?: throw IllegalStateException("KissKH omitted its catalogue count.")
        val pages = ((total + 499) / 500).toInt()
        require(pages in 0..50) { "KissKH catalogue exceeds the bounded search index." }
        val other = coroutineScope { (2..pages).map { page -> async { indexRequests.withPermit {
            http.get(listUrl(page, 500, sub, status, order).toString()).json().asJsonObject.kObjects("data")
        } } }.awaitAll().flatten() }
        val rows = (first.kObjects("data") + other).distinctBy { it.kString("id") }
        indexes[key] = Index(System.currentTimeMillis(), rows)
        while (indexes.size > 8) indexes.remove(indexes.keys.first())
        rows
    }

    private fun listUrl(page: Int, size: Int, sub: String, status: String, order: String): HttpUrl = origin.resolve("api/DramaList/List")!!.newBuilder()
        .addQueryParameter("page", page.toString()).addQueryParameter("pageSize", size.toString())
        .addQueryParameter("type", "1").addQueryParameter("country", "2")
        .addQueryParameter("sub", sub).addQueryParameter("status", status).addQueryParameter("order", order).build()

    private fun record(row: JsonObject): MetadataRecord {
        val id = row.kString("id")
        val title = row.kString("title")
        val url = kisskhDetailUrl(id.toLong(), title).toString()
        val links = buildList {
            row.kString("trailer").takeIf(String::isNotBlank)?.let { add(MetadataLink(it, "trailer")) }
            row.kString("nextEpDateID").toLongOrNull()?.takeIf { it > 0 }?.let {
                add(MetadataLink("https://www.tickcounter.com/widget/countdown/$it", "next-episode-countdown"))
            }
        }
        val date = row.kString("releaseDate").ifBlank { null }
        return MetadataRecord(id, url, title, description = row.kString("description").ifBlank { null },
            images = row.kString("thumbnail").takeIf(String::isNotBlank)?.let { listOf(MetadataImage(it, "thumbnail")) }.orEmpty(),
            countries = row.kString("country").takeIf(String::isNotBlank)?.let(::listOf).orEmpty(),
            status = row.kString("status").ifBlank { null }, format = row.kString("type").ifBlank { null },
            year = date?.take(4)?.toIntOrNull(), episodeCount = row.kString("episodesCount").toIntOrNull(), releaseDate = date,
            externalIds = mapOf("kisskh" to id), links = links, attributes = row.kAttributes(),
            payloads = listOf(metadataPayload("drama-row", url, row)))
    }

    private fun episodeRecord(row: JsonObject, show: MetadataRecord): MetadataEpisode {
        val number = row.kString("number").toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: row.kString("number")
        val id = row.kString("id")
        val url = show.url.toHttpUrl().newBuilder().addPathSegment("Episode-$number").addQueryParameter("ep", id).build().toString()
        val attributes = row.kAttributes().toMutableMap()
        // `sub` is an opaque website hint; retain it and expose only whether it is positive.
        attributes["hasSubtitleHint"] = ((row.get("sub")?.takeIf { it.isJsonPrimitive }?.asString?.toDoubleOrNull() ?: 0.0) > 0).toString()
        return MetadataEpisode(id, number, row.kString("title").ifBlank { "Episode $number" }, url,
            attributes = attributes, payloads = listOf(metadataPayload("episode-row", url, row)))
    }
    private companion object {
        val sections = linkedMapOf("featured" to "Show", "lastUpdate" to "LastUpdate?ispc=false", "upcoming" to "Upcoming?ispc=false",
            "mostSearch" to "MostSearch?ispc=false", "topRating" to "TopRating?ispc=false", "mostViewed" to "MostView?ispc=false&c=2")
    }
}

private fun JsonObject.kString(key: String): String = get(key)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
private fun JsonObject.kObjects(key: String): List<JsonObject> = get(key)?.takeIf(JsonElement::isJsonArray)?.asJsonArray
    ?.mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject }.orEmpty()
private fun JsonObject.kAttributes(): Map<String, String> = entrySet().filter { it.value.isJsonPrimitive }.associate { it.key to it.value.asString }
