package com.anilili.playback

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class LocalStreamProxyHeaderPolicyTest {
    @Test
    fun redirectToAnotherOriginKeepsOnlySafeRootHeaders() {
        val first = MockWebServer()
        val second = MockWebServer()
        first.start()
        second.start()
        try {
            first.enqueue(MockResponse().setResponseCode(302).addHeader("Location", second.url("/segment.ts")))
            second.enqueue(MockResponse().setBody("segment"))

            val context = rootContext(first, mapOf(
                "Cookie" to "session=private",
                "Authorization" to "Bearer private",
                "X-Private-Token" to "private",
                "Referer" to "https://watch.example/episode",
                "Origin" to "https://watch.example",
                "User-Agent" to "Anilili/1.0",
                "Accept" to "*/*",
                "Accept-Language" to "en-US",
            ))
            val request = request(first.url("/root.m3u8"), context)

            OkHttpClient.Builder()
                .addNetworkInterceptor(RootHeaderPolicyInterceptor)
                .build()
                .newCall(request)
                .execute()
                .use { assertEquals("segment", it.body.string()) }

            val initial = first.takeRequest()
            assertEquals("session=private", initial.getHeader("Cookie"))
            assertEquals("Bearer private", initial.getHeader("Authorization"))
            val redirected = second.takeRequest()
            assertEquals("https://watch.example/episode", redirected.getHeader("Referer"))
            assertEquals("https://watch.example", redirected.getHeader("Origin"))
            assertEquals("Anilili/1.0", redirected.getHeader("User-Agent"))
            assertEquals("*/*", redirected.getHeader("Accept"))
            assertEquals("en-US", redirected.getHeader("Accept-Language"))
            assertNull(redirected.getHeader("Cookie"))
            assertNull(redirected.getHeader("Authorization"))
            assertNull(redirected.getHeader("X-Private-Token"))
        } finally {
            first.shutdown()
            second.shutdown()
        }
    }

    @Test
    fun hlsChildUrlInheritsRootContextAndKeepsRangePassthrough() {
        val root = MockWebServer()
        val child = MockWebServer()
        root.start()
        child.start()
        try {
            child.enqueue(MockResponse().setResponseCode(206).setBody("part"))
            val context = rootContext(root, mapOf(
                "Cookie" to "session=private",
                "X-Private-Token" to "private",
                "Referer" to "https://watch.example/episode",
                "User-Agent" to "Anilili/1.0",
            ))
            var childUrl: String? = null
            val playlist = rewriteHlsPlaylist(
                "#EXTM3U\n#EXTINF:4,\n${child.url("/video.ts")}",
                root.url("/root.m3u8").toString(),
            ) { url ->
                childUrl = url
                "http://127.0.0.1:12345/child-token"
            }
            assertNotNull(childUrl)
            assertTrue(playlist.contains("http://127.0.0.1:12345/child-token"))

            val request = request(child.url("/video.ts"), context)
                .newBuilder()
                .header("Range", "bytes=0-3")
                .build()
            OkHttpClient.Builder()
                .addNetworkInterceptor(RootHeaderPolicyInterceptor)
                .build()
                .newCall(request)
                .execute()
                .use { assertEquals(206, it.code) }

            val received = child.takeRequest()
            assertEquals("bytes=0-3", received.getHeader("Range"))
            assertEquals("https://watch.example/episode", received.getHeader("Referer"))
            assertEquals("Anilili/1.0", received.getHeader("User-Agent"))
            assertEquals(null, received.getHeader("Cookie"))
            assertEquals(null, received.getHeader("X-Private-Token"))
        } finally {
            root.shutdown()
            child.shutdown()
        }
    }

    @Test
    fun originIncludesSchemeHostAndEffectivePort() {
        val http = "http://cdn.example/video".toHttpUrl()
        val defaultHttp = "http://cdn.example:80/other".toHttpUrl()
        val https = "https://cdn.example/video".toHttpUrl()
        val otherPort = "http://cdn.example:8080/video".toHttpUrl()

        assertEquals(HttpOrigin.from(http), HttpOrigin.from(defaultHttp))
        assertFalse(HttpOrigin.from(http) == HttpOrigin.from(https))
        assertFalse(HttpOrigin.from(http) == HttpOrigin.from(otherPort))
    }

    @Test
    fun externalPlayerCanOnlySupplyRangeAndConditionalHeaders() {
        val raw = """GET /token HTTP/1.1
            |Cookie: player=cookie
            |Referer: https://player.example/
            |User-Agent: player-agent
            |Authorization: player-secret
            |Range: bytes=10-20
            |If-Range: etag-value
            |
            |
        """.trimMargin().replace("\n", "\r\n")

        val parsed = readRequest(ByteArrayInputStream(raw.toByteArray()))

        assertEquals(listOf("Range" to "bytes=10-20", "If-Range" to "etag-value"), parsed?.forwardable)
    }

    private fun rootContext(server: MockWebServer, headers: Map<String, String>) =
        RootHeaderContext(HttpOrigin.from(server.url("/")), headers.toMap())

    private fun request(url: okhttp3.HttpUrl, context: RootHeaderContext) =
        Request.Builder()
            .url(url)
            .tag(RootHeaderContext::class.java, context)
            .apply { context.headers.forEach { (name, value) -> header(name, value) } }
            .build()
}
