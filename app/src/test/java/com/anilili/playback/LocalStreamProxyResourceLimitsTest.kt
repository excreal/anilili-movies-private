package com.anilili.playback

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LocalStreamProxyResourceLimitsTest {
    @Test
    fun saturatedRequestPoolClosesNewlyAcceptedSocket() {
        val executor = newBoundedProxyRequestExecutor(workerCount = 1, queueCapacity = 1)
        val running = CountDownLatch(1)
        val release = CountDownLatch(1)
        try {
            assertEquals(1, executor.maximumPoolSize)
            assertEquals(1, executor.queue.remainingCapacity())
            executor.execute {
                running.countDown()
                release.await()
            }
            assertTrue(running.await(2, TimeUnit.SECONDS))
            executor.execute { } // Fill the only queue slot while the only worker is occupied.

            val rejected = Socket()
            dispatchAcceptedSocket(executor, rejected) {
                throw AssertionError("Rejected socket must not be dispatched")
            }

            assertTrue(rejected.isClosed)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun headerFloodIsRejectedByTotalByteAndLineCaps() {
        val byteFlood = buildString {
            append("GET /token HTTP/1.1\r\n")
            repeat(5) { append("X-$it: ").append("a".repeat(7_000)).append("\r\n") }
            append("\r\n")
        }
        val lineFlood = buildString {
            append("GET /token HTTP/1.1\r\n")
            repeat(100) { append("X-$it: a\r\n") }
            append("\r\n")
        }

        assertNull(readRequest(ByteArrayInputStream(byteFlood.toByteArray())))
        assertNull(readRequest(ByteArrayInputStream(lineFlood.toByteArray())))
    }

    @Test
    fun incompleteRequestLineOrHeaderWithoutBlankTerminatorIsRejected() {
        assertNull(readRequest(ByteArrayInputStream("GET /token HTTP/1.1".toByteArray())))
        assertNull(readRequest(ByteArrayInputStream(
            "GET /token HTTP/1.1\r\nRange: bytes=0-1\r\n".toByteArray(),
        )))
        assertNull(readRequest(ByteArrayInputStream(
            "GET /token HTTP/1.1\r\nRange: bytes=0-1".toByteArray(),
        )))
    }

    @Test
    fun upstreamHttpUserinfoIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            requireNoHttpUserInfo("https://user:secret@cdn.example/video.m3u8")
        }
        requireNoHttpUserInfo("https://cdn.example/video.m3u8")
    }

    @Test
    fun boundedRegistryReusesDuplicatesAndRejectsCapacityWithoutEviction() {
        var nextToken = 0
        val registry = BoundedUrlTokenRegistry<String>(maxEntries = 2, maxApproximateBytes = 20) {
            "token-${++nextToken}"
        }
        val first = registry.register("url-a", 8)
        assertEquals(first, registry.register("url-a", 15))
        assertEquals(1, registry.size())
        assertEquals(8L, registry.approximateBytes())

        val second = registry.register("url-b", 8)
        assertThrows(UrlRegistryCapacityException::class.java) { registry.register("url-c", 1) }
        assertEquals(2, registry.size())
        assertEquals("url-a", registry.get(first))
        assertEquals("url-b", registry.get(second))
        assertEquals(16L, registry.approximateBytes())

        val byteLimited = BoundedUrlTokenRegistry<String>(maxEntries = 10, maxApproximateBytes = 5) { "byte-token" }
        byteLimited.register("fits", 4)
        assertThrows(UrlRegistryCapacityException::class.java) { byteLimited.register("too-large", 2) }
        assertEquals(1, byteLimited.size())
        assertEquals(4L, byteLimited.approximateBytes())
    }

    @Test
    fun failedPlaylistRegistryTransactionRollsBackNewEntries() {
        var nextToken = 0
        val registry = BoundedUrlTokenRegistry<String>(maxEntries = 2, maxApproximateBytes = 50) {
            "token-${++nextToken}"
        }
        val issued = registry.register("issued", 5)

        assertThrows(UrlRegistryCapacityException::class.java) {
            registry.transaction { register ->
                register("temporary", 5)
                register("overflow", 5)
            }
        }

        assertEquals(1, registry.size())
        assertEquals("issued", registry.get(issued))
    }

    @Test
    fun shutdownCancelsActiveOkHttpCallAndClosesAcceptedSocket() {
        val server = MockWebServer()
        var peer: Socket? = null
        var listener: ServerSocket? = null
        server.start()
        try {
            server.enqueue(MockResponse().setHeadersDelay(30, TimeUnit.SECONDS))
            val call = OkHttpClient().newCall(Request.Builder().url(server.url("/transfer")).build())
            val callbackDone = CountDownLatch(1)
            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, error: java.io.IOException) {
                    callbackDone.countDown()
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.close()
                    callbackDone.countDown()
                }
            })
            assertTrue(server.takeRequest(2, TimeUnit.SECONDS) != null)

            val socketListener = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
            listener = socketListener
            peer = Socket(InetAddress.getLoopbackAddress(), socketListener.localPort)
            val acceptedSocket = socketListener.accept()
            val active = ProxyActiveResources().apply {
                trackCall(call)
                trackSocket(acceptedSocket)
            }
            stopProxyResources(active.takeAll())

            assertTrue(call.isCanceled())
            assertTrue(acceptedSocket.isClosed)
            assertTrue(callbackDone.await(2, TimeUnit.SECONDS))
        } finally {
            peer?.close()
            listener?.close()
            server.shutdown()
        }
    }
}
