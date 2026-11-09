package com.example.animepahedl

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class KisskhResilienceTest {
    @Test fun subtitleRedirectDoesNotForwardProviderRefererToCdn() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", server.url("/cdn/one.srt")))
            server.enqueue(MockResponse().setBody("1\n00:00:01,000 --> 00:00:02,000\nCaption"))
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                val original = chain.request()
                val routed = original.newBuilder().url(server.url(original.url.encodedPath +
                    original.url.encodedQuery?.let { "?$it" }.orEmpty())).build()
                chain.proceed(routed)
            }.build()
            fetchProviderSubtitleBytes(client,
                providerSubtitleRequest(MediaSubtitle("https://kisskh.do/captions/one.srt", "English", "en")), "English")
            server.takeRequest().also { assertNull(it.getHeader("Referer")) }
            server.takeRequest().also { assertNull(it.getHeader("Referer")) }
            Unit
        } finally { server.shutdown() }
    }

    @Test fun subtitleParsingKeepsLanguagesAndDefaultFirstOrder() {
        val items = JsonParser.parseString("""[
            {"src":"https://cdn.test/first.srt","label":"English","land":"en","default":"false"},
            {"src":"https://cdn.test/default.srt","label":"한국어","land":"ko","default":"true"},
            {"src":"https://cdn.test/last.srt","label":"Japanese","land":"ja","default":"false"},
            {"src":"https://user:secret@cdn.test/private.srt","label":"Private","land":"xx"},
            {"src":"http://cdn.test/insecure.srt","label":"Ignored","land":"xx"}
        ]""").asJsonArray
        val tracks = parseKisskhSubtitles(items)
        assertEquals(listOf("ko", "en", "ja"), tracks.map { it.language })
        assertEquals(listOf("한국어", "English", "Japanese"), tracks.map { it.label })
        assertEquals(listOf(true, false, false), tracks.map { it.isDefault })
    }

    private fun api(server: MockWebServer): KisskhApi {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val original = chain.request()
            val routed = original.newBuilder().url(server.url(original.url.encodedPath +
                original.url.encodedQuery?.let { "?$it" }.orEmpty())).build()
            chain.proceed(routed)
        }.build()
        return KisskhApi(client)
    }

    private fun enqueueEpisode(server: MockWebServer) {
        server.enqueue(MockResponse().setBody("""{"country":"South Korea","type":"TVSeries","title":"Show","episodes":[{"id":456,"number":1}]}"""))
        server.enqueue(MockResponse().setBody("""{"Video":"https://cdn.test/one.m3u8"}"""))
    }

    @Test fun videoVariantsSkipStalledCaptionEndpoint() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            enqueueEpisode(server)
            server.enqueue(MockResponse().setBodyDelay(5, TimeUnit.SECONDS).setBody("caption response"))
            val started = System.nanoTime()
            val result = api(server).variants("https://kisskh.do/Drama/Show?id=123&ep=456".toHttpUrl(), includeSubtitles = false)
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
            assertEquals("https://cdn.test/one.m3u8", result.single().url)
            assertTrue(result.single().subtitles.isNullOrEmpty())
            assertTrue("Video resolution took $elapsedMs ms", elapsedMs < 2_000)
            assertEquals(2, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun captionEndpointFailureDoesNotDiscardVideoVariant() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            enqueueEpisode(server)
            server.enqueue(MockResponse().setResponseCode(503).setBody("unavailable"))
            val result = api(server).variants("https://kisskh.do/Drama/Show?id=123&ep=456".toHttpUrl())
            assertEquals("https://cdn.test/one.m3u8", result.single().url)
            assertTrue(result.single().subtitles.isNullOrEmpty())
            assertEquals(3, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun independentLocalizationFailuresKeepOriginalTracksAndInputOrder() = runBlocking {
        val tracks = (0 until 8).map { index ->
            MediaSubtitle("https://cdn.example/$index.srt", "Track $index", "lang$index")
        }
        val active = AtomicInteger()
        val peak = AtomicInteger()

        val result = bestEffortKisskhSubtitleLocalization(tracks) { track ->
            val now = active.incrementAndGet()
            peak.updateAndGet { maxOf(it, now) }
            try {
                val index = track.label.removePrefix("Track ").toInt()
                delay(((tracks.size - index) * 5).toLong()) // Force completions to arrive out of order.
                if (index == 3 || index == 6) throw IllegalStateException("Caption server unavailable")
                track.copy(url = "file:///captions/$index.vtt")
            } finally { active.decrementAndGet() }
        }

        val expected = tracks.mapIndexed { index, track ->
            if (index == 3 || index == 6) track else track.copy(url = "file:///captions/$index.vtt")
        }
        assertEquals(expected, result)
        assertTrue("Expected bounded parallel localization; observed peak ${peak.get()}", peak.get() in 2..3)
    }

    @Test fun subtitleLocalizationDoesNotSwallowCoroutineCancellation() {
        val track = MediaSubtitle("https://cdn.example/one.srt", "English", "en")

        assertThrows(CancellationException::class.java) {
            runBlocking {
                bestEffortKisskhSubtitleLocalization(listOf(track)) {
                    throw CancellationException("Cancelled")
                }
            }
        }
    }
}
