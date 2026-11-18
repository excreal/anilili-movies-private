@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
package com.example.animepahedl

import androidx.media3.common.StreamKey
import androidx.media3.common.util.UriUtil
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata
import kotlin.math.roundToLong

internal fun projectHlsTrackSize(totalDurationUs: Long, measuredDurationUs: Long, measuredBytes: Long): Long? {
    // Intros and action scenes can skew early samples. Wait for half the selected media time.
    if (totalDurationUs <= 0 || measuredDurationUs <= 0 || measuredBytes <= 0 ||
        measuredDurationUs.toDouble() / totalDurationUs < 0.5) return null
    return (measuredBytes.toDouble() * totalDurationUs / measuredDurationUs).roundToLong()
}

internal data class HlsTransferSample(val percent: Float, val sourceBytes: Long?, val durationUs: Long)

internal fun estimateSavedHlsSize(transfer: HlsTransferSample?, siblings: List<ExportRecord>): Long {
    val usable = siblings.filter { it.uri.isNotBlank() && it.bytes > 0 }
    if (transfer?.sourceBytes != null) {
        val ratios = usable.filter { it.sourceBytes > 0 }.map { it.bytes.toDouble() / it.sourceBytes }.sorted()
        val ratio = ratios.getOrNull(ratios.size / 2) ?: 1.0
        return (transfer.sourceBytes * ratio).roundToLong()
    }
    // The same show's measured files provide an initial estimate before enough fragments finish.
    if (usable.size < 2) return -1L
    val typical = usable.map { saved ->
        if (transfer != null && transfer.durationUs > 0 && saved.durationUs > 0)
            (saved.bytes.toDouble() * transfer.durationUs / saved.durationUs).roundToLong()
        else saved.bytes
    }.sorted()
    return typical[typical.size / 2]
}

/** Samples completed media fragments from cache. Manifest bitrate and engine totals aren't output sizes. */
internal class HlsSizeEstimator(private val cache: Cache, private val root: CachedHlsPlaylist, private val keys: List<StreamKey>, private val cachePrefix: String) {
    private data class Segment(val key: String, val offset: Long, val declaredLength: Long, val durationUs: Long)
    private data class Track(val durationUs: Long, val segments: List<Segment>)
    private var tracks: List<Track>? = null
    private var lastSampleMs = -2_000L

    @Synchronized
    fun update(): HlsTransferSample? {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastSampleMs < 2_000L) return null
        lastSampleMs = now
        val selected = tracks ?: runCatching {
            val factory = offlineCacheFactory(cache, cachePrefix)
            cachedHlsMediaPlaylists(root, keys, factory).takeIf { playlists ->
                playlists.isNotEmpty() && playlists.all { it.hasEndTag && it.durationUs > 0 }
            }?.map { playlist ->
                Track(playlist.durationUs, playlist.segments.map { segment ->
                    Segment(cachePrefix + UriUtil.resolve(playlist.baseUri, segment.url), segment.byteRangeOffset,
                        segment.byteRangeLength, segment.durationUs)
                })
            }?.also { tracks = it }
        }.getOrNull() ?: return null
        val measurements = selected.map(::measureTrack)
        val estimates = measurements.zip(selected).mapNotNull { (sample, track) ->
            projectHlsTrackSize(track.durationUs, sample.first, sample.second)
        }
        val projected = if (estimates.size == selected.size) estimates.fold(0L, Math::addExact) else null
        val percent = measurements.sumOf { it.first.toDouble() } * 100 / selected.sumOf { it.durationUs.toDouble() }
        return HlsTransferSample(percent.toFloat(), projected, selected.first().durationUs)
    }

    private fun measureTrack(track: Track): Pair<Long, Long> {
        var measuredBytes = 0L
        var measuredDurationUs = 0L
        for (segment in track.segments) {
            val resourceLength = ContentMetadata.getContentLength(cache.getContentMetadata(segment.key))
            val remaining = if (resourceLength > segment.offset) resourceLength - segment.offset else -1L
            val length = when {
                segment.declaredLength > 0 && remaining > 0 -> minOf(segment.declaredLength, remaining)
                segment.declaredLength > 0 -> segment.declaredLength
                else -> remaining
            }
            if (length > 0 && cache.isCached(segment.key, segment.offset, length)) {
                measuredBytes = Math.addExact(measuredBytes, length)
                measuredDurationUs += segment.durationUs
            }
        }
        return measuredDurationUs.coerceIn(0L, track.durationUs) to measuredBytes
    }
}
