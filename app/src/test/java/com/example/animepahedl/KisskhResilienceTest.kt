package com.example.animepahedl

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Dns
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.net.InetAddress
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class KisskhResilienceTest {
    private fun secureServer(): Pair<MockWebServer, OkHttpClient> {
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("kisskh.do")
            .addSubjectAlternativeName("www.kisskh.do").build()
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setKeyEntry("kisskh", certificate.keyPair.private, "password".toCharArray(), arrayOf(certificate.certificate))
        }
        val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, "password".toCharArray())
        }.keyManagers
        val trustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        }
        val ssl = SSLContext.getInstance("TLS").apply { init(keyManagers, arrayOf(trustManager), SecureRandom()) }.socketFactory
        val server = MockWebServer().apply { useHttps(ssl, false) }
        val client = OkHttpClient.Builder().sslSocketFactory(ssl, trustManager)
            .hostnameVerifier { _, _ -> true }
            .dns(Dns { InetAddress.getAllByName("127.0.0.1").toList() })
            .addInterceptor { chain ->
                val request = chain.request()
                val routed = request.newBuilder().url(request.url.newBuilder().port(server.port).build()).build()
                chain.proceed(routed)
            }.build()
        return server to client
    }

    @Test fun kisskhAliasRedirectGetsOnlyTheTargetHostsCookie() = runBlocking {
        val (server, client) = secureServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "https://www.kisskh.do:${server.port}/api/redirected"))
            server.enqueue(MockResponse().setBody("{}"))
            val seenCookies = mutableListOf<String?>()
            val api = KisskhApi(client) { raw ->
                when (raw.toHttpUrl().host) {
                    "kisskh.do" -> "session=apex; apex-only=secret"
                    "www.kisskh.do" -> "session=www"
                    else -> "session=leaked"
                }
            }
            api.json("https://kisskh.do/api/start".toHttpUrl())
            seenCookies += server.takeRequest().getHeader("Cookie")
            seenCookies += server.takeRequest().getHeader("Cookie")
            assertEquals(listOf("session=apex; apex-only=secret", "session=www"), seenCookies)
        } finally { server.shutdown() }
    }

    @Test fun kisskhExternalRedirectIsRejectedBeforeCookieLookupOrRequest() = runBlocking {
        val (server, client) = secureServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "https://evil.test/api/steal"))
            val cookieLookups = AtomicInteger()
            val api = KisskhApi(client) { cookieLookups.incrementAndGet(); "session=secret" }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { api.json("https://kisskh.do/api/start".toHttpUrl()) }
            }
            assertEquals(1, server.requestCount)
            assertEquals(1, cookieLookups.get())
            assertEquals("session=secret", server.takeRequest().getHeader("Cookie"))
        } finally { server.shutdown() }
    }

    @Test fun kisskhApiRejectsCredentialsBeforeCookieLookup() = runBlocking {
        val (server, client) = secureServer()
        server.start()
        try {
            val cookieLookups = AtomicInteger()
            val api = KisskhApi(client) { cookieLookups.incrementAndGet(); "session=secret" }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { api.json("https://user:password@kisskh.do/api/start".toHttpUrl()) }
            }
            assertEquals(0, cookieLookups.get())
            assertEquals(0, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun videoDescriptorSafetyMatchesPlaybackRequirements() {
        assertEquals("https://cdn.example/video.m3u8", kisskhPlayableVideoUrl("https://cdn.example/video.m3u8").toString())
        assertEquals(null, kisskhPlayableVideoUrl("http://cdn.example/video.mp4"))
        assertEquals(null, kisskhPlayableVideoUrl("https://user:secret@cdn.example/video.mp4"))
        assertEquals(null, kisskhPlayableVideoUrl("https://cdn.example/page"))
    }

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
            {"src":"https://cdn.test/default.vtt","label":"한국어","land":"ko","default":"true"},
            {"src":"https://cdn.test/last.srt","label":"Japanese","land":"ja","default":"false"},
            {"src":"https://user:secret@cdn.test/private.srt","label":"Private","land":"xx"},
            {"src":"http://cdn.test/insecure.srt","label":"Ignored","land":"xx"}
        ]""").asJsonArray
        val tracks = parseKisskhSubtitles(items)
        assertEquals(listOf("ko", "en", "ja"), tracks.map { it.language })
        assertEquals(listOf("한국어", "English", "Japanese"), tracks.map { it.label })
        assertEquals(listOf(true, false, false), tracks.map { it.isDefault })
        assertEquals(listOf("text/vtt", "application/x-subrip", "application/x-subrip"), tracks.map { it.mimeType })
        assertEquals(listOf("한국어", "English"), prioritizedKisskhSubtitles(tracks).map { it.label })
        assertEquals(listOf("Japanese"), prioritizedKisskhSubtitles(listOf(tracks.last())).map { it.label })
    }

    @Test fun failedLocalizationRetainsSourceSubtitleMime() = runBlocking {
        val source = MediaSubtitle("https://cdn.example/one.srt", "English", "en", "application/x-subrip")
        val result = bestEffortKisskhSubtitleLocalization(listOf(source)) { throw IllegalStateException("offline") }
        assertEquals(source, result.single())
    }

    @Test fun malformedEncryptedCueIsSkippedWhileValidCuesSurvive() {
        fun encrypt(text: String): String {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec("8056483646328763".toByteArray(), "AES"),
                IvParameterSpec("6852612370185273".toByteArray()))
            return java.util.Base64.getEncoder().encodeToString(cipher.doFinal(text.toByteArray()))
        }
        val input = """
            00:00:01,000 --> 00:00:02,000
            ### definitely not base64 ###

            00:00:03,000 --> 00:00:04,000
            ${encrypt("Valid caption")}
        """.trimIndent()
        val normalized = normalizeKisskhSubtitles(input, "txt")
        assertTrue(normalized.contains("00:00:03.000 --> 00:00:04.000"))
        assertTrue(normalized.contains("Valid caption"))
        assertTrue(!normalized.contains("00:00:01"))
    }

    private fun api(client: OkHttpClient): KisskhApi = KisskhApi(client)

    private fun enqueueEpisode(server: MockWebServer) {
        server.enqueue(MockResponse().setBody("""{"country":"South Korea","type":"TVSeries","title":"Show","episodes":[{"id":456,"number":1}]}"""))
        server.enqueue(MockResponse().setBody("""{"Video":"https://cdn.test/one.m3u8"}"""))
    }

    private fun enqueueDramaDetails(server: MockWebServer, body: String) {
        server.enqueue(MockResponse().setBody(body))
    }

    @Test fun seriesKeepsServerEpisodeOrderAndNumbersAndAcceptsMatchingAdvertisedCount() = runBlocking {
        val (server, client) = secureServer()
        server.start()
        try {
            enqueueDramaDetails(server, """{"country":"South Korea","type":"TVSeries","title":"Show","episodesCount":3,"episodes":[{"id":456,"number":3},{"id":123,"number":1},{"id":789,"number":2}]}""")
            val result = api(client).series("https://kisskh.do/Drama/Show?id=44".toHttpUrl())
            assertEquals(listOf("456", "123", "789"), result.episodes.map { it.id })
            assertEquals(listOf("3", "1", "2"), result.episodes.map { it.displayNumber })
            assertEquals(3, result.episodes.size)
        } finally { server.shutdown() }
    }

    @Test fun seriesUsesServerEpisodeListAsIsWhenAdvertisedCountDiffers() = runBlocking {
        val (server, client) = secureServer()
        server.start()
        try {
            enqueueDramaDetails(server, """{"country":"South Korea","type":"TVSeries","title":"Show","episodesCount":4,"episodes":[{"id":456,"number":3},{"id":123,"number":1},{"id":789,"number":2}]}""")
            val result = api(client).series("https://kisskh.do/Drama/Show?id=44".toHttpUrl())
            assertEquals(listOf("456", "123", "789"), result.episodes.map { it.id })
            assertEquals(listOf("3", "1", "2"), result.episodes.map { it.displayNumber })
            assertEquals(3, result.episodes.size)
        } finally { server.shutdown() }
    }

    @Test fun videoVariantsSkipStalledCaptionEndpoint() = runBlocking {
        val (server, client) = secureServer()
        server.start()
        try {
            enqueueEpisode(server)
            server.enqueue(MockResponse().setBodyDelay(5, TimeUnit.SECONDS).setBody("caption response"))
            val started = System.nanoTime()
            val result = api(client).variants("https://kisskh.do/Drama/Show?id=123&ep=456".toHttpUrl(), includeSubtitles = false)
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
            assertEquals("https://cdn.test/one.m3u8", result.single().url)
            assertTrue(result.single().subtitles.isNullOrEmpty())
            assertTrue("Video resolution took $elapsedMs ms", elapsedMs < 2_000)
            assertEquals(2, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun captionEndpointFailureDoesNotDiscardVideoVariant() = runBlocking {
        val (server, client) = secureServer()
        server.start()
        try {
            enqueueEpisode(server)
            server.enqueue(MockResponse().setResponseCode(503).setBody("unavailable"))
            val result = api(client).variants("https://kisskh.do/Drama/Show?id=123&ep=456".toHttpUrl())
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
