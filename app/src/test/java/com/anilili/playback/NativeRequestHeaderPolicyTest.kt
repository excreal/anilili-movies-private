package com.anilili.playback

import com.example.animepahedl.siteDownloadCookieHeader
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeRequestHeaderPolicyTest {
    @Test
    fun sameOriginPlaylistChildAndKeyReceivePrivateHeaders() {
        val server = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setBody("#EXTM3U"))
            server.enqueue(MockResponse().setBody("key"))
            val policy = NativeRequestHeaderPolicyInterceptor().apply {
                update(NativeHeaderContext(server.url("/root.m3u8"), mapOf(
                    "Authorization" to "Bearer episode",
                    "X-Playback-Token" to "private",
                    "Referer" to "https://watch.example/",
                )))
            }
            val client = OkHttpClient.Builder().addNetworkInterceptor(policy).build()
            client.newCall(Request.Builder().url(server.url("/root.m3u8")).build()).execute().close()
            client.newCall(Request.Builder().url(server.url("/keys/key.bin")).build()).execute().close()
            assertEquals("Bearer episode", server.takeRequest().getHeader("Authorization"))
            val key = server.takeRequest()
            assertEquals("private", key.getHeader("X-Playback-Token"))
            assertEquals("https://watch.example/", key.getHeader("Referer"))
        } finally { server.shutdown() }
    }

    @Test
    fun redirectStripsPrivateHeadersAndRecomputesCookieForDestination() {
        val root = MockWebServer().apply { start() }
        val other = MockWebServer().apply { start() }
        try {
            root.enqueue(MockResponse().setResponseCode(302).addHeader("Location", other.url("/private/segment.ts")))
            other.enqueue(MockResponse().setBody("segment"))
            val cookies = "localhost\tTRUE\t/private\tFALSE\t0\tvalid\tyes\n" +
                "localhost\tTRUE\t/elsewhere\tFALSE\t0\twrong\tpath\n"
            val policy = NativeRequestHeaderPolicyInterceptor().apply {
                update(NativeHeaderContext(root.url("/root.m3u8"), mapOf(
                    "Cookie" to "raw=must-not-escape",
                    "Authorization" to "Bearer secret",
                    "X-Api-Key" to "key-secret",
                    "X-Custom" to "custom-secret",
                    "Referer" to "https://watch.example/",
                ), { url -> siteDownloadCookieHeader(cookies, url, nowSeconds = 10) }))
            }
            OkHttpClient.Builder().addNetworkInterceptor(policy).build()
                .newCall(Request.Builder().url(root.url("/private/root.m3u8")).build()).execute().close()
            val redirected = other.takeRequest()
            assertNull(redirected.getHeader("Authorization"))
            assertNull(redirected.getHeader("X-Api-Key"))
            assertNull(redirected.getHeader("X-Custom"))
            assertEquals("valid=yes", redirected.getHeader("Cookie"))
            assertEquals("https://watch.example/", redirected.getHeader("Referer"))
        } finally { root.shutdown(); other.shutdown() }
    }

    @Test
    fun streamSwitchReplacesPolicyAndCookiePolicyHonorsPathSecureAndExpiry() {
        val server = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setBody("ok"))
            val policy = NativeRequestHeaderPolicyInterceptor()
            policy.update(NativeHeaderContext(server.url("/one.m3u8"), mapOf("X-Session-Token" to "one")))
            policy.update(NativeHeaderContext(server.url("/two.m3u8"), mapOf("X-Session-Token" to "two")))
            OkHttpClient.Builder().addNetworkInterceptor(policy).build()
                .newCall(Request.Builder().url(server.url("/two.m3u8")).build()).execute().close()
            assertEquals("two", server.takeRequest().getHeader("X-Session-Token"))

            val cookies = "example.test\tFALSE\t/private\tTRUE\t200\tsecure\tyes\n" +
                "example.test\tFALSE\t/private\tFALSE\t200\tplain\tyes\n" +
                "example.test\tFALSE\t/private\tFALSE\t90\texpired\tno\n"
            val context = NativeHeaderContext(
                "https://example.test/private/root.m3u8".toHttpUrl(), emptyMap(),
            ) { siteDownloadCookieHeader(cookies, it, nowSeconds = 100) }
            assertEquals("secure=yes; plain=yes", context.headersFor("https://example.test/private/segment.ts".toHttpUrl())["Cookie"])
            assertNull(context.headersFor("https://example.test/private/segment.ts".toHttpUrl())["Cookie"]?.takeIf { it.contains("expired=") })
            assertEquals("plain=yes", context.headersFor("http://example.test/private/segment.ts".toHttpUrl())["Cookie"])
            assertTrue(context.headersFor("https://example.test/elsewhere/segment.ts".toHttpUrl())["Cookie"].isNullOrEmpty())
            assertTrue(context.headersFor("https://example.test/private/segment.ts".toHttpUrl()).getValue("Cookie").contains("secure=yes"))
        } finally { server.shutdown() }
    }

    @Test
    fun rawCookieCannotEscapeItsRootPathWhenNoNetscapeJarMatches() {
        val root = "https://media.example/private/root.m3u8".toHttpUrl()
        val context = NativeHeaderContext(root, mapOf("Cookie" to "session=raw"))
        assertEquals("session=raw", context.headersFor(root)["Cookie"])
        assertNull(context.headersFor("https://media.example/other/segment.ts".toHttpUrl())["Cookie"])
        val blankCallback = NativeHeaderContext(root, mapOf("Cookie" to "session=raw")) { "" }
        assertNull(blankCallback.headersFor(root)["Cookie"])
    }

    @Test
    fun evictedRequestMarkerDoesNotFallBackToTheNewStreamsContext() {
        val server = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setBody("ok"))
            val policy = NativeRequestHeaderPolicyInterceptor()
            val oldId = policy.update(NativeHeaderContext(server.url("/old.m3u8"), mapOf("X-Session" to "old")))!!
            repeat(32) {
                policy.update(NativeHeaderContext(server.url("/new.m3u8"), mapOf("X-Session" to "new")))
            }
            OkHttpClient.Builder().addNetworkInterceptor(policy).build().newCall(
                Request.Builder().url(server.url("/old.m3u8"))
                    .header(NativeRequestHeaderPolicyInterceptor.POLICY_MARKER, oldId).build(),
            ).execute().close()
            val request = server.takeRequest()
            assertNull(request.getHeader("X-Session"))
            assertNull(request.getHeader(NativeRequestHeaderPolicyInterceptor.POLICY_MARKER))
        } finally { server.shutdown() }
    }
}
