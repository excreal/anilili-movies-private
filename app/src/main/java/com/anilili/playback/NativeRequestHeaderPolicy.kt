package com.anilili.playback

import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Interceptor
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicLong

/** Per-request header policy for native Media3 playback, including every OkHttp redirect hop. */
internal data class NativeHeaderContext(
    val root: HttpUrl,
    val headers: Map<String, String>,
    val cookieHeaderFor: ((String) -> String)? = null,
) {
    fun headersFor(destination: HttpUrl): Map<String, String> {
        val sameOrigin = destination.scheme == root.scheme && destination.host == root.host && destination.port == root.port
        val safe = headers.filterKeys { !isPrivatePlaybackHeader(it) }
        val scopedCookie = cookieHeaderFor?.invoke(destination.toString()).orEmpty()
        if (!sameOrigin) {
            return if (scopedCookie.isBlank()) safe else safe + ("Cookie" to scopedCookie)
        }
        val private = headers.filterKeys(::isPrivatePlaybackHeader).toMutableMap()
        if (headers.keys.any { it.equals("Cookie", true) } || scopedCookie.isNotBlank()) {
            private.keys.removeAll { it.equals("Cookie", true) }
            when {
                cookieHeaderFor != null -> scopedCookie.takeIf(String::isNotBlank)?.let { private["Cookie"] = it }
                destination == root -> headers.entries.firstOrNull { it.key.equals("Cookie", true) }
                    ?.let { private["Cookie"] = it.value }
            }
        }
        return safe + private
    }
}

/** Only known request metadata is safe across origins; every custom header is private by default. */
internal fun isPrivatePlaybackHeader(name: String): Boolean =
    name.lowercase() !in SAFE_PLAYBACK_HEADERS

private val SAFE_PLAYBACK_HEADERS = setOf(
    "user-agent", "referer", "origin", "accept", "accept-language", "accept-encoding",
    "range", "if-range", "if-none-match", "if-modified-since", "cache-control",
)

internal class NativeRequestHeaderPolicyInterceptor : Interceptor {
    private val current = AtomicReference<NativeHeaderContext?>(null)
    private val sequence = AtomicLong()
    private val contexts = LinkedHashMap<String, NativeHeaderContext>()

    /** Marker travels inside Media3's per-data-source defaults and is removed before the wire. */
    fun update(context: NativeHeaderContext?): String? {
        current.set(context)
        if (context == null) return null
        val id = sequence.incrementAndGet().toString(36)
        synchronized(contexts) {
            contexts[id] = context
            while (contexts.size > 32) contexts.remove(contexts.keys.first())
        }
        return id
    }

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val builder = request.newBuilder()
        val id = request.header(POLICY_MARKER)
        val context = if (id != null) synchronized(contexts) { contexts[id] } else current.get()
        builder.removeHeader(POLICY_MARKER)
        // Media3 defaults may contain a prior stream's headers. Remove every managed private field
        // first, then apply only the current stream's policy for this actual destination.
        request.headers.names().filter(::isPrivatePlaybackHeader).forEach(builder::removeHeader)
        context?.headersFor(request.url)?.forEach { (name, value) -> builder.header(name, value) }
        return chain.proceed(builder.build())
    }

    companion object {
        const val POLICY_MARKER = "X-Anilili-Playback-Context"
    }
}
