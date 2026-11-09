package com.example.animepahedl

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Requests follow the public KissKH web client; country=2/type=1 is its Korean-TV filter. */
internal class KisskhApi(
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).callTimeout(40, TimeUnit.SECONDS).build(),
    private val cookies: ((String) -> String)? = null,
) {
    private val details = linkedMapOf<Long, Pair<Long, JsonObject>>()
    suspend fun json(url: HttpUrl): com.google.gson.JsonElement {
        require(url.isHttps && url.host == "kisskh.do" && url.encodedPath.startsWith("/api/"))
        val request = Request.Builder().url(url).header("Referer", "https://kisskh.do/")
            .header("User-Agent", AnimeNetwork.userAgent).header("Accept", "application/json")
        cookies?.invoke(url.toString())?.takeIf(String::isNotBlank)?.let { request.header("Cookie", it) }
        return client.newCall(request.build()).consumeCancellable { response ->
            if (!response.isSuccessful) throw IOException("KissKH returned ${response.code}. Please try again.")
            val data = response.body.byteStream().use { it.readNBytes(5_000_001) }
            require(data.size <= 5_000_000) { "KissKH response is too large." }
            runCatching { JsonParser.parseString(data.toString(Charsets.UTF_8)) }
                .getOrElse { throw IOException("KissKH did not return its catalog. Please try again.") }
        }
    }
    suspend fun list(page: Int, size: Int = 48): JsonObject = json("https://kisskh.do/api/DramaList/List".toHttpUrl().newBuilder()
        .addQueryParameter("page", page.toString()).addQueryParameter("pageSize", size.toString())
        .addQueryParameter("type", "1").addQueryParameter("country", "2")
        .addQueryParameter("sub", "0").addQueryParameter("status", "0").addQueryParameter("order", "1").build()).asJsonObject
    suspend fun detail(id: Long): JsonObject {
        require(id > 0)
        synchronized(details) { details[id]?.takeIf { System.currentTimeMillis() - it.first < 900_000 }?.let { return it.second } }
        val result = json("https://kisskh.do/api/DramaList/Drama/$id?isq=false".toHttpUrl()).asJsonObject
        require(result.dramaString("country") in setOf("South Korea", "Korea") && result.dramaString("type") == "TVSeries") {
            "KissKH is limited to Korean dramas."
        }
        synchronized(details) {
            details[id] = System.currentTimeMillis() to result
            while (details.size > 20) details.remove(details.keys.first())
        }
        return result
    }
    suspend fun series(url: HttpUrl): SiteDownloadSeries {
        val id = url.queryParameter("id")?.toLongOrNull()?.takeIf { it > 0 }
            ?: throw IOException("Open a KissKH drama link with a series ID.")
        val data = detail(id)
        val title = data.dramaString("title")
        val base = kisskhDetailUrl(id, title)
        val episodes = data.dramaArray("episodes").mapNotNull { it.takeIf { it.isJsonObject }?.asJsonObject }
            .sortedBy { it.dramaString("number").toDoubleOrNull() ?: Double.MAX_VALUE }
            .mapIndexed { index, episode ->
                val episodeId = episode.dramaString("id").toLongOrNull()?.takeIf { it > 0 }
                    ?: throw IOException("KissKH episode has no ID.")
                val number = episode.dramaString("number").toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString()
                    ?: (index + 1).toString()
                val locator = base.newBuilder().addPathSegment("Episode-$number").addQueryParameter("ep", episodeId.toString()).build()
                SiteDownloadEpisode(episodeId.toString(), index + 1, "", locator.toString(), number)
            }
        require(episodes.isNotEmpty()) { "This drama has no available episodes yet." }
        return SiteDownloadSeries(SiteDownloadProvider.KISSKH, id.toString(), title, data.dramaString("thumbnail"),
            base.toString(), episodes, description = data.dramaString("description"))
    }
    suspend fun subtitles(episodeId: String): List<MediaSubtitle> {
        val target = "https://kisskh.do/api/Sub/$episodeId".toHttpUrl().newBuilder()
            .addQueryParameter("kkey", kisskhRequestKey(episodeId, "VgV52sWhwvBSf8BsM3BRY9weWiiCbtGp")).build()
        val tracks = json(target).asJsonArray.mapNotNull { element ->
            val item = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val url = runCatching { item.dramaString("src").toHttpUrl() }.getOrNull()?.takeIf { it.isHttps } ?: return@mapNotNull null
            MediaSubtitle(url.toString(), item.dramaString("label"), item.dramaString("land").ifBlank { "und" },
                isDefault = item.dramaString("default").equals("true", true))
        }.distinctBy { it.url }.sortedByDescending { it.isDefault }
        return bestEffortKisskhSubtitleLocalization(tracks) { ProviderSubtitles.localize(client, it) }
            .also { runCatching { ProviderSubtitles.trim() } }
    }
    suspend fun variants(url: HttpUrl): List<DramaVariant> {
        val show = series(url) // Also enforces the Korean-only rule for pasted episode links.
        val episodeId = url.queryParameter("ep") ?: show.episodes.first().id
        require(show.episodes.any { it.id == episodeId }) { "This episode does not belong to this Korean drama." }
        val target = "https://kisskh.do/api/DramaList/Episode/$episodeId.png".toHttpUrl().newBuilder()
            .addQueryParameter("err", "false").addQueryParameter("ts", "null").addQueryParameter("time", "null")
            .addQueryParameter("kkey", kisskhRequestKey(episodeId)).build()
        val body = json(target).asJsonObject
        val media = body.dramaString("Video").takeIf { it.isNotBlank() } ?: body.dramaString("Video_tmp")
        val parsed = runCatching { media.toHttpUrl() }.getOrNull()
        require(parsed != null && parsed.isHttps && (isDramaHls(parsed) || isDramaFile(parsed))) {
            "This KissKH episode has no playable video. Try another episode."
        }
        val captions = try { subtitles(episodeId) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { emptyList() }
        return listOf(DramaVariant(media, subtitles = captions, headers = mapOf("Referer" to "https://kisskh.do/"),
            mimeType = if (isDramaHls(parsed)) "application/x-mpegURL" else "video/mp4"))
    }
}

/** A broken or unavailable caption must not prevent video playback or discard other tracks. */
internal suspend fun bestEffortKisskhSubtitleLocalization(
    tracks: List<MediaSubtitle>,
    localize: suspend (MediaSubtitle) -> MediaSubtitle,
): List<MediaSubtitle> = coroutineScope {
    val permits = Semaphore(3)
    tracks.map { track ->
        async(Dispatchers.IO) {
            permits.withPermit {
                try { localize(track) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { track }
            }
        }
    }.awaitAll()
}

internal fun kisskhDetailUrl(id: Long, title: String): HttpUrl = "https://kisskh.do/".toHttpUrl().newBuilder()
    .addPathSegment("Drama").addPathSegment(title.replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-').ifBlank { "Drama" })
    .addQueryParameter("id", id.toString()).build()

/** AES-CBC request token used by kisskh.do/common.js (public web-client protocol, not a media key). */
internal fun kisskhRequestKey(episodeId: String, guid: String = "62f176f3bb1b5b8e70e39932ad34a0c7"): String {
    require(episodeId.toLongOrNull()?.let { it > 0 } == true)
    val parts = mutableListOf("", episodeId, "", "mg3c3b04ba", "2.8.10", guid, "4830201")
    repeat(6) { parts.add("kisskh") }; parts.add("00"); parts.add("")
    var hash = 0L
    parts.joinToString("|").forEach { hash = (hash.toInt() shl 5).toLong() - hash + it.code }
    parts.add(1, hash.toString())
    fun hex(value: String) = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(hex("4f6bdaa39e2f8cb07f5e722d9edef314"), "AES"),
        IvParameterSpec(hex("01504af356e619cf2e42bba68c3f70f9")))
    return cipher.doFinal(parts.joinToString("|").toByteArray(Charsets.ISO_8859_1)).joinToString("") { "%02X".format(it) }
}

internal class KisskhCatalog(cacheDirectory: File, cookies: ((String) -> String)? = null) : SiteCatalog {
    private val api = KisskhApi(cookies = cookies)
    private val file = File(cacheDirectory, "kisskh-korean-tv-index.json")
    private val mutex = Mutex()
    private val indexFetch = Semaphore(4)
    private var index: List<CatalogItem>? = null
    private var indexTime = 0L
    private fun items(data: JsonObject): List<CatalogItem> = data.dramaArray("data").mapNotNull {
        val row = it.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
        val id = row.dramaString("id").toLongOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        CatalogItem(row.dramaString("title"), kisskhDetailUrl(id, row.dramaString("title")).toString(), row.dramaString("thumbnail"))
    }
    override suspend fun home(): CatalogPage = catalogPage(1)
    override suspend fun search(query: String): CatalogPage = searchPage(query, 0)
    override suspend fun page(url: String): CatalogPage {
        val parsed = url.toHttpUrl()
        require(parsed.host == "kisskh.do" && parsed.isHttps)
        val query = parsed.queryParameter("_search")
        return if (query != null) searchPage(query, parsed.queryParameter("offset")?.toIntOrNull()?.coerceAtLeast(0) ?: 0)
        else catalogPage(parsed.queryParameter("page")?.toIntOrNull()?.coerceAtLeast(1) ?: 1)
    }
    private suspend fun catalogPage(page: Int): CatalogPage = withContext(Dispatchers.IO) {
        val data = api.list(page)
        CatalogPage(items(data), if (page.toLong() * 48 < data.dramaInt("totalCount"))
            "https://kisskh.do/api/DramaList/List?page=${page + 1}&type=1&country=2" else null)
    }
    private suspend fun koreanIndex(): List<CatalogItem> = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (System.currentTimeMillis() - indexTime < 1_800_000) index?.let { return@withContext it }
            if (file.exists() && System.currentTimeMillis() - file.lastModified() < 1_800_000 && file.length() < 8_000_000) {
                runCatching { com.google.gson.Gson().fromJson(file.readText(), Array<CatalogItem>::class.java).toList() }.getOrNull()?.let {
                    index = it; indexTime = file.lastModified(); return@withContext it
                }
            }
            val first = api.list(1, 500)
            val pages = (first.dramaInt("totalCount") + 499) / 500
            require(pages <= 50) { "KissKH search is temporarily unavailable. Browse its catalog instead." }
            val rest = coroutineScope { (2..pages).map { page -> async { indexFetch.withPermit { items(api.list(page, 500)) } } }.awaitAll().flatten() }
            val result = (items(first) + rest).distinctBy { it.url }
            index = result; indexTime = System.currentTimeMillis()
            runCatching {
                val temporary = File(file.path + ".tmp")
                temporary.writeText(com.google.gson.Gson().toJson(result)); temporary.renameTo(file)
            }
            result
        }
    }
    private suspend fun searchPage(query: String, offset: Int): CatalogPage {
        // The website's search endpoint ignores country. Search only the filtered Korean index.
        val matches = koreanIndex().filter { it.title.contains(query.trim(), ignoreCase = true) }
        val selected = matches.drop(offset).take(48)
        val next = if (offset + selected.size < matches.size) "https://kisskh.do/api/DramaList/List".toHttpUrl().newBuilder()
            .addQueryParameter("_search", query).addQueryParameter("offset", (offset + selected.size).toString()).build().toString() else null
        return CatalogPage(selected, next)
    }
}
