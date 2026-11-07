package com.example.animepahedl

import androidx.core.net.toUri
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ProgressiveExportTest {
    @Test fun completedProgressiveCacheIsCopiedByteForByteWithoutNetwork() = runBlocking {
        withCache { cache, output ->
            val bytes = ByteArray(16_384) { (it % 251).toByte() }
            val request = DownloadRequest.Builder("sites:test", "https://unreachable.invalid/video.mp4".toUri()).build()
            write(cache, request, bytes)
            var copied = 0L
            copyCachedProgressive(cache, request, bytes.size.toLong(), output) { copied = it }
            assertArrayEquals(bytes, output.readBytes())
            assertEquals(bytes.size.toLong(), copied)
        }
    }

    @Test fun missingCacheBytesFailInsteadOfFetchingOrAcceptingPartialOutput() = runBlocking {
        withCache { cache, output ->
            val request = DownloadRequest.Builder("sites:partial", "https://unreachable.invalid/video.mp4".toUri()).build()
            write(cache, request, ByteArray(128) { 7 })
            var failed = false
            try { copyCachedProgressive(cache, request, 256, output) {} }
            catch (_: java.io.IOException) { failed = true }
            assertTrue("A cache hole must fail without a network request", failed)
        }
    }

    private fun write(cache: SimpleCache, request: DownloadRequest, bytes: ByteArray) {
        val sink = CacheDataSink.Factory().setCache(cache).createDataSink()
        sink.open(DataSpec.Builder().setUri(request.uri).setKey(request.uri.toString()).setLength(bytes.size.toLong()).build())
        try { sink.write(bytes, 0, bytes.size) } finally { sink.close() }
    }

    private suspend fun withCache(block: suspend (SimpleCache, File) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "progressive-export-test-${UUID.randomUUID()}").apply { mkdirs() }
        val cache = SimpleCache(File(root, "cache"), NoOpCacheEvictor(), StandaloneDatabaseProvider(context))
        try { block(cache, File(root, "output.mp4")) }
        finally { cache.release(); root.deleteRecursively() }
    }
}
