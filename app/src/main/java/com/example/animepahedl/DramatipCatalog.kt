package com.example.animepahedl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Dramatip's server-rendered popular listing and paginated title search. */
internal class DramatipCatalog(
    private val cookieHeader: (String) -> String = { "" },
    client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS).build()
) : SiteCatalog {
    // Preserve response/session cookies for this adapter's lifetime, including redirect hops.
    private val sessionCookies = DramatipSessionCookies()
    private val sessionClient = client.newBuilder().cookieJar(sessionCookies)
        .addNetworkInterceptor { chain ->
            val request = chain.request()
            checkedUrl(request.url.toString())
            val imported = cookieHeader(request.url.toString()).split(';').map(String::trim)
                .filter { it.contains('=') }
            val importedNames = imported.map { it.substringBefore('=') }.toSet()
            val received = sessionCookies.loadForRequest(request.url)
                .filterNot { it.name in importedNames }.map { "${it.name}=${it.value}" }
            val builder = request.newBuilder().removeHeader("Cookie")
            (received + imported).joinToString("; ").takeIf(String::isNotBlank)?.let { builder.header("Cookie", it) }
            chain.proceed(builder.build())
        }.build()

    override suspend fun home(): CatalogPage = page("https://dramatip.net/category/popular")

    override suspend fun search(query: String): CatalogPage {
        val term = query.trim()
        if (term.isEmpty()) return home()
        return page("https://dramatip.net/search".toHttpUrl().newBuilder().addQueryParameter("q", term).build().toString())
    }

    override suspend fun page(url: String): CatalogPage = withContext(Dispatchers.IO) {
        val target = checkedUrl(url)
        val request = Request.Builder().url(target)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://dramatip.net/")
            .header("Upgrade-Insecure-Requests", "1")
        sessionClient.newCall(request.build()).consumeCancellable { response ->
            if (!response.isSuccessful) throw IOException("Dramatip returned HTTP ${response.code}. Try again or import session cookies.")
            checkedUrl(response.request.url.toString())
            val body = response.body ?: throw IOException("Dramatip returned an empty response.")
            val bytes = body.byteStream().use { it.readNBytes(4_000_001) }
            if (bytes.size > 4_000_000) throw IOException("Dramatip listing is too large.")
            parseDramatipCatalog(bytes.toString(Charsets.UTF_8), response.request.url.toString())
        }
    }

    private fun checkedUrl(raw: String) = raw.toHttpUrl().also {
        require(it.scheme == "https" && it.host == "dramatip.net" && it.username.isEmpty() && it.password.isEmpty() &&
            (it.encodedPath == "/" || it.encodedPath == "/search" || it.encodedPath.startsWith("/category/"))) {
            "Invalid Dramatip catalog URL."
        }
    }
}

internal fun parseDramatipCatalog(html: String, pageUrl: String): CatalogPage {
    val doc = Jsoup.parse(html, pageUrl)
    val cards = doc.select("a[href]").mapNotNull { anchor ->
        val url = runCatching { anchor.absUrl("href").toHttpUrl() }.getOrNull() ?: return@mapNotNull null
        if (url.host != "dramatip.net" || !Regex("^/(?:[a-z]{2}/)?series/[^/]+/?$").matches(url.encodedPath)) return@mapNotNull null
        val image = anchor.select("img").firstOrNull { it.attr("aria-hidden") != "true" && !it.attr("src").contains("/flags/") } ?: return@mapNotNull null
        val title = image.attr("alt").trim().ifBlank {
            anchor.selectFirst("h2,h3")?.text().orEmpty().trim()
        }
        if (title.isBlank()) return@mapNotNull null
        val poster = catalogPosterUrl(image)
        CatalogItem(title, url.newBuilder().query(null).fragment(null).build().toString(), poster)
    }.distinctBy { it.url }
    if (cards.isEmpty() && (doc.title().contains("Just a moment", true) || doc.select("#challenge-running, #cf-challenge-running").isNotEmpty()))
        throw IOException("Dramatip requires browser verification. Import session cookies and retry.")
    val next = doc.selectFirst("a[rel=next]")?.absUrl("href")?.takeIf { raw ->
        runCatching { raw.toHttpUrl().let { it.scheme == "https" && it.host == "dramatip.net" } }.getOrDefault(false)
    }
    return CatalogPage(cards, next)
}

/** Session cookies stay in memory and retain OkHttp's domain/path/secure matching. */
private class DramatipSessionCookies : CookieJar {
    private val cookies = mutableListOf<Cookie>()
    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        for (cookie in cookies) {
            this.cookies.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
            if (cookie.expiresAt > now) this.cookies += cookie
        }
        this.cookies.removeAll { it.expiresAt <= now }
    }
    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        cookies.removeAll { it.expiresAt <= now }
        return cookies.filter { it.matches(url) }.sortedByDescending { it.path.length }
    }
}

/** Responsive poster URLs may contain commas in CDN resize queries; split only at candidate boundaries. */
internal fun catalogPosterUrl(image: org.jsoup.nodes.Element): String {
    fun absolute(raw: String): String? = runCatching {
        val resolved = java.net.URI(image.baseUri()).resolve(raw.trim()).toString().toHttpUrl()
        resolved.takeIf { it.scheme in setOf("http", "https") && it.username.isEmpty() && it.password.isEmpty() }?.let { dramatipPublicPosterUrl(it) ?: it.toString() }
    }.getOrNull()
    for (attribute in listOf("data-srcset", "srcset")) {
        val candidates = image.attr(attribute).split(Regex(""",\s*(?=https?://|/|\.\.?/)""")).mapNotNull { candidate ->
            val parts = Regex("""^(.+?)\s+(\d+)w$""").matchEntire(candidate.trim()) ?: return@mapNotNull null
            val url = absolute(parts.groupValues[1]) ?: return@mapNotNull null
            (parts.groupValues[2].toIntOrNull() ?: return@mapNotNull null) to url
        }.sortedBy { it.first }
        if (candidates.isNotEmpty()) return (candidates.firstOrNull { it.first >= 384 } ?: candidates.last()).second
    }
    for (attribute in listOf("data-src", "data-lazy-src", "data-original", "src")) {
        absolute(image.attr(attribute).takeIf(String::isNotBlank) ?: continue)?.let { return it }
    }
    return ""
}

/** These CDNs expose the original public upload as well as an expiring image-transform URL.
 * Dramatip sometimes publishes expired transforms; verified same-host originals remain readable.
 * Never decode arbitrary URLs into new origins, and never weaken TLS for an expired certificate.
 */
internal fun dramatipPublicPosterUrl(url: HttpUrl): String? {
    if (url.scheme != "https" || url.host !in setOf(
        "static.rivoshort.com", "static.winknoveltv.com", "static.fliksotv.com",
        "static.ziptaletv.com", "static.popnovava.com", "static.hnytread.com",
    ) || url.queryParameter("sign").isNullOrBlank()) return null
    return runCatching {
        val encoded = url.encodedPath.removePrefix("/")
        if (encoded.length > 4096 || '/' in encoded) return null
        val decoded = String(java.util.Base64.getDecoder().decode(encoded), Charsets.UTF_8)
        val key = com.google.gson.JsonParser.parseString(decoded).asJsonObject.get("key")?.asString ?: return null
        if (!Regex("""^uploads/[0-9]{4}-[0-9]{2}-[0-9]{2}/[A-Za-z0-9_-]+[.](?:png|jpe?g|webp)$""", RegexOption.IGNORE_CASE).matches(key)) return null
        url.newBuilder().encodedPath("/$key").query(null).fragment(null).build().toString()
    }.getOrNull()
}

/** Refresh only the failed title's advertised artwork; the UI calls this once, never per frame. */
internal suspend fun freshDramatipPoster(detailUrl: String, cookieHeader: (String) -> String): String? = withContext(Dispatchers.IO) {
    val url = detailUrl.toHttpUrl()
    if (url.scheme != "https" || url.host != "dramatip.net" ||
        !Regex("""^/(?:[a-z]{2}/)?series/[^/]+/?$""").matches(url.encodedPath)) return@withContext null
    val request = Request.Builder().url(url)
        .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36")
        .header("Accept", "text/html,application/xhtml+xml").header("Cache-Control", "no-cache")
    cookieHeader(url.toString()).takeIf(String::isNotBlank)?.let { request.header("Cookie", it) }
    DramatipArtworkHttp.client.newCall(request.build()).consumeCancellable { response ->
        if (!response.isSuccessful) return@consumeCancellable null
        val bytes = response.body.byteStream().use { it.readNBytes(4_000_001) }
        if (bytes.size > 4_000_000) return@consumeCancellable null
        val doc = Jsoup.parse(bytes.toString(Charsets.UTF_8), url.toString())
        val raw = doc.selectFirst("meta[property=og:image]")?.attr("content").orEmpty()
            .ifBlank { doc.selectFirst("meta[name=twitter:image]")?.attr("content").orEmpty() }
        val target = url.resolve(raw)?.takeIf { raw.isNotBlank() && it.username.isEmpty() && it.password.isEmpty() && it.scheme in setOf("http", "https") }
        target?.let { dramatipPublicPosterUrl(it) ?: it.toString() }
    }
}

private object DramatipArtworkHttp {
    // The metadata request is same-origin only; a redirect must not forward imported sessions.
    val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
}
