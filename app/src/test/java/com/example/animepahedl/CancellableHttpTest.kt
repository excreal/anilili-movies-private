package com.example.animepahedl

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CancellableHttpTest {
    @Test fun captionBodyReadIsCancelledWithItsCoroutine() = runBlocking {
        ServerSocket(0).use { server ->
            val bodyStarted = CountDownLatch(1)
            val peerClosed = CountDownLatch(1)
            val thread = Thread {
                server.accept().use { socket ->
                    val input = socket.getInputStream().bufferedReader()
                    while (!input.readLine().isNullOrEmpty()) { }
                    socket.getOutputStream().apply {
                        write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\nx".toByteArray()); flush()
                    }
                    bodyStarted.countDown()
                    if (socket.getInputStream().read() == -1) peerClosed.countDown()
                }
            }.apply { start() }
            val client = OkHttpClient()
            val request = Request.Builder().url("http://127.0.0.1:${server.localPort}/caption.vtt").build()
            val job = launch(Dispatchers.IO) { fetchProviderSubtitleBytes(client, request, "English") }
            try {
                assertTrue(bodyStarted.await(5, TimeUnit.SECONDS))
                withTimeout(2000) { job.cancelAndJoin() }
                assertTrue("Cancelling caption load should close its HTTP socket", peerClosed.await(2, TimeUnit.SECONDS))
            } finally {
                job.cancel()
                server.close()
                thread.join(2000)
                client.dispatcher.executorService.shutdownNow()
                client.connectionPool.evictAll()
            }
        }
    }

    @Test fun cancellationClosesAnHttpCallWhileItsBodyIsBlocked() = runBlocking {
        ServerSocket(0).use { server ->
            val bodyStarted = CountDownLatch(1)
            val finished = CountDownLatch(1)
            val thread = Thread {
                server.accept().use { socket ->
                    val input = socket.getInputStream().bufferedReader()
                    while (!input.readLine().isNullOrEmpty()) { }
                    socket.getOutputStream().apply {
                        write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\nx".toByteArray()); flush()
                    }
                    bodyStarted.countDown()
                    finished.await(5, TimeUnit.SECONDS)
                }
            }.apply { start() }
            val client = OkHttpClient()
            val call = client.newCall(Request.Builder().url("http://127.0.0.1:${server.localPort}/").build())
            val job = launch(Dispatchers.IO) { call.consumeCancellable { it.body.string() } }
            try {
                assertTrue(bodyStarted.await(5, TimeUnit.SECONDS))
                withTimeout(2000) { job.cancelAndJoin() }
                assertTrue(call.isCanceled())
            } finally { finished.countDown(); thread.join(2000); client.dispatcher.executorService.shutdownNow(); client.connectionPool.evictAll() }
        }
    }
}
