package com.example.animepahedl

import android.content.Context
import androidx.media3.datasource.cache.CacheDataSource
import java.io.File

/** Check against the playlist, not the exporter's success callback: a short file must never evict the source. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun validateCachedHlsExport(context: Context, id: String, output: File) =
    validateCachedHlsExport(context, id, { it.setDataSource(output.absolutePath) },
        { expected -> validateMp4Tracks(output, expected, Exports.all(context)[id]?.audio?.isNotBlank() == true) })

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun validateCachedHlsExport(context: Context, id: String, output: android.net.Uri) =
    validateCachedHlsExport(context, id, { it.setDataSource(context, output) },
        { expected -> validateMp4Tracks(context, output, expected, Exports.all(context)[id]?.audio?.isNotBlank() == true) })

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
internal fun validateMp4Tracks(output: File, expectedMs: Long, requireAudio: Boolean = false) {
    val extractor = android.media.MediaExtractor()
    try {
        extractor.setDataSource(output.absolutePath)
        validateMp4Tracks(extractor, expectedMs, requireAudio)
    } finally { extractor.release() }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun validateMp4Tracks(context: Context, output: android.net.Uri, expectedMs: Long, requireAudio: Boolean = false) {
    val descriptor = context.contentResolver.openFileDescriptor(output, "r")
        ?: error("Cannot read exported MP4; cached media retained. Retry save.")
    val extractor = android.media.MediaExtractor()
    try {
        extractor.setDataSource(descriptor.fileDescriptor)
        validateMp4Tracks(extractor, expectedMs, requireAudio)
    } finally { extractor.release(); descriptor.close() }
}

private fun validateMp4Tracks(extractor: android.media.MediaExtractor, expectedMs: Long, requireAudio: Boolean) {
    var hasVideo = false
    var hasAudio = false
    for (track in 0 until extractor.trackCount) {
        val mime = extractor.getTrackFormat(track).getString(android.media.MediaFormat.KEY_MIME).orEmpty()
        if (!mime.startsWith("video/") && !mime.startsWith("audio/")) continue
        hasVideo = hasVideo || mime.startsWith("video/")
        hasAudio = hasAudio || mime.startsWith("audio/")
        extractor.selectTrack(track)
        // Start near the expected end; the final sample still detects truncated tracks.
        extractor.seekTo(expectedMs * 1000, android.media.MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        var endUs = -1L
        var samples = 0L
        while (extractor.sampleTrackIndex >= 0) {
            if (Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException("Validation interrupted")
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
    require(!requireAudio || hasAudio) { "Export has no expected audio; cached media retained. Retry save." }
    require(hasVideo) { "Export has no video; cached media retained. Retry save." }
}
