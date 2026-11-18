package com.example.animepahedl

import com.google.gson.Gson
import com.sun.net.httpserver.HttpServer
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test
import java.net.InetSocketAddress

class SiteDownloadDownloadTest {
    @Test fun legacyJsonRetainsIdentityAndFileNames() {
        val fixture = """{"title":"Old title","tree":"","folder":"","uri":"","error":"","bytes":0,
            "identity":{"anime":{"session":"show","title":"Existing Show","poster":"","year":"","type":"","status":""},
            "episode":{"session":"episode","number":"3","snapshot":"","duration":""},"quality":"720p","audio":"jpn"}}"""
        val restored = Gson().fromJson(fixture, ExportRecord::class.java)
        assertFalse(restored.identity!!.isSiteDownload)
        assertEquals("show-episode-720p-jpn", restored.identity.key)
        assertEquals("Existing Show ep03 720p JPN.mp4", restored.outputLocation("mp4").fileName)
        assertNull(restored.media)
        val roundTrip = Gson().fromJson(Gson().toJson(restored), ExportRecord::class.java)
        assertEquals(restored.identity.key, roundTrip.identity!!.key)
    }

    @Test fun siteDownloadIdentitySurvivesPersistenceWithoutCollidingWithAnimePahe() {
        val anime = Anime("show", "Drama", "", "", "", "")
        val episode = Episode("ep", "1", "", "")
        val legacy = DownloadIdentity(anime, episode, "720p", "und")
        val drama = legacy.copy(source = "sites", locator = "https://example.test/episode/1")
        val restored = Gson().fromJson(Gson().toJson(drama), DownloadIdentity::class.java)
        assertTrue(restored.isSiteDownload)
        assertTrue(restored.key.startsWith("sites:"))
        assertEquals(drama.key, restored.key)
        assertEquals(drama.locator, restored.locator)
        assertNotEquals(legacy.key, restored.key)
        assertNotEquals(drama.key, drama.copy(episode = episode.copy(session = "ep2")).key)
        assertNotEquals(drama.key, drama.copy(anime = anime.copy(session = "different-provider:show")).key)
    }

    @Test fun episodeContextSurvivesPersistenceAndDoesNotExposeCredentialsInToString() {
        val media = ResolvedMedia("https://cdn.test/video?token=secret-token", "720p", "und",
            mapOf("Cookie" to "session=secret-cookie", "Referer" to "https://provider.test/", "Origin" to "https://provider.test"),
            listOf("https://backup.test/video.m3u8"), "application/x-mpegURL")
        val record = ExportRecord("Drama", "", "", media = media)
        val restored = Gson().fromJson(Gson().toJson(record), ExportRecord::class.java).media!!
        assertEquals(media, restored)
        assertTrue(restored.isHls())
        assertFalse(restored.toString().contains("secret"))
    }

    @Test fun redirectDoesNotLeakCookiesAndKeepsProviderContext() {
        val original = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val redirected = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var firstCookie: String? = null
        var secondCookie: String? = null
        var secondReferer: String? = null
        var secondOrigin: String? = null
        try {
            redirected.createContext("/video") { exchange ->
                secondCookie = exchange.requestHeaders.getFirst("Cookie")
                secondReferer = exchange.requestHeaders.getFirst("Referer")
                secondOrigin = exchange.requestHeaders.getFirst("Origin")
                val bytes = "video".toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            redirected.start()
            original.createContext("/video") { exchange ->
                firstCookie = exchange.requestHeaders.getFirst("Cookie")
                exchange.responseHeaders.add("Location", "http://127.0.0.1:${redirected.address.port}/video")
                exchange.sendResponseHeaders(302, -1)
                exchange.close()
            }
            original.start()
            val media = ResolvedMedia("http://127.0.0.1:${original.address.port}/video", "Best", "und",
                mapOf("Cookie" to "session=private", "Referer" to "https://provider.test/episode/1", "Origin" to "https://provider.test"))
            val request = Request.Builder().url(media.url).apply {
                mediaRequestHeaders(media).forEach { (name, value) -> header(name, value) }
            }.build()
            MediaRequests.client(media).newCall(request).execute().use { assertEquals("video", it.body.string()) }
            assertEquals("session=private", firstCookie)
            assertNull(secondCookie)
            assertEquals("https://provider.test/episode/1", secondReferer)
            assertEquals("https://provider.test", secondOrigin)
        } finally { original.stop(0); redirected.stop(0) }
    }

}
