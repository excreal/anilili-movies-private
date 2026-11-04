package com.example.animepahedl

import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class CacheServerTest {
    @Test fun oversizedUnauthenticatedRequestIsRejectedWithoutBreakingExports() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { OfflineDownloads.manager(instrumentation.targetContext) }
        CachedHlsServer().use { server ->
            val address = URL(server.url("https://fixture.invalid/none"))
            java.net.Socket("127.0.0.1", address.port).use { socket ->
                socket.soTimeout = 2000
                runCatching {
                    socket.getOutputStream().write(("GET /" + "x".repeat(16384) + " HTTP/1.1\r\n\r\n").toByteArray())
                    val response = socket.getInputStream().bufferedReader().readLine()
                    assertFalse(response.orEmpty().contains("200 OK"))
                }.onFailure { assertTrue(it is java.io.IOException) }
            }
            val connection = address.openConnection() as HttpURLConnection
            connection.connectTimeout = 2000; connection.readTimeout = 2000
            try { assertEquals(404, connection.responseCode) } finally { connection.disconnect() }
        }
    }
    @Test fun binaryResourcesStreamRangesAndShutdownClosesTheListener() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { OfflineDownloads.manager(instrumentation.targetContext) }
        val key = "https://fixture.invalid/server-${System.nanoTime()}.ts"
        val bytes = ByteArray(4 * 1024 * 1024) { (it % 251).toByte() }
        CacheWriter(CacheDataSource.Factory().setCache(OfflineDownloads.cache)
            .setUpstreamDataSourceFactory { ByteArrayDataSource(bytes) }.createDataSource(), DataSpec.Builder().setUri(key).build(), null, null).cache()
        val server = CachedHlsServer()
        val url = server.url(key)
        fun request(range: String?): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 2000; readTimeout = 2000
            range?.let { setRequestProperty("Range", it) }
        }
        try {
            request("bytes=100-999").let { connection ->
                try { assertEquals(206, connection.responseCode); assertArrayEquals(bytes.copyOfRange(100, 1000), connection.inputStream.readBytes()) }
                finally { connection.disconnect() }
            }
            request("bytes=-32").let { connection ->
                try { assertEquals(206, connection.responseCode); assertArrayEquals(bytes.takeLast(32).toByteArray(), connection.inputStream.readBytes()) }
                finally { connection.disconnect() }
            }
            request("bytes=${bytes.size}-").let { connection -> try { assertEquals(416, connection.responseCode) } finally { connection.disconnect() } }
            server.close()
            try { request(null).inputStream.close(); fail("Listener remained open") } catch (_: java.io.IOException) { }
        } finally { server.close(); OfflineDownloads.cache.removeResource(key) }
    }
}
