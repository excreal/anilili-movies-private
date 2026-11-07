package com.example.animepahedl

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SiteDownloadRepositoryTest {
    private val requests = mutableListOf<Request>()
    private fun repository(respond: (Request) -> String): SiteDownloadRepository {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += request
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(respond(request).toResponseBody("text/html".toMediaType())).build()
        }.build()
        return SiteDownloadRepository(client, "Mozilla/5.0")
    }
    private fun encrypted(url: String): String {
        val nonce = ByteArray(12) { 7 }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(Base64.getDecoder().decode("QC6Ir2trghxRAyyyWZEOEFR4GgLhnfQ4A19I3QBlQkc="), "AES"), GCMParameterSpec(128, nonce))
        return Base64.getEncoder().encodeToString(nonce + cipher.doFinal(url.toByteArray()))
    }
    @Test fun inspectThenResolveRetainsEpisodeAndDoesNotSendCookiesToCdn() = runBlocking {
        val payload = encrypted("https://cdn.test/one.mp4")
        val repo = repository { req ->
            if (req.url.encodedPath == "/series/story") """<title>A Story | Dramatip</title><a href="/series/story/episode-1">1</a>"""
            else """{"sourceFirst":{"chain":[{"type":"mp4","encByQuality":{"720":"$payload"}}]}}"""
        }
        val series = repo.inspect("https://dramatip.net/series/story", "session=secret")
        assertEquals("A Story", series.title)
        assertEquals(listOf(1), series.episodes.map { it.number })
        assertEquals(listOf("Best", "720p"), series.qualities)
        val media = repo.resolve(series.episodes.single().url, cookie = "session=secret")
        assertEquals("https://cdn.test/one.mp4", media.url)
        assertFalse(media.headers.containsKey("Cookie"))
        assertTrue(requests.all { it.header("Cookie") == "session=secret" })
        assertTrue(requests.all { it.url.host == "dramatip.net" })
    }
    @Test fun providerSameOriginMediaRetainsCookie() = runBlocking {
        val payload = encrypted("https://dramatip.net/media/one.mp4")
        val repo = repository { """{"sourceFirst":{"chain":[{"type":"mp4","enc":"$payload"}]}}""" }
        val media = repo.resolve("https://dramatip.net/series/story/episode-1", cookie = "session=private")
        assertEquals("session=private", media.headers["Cookie"])
    }
    @Test fun episodeRedirectMustNotSubstituteAnotherVideo() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val wrong = chain.request().newBuilder().url("https://kiss18.vip/watch/story/1").build()
            val schema = """<script type="application/ld+json">{"@type":"VideoObject","url":"/watch/story/1","contentUrl":"https://cdn.test/1.mp4"}</script>"""
            Response.Builder().request(wrong).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(schema.toResponseBody("text/html".toMediaType())).build()
        }.build()
        val result = runCatching { SiteDownloadRepository(client).resolve("https://kiss18.vip/watch/story/2") }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message.orEmpty().contains("different episode"))
    }
    @Test fun httpErrorsDoNotExposeCookieOrSignedUrl() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(403).message("Forbidden")
                .body("private backend detail".toResponseBody()).build()
        }.build()
        val result = runCatching { SiteDownloadRepository(client).inspect("https://dramatip.net/series/story", "session=secret") }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message.orEmpty().contains("HTTP 403"))
        assertFalse(result.exceptionOrNull()!!.message.orEmpty().contains("secret"))
        assertFalse(result.exceptionOrNull()!!.message.orEmpty().contains("private backend"))
    }
    @Test fun directFilesPreserveTypesAndHttpLinks() = runBlocking {
        val repo = repository { error("Direct resolution should not fetch metadata") }
        assertEquals("video/webm", repo.resolve("https://cdn.test/video.webm").mimeType)
        assertEquals("http://cdn.test/video.mp4", repo.inspect("http://cdn.test/video.mp4").episodes.single().url)
        assertTrue(requests.isEmpty())
    }
}
