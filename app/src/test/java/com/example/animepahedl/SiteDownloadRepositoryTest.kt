package com.example.animepahedl

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress

class SiteDownloadRepositoryTest {
    @Test fun metadataMergeDropsResultsForReplacedTitleAndGeneration() {
        val old = SiteDownloadSeries(SiteDownloadProvider.KISSKH, "old", "Old", "", "https://kisskh.do/Drama/Old?id=1", emptyList())
        val replacement = old.copy(id = "new", title = "New")
        assertSame(replacement, mergeInspectionMetadata(replacement, old.provider, old.id, 4, 5, SiteShowMetadata(description = "stale")))
        val updated = mergeInspectionMetadata(replacement, replacement.provider, replacement.id, 5, 5,
            SiteShowMetadata(description = "Current synopsis"))
        assertEquals("New", updated.title)
        assertEquals("Current synopsis", updated.metadata?.description)
    }

    @Test fun httpErrorsDoNotExposeCookieOrBackendResponse() = runBlocking {
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(403).message("Forbidden")
                .body("private backend detail".toResponseBody("text/html".toMediaType())).build()
        }.build()
        val result = runCatching { SiteDownloadRepository(client).inspect("https://eroticmv.com/film/", "session=secret") }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message.orEmpty().contains("HTTP 403"))
        assertFalse(result.exceptionOrNull()!!.message.orEmpty().contains("secret"))
        assertFalse(result.exceptionOrNull()!!.message.orEmpty().contains("private backend"))
    }

    @Test fun directFilesPreserveTypesAndHttpLinks() = runBlocking {
        val repo = SiteDownloadRepository(OkHttpClient.Builder().addInterceptor { error("Direct URLs must not be visited") }.build())
        assertEquals("video/webm", repo.resolve("https://cdn.test/video.webm").mimeType)
        assertEquals("http://cdn.test/video.mp4", repo.inspect("http://cdn.test/video.mp4").episodes.single().url)
        val signed = "https://eroticmv.com/cdn/video.mp4?token=use-once"
        assertEquals(signed, repo.resolve(repo.inspect(signed).episodes.single().url).url)
    }

    @Test fun resolvesExtensionlessMediaWithoutRequestingIt() = runBlocking {
        val title = "https://eroticmv.com/film/"
        val media = "https://cdn.test/redeem?token=use-once"
        val repo = SiteDownloadRepository(OkHttpClient.Builder().addInterceptor { chain ->
            assertEquals(title, chain.request().url.toString())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("""<video><source src="$media" type="application/vnd.apple.mpegurl"></video>""".toResponseBody()).build()
        }.build())
        val resolved = repo.resolve(repo.inspect(title).episodes.single().url)
        assertEquals(media, resolved.url)
        assertTrue(resolved.isHls())
        assertEquals(title, resolved.headers["Referer"])
    }

    @Test fun handsOffPlayerRedirectWithoutFollowingIt() = runBlocking {
        val host = "eroticmv.com"
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName(host).build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        MockWebServer().use { server ->
            server.useHttps(serverTls.sslSocketFactory(), false)
            server.start()
            val base = "https://$host:${server.port}"
            val player = "$base/embed"
            val media = "$base/video.m3u8?token=use-once"
            val client = OkHttpClient.Builder().sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager)
                .dns(Dns { listOf(InetAddress.getByName("127.0.0.1")) }).build()
            val html = """<meta property="og:video:url" content="$player">
                <meta property="og:video:type" content="text/html">"""
            repeat(2) { server.enqueue(MockResponse().setBody(html)) }
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", media))
            server.enqueue(MockResponse().setBody("#EXTM3U\n"))
            server.enqueue(MockResponse().setResponseCode(410))
            val repo = SiteDownloadRepository(client)
            val inspected = repo.inspect("$base/film/")
            assertEquals(1, server.requestCount)
            val resolved = repo.resolve(inspected.episodes.single().url)
            assertEquals(media, resolved.url)
            assertEquals(3, server.requestCount)
            val request = Request.Builder().url(resolved.url).build()
            client.newCall(request).execute().use { assertEquals("#EXTM3U\n", it.body.string()) }
            client.newCall(request).execute().use { assertEquals(410, it.code) }
        }
    }
}
