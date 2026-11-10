package com.example.animepahedl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit

internal fun isAllowedArtworkRecoveryUrl(url: HttpUrl): Boolean =
    url.isHttps && url.username.isEmpty() && url.password.isEmpty() &&
        url.host in setOf("eroticmv.com", "www.eroticmv.com")

internal fun artworkRecoveryHostPolicy(): Interceptor = Interceptor { chain ->
    if (!isAllowedArtworkRecoveryUrl(chain.request().url)) throw IOException("Artwork redirect left the Movies source.")
    chain.proceed(chain.request())
}

/** Refresh a failed cover once from its public detail metadata, never from a media locator. */
internal object ArtworkRecovery {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS)
        // OkHttp runs network interceptors on every redirect hop, so the allowlist applies to the
        // destination too, not only to the catalogue URL that initiated artwork recovery.
        .addNetworkInterceptor(artworkRecoveryHostPolicy()).build()
    private fun key(detail: String, original: String) = java.security.MessageDigest.getInstance("SHA-256").digest((detail + "|" + original).toByteArray()).joinToString("") { "%02x".format(it) }
    fun cached(context: android.content.Context, detail: String, original: String): String =
        context.applicationContext.getSharedPreferences("artwork-recovery", android.content.Context.MODE_PRIVATE).getString(key(detail, original), original) ?: original
    fun retain(context: android.content.Context, detail: String, original: String, replacement: String) {
        context.applicationContext.getSharedPreferences("artwork-recovery", android.content.Context.MODE_PRIVATE).edit().putString(key(detail, original), replacement).apply()
    }
    suspend fun poster(detail: String, previous: String): String? = withContext(Dispatchers.IO) {
        val target = detail.toHttpUrlOrNull() ?: return@withContext null
        if (!isAllowedArtworkRecoveryUrl(target)) return@withContext null
        client.newCall(Request.Builder().url(target).header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36")
            .header("Referer", target.newBuilder().encodedPath("/").query(null).build().toString())
            .header("Accept", "text/html,application/xhtml+xml,*/*").header("Accept-Language", "en-US,en;q=0.9").build()).consumeCancellable { response ->
            if (!response.isSuccessful) return@consumeCancellable null
            val bytes = response.body.byteStream().use { it.readNBytes(4_000_001) }
            if (bytes.size > 4_000_000) return@consumeCancellable null
            val doc = Jsoup.parse(bytes.toString(Charsets.UTF_8), response.request.url.toString())
            val raw = doc.selectFirst("meta[property=og:image],meta[name=twitter:image]")?.attr("content").orEmpty()
            if (raw.isBlank()) return@consumeCancellable null
            val url = target.resolve(raw)?.takeIf { it.isHttps } ?: return@consumeCancellable null
            url.toString().takeIf { it != previous }
        }
    }
}
