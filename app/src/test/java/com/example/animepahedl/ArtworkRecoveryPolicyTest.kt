package com.example.animepahedl

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class ArtworkRecoveryPolicyTest {
    @Test fun recoveryAllowsOnlyHttpsMoviesSourceUrlsWithoutCredentials() {
        assertTrue(isAllowedArtworkRecoveryUrl("https://eroticmv.com/movie/example".toHttpUrl()))
        assertTrue(isAllowedArtworkRecoveryUrl("https://www.eroticmv.com/movie/example".toHttpUrl()))
        assertFalse(isAllowedArtworkRecoveryUrl("http://eroticmv.com/movie/example".toHttpUrl()))
        assertFalse(isAllowedArtworkRecoveryUrl("https://dramatip.net/series/example".toHttpUrl()))
        assertFalse(isAllowedArtworkRecoveryUrl("https://evil.test/".toHttpUrl()))
        assertFalse(isAllowedArtworkRecoveryUrl("https://eroticmv.com.evil.test/".toHttpUrl()))
        assertFalse(isAllowedArtworkRecoveryUrl("https://user@eroticmv.com/".toHttpUrl()))
    }

    @Test fun recoveryRejectsRedirectsOutsideMoviesHostsBeforeRequestingThem() {
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("eroticmv.com").build()
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setKeyEntry("artwork", certificate.keyPair.private, "password".toCharArray(), arrayOf(certificate.certificate))
        }
        val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            .apply { init(keyStore, "password".toCharArray()) }.keyManagers
        val trustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        }
        val ssl = SSLContext.getInstance("TLS").apply {
            init(keyManagers, arrayOf(trustManager), SecureRandom())
        }.socketFactory
        val server = MockWebServer().apply { useHttps(ssl, false) }
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(302)
                .setHeader("Location", "https://evil.test:${server.port}/artwork"))
            val client = OkHttpClient.Builder().sslSocketFactory(ssl, trustManager)
                .hostnameVerifier { _, _ -> true }
                .dns(Dns { InetAddress.getAllByName("127.0.0.1").toList() })
                .addNetworkInterceptor(artworkRecoveryHostPolicy()).build()

            val failure = runCatching {
                client.newCall(Request.Builder().url("https://eroticmv.com:${server.port}/movie").build()).execute()
            }.exceptionOrNull()

            assertTrue(failure is java.io.IOException)
            assertEquals("Artwork redirect left the Movies source.", failure?.message)
            assertEquals(1, server.requestCount)
        } finally { server.shutdown() }
    }
}
