package com.example.animepahedl

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Headers belong to one resolved episode, never to a mutable shared client. */
internal fun mediaRequestHeaders(media: ResolvedMedia, target: String = media.url): Map<String, String> {
    val origin = media.url.toHttpUrl()
    val destination = target.toHttpUrl()
    val credentialsAllowed = destination.host == origin.host && destination.isHttps == origin.isHttps && destination.port == origin.port
    return buildMap {
        put("User-Agent", AnimeNetwork.userAgent)
        media.headers.orEmpty().forEach { (name, value) ->
            if (name.lowercase() in setOf("host", "connection", "content-length", "range")) return@forEach
            if (!credentialsAllowed && name.lowercase() in setOf("cookie", "authorization", "proxy-authorization")) return@forEach
            keys.firstOrNull { it.equals(name, true) }?.let { remove(it) }
            put(name, value)
        }
    }
}

internal fun normalizedMediaType(type: String?): String? = type?.substringBefore(';')?.trim()?.lowercase()

internal fun isHlsMimeType(type: String?): Boolean = normalizedMediaType(type) in
    setOf("application/x-mpegurl", "application/vnd.apple.mpegurl", "application/mpegurl")

internal fun ResolvedMedia.isHls(): Boolean = isHlsMimeType(mimeType) || isHlsMedia(url.toHttpUrl())

internal object MediaRequests {
    // This client deliberately has no AnimePahe cookie jar or default AnimePahe Referer.
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS).callTimeout(90, TimeUnit.SECONDS).build()

    fun client(media: ResolvedMedia, cookieHeader: ((String) -> String)? = null): OkHttpClient = client.newBuilder().addNetworkInterceptor { chain ->
        val builder = chain.request().newBuilder()
        val allowed = mediaRequestHeaders(media, chain.request().url.toString())
        // OkHttp follows redirects inside one application call. Check credentials on every hop.
        listOf("Cookie", "Authorization", "Proxy-Authorization").forEach { name ->
            if (allowed.keys.none { it.equals(name, true) }) builder.removeHeader(name)
        }
        if (cookieHeader != null) {
            builder.removeHeader("Cookie")
            cookieHeader.invoke(chain.request().url.toString()).takeIf { it.isNotBlank() }?.let { builder.header("Cookie", it) }
        }
        chain.proceed(builder.build())
    }.build()
}
