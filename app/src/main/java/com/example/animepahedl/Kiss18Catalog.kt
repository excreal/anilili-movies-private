package com.example.animepahedl

import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import com.example.animepahedl.metadata.Kiss18MetadataApi
import com.example.animepahedl.metadata.MetadataBrowseQuery
import java.io.IOException
import java.io.StringReader
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Uses the same public catalogue snapshot and local search model as Kiss18's web app. */
internal class Kiss18Catalog : SiteCatalog {
    private val metadataApi by lazy { Kiss18MetadataApi() }
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS).callTimeout(60, TimeUnit.SECONDS).build()
    private data class Entry(val item: CatalogItem, val search: String)
    private val lock = Mutex()
    private var cached: List<Entry>? = null
    private var loadedAt = 0L
    private val origin = "https://kiss18.vip/".toHttpUrl()

    override suspend fun home(): CatalogPage = withContext(Dispatchers.IO) {
        val html = fetch(origin.toString())
        val doc = Jsoup.parse(html, origin.toString())
        val items = doc.select("main a[href^=/title/]").mapNotNull { link ->
            val image = link.selectFirst("img")
            val title = link.selectFirst("h2,h3")?.text().orEmpty().ifBlank { image?.attr("alt").orEmpty() }.trim()
            val url = safeUrl(link.attr("href")) ?: return@mapNotNull null
            if (title.isEmpty()) return@mapNotNull null
            CatalogItem(title, url, image?.let(::catalogPosterUrl).orEmpty())
        }.distinctBy { it.url }
        if (items.isEmpty()) throw IOException("Kiss18 did not return its featured stories. Try again later.")
        // The prerendered homepage has only eight cards; the live app's discovery feed uses
        // the complete playable snapshot. Preserve curated order, then append that same feed.
        CatalogPage((items + entries().map { it.item }).distinctBy { it.url })
    }

    override suspend fun search(query: String): CatalogPage = withContext(Dispatchers.IO) {
        val needle = normalize(query)
        if (needle.isBlank()) return@withContext home()
        val terms = needle.split(' ').filter(String::isNotBlank)
        val matches = entries().filter { entry -> terms.all { it in entry.search } }
            .sortedWith(compareBy<Entry> { !normalize(it.item.title).startsWith(needle) }.thenBy { it.item.title.lowercase(Locale.ROOT) })
        CatalogPage(matches.map { it.item })
    }

    override suspend fun filter(kind: String, term: SiteShowTerm): CatalogPage {
        require(kind in setOf("category", "tag", "genre")) { "Kiss18 supports category, tag and genre filters." }
        val url = if (kind == "genre") term.url ?: throw IllegalArgumentException("Kiss18 genre has no public route.") else "https://kiss18.vip/browse"
        return metadataPage(url, if (kind == "genre") emptyMap() else mapOf(kind to term.name))
    }

    /** Kiss18 ships all browse metadata in one snapshot; there is no server pagination. */
    override suspend fun page(url: String): CatalogPage {
        val target = url.toHttpUrl()
        require(target.isHttps && target.host == origin.host && target.username.isEmpty() && target.password.isEmpty()) { "Invalid Kiss18 catalogue URL." }
        return when {
            target.encodedPath.trimEnd('/').matches(Regex("/genres/[a-z0-9]+(?:-[a-z0-9]+)*")) -> metadataPage(url, emptyMap())
            target.encodedPath.trimEnd('/') == "" -> home()
            target.encodedPath.trimEnd('/') == "/search" -> search(target.queryParameter("q").orEmpty())
            target.encodedPath.trimEnd('/') == "/browse" -> metadataPage(url, emptyMap())
            else -> throw IOException("Kiss18 does not provide that catalogue page.")
        }
    }

    private suspend fun metadataPage(url: String, filters: Map<String, String>): CatalogPage {
        val target = url.toHttpUrl().newBuilder().setQueryParameter("pageSize", "48")
        (filters + ("playable" to "true")).forEach { (key, value) -> target.setQueryParameter(key, value) }
        val query = MetadataBrowseQuery(url = target.build().toString())
        val result = metadataApi.browse(query)
        return CatalogPage(result.items.mapNotNull { item -> item.images.firstOrNull { it.role == "poster" }?.url?.let { poster ->
            CatalogItem(item.title, item.url, poster)
        } }, result.nextUrl)
    }

    private suspend fun entries(): List<Entry> = lock.withLock {
        val now = System.nanoTime()
        cached?.takeIf { now - loadedAt < TimeUnit.MINUTES.toNanos(10) }?.let { return@withLock it }
        val script = fetch(origin.resolve("catalogue-browse.mjs")!!.toString())
        val marker = Regex("export\\s+const\\s+snapshot\\s*=").find(script)
            ?: throw IOException("Kiss18 catalogue format changed.")
        val raw = script.substring(marker.range.last + 1).trimStart()
        // JsonReader reads the first JSON object, leaving the JavaScript semicolon untouched.
        val root = JsonReader(StringReader(raw)).use { JsonParser.parseReader(it).asJsonObject }
        val items = root.getAsJsonArray("items") ?: throw IOException("Kiss18 catalogue is unavailable.")
        val entries = items.mapNotNull { value ->
            val item = value.asJsonObject
            fun text(key: String) = item.get(key)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
            val slug = text("public_slug")
            val title = text("title").trim()
            if (slug.isBlank() || title.isBlank()) return@mapNotNull null
            // Hide records without any playable episode, as the site's default browser does.
            if (item.getAsJsonArray("episodes")?.none { it.asJsonObject.get("playable")?.asBoolean == true } != false) return@mapNotNull null
            val url = origin.newBuilder().addPathSegment("title").addPathSegment(slug).build().toString()
            Entry(CatalogItem(title, url, safeImage(text("cover"))), normalize("$title ${text("synopsis")} ${text("category")}"))
        }.distinctBy { it.item.url }
        if (entries.isEmpty()) throw IOException("Kiss18 returned an empty catalogue.")
        cached = entries
        loadedAt = now
        entries
    }

    private fun safeUrl(raw: String): String? = origin.resolve(raw)?.takeIf {
        it.isHttps && it.host == origin.host && it.username.isEmpty() && it.password.isEmpty() && it.encodedPath.startsWith("/title/")
    }?.toString()
    private fun safeImage(raw: String): String = origin.resolve(raw)?.takeIf {
        raw.isNotBlank() && it.isHttps && it.username.isEmpty() && it.password.isEmpty()
    }?.toString().orEmpty()
    private fun normalize(raw: String): String = Normalizer.normalize(raw, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "").replace(Regex("['’‘`´]"), "").lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

    private suspend fun fetch(url: String): String {
        val call = client.newCall(Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build())
        val response = suspendCancellableCoroutine<Response> { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    if (!continuation.isActive) response.close()
                    else continuation.resume(response, onCancellation = { _, value, _ -> value.close() })
                }
            })
        }
        return withContext(Dispatchers.IO) {
            response.use {
                if (!it.isSuccessful) throw IOException("Kiss18 returned HTTP ${it.code}.")
                val body = it.body ?: throw IOException("Kiss18 returned an empty response.")
                val bytes = body.byteStream().readNBytes(8 * 1024 * 1024 + 1)
                if (bytes.size > 8 * 1024 * 1024) throw IOException("Kiss18 catalogue response is too large.")
                bytes.toString(Charsets.UTF_8)
            }
        }
    }
}
