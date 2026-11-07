package com.example.animepahedl

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.WritableDownloadIndex
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.Assume.assumeTrue
import java.io.File

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class HlsExportTest {
    @Test fun cleanupInterruptedTestFixtures() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        instrumentation.runOnMainSync { OfflineDownloads.manager(app) }
        val manager = OfflineDownloads.manager(app)
        manager.downloadIndex.getDownloads().use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.download.request.id
                if (id.startsWith("test-hls-export-")) {
                    Exports.remove(app, id)
                    (manager.downloadIndex as WritableDownloadIndex).removeDownload(id)
                }
            }
        }
        Exports.all(app).keys.filter { it.startsWith("test-hls-export-") }.forEach { Exports.remove(app, it) }
    }

    @Test fun cachedHlsBecomesPlayableMp4AndTemporaryMediaIsRemoved() = verifyExport(1)

    @Test fun missingSegmentRetainsCachedMediaForRetry() = verifyExport(1, expectIncomplete = true)

    @Test fun missingEncryptionKeyRetainsCachedMediaForRetry() = verifyExport(1, assetFolder = "hls-encrypted", expectIncomplete = true, missingResource = "key.bin")

    @Test fun ffmpegOutputPreservesTracksAndDuration() = verifyExport(1, directAssembly = true)

    /** Optional device regression: remux a retained download without modifying its record or cache. */
    @Test fun retainedEpisodeRemuxesWithCompleteSegments() {
        val id = InstrumentationRegistry.getArguments().getString("retainedDownloadId")
        assumeTrue("Supply retainedDownloadId to inspect an existing cached episode", !id.isNullOrBlank())
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        instrumentation.runOnMainSync { OfflineDownloads.manager(app) }
        kotlinx.coroutines.runBlocking { DownloadStore.awaitReady() }
        val keepOutput = InstrumentationRegistry.getArguments().getString("keepRetainedExport") == "true"
        val output = File(app.getExternalFilesDir(null), "retained-episode-validation.mp4")
        try {
            kotlinx.coroutines.runBlocking { ffmpegRemuxCachedHls(app, id!!, output) {} }
        } finally { if (!keepOutput) output.delete() }
    }


    @Test fun timestampResetsAcrossDiscontinuitiesKeepEverySegment() = verifyExport(2, repeatPlaylist = true)

    @Test fun encryptedHlsExportsWithoutDecoding() = verifyExport(1, assetFolder = "hls-encrypted")

    @Test fun shorterDubbedAudioDoesNotRejectCompleteVideo() = verifyExport(1, assetFolder = "hls-short-audio")

    @Test fun fullLengthEpisodeExportsWithoutRepeatedSeekTableRebuilds() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = File(app.getExternalFilesDir(null), "benchmark-hls")
        assumeTrue("Generate the optional 24-minute benchmark fixture first", File(fixture, "index.m3u8").exists())
        verifyExport(480, fixture)
    }

    /** Writes the benchmark export to external storage for container inspection. */
    @Test fun inspectBenchmarkExport() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        val fixture = File(app.getExternalFilesDir(null), "benchmark-hls")
        assumeTrue(File(fixture, "index.m3u8").exists())
        val id = "test-hls-export-inspect-${System.currentTimeMillis()}"
        val base = "https://fixture.invalid/$id/"
        instrumentation.runOnMainSync { OfflineDownloads.manager(app) }
        val cache = OfflineDownloads.cache
        DownloadStore.operation(id, "delete", "Benchmark owns this fixture")
        try {
            fixture.list()!!.forEach { name ->
                val bytes = fixture.resolve(name).readBytes()
                val key = base + name
                val dataSource = CacheDataSource.Factory().setCache(cache)
                    .setUpstreamDataSourceFactory { ByteArrayDataSource(bytes) }.createDataSource()
                CacheWriter(dataSource, DataSpec.Builder().setUri(key).build(), null, null).cache()
            }
            val request = DownloadRequest.Builder(id, Uri.parse(base + "index.m3u8"))
                .setMimeType(MimeTypes.APPLICATION_M3U8).setData("Inspect".toByteArray()).build()
            (OfflineDownloads.manager(app).downloadIndex as WritableDownloadIndex).putDownload(
                Download(request, Download.STATE_COMPLETED, 0, 0, -1, 0, 0))
            val output = File(app.getExternalFilesDir(null), "benchmark-export.mp4")
            output.delete()
            val started = android.os.SystemClock.elapsedRealtime()
            kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    ffmpegRemuxCachedHls(app, id, output) {}
                }
            }
            android.util.Log.i("HlsExportBenchmark", "inspect elapsed_ms=${android.os.SystemClock.elapsedRealtime() - started} bytes=${output.length()}")
        } finally {
            DownloadStore.operation(id, "delete", null)
            (OfflineDownloads.manager(app).downloadIndex as WritableDownloadIndex).removeDownload(id)
            fixture.list()!!.forEach { cache.removeResource(base + it) }
        }
    }

    private fun verifyExport(repetitions: Int, fixture: File? = null, assetFolder: String = "hls-fixture", expectIncomplete: Boolean = false, repeatPlaylist: Boolean = false, directAssembly: Boolean = false, missingResource: String? = null) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        val id = "test-hls-export-${System.currentTimeMillis()}"
        val base = "https://fixture.invalid/$id/"
        instrumentation.runOnMainSync { OfflineDownloads.manager(app) }
        val cache = OfflineDownloads.cache
        val assets = instrumentation.context.assets
        val resources = fixture?.list()?.toList() ?: assets.list(assetFolder)!!.toList()
        val earlyWatchdog = java.util.Timer(true)
        earlyWatchdog.schedule(object : java.util.TimerTask() {
            override fun run() {
                File(app.cacheDir, "export-test-stacks.txt").writeText(Thread.getAllStackTraces().entries.joinToString("\n\n") { (thread, stack) -> thread.name + " " + thread.state + "\n" + stack.joinToString("\n") })
            }
        }, 10000, 10000)
        try {
            resources.forEach { name ->
                val bytes = fixture?.resolve(name)?.readBytes() ?: assets.open("$assetFolder/$name").use { it.readBytes() }
                val cachedBytes = when {
                    repeatPlaylist && name == "index.m3u8" -> {
                        val body = String(bytes).substringAfter("#EXT-X-PLAYLIST-TYPE:VOD\n").substringBefore("#EXT-X-ENDLIST")
                        (String(bytes).substringBefore("#EXTINF:") +
                            List(repetitions) { body }.joinToString("#EXT-X-DISCONTINUITY\n") + "#EXT-X-ENDLIST\n").toByteArray()
                    }
                    else -> bytes
                }
                val key = base + name
                val dataSource = CacheDataSource.Factory().setCache(cache)
                    .setUpstreamDataSourceFactory { ByteArrayDataSource(cachedBytes) }.createDataSource()
                CacheWriter(dataSource, DataSpec.Builder().setUri(key).build(), null, null).cache()
            }
            val request = DownloadRequest.Builder(id, Uri.parse(base + "index.m3u8"))
                .setMimeType(MimeTypes.APPLICATION_M3U8).setData("Export test".toByteArray()).build()
            if (directAssembly) DownloadStore.operation(id, "delete", "Test owns this cached fixture")
            (OfflineDownloads.manager(app).downloadIndex as WritableDownloadIndex).putDownload(
                Download(request, Download.STATE_COMPLETED, 0, 0, -1, 0, 0))
            if (!expectIncomplete && !directAssembly) Exports.put(app, id, ExportRecord("Fixture Show · Episode 1 · 180p · JPN", "", "Animepahe-test", quality = "180p", audio = "jpn", showTitle = "Fixture Show", episodeNumber = "1"))
            if (directAssembly) {
                val output = File(app.cacheDir, "$id-remux.mp4")
                try {
                    kotlinx.coroutines.runBlocking { ffmpegRemuxCachedHls(app, id, output) {} }
                    assertTrue(output.length() > 0)
                    assertTrue(cache.keys.any { it.startsWith(base) })
                } finally { output.delete() }
                return
            }
            if (expectIncomplete) {
                cache.removeResource(base + (missingResource ?: resources.first { it.endsWith(".ts") }))
                val failure = runCatching { requireCachedHlsSegments(app, id) }.exceptionOrNull()
                assertTrue("Missing segment must fail validation: $failure", failure?.message?.contains("missing or incomplete") == true)
                assertTrue("Source must remain available for Retry save", cache.keys.any { it.startsWith(base) })
                assertEquals(Download.STATE_COMPLETED, OfflineDownloads.manager(app).downloadIndex.getDownload(id)?.state)
                assertTrue("Invalid output must not be published", Exports.all(app)[id]?.uri.isNullOrEmpty())
                return
            }
            val watchdog = java.util.Timer(true)
            watchdog.schedule(object : java.util.TimerTask() {
                override fun run() {
                    val stacks = Thread.getAllStackTraces().filter { (thread, _) ->
                        thread.name == "main" || thread.name.contains("Transformer") || thread.name.contains("ExoPlayer") || thread.name.contains("Instr")
                    }.entries.joinToString("\n\n") { (thread, stack) -> thread.name + " " + thread.state + "\n" + stack.joinToString("\n") }
                    File(app.getExternalFilesDir(null), "export-stacks.txt").writeText(stacks)
                }
            }, 10000, 10000)
            val started = android.os.SystemClock.elapsedRealtime()
            instrumentation.runOnMainSync { Exports.start(app) }
            val deadline = System.currentTimeMillis() + if (fixture != null) 180000 else 60000
            var record: ExportRecord? = null
            while (System.currentTimeMillis() < deadline) {
                record = Exports.all(app)[id]
                if (record?.uri?.isNotEmpty() == true || record?.error?.isNotEmpty() == true) break
                Thread.sleep(200)
            }
            watchdog.cancel()
            android.util.Log.i("HlsExportBenchmark", "duration_seconds=${3 * repetitions} elapsed_ms=${android.os.SystemClock.elapsedRealtime() - started} bytes=${record?.bytes}")
            assertEquals("Export failed: ${record?.error}", "", record?.error)
            assertTrue("Output URI missing", record?.uri?.isNotEmpty() == true)
            assertTrue(record!!.bytes > 0)
            val sourceBytes = resources.filter { it.endsWith(".ts") }.sumOf { name ->
                fixture?.resolve(name)?.length() ?: assets.open("$assetFolder/$name").use { it.readBytes().size.toLong() }
            }
            if (sourceBytes > 64L * 1024 * 1024) assertTrue("Large export lost media payload: ${record.bytes} of $sourceBytes", record.bytes > sourceBytes * 0.85)
            app.contentResolver.query(Uri.parse(record.uri), arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
                android.provider.MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)!!.use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Fixture Show ep01 180p JPN.mp4", cursor.getString(0))
                assertTrue(cursor.getString(1).endsWith("Animepahe-test/Fixture Show/"))
            }
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(app, Uri.parse(record.uri))
                val reportedDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
                assertTrue(reportedDuration == 0L || reportedDuration >= repetitions * 3000L - 500)

                assertEquals("yes", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO))
                assertEquals("yes", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
                val frame = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                assertNotNull("Exported video should decode", frame)
                frame?.recycle()
            } finally { retriever.release() }
            val cleanupDeadline = System.currentTimeMillis() + 5000
            while (cache.keys.any { it.startsWith(base) } && System.currentTimeMillis() < cleanupDeadline) Thread.sleep(100)
            assertFalse("Temporary segments should be removed", cache.keys.any { it.startsWith(base) })
        } finally {
            earlyWatchdog.cancel()
            DownloadStore.operation(id, "delete", null)
            Exports.all(app)[id]?.uri?.takeIf { it.isNotEmpty() }?.let { app.contentResolver.delete(Uri.parse(it), null, null) }
            Exports.remove(app, id)
            (OfflineDownloads.manager(app).downloadIndex as WritableDownloadIndex).removeDownload(id)
            resources.forEach { cache.removeResource(base + it) }
        }
    }
}
