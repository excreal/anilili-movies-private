package com.example.animepahedl.metadata

import com.example.animepahedl.*

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.parser.Parser
import java.io.IOException
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Public data API, separate from the consumer UI's playable-only catalogue. No remote JS executes. */
internal class Kiss18MetadataApi(cookieHeader: ((String) -> String)? = null) : SiteMetadataApi {
    override val site = MetadataSite.KISS18
    private val origin = "https://kiss18.vip/".toHttpUrl()
    private val http = MetadataHttp(setOf("kiss18.vip", "www.kiss18.vip", "cdn.kiss18.vip"), cookieHeader)
    private val lock = Mutex()
    private data class Snapshot(val root: JsonObject, val entries: List<JsonObject>, val presentation: Map<String, JsonObject>,
        val seo: JsonObject, val longPremises: JsonObject, val relatedGroups: List<List<String>>, val featured: List<String>,
        val heroes: List<String>, val arrivals: List<String>, val collections: JsonArray, val continuations: List<List<String>>, val loaded: Long)
    private var cached: Snapshot? = null
    private var cachedTaxonomy: Pair<Long, Map<String, List<MetadataTerm>>>? = null

    override suspend fun browse(query: MetadataBrowseQuery): MetadataPage {
        val supported = setOf("pageSize", "category", "tag", "kind", "playable")
        require(query.filters.keys.all { it in supported }) { "Unsupported Kiss18 metadata filter. Use category, tag, kind, playable or pageSize." }
        val snapshot = snapshot()
        val requested = query.url?.let { checkedPage(it) }
        require(requested == null || requested.encodedPath.trimEnd('/') in setOf("", "/browse", "/search")) { "Expected a Kiss18 browse URL." }
        require(requested == null || requested.queryParameterNames.all { it in supported || it in setOf("q", "page") }) { "Unsupported Kiss18 browse parameter." }
        val filters = requested?.queryParameterNames.orEmpty().filter { it in supported }.associateWith { requested!!.queryParameter(it).orEmpty() } + query.filters
        val page = requested?.queryParameter("page")?.let { it.toIntOrNull() ?: throw IllegalArgumentException("Invalid metadata page.") } ?: query.page
        require(page > 0) { "Metadata page must be positive." }
        val size = filters["pageSize"]?.let { it.toIntOrNull() ?: throw IllegalArgumentException("Invalid metadata page size.") } ?: 48
        require(size in 1..500) { "Metadata page size must be between 1 and 500." }
        require(filters["kind"] == null || filters["kind"] in setOf("series", "clip", "test")) { "Unknown Kiss18 story kind." }
        require(filters["playable"] == null || filters["playable"] in setOf("true", "false")) { "playable must be true or false." }
        val needle = normalized(query.query.ifBlank { requested?.queryParameter("q").orEmpty() })
        val terms = needle.split(' ').filter(String::isNotBlank)
        val entries = snapshot.entries.filter { entry ->
            val id = entry.text("id")
            val editorial = snapshot.presentation[id] ?: JsonObject()
            val seo = snapshot.seo.get(id)?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
            val category = entry.text("category")
            val rawKind = editorial.text("kind").ifBlank { when {
                category == "Test media" -> "test"
                category == "Standalone clip" -> "clip"
                id.startsWith("chartdrama:") || id.startsWith("bunny:series:") || entry.array("episodes").size() > 1 -> "series"
                else -> "clip"
            } }
            val searchable = normalized(listOf(entry.text("title"), editorial.text("title"), seo.text("title"),
                entry.text("synopsis"), editorial.text("premise"), snapshot.longPremises.get(id)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                category, editorial.array("moods").strings().joinToString(" ")).joinToString(" "))
            val playable = entry.array("episodes").any { it.isJsonObject && it.asJsonObject.text("playable") == "true" }
            (terms.isEmpty() || terms.all { it in searchable }) &&
                (filters["category"].isNullOrBlank() || category.equals(filters["category"], true)) &&
                (filters["tag"].isNullOrBlank() || editorial.array("moods").strings().any { it.equals(filters["tag"], true) }) &&
                (filters["kind"].isNullOrBlank() || rawKind == filters["kind"]) &&
                (filters["playable"] == null || playable == filters.getValue("playable").toBoolean())
        }
        val start = (page.toLong() - 1L) * size
        val slice = if (start >= entries.size) emptyList() else entries.drop(start.toInt()).take(size).map { record(it, snapshot) }
        val next = if (start + size < entries.size) origin.resolve("browse")!!.newBuilder()
            .addQueryParameter("page", (page + 1).toString()).addQueryParameter("pageSize", size.toString())
            .apply {
                val term = query.query.ifBlank { requested?.queryParameter("q").orEmpty() }
                if (term.isNotBlank()) addQueryParameter("q", term)
                filters.filterKeys { it != "pageSize" }.forEach { (key, value) -> addQueryParameter(key, value) }
            }.build().toString() else null
        val byId = snapshot.entries.associateBy { it.text("id") }
        fun selected(ids: List<String>) = ids.mapNotNull { byId[it] }.map { record(it, snapshot) }
        val sections = buildList {
            if (page == 1 && needle.isEmpty()) {
                add(MetadataSection("Featured", selected(snapshot.heroes), origin.toString()))
                add(MetadataSection("Popular picks", selected(snapshot.featured), origin.toString()))
                add(MetadataSection("New arrivals", selected(snapshot.arrivals), origin.toString()))
                snapshot.collections.objects().forEach { section ->
                    val ids = section.getAsJsonArray("ids")?.strings().orEmpty().ifEmpty { section.getAsJsonArray("items")?.strings().orEmpty() }
                    if (ids.isNotEmpty()) add(MetadataSection(section.text("title").ifBlank { section.text("name") }, selected(ids)))
                }
            }
        }
        return MetadataPage(slice, next, entries.size.toLong(), sections, taxonomyFrom(snapshot), listOf(
            metadataPayload("snapshotInfo", origin.resolve("catalogue-browse.mjs")!!.toString(), JsonObject().apply {
                add("legacySnapshotSha256", snapshot.root.get("legacySnapshotSha256") ?: JsonNull.INSTANCE)
                addProperty("recordCount", snapshot.entries.size); addProperty("includesUnplayableAndTestRecords", true)
            })))
    }

    override suspend fun details(url: String): MetadataDetails {
        val snapshot = snapshot()
        val entry = find(url, snapshot).first
        val canonical = titleUrl(entry)
        val html = http.get(canonical)
        val web = metadataFromDocument(html.document())
        val item = record(entry, snapshot)
        val relatedIds = (snapshot.relatedGroups + snapshot.continuations).filter { entry.text("id") in it }.flatten().distinct()
        return MetadataDetails(item.copy(images = (item.images + web.images).distinctBy { it.url to it.role },
            links = (item.links + web.links).distinctBy { it.url to it.relation }, payloads = item.payloads + web.payloads),
            entry.array("episodes").objects().map { episode(it, entry) },
            snapshot.entries.filter { it.text("id") in relatedIds && it.text("id") != entry.text("id") }.map { record(it, snapshot) })
    }

    /** Only this episode's public playback chunk and one same-site HLS master are fetched. */
    override suspend fun episode(url: String): MetadataEpisode {
        val snapshot = snapshot()
        val (entry, number) = find(url, snapshot)
        val selected = entry.array("episodes").objects().firstOrNull { it.integer("ordinal") == (number ?: 1) }
            ?: throw IOException("This Kiss18 episode is not listed.")
        var result = episode(selected, entry)
        val chunk = selected.text("playbackChunk")
        if (chunk.matches(Regex("[a-fA-F0-9]{64}"))) {
            val response = http.get(origin.resolve("playback/$chunk.json")!!.toString())
            val source = response.json().asJsonObject.getAsJsonObject("playback") ?: JsonObject()
            val matching = JsonObject().apply { source.entrySet().forEach { (key, value) ->
                if (value.isJsonObject && value.asJsonObject.text("episodeId") == selected.text("id")) add(key, value)
            } }
            result = result.copy(payloads = result.payloads + metadataPayload("playbackCandidates", response.url, matching),
                attributes = result.attributes + ("playbackCandidateCount" to matching.size().toString()))
            val subtitles = mutableListOf<MetadataSubtitle>()
            val manifests = mutableListOf<MetadataPayload>()
            // Existing playback adapter handles resolver/gateway/non-Kiss18 sources. Metadata never
            // follows an arbitrary candidate URL or consumes video segments merely to inspect data.
            val direct = matching.entrySet().mapNotNull { (_, value) -> value.takeIf { it.isJsonObject }?.asJsonObject }
                .firstOrNull { it.text("format") == "hls" && it.text("url").let { raw ->
                    runCatching { raw.toHttpUrl().let { it.isHttps && it.host in setOf("kiss18.vip", "cdn.kiss18.vip") } }.getOrDefault(false)
                } }
            if (direct != null) {
                try {
                    val manifest = http.get(direct.text("url"), 256 * 1024)
                    if (manifest.text.startsWith("#EXTM3U")) {
                        val renditions = JsonArray()
                        val lines = manifest.text.lines()
                        lines.forEachIndexed { index, line ->
                            if (line.startsWith("#EXT-X-MEDIA:") || line.startsWith("#EXT-X-STREAM-INF:")) {
                                val values = hlsAttributes(line.substringAfter(':'))
                                val rendition = JsonObject().apply { values.forEach { (key, value) -> addProperty(key, value) } }
                                val locator = values["URI"] ?: if (line.startsWith("#EXT-X-STREAM-INF:")) lines.drop(index + 1).firstOrNull { it.isNotBlank() && !it.startsWith('#') } else null
                                locator?.let { manifest.url.toHttpUrl().resolve(it) }?.let { rendition.addProperty("url", it.toString()) }
                                renditions.add(rendition)
                                if (values["TYPE"] == "SUBTITLES" && values["URI"] != null) {
                                    manifest.url.toHttpUrl().resolve(values.getValue("URI"))?.let { sub ->
                                        subtitles += MetadataSubtitle(sub.toString(), values["NAME"] ?: values["LANGUAGE"].orEmpty(),
                                            values["LANGUAGE"], "hls", values["DEFAULT"] == "YES", values)
                                    }
                                }
                            }
                        }
                        manifests += metadataPayload("hlsRenditions", manifest.url, renditions)
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { result = result.copy(attributes = result.attributes + ("manifestMetadataStatus" to "unavailable")) }
            }
            result = result.copy(subtitles = (result.subtitles + subtitles).distinctBy { it.url }, payloads = result.payloads + manifests)
        }
        return result
    }

    override suspend fun taxonomy(): MetadataTaxonomy = MetadataTaxonomy(taxonomyFrom(snapshot()))

    private fun taxonomyFrom(snapshot: Snapshot): Map<String, List<MetadataTerm>> = synchronized(this) {
        cachedTaxonomy?.takeIf { it.first == snapshot.loaded }?.let { return@synchronized it.second }
        val categories = mutableMapOf<String, Int>()
        val tags = mutableMapOf<String, Int>()
        val kinds = mutableMapOf<String, Int>()
        snapshot.entries.forEach { entry ->
            entry.text("category").takeIf(String::isNotBlank)?.let { categories[it] = (categories[it] ?: 0) + 1 }
            val editorial = snapshot.presentation[entry.text("id")] ?: JsonObject()
            editorial.array("moods").strings().forEach { tags[it] = (tags[it] ?: 0) + 1 }
            val kind = editorial.text("kind").ifBlank { when {
                entry.text("category") == "Test media" -> "test"
                entry.text("category") == "Standalone clip" -> "clip"
                entry.text("id").startsWith("chartdrama:") || entry.text("id").startsWith("bunny:series:") || entry.array("episodes").size() > 1 -> "series"
                else -> "clip"
            } }
            kinds[kind] = (kinds[kind] ?: 0) + 1
        }
        fun terms(counts: Map<String, Int>) = counts.map { (name, count) -> MetadataTerm(name, count = count.toLong()) }.sortedBy { it.name }
        val result = mapOf("categories" to terms(categories), "tags" to terms(tags), "kinds" to terms(kinds))
        cachedTaxonomy = snapshot.loaded to result
        result
    }

    private fun record(entry: JsonObject, snapshot: Snapshot): MetadataRecord {
        val id = entry.text("id"); val editorial = snapshot.presentation[id] ?: JsonObject()
        val seo = snapshot.seo.get(id)?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
        val title = clean(editorial.text("title").ifBlank { seo.text("title") }.ifBlank { entry.text("title") })
        val rawCategory = entry.text("category")
        val episodes = entry.array("episodes").objects()
        val images = mutableListOf<MetadataImage>()
        fun image(raw: String, role: String, width: Int? = null, height: Int? = null, caption: String? = null) {
            imageUrl(raw)?.let { images += MetadataImage(it, role, width, height, caption = caption) }
        }
        image(entry.text("cover"), "poster")
        entry.array("cover_variants").objects().forEach { image(it.text("cover"), "poster", it.integer("width"), it.integer("height")) }
        entry.obj("artwork_variants")?.entrySet()?.forEach { (raw, variants) ->
            image(raw, "alternate_artwork")
            variants.takeIf { it.isJsonArray }?.asJsonArray?.objects()?.forEach { image(it.text("cover"), "alternate_artwork", it.integer("width"), it.integer("height"), raw) }
        }
        image(editorial.text("artwork"), "featured_artwork")
        val category = rawCategory.takeIf { it.isNotBlank() }?.let { MetadataTerm(it, attributes = mapOf("sourceField" to "category")) }
        val kind = editorial.text("kind").ifBlank { when {
            rawCategory == "Test media" -> "test"
            rawCategory == "Standalone clip" -> "clip"
            id.startsWith("chartdrama:") || id.startsWith("bunny:series:") || episodes.size > 1 -> "series"
            else -> "clip"
        } }
        val synopsis = snapshot.longPremises.get(id)?.takeIf { it.isJsonPrimitive }?.asString
            ?: editorial.text("premise").ifBlank { seo.text("premise") }.ifBlank { entry.text("synopsis") }
        val attributes = linkedMapOf("inventoryComplete" to entry.text("inventory_complete"), "playableEpisodeCount" to episodes.count { it.text("playable") == "true" }.toString(),
            "sourceCategory" to rawCategory, "kindDerivation" to "website editorial.storyKind", "rawTitle" to entry.text("title"))
        listOf("rank", "finalEpisode").forEach { key -> editorial.get(key)?.let { attributes[key] = it.toString() } }
        val links = entry.array("public_slug_aliases").strings().map { MetadataLink(origin.resolve("title/$it")!!.toString(), "alias") }.toMutableList()
        editorial.obj("heroPreview")?.text("asset")?.takeIf { it.isNotBlank() }?.let { imageUrl(it) }?.let { links += MetadataLink(it, "video_preview") }
        val continuationIds = snapshot.continuations.filter { id in it }.flatten().filter { it != id }.toSet()
        snapshot.entries.filter { it.text("id") in continuationIds }.forEach { links += MetadataLink(titleUrl(it), "continuation") }
        if (id.startsWith("bunny:series:")) snapshot.entries.firstOrNull { it.text("id").startsWith("chartdrama:shortpeak:") && it.text("title").trim().equals(entry.text("title").trim(), true) }
            ?.let { links += MetadataLink(titleUrl(it), "website_canonical") }
        val publicPresentation = JsonObject().apply {
            listOf("title", "premise", "artwork", "moods", "rank", "finalEpisode", "kind", "heroPreview").forEach { key -> editorial.get(key)?.let { add(key, it) } }
            snapshot.longPremises.get(id)?.let { add("detailPremise", it) }
            if (seo.size() > 0) add("seo", seo)
        }
        return MetadataRecord(id, titleUrl(entry), title, listOf(entry.text("title"), seo.text("title")).filter { it.isNotBlank() && it != title }.distinct(),
            clean(synopsis).takeIf { it.isNotBlank() }, images.distinctBy { Triple(it.url, it.role, it.width) },
            categories = listOfNotNull(category), tags = editorial.array("moods").strings().map { MetadataTerm(it, attributes = mapOf("sourceField" to "editorial.moods")) },
            format = kind, episodeCount = episodes.size, attributes = attributes, links = links,
            payloads = listOf(metadataPayload("catalogueRecord", origin.resolve("catalogue-browse.mjs")!!.toString(), entry),
                metadataPayload("publicPresentation", origin.resolve("editorial.mjs")!!.toString(), publicPresentation)))
    }

    private fun episode(value: JsonObject, entry: JsonObject): MetadataEpisode {
        val number = value.integer("ordinal") ?: 0
        val video = value.obj("video") ?: JsonObject()
        val images = imageUrl(video.text("thumbnailUrl"))?.let { listOf(MetadataImage(it, "episode_thumbnail")) }.orEmpty()
        return MetadataEpisode(value.text("id"), number.toString(), value.text("title"), origin.resolve("watch/${entry.text("public_slug")}/$number")!!.toString(),
            images = images, durationSeconds = video.get("duration")?.takeIf { it.isJsonPrimitive }?.asString?.toDoubleOrNull()?.toLong(),
            releaseDate = video.text("uploadDate").takeIf { it.isNotBlank() }, attributes = mapOf("playable" to value.text("playable"),
                "sourceCandidateCount" to value.array("sources").size().toString(), "playbackChunk" to value.text("playbackChunk")),
            payloads = listOf(metadataPayload("catalogueEpisode", origin.resolve("catalogue-browse.mjs")!!.toString(), value)))
    }

    private suspend fun snapshot(): Snapshot = lock.withLock {
        val now = System.nanoTime()
        cached?.takeIf { now - it.loaded < TimeUnit.MINUTES.toNanos(10) }?.let { return@withLock it }
        val (module, presentationResponse, catalogueRulesResponse) = coroutineScope {
            val catalogue = async { http.get(origin.resolve("catalogue-browse.mjs")!!.toString(), 16 * 1024 * 1024) }
            val editorial = async { http.get(origin.resolve("editorial.mjs")!!.toString(), 1024 * 1024) }
            val rules = async { http.get(origin.resolve("catalogue.mjs")!!.toString(), 256 * 1024) }
            Triple(catalogue.await(), editorial.await(), rules.await())
        }
        val root = literalAfter(module.text, Regex("export\\s+const\\s+snapshot\\s*="))?.asJsonObject
            ?: throw IOException("Kiss18 public catalogue format changed.")
        val presentation = presentationResponse.text
        val editorial = literalAfter(presentation, Regex("export\\s+const\\s+editorial\\s*="))?.asJsonObject ?: JsonObject()
        Regex("editorial\\[([\"'])(.*?)\\1]\\s*=").findAll(presentation).forEach { match ->
            val fields = runCatching { Kiss18Literal(presentation, match.range.last + 1).read() }.getOrNull()?.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
            val target = editorial.get(match.groupValues[2])?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject().also { editorial.add(match.groupValues[2], it) }
            fields.entrySet().forEach { (key, value) -> target.add(key, value) }
        }
        // Only public display fields are used from the site's final presentation overlay. Its
        // admin/owner configuration is never returned as a payload or exposed as an API operation.
        val overlay = literalAfter(presentation, Regex("const\\s+adminDraft\\s*="))?.asJsonObject ?: JsonObject()
        overlay.getAsJsonObject("titles")?.entrySet()?.forEach { (id, raw) ->
            if (raw.isJsonObject) {
                val target = editorial.get(id)?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject().also { editorial.add(id, it) }
                raw.asJsonObject.entrySet().filter { it.key in setOf("title", "premise", "artwork", "moods", "heroPreview", "kind", "finalEpisode") }.forEach { (key, value) -> target.add(key, value) }
            }
        }
        val longPremises = literalAfter(presentation, Regex("const\\s+detailPremises\\s*="))?.asJsonObject ?: JsonObject()
        Regex("Object\\.assign\\(detailPremises\\s*,").findAll(presentation).forEach { match ->
            literalAt(presentation, match.range.last + 1)?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()?.forEach { (key, value) -> longPremises.add(key, value) }
        }
        val continuations = literalAfter(catalogueRulesResponse.text, Regex("const\\s+continuations\\s*="))?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asJsonArray.strings() }.orEmpty()
        Snapshot(root, root.array("items").objects(), editorial.entrySet().filter { it.value.isJsonObject }.associate { it.key to it.value.asJsonObject },
            literalAfter(presentation, Regex("export\\s+const\\s+seoEditorial\\s*="))?.asJsonObject ?: JsonObject(), longPremises,
            literalAfter(presentation, Regex("const\\s+otherPartGroups\\s*="))?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asJsonArray.strings() }.orEmpty(),
            overlay.array("featured").strings(), overlay.array("heroes").strings(),
            literalAfter(presentation, Regex("const\\s+arrivals\\s*="))?.takeIf { it.isJsonArray }?.asJsonArray?.strings().orEmpty(), overlay.array("sections"), continuations, now).also { cached = it }
    }

    private fun find(raw: String, snapshot: Snapshot): Pair<JsonObject, Int?> {
        val url = checkedPage(raw)
        val parts = url.pathSegments.filter { it.isNotBlank() }
        require((parts.firstOrNull() == "title" && parts.size == 2) || (parts.firstOrNull() == "watch" && parts.size == 3)) { "Expected a Kiss18 title or episode URL." }
        val entry = snapshot.entries.firstOrNull { it.text("public_slug") == parts[1] || parts[1] in it.array("public_slug_aliases").strings() }
            ?: throw IOException("Kiss18 title is absent from its current public catalogue.")
        val number = parts.getOrNull(2)?.toIntOrNull()
        require(parts.size < 3 || number != null && number > 0) { "Invalid Kiss18 episode number." }
        return entry to number
    }
    private fun checkedPage(raw: String) = http.checked(raw).also { require(it.host in setOf("kiss18.vip", "www.kiss18.vip")) }
    private fun titleUrl(entry: JsonObject) = origin.resolve("title/${entry.text("public_slug")}")!!.toString()
    private fun imageUrl(raw: String): String? = raw.takeIf { it.isNotBlank() }?.let { origin.resolve(it) }?.takeIf { it.isHttps && it.username.isEmpty() && it.password.isEmpty() }?.toString()
}

private fun JsonObject.text(key: String) = get(key)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
private fun JsonObject.obj(key: String) = get(key)?.takeIf { it.isJsonObject }?.asJsonObject
private fun JsonObject.integer(key: String) = text(key).toIntOrNull()
private fun JsonObject.array(key: String) = get(key)?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
private fun JsonArray.objects() = mapNotNull { it.takeIf { value -> value.isJsonObject }?.asJsonObject }
private fun JsonArray.strings() = mapNotNull { it.takeIf { value -> value.isJsonPrimitive }?.asString }
private fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKD).replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
private fun clean(value: String) = Parser.unescapeEntities(value, false).replace("â\u0080\u0099", "’").replace(Regex("[\\u0080-\\u009f\\u200b]"), "").replace(Regex("\\s+"), " ").trim()
private fun hlsAttributes(raw: String): Map<String, String> = Regex("([A-Z0-9-]+)=(?:\"([^\"]*)\"|([^,]*))").findAll(raw).associate { it.groupValues[1] to it.groupValues[2].ifBlank { it.groupValues[3] } }
private fun literalAt(source: String, offset: Int): JsonElement? = runCatching { Kiss18Literal(source, offset).read() }.getOrNull()
private fun literalAfter(source: String, marker: Regex): JsonElement? = marker.find(source)?.let { literalAt(source, it.range.last + 1) }

/** A small data-literal reader, not a JavaScript interpreter: objects/arrays/scalars only. */
private class Kiss18Literal(private val source: String, private var cursor: Int) {
    fun read(): JsonElement {
        space()
        return when (source.getOrNull(cursor)) {
            '{' -> obj()
            '[' -> array()
            '\'', '"' -> JsonPrimitive(string())
            else -> {
                if (source.startsWith("cleanText(", cursor)) { cursor += 10; val value = read(); space(); require(source[cursor++] == ')'); value }
                else {
                    val start = cursor
                    while (source.getOrNull(cursor)?.let { it.isLetterOrDigit() || it in ".+-_" } == true) cursor++
                    require(cursor > start)
                    when (val token = source.substring(start, cursor)) {
                        "true" -> JsonPrimitive(true)
                        "false" -> JsonPrimitive(false)
                        "null" -> JsonNull.INSTANCE
                        else -> JsonPrimitive(token.toBigDecimal())
                    }
                }
            }
        }
    }
    private fun obj(): JsonObject {
        cursor++; val result = JsonObject(); space()
        while (source.getOrNull(cursor) != '}') {
            if (source.startsWith("...", cursor)) {
                // Ignore an existing-object spread; the caller merges prior fields explicitly.
                while (cursor < source.length && source[cursor] != ',' && source[cursor] != '}') cursor++
                require(cursor < source.length) { "Unterminated Kiss18 object spread." }
            } else {
                val key = if (source.getOrNull(cursor) in listOf('\'', '"')) string() else {
                    val start = cursor; while (source.getOrNull(cursor)?.let { it.isLetterOrDigit() || it in "_$" } == true) cursor++
                    require(cursor > start); source.substring(start, cursor)
                }
                space(); require(source[cursor++] == ':'); result.add(key, read())
            }
            space(); if (source.getOrNull(cursor) == ',') { cursor++; space() } else break
        }
        require(source.getOrNull(cursor) == '}'); cursor++; return result
    }
    private fun array(): JsonArray {
        cursor++; val result = JsonArray(); space()
        while (source.getOrNull(cursor) != ']') {
            result.add(read()); space(); if (source.getOrNull(cursor) == ',') { cursor++; space() } else break
        }
        require(source.getOrNull(cursor) == ']'); cursor++; return result
    }
    private fun string(): String {
        val quote = source[cursor++]; val result = StringBuilder()
        while (cursor < source.length) {
            val ch = source[cursor++]; if (ch == quote) return result.toString()
            if (ch != '\\') result.append(ch) else {
                val next = source[cursor++]
                result.append(when (next) {
                    'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'; 'b' -> '\b'; 'f' -> '\u000c'
                    'u' -> source.substring(cursor, cursor + 4).toInt(16).toChar().also { cursor += 4 }
                    'x' -> source.substring(cursor, cursor + 2).toInt(16).toChar().also { cursor += 2 }
                    else -> next
                })
            }
        }
        throw IOException("Unterminated Kiss18 data string.")
    }
    private fun space() { while (source.getOrNull(cursor)?.isWhitespace() == true) cursor++ }
}
