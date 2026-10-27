package com.example.animepahedl

import android.webkit.CookieManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class Anime(
    val session: String,
    val title: String,
    val poster: String,
    val year: String,
    val type: String,
    val status: String,
    val synopsis: String = ""
)

data class Episode(val session: String, val number: String, val snapshot: String, val duration: String)
data class AnimeDetails(val anime: Anime, val episodes: List<Episode>)
class VerificationRequired(val url: String, val browserUrl: String = url) : IOException("${url.toHttpUrl().host} requires manual verification.")
class HttpFailure(val url: String, val browserUrl: String, val status: Int, summary: String) :
    IOException("HTTP $status from ${url.toHttpUrl().host}${url.toHttpUrl().encodedPath}" + if (summary.isBlank()) "" else ": $summary")

object AnimeNetwork {
    const val HOST = "https://animepahe.pw"
    // Updated to match WebView when the user opens the verification screen.
    var userAgent = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun loadForRequest(url: HttpUrl): List<Cookie> =
                CookieManager.getInstance().getCookie(url.toString()).orEmpty().split(';')
                    .mapNotNull { Cookie.parse(url, it.trim()) }

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookies.forEach { CookieManager.getInstance().setCookie(url.toString(), it.toString()) }
                CookieManager.getInstance().flush()
            }
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder().header("User-Agent", userAgent)
            if (chain.request().header("Referer") == null) request.header("Referer", "$HOST/")
            chain.proceed(request.build())
        }.build()

    // Media3 uses this client for manifests, AES keys, and HLS segments too.
    val mediaClient: OkHttpClient by lazy {
        client.newBuilder().addNetworkInterceptor { chain ->
            val response = chain.proceed(chain.request())
            val type = response.header("Content-Type").orEmpty().lowercase()
            val inspect = response.code >= 400 || response.header("cf-mitigated") != null ||
                (!type.startsWith("video/") && !type.startsWith("audio/") && !type.contains("octet-stream"))
            if (inspect && SessionChecks.isChallenge(response.peekBody(256 * 1024).string(), response.header("cf-mitigated"))) {
                val url = response.request.url.toString()
                DebugTrace.event("media_challenge_response", "url" to DebugTrace.safeUrl(url), "status" to response.code)
                response.close()
                throw VerificationRequired(url)
            }
            response
        }.build()
    }
}

class AnimeRepository {
    suspend fun checkMedia(url: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("Referer", "https://kwik.cx/").apply {
            if (!url.toHttpUrl().encodedPath.endsWith(".m3u8", true)) header("Range", "bytes=0-1023")
        }.build()
        AnimeNetwork.mediaClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw HttpFailure(url, url, response.code, "Media server rejected the request")
            if (response.header("Content-Type").orEmpty().contains("text/html", true)) {
                throw IOException("The media URL returned a web page, not a video or HLS playlist.")
            }
        }
    }

    private fun request(url: String, referer: String = "${AnimeNetwork.HOST}/", body: String? = null): String {
        val request = Request.Builder().url(url).header("Referer", referer).apply {
            if (body != null) post(body.toRequestBody("application/json".toMediaType()))
        }.build()
        DebugTrace.event("request", "method" to request.method, "url" to DebugTrace.safeUrl(url))
        return AnimeNetwork.client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val finalUrl = response.request.url.toString()
            val challenge = SessionChecks.isChallenge(text, response.header("cf-mitigated"))
            val browserUrl = if (body == null) finalUrl else referer
            val summary = if (response.isSuccessful) "" else Jsoup.parse(text).text().take(350)
            DebugTrace.event("response", "url" to DebugTrace.safeUrl(finalUrl), "status" to response.code,
                "challenge" to challenge, "type" to response.header("Content-Type"), "summary" to summary)
            if (challenge) {
                throw VerificationRequired(finalUrl, browserUrl)
            }
            if (!response.isSuccessful) throw HttpFailure(finalUrl, browserUrl, response.code, summary)
            text
        }
    }

    suspend fun search(query: String): List<Anime> = withContext(Dispatchers.IO) {
        val url = "${AnimeNetwork.HOST}/api".toHttpUrl().newBuilder()
            .addQueryParameter("m", "search").addQueryParameter("q", query).build()
        val result = JsonParser.parseString(request(url.toString())).asJsonObject
        result.getAsJsonArray("data")?.mapNotNull { element ->
            val row = element.asJsonObject
            val session = row.text("session")
            if (session.isBlank()) null else Anime(session, row.text("title"), row.text("poster"),
                row.text("year"), row.text("type"), row.text("status"))
        }.orEmpty()
    }

    suspend fun details(anime: Anime): AnimeDetails = withContext(Dispatchers.IO) {
        val page = "${AnimeNetwork.HOST}/anime/${anime.session}"
        val document = Jsoup.parse(request(page), page)
        val synopsis = document.selectFirst(".anime-synopsis")?.text()
            ?: document.selectFirst("[itemprop=description]")?.text().orEmpty()
        val episodes = mutableListOf<Episode>()
        var current = 1
        do {
            coroutineContext.ensureActive()
            val url = "${AnimeNetwork.HOST}/api".toHttpUrl().newBuilder()
                .addQueryParameter("m", "release").addQueryParameter("id", anime.session)
                .addQueryParameter("sort", "episode_asc").addQueryParameter("page", "$current").build()
            val result = JsonParser.parseString(request(url.toString(), page)).asJsonObject
            val rows = result.getAsJsonArray("data") ?: throw IOException("The site returned an unexpected episode list.")
            rows.forEach { element ->
                val row = element.asJsonObject
                if (row.text("session").isNotBlank()) episodes += Episode(row.text("session"),
                    row.text("episode"), row.text("snapshot"), row.text("duration"))
            }
            val last = result.text("last_page").toIntOrNull()
                ?: throw IOException("The episode pagination could not be read.")
            current++
        } while (current <= last)
        AnimeDetails(anime.copy(synopsis = synopsis), episodes.distinctBy { it.session })
    }

    suspend fun poster(session: String): String = withContext(Dispatchers.IO) {
        val page = "${AnimeNetwork.HOST}/anime/$session"
        val doc = Jsoup.parse(request(page), page)
        doc.selectFirst(".anime-poster img")?.absUrl("src")?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("meta[property=og:image]")?.attr("content").orEmpty()
    }

    suspend fun sources(anime: Anime, episode: Episode): List<EpisodeSource> = withContext(Dispatchers.IO) {
        val page = "${AnimeNetwork.HOST}/play/${anime.session}/${episode.session}"
        parseEpisodeSources(request(page))
    }

    suspend fun playlist(anime: Anime, episode: Episode, quality: String, audio: String): ResolvedMedia = withContext(Dispatchers.IO) {
        val playUrl = "${AnimeNetwork.HOST}/play/${anime.session}/${episode.session}"
        val source = selectEpisodeSource(sources(anime, episode), quality, audio)
        fun resolved(url: String) = ResolvedMedia(url, source.quality, source.audio)
        val kwik = source.url
        if (isMediaPath(kwik.toHttpUrl().encodedPath)) return@withContext resolved(kwik)
        val playerHtml = request(kwik, playUrl)
        findMediaUrl(playerHtml)?.let { return@withContext resolved(it) }
        val player = Jsoup.parse(playerHtml)
        for (script in player.select("script")) {
            coroutineContext.ensureActive()
            if (!script.data().contains("eval")) continue
            // Preserve the desktop project's JavaScript runner protocol.
            val payload = JsonObject().apply {
                addProperty("action", "run")
                add("data", JsonObject().apply {
                    addProperty("image", "glot/javascript:latest")
                    add("payload", JsonObject().apply {
                        addProperty("stdin", "")
                        add("runInstructions", JsonObject().apply {
                            add("buildCommands", com.google.gson.JsonArray())
                            addProperty("runCommand", "node main.js")
                        })
                        add("files", com.google.gson.JsonArray().apply {
                            add(JsonObject().apply { addProperty("name", "main.js"); addProperty("content", script.data()) })
                        })
                    })
                })
            }
            val result = request("https://glot.io/api/mux", "https://glot.io/", payload.toString())
            val json = JsonParser.parseString(result)
            fun strings(value: com.google.gson.JsonElement): List<String> = when {
                value.isJsonObject -> value.asJsonObject.entrySet().flatMap { strings(it.value) }
                value.isJsonArray -> value.asJsonArray.flatMap { strings(it) }
                value.isJsonPrimitive && value.asJsonPrimitive.isString -> listOf(value.asString)
                else -> emptyList()
            }
            val link = strings(json).firstNotNullOfOrNull(::findMediaUrl)
            if (link != null) return@withContext resolved(link)
        }
        throw IOException("The player did not return a downloadable playlist.")
    }
}

private fun String.toHttpUrlOrNullSafe(): HttpUrl? = try { toHttpUrl() } catch (_: IllegalArgumentException) { null }

private fun isMediaPath(path: String): Boolean = listOf(".m3u8", ".mp4", ".webm").any { path.endsWith(it, true) }

internal fun findMediaUrl(text: String): String? {
    val decoded = text.replace("\\/", "/").replace("&amp;", "&")
    // Prefer HLS if both a preview MP4 and the actual HLS source are present.
    return listOf("m3u8", "mp4", "webm").firstNotNullOfOrNull { extension ->
        Regex("https://[^\\s\"'<>\\\\]+\\.$extension(?:\\?[^\\s\"'<>\\\\]*)?")
            .find(decoded)?.value
    }
}

private fun JsonObject.text(key: String): String = get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()

enum class ResultSort(val label: String) { RELEVANCE("Relevance"), TITLE("Title A–Z"), NEWEST("Newest"), OLDEST("Oldest") }

fun filterResults(items: List<Anime>, year: String, type: String, sort: ResultSort): List<Anime> {
    val filtered = items.filter { (year == "All years" || it.year == year) && (type == "All types" || it.type == type) }
    return when (sort) {
        ResultSort.RELEVANCE -> filtered
        ResultSort.TITLE -> filtered.sortedBy { it.title.lowercase() }
        ResultSort.NEWEST -> filtered.sortedByDescending { it.year.toIntOrNull() ?: Int.MIN_VALUE }
        ResultSort.OLDEST -> filtered.sortedBy { it.year.toIntOrNull() ?: Int.MAX_VALUE }
    }
}
