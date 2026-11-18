@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
package com.example.animepahedl

import androidx.media3.common.StreamKey
import androidx.media3.common.util.UriUtil
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata
import kotlin.math.roundToLong

internal fun projectHlsTrackSize(totalDurationUs: Long, measuredDurationUs: Long, measuredBytes: Long): Long? {
    // Early samples can be dominated by intros or credits. Wait for at least 10% of the track.
    if (totalDurationUs <= 0 || measuredDurationUs <= 0 || measuredBytes <= 0 ||
        measuredDurationUs.toDouble() / totalDurationUs < 0.1) return null
    return (measuredBytes.toDouble() * totalDurationUs / measuredDurationUs).roundToLong()
}

/** Estimates from the selected, already cached playlists. Never opens a network data source. */
internal class HlsSizeEstimator(private val cache: Cache, private val root: CachedHlsPlaylist, private val keys: List<StreamKey>) {
    private data class Segment(val key: String, val offset: Long, val declaredLength: Long, val durationUs: Long)
    private data class Track(val durationUs: Long, val segments: List<Segment>)
    private var tracks: List<Track>? = null
    private var advertisedBytes: Long? = null
    private var lastSampleMs = -2_000L

    @Synchronized
    fun update(reportedTotal: Long, downloadedBytes: Long): Long? {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastSampleMs < 2_000L) return null
        lastSampleMs = now
        val selected = tracks ?: runCatching {
            val factory = CacheDataSource.Factory().setCache(cache)
            val plan = cachedHlsMediaPlaylists(root, keys, factory)
            val duration = plan.media.firstOrNull()?.durationUs ?: 0L
            if (duration > 0 && plan.averageBitrate > 0) {
                advertisedBytes = (duration.toDouble() * plan.averageBitrate / 8_000_000).roundToLong()
            }
            plan.media.map { playlist ->
                Track(playlist.durationUs, playlist.segments.map { segment ->
                    Segment(UriUtil.resolve(playlist.baseUri, segment.url), segment.byteRangeOffset,
                        segment.byteRangeLength, segment.durationUs)
                })
            }.also { tracks = it }
        }.getOrNull()
        val estimates = selected.orEmpty().mapNotNull(::estimateTrack)
        val total = when {
            estimates.isNotEmpty() && estimates.size == selected?.size -> estimates.fold(0L, Math::addExact)
            reportedTotal > 0 -> reportedTotal
            else -> advertisedBytes
        }
        return total?.coerceAtLeast(downloadedBytes)
    }

    private fun estimateTrack(track: Track): Long? {
        if (track.segments.isEmpty()) return null
        var knownBytes = 0L
        var allLengthsKnown = true
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
            if (length <= 0) allLengthsKnown = false
            else {
                knownBytes = Math.addExact(knownBytes, length)
                if (cache.isCached(segment.key, segment.offset, length)) {
                    measuredBytes = Math.addExact(measuredBytes, length)
                    measuredDurationUs += segment.durationUs
                }
            }
        }
        return if (allLengthsKnown) knownBytes else projectHlsTrackSize(track.durationUs, measuredDurationUs, measuredBytes)
    }
}
