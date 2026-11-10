package com.example.animepahedl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Provider HTTP is independent from AnimePahe's WebView cookie jar and Kwik defaults. */
internal class SiteDownloadRepository(
    private val client: OkHttpClient = OkHttpClient.Builder().cookieJar(CookieJar.NO_COOKIES)
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS).callTimeout(90, TimeUnit.SECONDS).build(),
    private val userAgent: String = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36",
    private val cookieHeader: ((String) -> String)? = null,
    // Keep the edition boundary safe even when a caller constructs the repository directly.
    // Production entry points use appSiteDownloadRepository(), but tests and future call sites
    // should not silently gain providers from another app edition.
    private val allowedProviders: Set<SiteDownloadProvider> = MovieCatalogConfig.supportedProviders
) {
    private val providerSession = CatalogSessionCookies()
    private val kisskh = KisskhApi(client, cookieHeader)
    private data class Page(val text: String, val url: HttpUrl)

    suspend fun inspect(input: String, cookie: String = ""): SiteDownloadSeries = withContext(Dispatchers.IO) {
        val url = siteDownloadInput(input)
        val provider = requireAllowedProvider(url)
        when (provider) {
            SiteDownloadProvider.KISSKH -> kisskh.series(url.newBuilder().host("kisskh.do").build())
            SiteDownloadProvider.DIRECT -> {
                val title = url.pathSegments.lastOrNull()?.takeIf(String::isNotBlank) ?: "Direct video"
                SiteDownloadSeries(provider, "DIRECT:${url}", title, "", url.toString(), listOf(SiteDownloadEpisode(url.toString(), 1, title, url.toString())))
            }
            SiteDownloadProvider.EROTICMV -> {
                val initial = fetch(url, cookie = cookie)
                val (name, poster) = parseProviderTitleAndPoster(initial.text, initial.url)
                val title = cleanProviderTitle(name, provider).ifBlank { "Video" }
                val variants = eroticVariants(initial, cookie)
                SiteDownloadSeries(provider, "EROTICMV:${initial.url}", title, poster, initial.url.toString(),
                    listOf(SiteDownloadEpisode(initial.url.toString(), 1, title, initial.url.toString())), variantQualities(variants))
            }
            else -> throw IOException("${provider.contentLabel} sources aren't included in this edition.")
        }
    }

    suspend fun resolve(locator: String, quality: String = "Best", cookie: String = ""): ResolvedMedia = withContext(Dispatchers.IO) {
        val input = siteDownloadInput(locator)
        val provider = requireAllowedProvider(input)
        val variants = episodeVariants(input, provider, cookie)
        val chosen = chooseMediaVariant(variants, quality)
        val headers = linkedMapOf("User-Agent" to userAgent)
        if (provider != SiteDownloadProvider.DIRECT) headers["Referer"] = input.toString()
        val mediaUrl = chosen.url.toHttpUrl()
        if (cookie.isNotBlank() && mediaUrl.scheme == input.scheme && mediaUrl.host == input.host && mediaUrl.port == input.port) headers["Cookie"] = cookie
        headers.putAll(chosen.headers)
        ResolvedMedia(chosen.url, chosen.quality, "und", headers, chosen.backupUrls, chosen.mimeType, chosen.subtitles)
    }

    suspend fun resolveChoices(locator: String, cookie: String = "", includeSubtitles: Boolean = true): List<ResolvedMedia> = withContext(Dispatchers.IO) {
        val input = siteDownloadInput(locator)
        val provider = requireAllowedProvider(input)
        episodeVariants(input, provider, cookie, includeSubtitles).distinctBy { it.url }.map { variant ->
            val headers = linkedMapOf("User-Agent" to userAgent)
            if (provider != SiteDownloadProvider.DIRECT) headers["Referer"] = input.toString()
            val mediaUrl = variant.url.toHttpUrl()
            if (cookie.isNotBlank() && mediaUrl.scheme == input.scheme && mediaUrl.host == input.host && mediaUrl.port == input.port) headers["Cookie"] = cookie
            headers.putAll(variant.headers)
            ResolvedMedia(variant.url, variant.quality, "und", headers, variant.backupUrls, variant.mimeType, variant.subtitles)
        }
    }

    private fun variantQualities(variants: List<MediaVariant>): List<String> = listOf("Best") + variants.map { it.quality }.distinct()
        .sortedByDescending { it.removeSuffix("p").toIntOrNull() ?: 0 }

    private fun requireAllowedProvider(url: HttpUrl): SiteDownloadProvider {
        val provider = siteDownloadProvider(url)
        if (provider !in allowedProviders) throw IOException("${provider.contentLabel} sources aren't included in this edition.")
        return provider
    }

    private suspend fun episodeVariants(url: HttpUrl, provider: SiteDownloadProvider, cookie: String,
        includeSubtitles: Boolean = true): List<MediaVariant> = when (provider) {
        SiteDownloadProvider.KISSKH -> kisskh.variants(url.newBuilder().host("kisskh.do").build(), includeSubtitles)
        SiteDownloadProvider.DIRECT -> listOf(MediaVariant(decodeEroticMedia(url.toString()), mimeType = if (isHlsMedia(url)) "application/x-mpegURL" else if (url.encodedPath.endsWith(".webm", true) || url.queryParameter("mime_type") == "video/webm") "video/webm" else "video/mp4"))
        SiteDownloadProvider.EROTICMV -> eroticVariants(fetch(url, cookie = cookie), cookie)
        else -> throw IOException("${provider.contentLabel} sources aren't included in this edition.")
    }

    private suspend fun eroticVariants(initial: Page, cookie: String): List<MediaVariant> {
        var page = initial
        val seen = mutableSetOf<String>()
        repeat(3) {
            val raw = eroticVideoSource(page.text) ?: throw IOException("The player did not expose a supported video link.")
            val target = resolveProviderUrl(page.url, decodeEroticMedia(raw)).toHttpUrl()
            if (isHlsMedia(target) || isSupportedVideoFile(target)) return listOf(MediaVariant(target.toString(), mimeType = if (isHlsMedia(target)) "application/x-mpegURL" else "video/mp4"))
            require(seen.add(target.toString())) { "Embedded player loop detected." }
            // An embedded host gets no provider cookies, including on redirect.
            page = fetch(target, referer = page.url.toString(), cookie = if (target.host == initial.url.host) cookie else "")
        }
        throw IOException("The embedded player is nested too deeply.")
    }

    private suspend fun fetch(url: HttpUrl, referer: String? = null, cookie: String = "", limit: Int = 8 * 1024 * 1024): Page {
        require('\r' !in cookie && '\n' !in cookie) { "Invalid Cookie header." }
        // Re-evaluate imported cookies per request; explicit headers stay on their original origin.
        // Provider pages carry session context, so never let an HTTPS page redirect downgrade
        // that request (or imported cookies) to cleartext. Direct HTTP media bypasses fetch and
        // remains supported by the DIRECT path.
        if (url.scheme != "https") throw IOException("Provider pages must use HTTPS.")
        val scoped = client.newBuilder().followSslRedirects(false).cookieJar(providerSession).addNetworkInterceptor { chain ->
            val builder = chain.request().newBuilder()
            val target = chain.request().url
            if (cookieHeader != null) {
                builder.removeHeader("Cookie")
                val imported = cookieHeader.invoke(target.toString()).split(';').map(String::trim).filter { '=' in it }
                val names = imported.map { it.substringBefore('=') }.toSet()
                val received = providerSession.loadForRequest(target).filterNot { it.name in names }.map { "${it.name}=${it.value}" }
                (received + imported).joinToString("; ").takeIf(String::isNotBlank)?.let { builder.header("Cookie", it) }
            } else if (target.host != url.host || target.scheme != url.scheme || target.port != url.port) builder.removeHeader("Cookie")
            chain.proceed(builder.build())
        }.build()
        val request = Request.Builder().url(url).header("User-Agent", userAgent).header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json;q=0.8,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document").header("Sec-Fetch-Mode", "navigate").header("Sec-Fetch-Site", "same-origin")
        request.header("Referer", referer ?: url.newBuilder().encodedPath("/").query(null).fragment(null).build().toString())
        if (cookie.isNotBlank()) request.header("Cookie", cookie)
        return scoped.newCall(request.build()).consumeCancellable { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} from ${response.request.url.host}. Try again or refresh provider cookies.")
            val body = response.body
            if (body.contentLength() > limit) throw IOException("Provider metadata exceeds the size limit.")
            val bytes = body.byteStream().use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(16384)
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    if (output.size() + read > limit) throw IOException("Provider metadata exceeds the size limit.")
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
            val text = bytes.toString(body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8)
            if (SessionChecks.isChallenge(text, response.header("cf-mitigated"))) throw IOException("${url.host} requires browser verification or fresh cookies.")
            Page(text, response.request.url)
        }
    }
}

/** All production entry points use this factory so pasted links and background jobs share the edition boundary. */
internal fun appSiteDownloadRepository(cookieHeader: ((String) -> String)? = null): SiteDownloadRepository =
    SiteDownloadRepository(cookieHeader = cookieHeader, allowedProviders = MovieCatalogConfig.supportedProviders)
