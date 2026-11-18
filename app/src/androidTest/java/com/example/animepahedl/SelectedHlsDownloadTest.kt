package com.example.animepahedl

import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executor

@androidx.annotation.OptIn(UnstableApi::class)
class SelectedHlsDownloadTest {
    @Test fun downloadsOnlyChosenQualityAndDefaultAudioAfterOneManifestRequest() {
        val gateway = "https://fixture.invalid/gateway"
        val cdn = "https://fixture.invalid/cdn/"
        val master = """#EXTM3U
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Alternate",URI="alternate.m3u8"
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Original",DEFAULT=YES,URI="audio.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=400000,RESOLUTION=640x360,AUDIO="audio"
            low.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=1000000,RESOLUTION=1920x1080,AUDIO="audio"
            best.m3u8
        """.trimIndent()
        val resources = mapOf(
            gateway to master.toByteArray(),
            "${cdn}best.m3u8" to mediaPlaylist("video.ts").toByteArray(),
            "${cdn}audio.m3u8" to mediaPlaylist("audio.ts").toByteArray(),
            "${cdn}video.ts" to ByteArray(500),
            "${cdn}audio.ts" to ByteArray(80),
        )
        assertEquals(resources.keys, download(gateway, "${cdn}master.m3u8", resources).toSet())
    }

    @Test fun redirectedSinglePlaylistDoesNotReopenOneTimeMediaUrl() {
        val gateway = "https://fixture.invalid/gateway"
        val cdn = "https://fixture.invalid/cdn/"
        val resources = mapOf(gateway to mediaPlaylist("video.ts").toByteArray(), "${cdn}video.ts" to ByteArray(500))
        assertEquals(resources.keys, download(gateway, "${cdn}media.m3u8", resources).toSet())
    }

    @Test fun renewalRereadsSameMasterAndChildWithoutDroppingCachedSegments() {
        val root = "https://fixture.invalid/master.m3u8"
        val child = "https://fixture.invalid/media.m3u8"
        val previous = "https://fixture.invalid/old.ts"
        val replacement = "https://fixture.invalid/new.ts"
        val master = "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1000000,RESOLUTION=1920x1080\nmedia.m3u8\n".toByteArray()
        val initial = mapOf(root to master, child to mediaPlaylist("old.ts").toByteArray(), previous to ByteArray(500))
        val renewed = mapOf(root to master, child to mediaPlaylist("new.ts").toByteArray(), replacement to ByteArray(700))
        val requests = download(root, root, initial, renewed)
        assertEquals(initial.keys, requests.take(3).toSet())
        assertEquals(renewed.keys, requests.drop(3).toSet())
    }

    private fun mediaPlaylist(segment: String) = "#EXTM3U\n#EXT-X-TARGETDURATION:4\n#EXTINF:4,\n$segment\n#EXT-X-ENDLIST\n"

    private fun download(gateway: String, redirect: String, resources: Map<String, ByteArray>,
        renewed: Map<String, ByteArray> = emptyMap()): List<String> {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(app.cacheDir, "hls-selection-${UUID.randomUUID()}")
        val cache = SimpleCache(directory, NoOpCacheEvictor(), StandaloneDatabaseProvider(app))
        val prefix = "offline/fixture/"
        val requests = mutableListOf<String>()
        var available = resources
        var openedRoot = false
        try {
            val upstream = DataSource.Factory {
                object : DataSource {
                    private var data: ByteArrayDataSource? = null
                    private var current: Uri? = null
                    override fun addTransferListener(transferListener: TransferListener) = Unit
                    override fun open(dataSpec: DataSpec): Long {
                        val url = dataSpec.uri.toString()
                        if (url == gateway) {
                            if (openedRoot) throw IOException("One-time URL already opened")
                            openedRoot = true
                        }
                        requests += url
                        val payload = available[url] ?: throw IOException("Unexpected HLS request: $url")
                        current = if (url == gateway) Uri.parse(redirect) else dataSpec.uri
                        val source = ByteArrayDataSource(payload)
                        data = source
                        return source.open(dataSpec)
                    }
                    override fun getUri(): Uri? = current
                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = requireNotNull(data).read(buffer, offset, length)
                    override fun close() { data?.close(); data = null; current = null }
                }
            }
            val request = DownloadRequest.Builder("fixture", Uri.parse(gateway)).setMimeType(MimeTypes.APPLICATION_M3U8).build()
            fun runTransfer() {
                val factory = offlineCacheFactory(cache, prefix).setUpstreamDataSourceFactory(upstream)
                selectedHlsDownloader(request, "Best", factory, Executor { it.run() }, prefix).download(null)
            }
            runTransfer()
            assertEquals(1, requests.count { it == gateway })
            assertTrue(cache.keys.all { it.startsWith(prefix) })
            if (renewed.isNotEmpty()) {
                val oldSegments = cache.keys.filter { it.endsWith(".ts") }
                assertTrue(oldSegments.isNotEmpty())
                resetCachedHlsPlaylists(cache, request.uri, prefix)
                assertTrue(cache.keys.containsAll(oldSegments))
                available = renewed
                openedRoot = false
                runTransfer()
                assertEquals(2, requests.count { it == gateway })
                assertTrue(cache.keys.containsAll(oldSegments))
            }
            return requests
        } finally {
            cache.release()
            directory.deleteRecursively()
        }
    }
}
