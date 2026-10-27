package com.example.animepahedl

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.util.Clock
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.muxer.Mp4Muxer
import androidx.media3.muxer.Muxer
import androidx.media3.transformer.*
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Standard MP4 keeps a real duration and sample tables so hardware players can seek cleanly. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class ExportMuxerFactory(
    private val fragmented: Boolean = false,
    private val openOutput: (String) -> FileOutputStream = ::FileOutputStream
) : Muxer.Factory {
    override fun create(path: String): Muxer {
        val stream = openOutput(path)
        return if (fragmented)
            androidx.media3.muxer.FragmentedMp4Muxer.Builder(stream).setFragmentDurationMs(2000).build()
        else Mp4Muxer.Builder(stream).setAttemptStreamableOutputEnabled(false).build()
    }
    override fun getSupportedSampleMimeTypes(trackType: Int): ImmutableList<String> =
        InAppMp4Muxer.Factory().getSupportedSampleMimeTypes(trackType)
}

internal class ExportWouldReencodeException(message: String) : java.io.IOException(message)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal suspend fun remuxCachedHls(context: Context, id: String, outputPath: String,
    muxerFactory: Muxer.Factory = ExportMuxerFactory(), outputBytes: () -> Long = { File(outputPath).length() },
    onProgress: (Int) -> Unit = {}) {
    val download = OfflineDownloads.manager(context).downloadIndex.getDownload(id) ?: error("Temporary download is unavailable")
    if (kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            DirectHlsRemuxer.remux(context, download, outputPath, muxerFactory, onProgress)
        }) {
        DebugTrace.event("export_container", "mode" to "direct")
        return
    }
    DebugTrace.event("export_container", "mode" to "transformer")
    suspendCancellableCoroutine<Unit> { continuation ->
        val cache = CacheDataSource.Factory().setCache(OfflineDownloads.cache).setUpstreamDataSourceFactory(null)
        val sources = DefaultMediaSourceFactory(cache)
        // Transformer consumes audio/video only. Do not select HLS text/metadata tracks
        // for which its asset loader has no renderer to drain the sample queue.
        val exportSources = object : androidx.media3.exoplayer.source.MediaSource.Factory by sources {
            override fun createMediaSource(item: androidx.media3.common.MediaItem): androidx.media3.exoplayer.source.MediaSource =
                androidx.media3.exoplayer.source.FilteringMediaSource(sources.createMediaSource(item),
                    setOf(androidx.media3.common.C.TRACK_TYPE_AUDIO, androidx.media3.common.C.TRACK_TYPE_VIDEO))
        }
        val loader = DefaultAssetLoaderFactory(context, DefaultDecoderFactory.Builder(context).build(), Clock.DEFAULT,
            exportSources, DataSourceBitmapLoader(context))
        val handler = Handler(Looper.getMainLooper())
        val started = android.os.SystemClock.elapsedRealtime()
        lateinit var poll: Runnable
        val transformer = Transformer.Builder(context).setMuxerFactory(muxerFactory).setAssetLoaderFactory(loader)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    handler.removeCallbacks(poll)
                    val videoEncoded = result.videoEncoderName != null || result.videoConversionProcess in setOf(
                        ExportResult.CONVERSION_PROCESS_TRANSCODED,
                        ExportResult.CONVERSION_PROCESS_TRANSMUXED_AND_TRANSCODED)
                    val audioEncoded = result.audioEncoderName != null || result.audioConversionProcess in setOf(
                        ExportResult.CONVERSION_PROCESS_TRANSCODED,
                        ExportResult.CONVERSION_PROCESS_TRANSMUXED_AND_TRANSCODED)
                    DebugTrace.event("export_remux_complete", "elapsed_ms" to android.os.SystemClock.elapsedRealtime() - started,
                        "bytes" to outputBytes(), "video_encoder" to result.videoEncoderName, "audio_encoder" to result.audioEncoderName,
                        "video_conversion" to result.videoConversionProcess, "audio_conversion" to result.audioConversionProcess,
                        "video_encoded" to videoEncoded, "audio_encoded" to audioEncoded)
                    if (continuation.isActive) {
                        if (videoEncoded || audioEncoded) continuation.resumeWithException(
                            ExportWouldReencodeException("This source requires re-encoding. No MP4 was saved; the downloaded HLS source was retained."))
                        else continuation.resume(Unit)
                    }
                }
                override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                    handler.removeCallbacks(poll)
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
            }).build()
        val progress = ProgressHolder()
        poll = Runnable {
            if (continuation.isActive) {
                if (transformer.getProgress(progress) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(progress.progress)
                handler.postDelayed(poll, 250)
            }
        }
        continuation.invokeOnCancellation { handler.post { handler.removeCallbacks(poll); transformer.cancel() } }
        transformer.start(download.request.toMediaItem(), outputPath)
        handler.post(poll)
    }
}

/** Each writer gets a bounded attempt; validation always finishes before publishing or source cleanup. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal suspend fun assembleCachedHls(context: Context, id: String, output: File,
    primaryMuxer: Muxer.Factory = ExportMuxerFactory(), onStage: (String) -> Unit = {}, onProgress: (Int) -> Unit = {}) {
    assembleCachedHlsInternal(context, id, output.absolutePath, primaryMuxer, ExportMuxerFactory(true),
        clearOutput = { output.delete() }, outputBytes = { output.length() },
        validate = { validateCachedHlsExport(context, id, output) }, onStage = onStage, onProgress = onProgress)
}

/** Writes the muxed MP4 directly to the pending MediaStore/SAF document. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal suspend fun assembleCachedHlsToDocument(context: Context, id: String, outputPath: String,
    openOutput: () -> FileOutputStream, clearOutput: () -> Unit, outputBytes: () -> Long,
    validate: () -> Unit, onStage: (String) -> Unit = {}, onProgress: (Int) -> Unit = {}) {
    assembleCachedHlsInternal(context, id, outputPath,
        ExportMuxerFactory(false) { openOutput() }, ExportMuxerFactory(true) { openOutput() },
        clearOutput, outputBytes, validate, onStage, onProgress)
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private suspend fun assembleCachedHlsInternal(context: Context, id: String, outputPath: String,
    primaryMuxer: Muxer.Factory, fallbackMuxer: Muxer.Factory, clearOutput: () -> Unit,
    outputBytes: () -> Long, validate: () -> Unit, onStage: (String) -> Unit, onProgress: (Int) -> Unit) {
    suspend fun attempt(factory: Muxer.Factory) {
        try {
            kotlinx.coroutines.withTimeout(5 * 60_000L) {
                remuxCachedHls(context, id, outputPath, factory, outputBytes, onProgress)
                onStage("Checking MP4")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { validate() }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw java.io.IOException("MP4 preparation timed out; cached media retained. Retry save.", e)
        }
    }
    try {
        attempt(primaryMuxer)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: ExportWouldReencodeException) {
        clearOutput()
        throw e
    } catch (first: Exception) {
        DebugTrace.event("export_muxer_fallback", "reason" to first.message)
        clearOutput()
        onStage("Retrying MP4")
        attempt(fallbackMuxer)
    }
}

/** Check against the playlist, not the exporter's success callback: a short file must never evict the source. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun validateCachedHlsExport(context: Context, id: String, output: File) =
    validateCachedHlsExport(context, id, { it.setDataSource(output.absolutePath) },
        { expected -> validateMp4Tracks(output, expected) })

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun validateCachedHlsExport(context: Context, id: String, output: android.net.Uri) =
    validateCachedHlsExport(context, id, { it.setDataSource(context, output) },
        { expected -> validateMp4Tracks(context, output, expected) })

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun validateCachedHlsExport(context: Context, id: String,
    setMetadataSource: (android.media.MediaMetadataRetriever) -> Unit,
    validateTracks: (Long) -> Unit) {
    val download = OfflineDownloads.manager(context).downloadIndex.getDownload(id)
        ?: error("Temporary download is unavailable")
    val cache = CacheDataSource.Factory().setCache(OfflineDownloads.cache).setUpstreamDataSourceFactory(null)
    fun playlist(uri: android.net.Uri): androidx.media3.exoplayer.hls.playlist.HlsPlaylist =
        androidx.media3.datasource.DataSourceInputStream(cache.createDataSource(),
            androidx.media3.datasource.DataSpec.Builder().setUri(uri).build()).use {
            androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser().parse(uri, it)
        }
    val manifest = playlist(download.request.uri)
    val media = when (manifest) {
        is androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist -> manifest
        is androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist -> {
            val variants = download.request.streamKeys.filter { it.groupIndex == 0 }.map { it.streamIndex }
            val variant = manifest.variants.getOrNull(variants.firstOrNull() ?: 0)
                ?: error("Video playlist is missing")
            playlist(variant.url) as? androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
                ?: error("Video playlist is unavailable")
        }
        else -> error("Unsupported cached playlist")
    }
    require(media.hasEndTag && media.durationUs > 0) { "Incomplete source playlist; cached media retained" }
    val metadata = android.media.MediaMetadataRetriever()
    try {
        setMetadataSource(metadata)
        val duration = metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
        require(metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes") {
            "Export has no video; cached media retained. Retry save."
        }
        // Android can report zero duration for a complete fragmented MP4. Always
        // verify the actual track samples below instead of rejecting missing metadata.
        require(duration == 0L || isExportDurationComplete(media.durationUs / 1000, duration)) {
            "Incomplete MP4 (${duration / 1000}s of ${media.durationUs / 1_000_000}s); cached media retained. Retry save."
        }
    } finally { metadata.release() }
    validateTracks(media.durationUs / 1000)
}

/** Container duration can be supplied by intact audio even when most video samples are missing. */
internal fun validateMp4Tracks(output: File, expectedMs: Long) {
    val extractor = android.media.MediaExtractor()
    try {
        extractor.setDataSource(output.absolutePath)
        validateMp4Tracks(extractor, expectedMs)
    } finally { extractor.release() }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun validateMp4Tracks(context: Context, output: android.net.Uri, expectedMs: Long) {
    val descriptor = context.contentResolver.openFileDescriptor(output, "r")
        ?: error("Cannot read exported MP4; cached media retained. Retry save.")
    val extractor = android.media.MediaExtractor()
    try {
        extractor.setDataSource(descriptor.fileDescriptor)
        validateMp4Tracks(extractor, expectedMs)
    } finally { extractor.release(); descriptor.close() }
}

private fun validateMp4Tracks(extractor: android.media.MediaExtractor, expectedMs: Long) {
    var hasVideo = false
    for (track in 0 until extractor.trackCount) {
        val mime = extractor.getTrackFormat(track).getString(android.media.MediaFormat.KEY_MIME).orEmpty()
        if (!mime.startsWith("video/") && !mime.startsWith("audio/")) continue
        hasVideo = hasVideo || mime.startsWith("video/")
        extractor.selectTrack(track)
        extractor.seekTo(0, android.media.MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        var endUs = -1L
        var samples = 0L
        while (extractor.sampleTrackIndex >= 0) {
            endUs = maxOf(endUs, extractor.sampleTime)
            samples++
            if (!extractor.advance()) break
        }
        extractor.unselectTrack(track)
        DebugTrace.event("export_track_validated", "mime" to mime, "samples" to samples, "end_us" to endUs)
        require(isExportDurationComplete(expectedMs, endUs / 1000)) {
            "Incomplete MP4 $mime track (${endUs / 1_000_000}s of ${expectedMs / 1000}s); cached media retained. Retry save."
        }
    }
    require(hasVideo) { "Export has no video; cached media retained. Retry save." }
}
