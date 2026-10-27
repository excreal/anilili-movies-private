package com.example.animepahedl

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSourceInputStream
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser
import androidx.media3.exoplayer.offline.Download
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.muxer.BufferInfo
import androidx.media3.muxer.Muxer
import kotlinx.coroutines.ensureActive
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

/**
 * Fast path for the common Animepahe case: one HLS media playlist containing muxed MPEG-TS.
 *
 * Samples go from the cached TS segments straight into the MP4 muxer. Transformer is still used
 * when the source needs decoding (separate audio renditions, fMP4 segments, unsupported codecs, or
 * encryption that the downloader did not already remove).
 */
@androidx.annotation.OptIn(UnstableApi::class)
internal object DirectHlsRemuxer {
    private class UnsupportedSource(message: String) : IOException(message)
    private data class TrackBinding(val trackId: Int, val mime: String, val width: Int, val height: Int)
    private data class MediaPlaylist(val playlist: HlsMediaPlaylist, val uri: Uri)
    private data class PendingSample(
        val trackId: Int,
        val data: ByteArray,
        val presentationTimeUs: Long,
        val flags: Int
    )

    suspend fun remux(
        context: Context,
        download: Download,
        outputPath: String,
        muxerFactory: Muxer.Factory,
        onProgress: (Int) -> Unit
    ): Boolean {
        val media = try {
            resolveMediaPlaylist(download)
        } catch (e: UnsupportedSource) {
            trace("direct_unsupported", e.message)
            return false
        } catch (e: IOException) {
            trace("direct_playlist_failed", e.message)
            return false
        }
        val playlist = media.playlist
        if (playlist.segments.isEmpty() || !playlist.hasEndTag) return false
        if (playlist.segments.any { it.initializationSegment != null }) return false
        if (playlist.segments.any { it.fullSegmentEncryptionKeyUri != null }) {
            trace("direct_encrypted_fallback")
            return false
        }

        val muxer = try {
            muxerFactory.create(outputPath)
        } catch (e: Exception) {
            trace("direct_muxer_unavailable", e.message)
            return false
        }
        var muxerClosed = false
        try {
            val tracks = mutableMapOf<Int, TrackBinding>()
            var offsetUs = 0L
            var lastPresentationUs = 0L
            var firstSegment = true
            val total = playlist.segments.size

            for ((index, segment) in playlist.segments.withIndex()) {
                coroutineContext.ensureActive()
                val bytes = readSegment(media.uri, segment.url)
                if (bytes == null || bytes.isEmpty() || bytes[0] != MPEG_TS_SYNC_BYTE) {
                    trace("direct_segment_unsupported", "segment $index")
                    return false
                }
                val samples = extractSamples(bytes, muxer, tracks)
                if (samples.isEmpty()) {
                    trace("direct_segment_empty", "segment $index")
                    return false
                }
                val earliest = samples.minOf { it.presentationTimeUs }
                val latest = samples.maxOf { it.presentationTimeUs }
                if (firstSegment) {
                    offsetUs = -earliest
                    firstSegment = false
                } else {
                    val shifted = earliest + offsetUs
                    // A discontinuity (for example a repeated playlist) resets the source PTS.
                    // Continue at the end of what was already written so nothing is lost.
                    if (shifted < lastPresentationUs - RESET_TOLERANCE_US ||
                        shifted > lastPresentationUs + FORWARD_GAP_LIMIT_US) {
                        offsetUs += lastPresentationUs - shifted
                    }
                }
                for (sample in samples) {
                    val info = BufferInfo(sample.presentationTimeUs + offsetUs, sample.data.size, sample.flags)
                    muxer.writeSampleData(sample.trackId, ByteBuffer.wrap(sample.data), info)
                }
                lastPresentationUs = latest + offsetUs
                onProgress(((index + 1) * 100) / total)
            }
            muxer.close()
            muxerClosed = true
            DebugTrace.event("export_direct_remux", "segments" to total)
            return true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: UnsupportedSource) {
            trace("direct_unsupported", e.message)
            return false
        } catch (e: Exception) {
            trace("direct_failed", "${e.javaClass.simpleName}: ${e.message}")
            return false
        } finally {
            if (!muxerClosed) runCatching { muxer.close() }
        }
    }

    private fun resolveMediaPlaylist(download: Download): MediaPlaylist {
        val cache = CacheDataSource.Factory().setCache(OfflineDownloads.cache)
            .setUpstreamDataSourceFactory(null)
        fun parse(uri: Uri): androidx.media3.exoplayer.hls.playlist.HlsPlaylist =
            DataSourceInputStream(cache.createDataSource(), DataSpec.Builder().setUri(uri).build()).use {
                HlsPlaylistParser().parse(uri, it)
            }

        return when (val manifest = parse(download.request.uri)) {
            is HlsMediaPlaylist -> MediaPlaylist(manifest, download.request.uri)
            is HlsMultivariantPlaylist -> {
                val variantIndex = download.request.streamKeys
                    .firstOrNull { it.groupIndex == 0 }?.streamIndex ?: 0
                val variant = manifest.variants.getOrNull(variantIndex)
                    ?: throw UnsupportedSource("Video playlist is missing")
                if (variant.audioGroupId != null) throw UnsupportedSource("Separate audio rendition")
                val uri = if (variant.url.isAbsolute) variant.url
                    else resolve(download.request.uri, variant.url.toString())
                val media = parse(uri) as? HlsMediaPlaylist
                    ?: throw UnsupportedSource("Video playlist is unavailable")
                MediaPlaylist(media, uri)
            }
            else -> throw UnsupportedSource("Unsupported HLS playlist")
        }
    }

    private fun readSegment(playlistUri: Uri, url: String): ByteArray? {
        val source = CacheDataSource.Factory().setCache(OfflineDownloads.cache)
            .setUpstreamDataSourceFactory(null).createDataSource()
        return try {
            val length = source.open(DataSpec.Builder().setUri(resolve(playlistUri, url)).build())
            val initial = if (length in 1..Int.MAX_VALUE) length.toInt() else 64 * 1024
            val out = ByteArrayOutputStream(initial)
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = source.read(buffer, 0, buffer.size)
                if (read == C.RESULT_END_OF_INPUT) break
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        } catch (e: IOException) {
            null
        } finally {
            runCatching { source.close() }
        }
    }

    private fun resolve(base: Uri, reference: String): Uri {
        val parsed = Uri.parse(reference)
        return if (parsed.isAbsolute) parsed else base.buildUpon().appendEncodedPath(reference).build()
    }

    private fun extractSamples(bytes: ByteArray, muxer: Muxer, tracks: MutableMap<Int, TrackBinding>): List<PendingSample> {
        val samples = mutableListOf<PendingSample>()
        val output = object : ExtractorOutput {
            override fun track(id: Int, type: Int): TrackOutput = Collector(id, type, muxer, tracks, samples)
            override fun endTracks() {}
            override fun seekMap(seekMap: SeekMap) {}
        }
        val extractor: Extractor = TsExtractor()
        try {
            extractor.init(output)
            val input = DefaultExtractorInput(ByteArrayReader(bytes), 0, bytes.size.toLong())
            val position = PositionHolder()
            while (true) {
                val result = extractor.read(input, position)
                if (result == Extractor.RESULT_END_OF_INPUT) break
                if (result == Extractor.RESULT_SEEK) throw UnsupportedSource("Unexpected seek in TS segment")
            }
        } finally {
            extractor.release()
        }
        return samples
    }

    private class Collector(
        private val id: Int,
        private val type: Int,
        private val muxer: Muxer,
        private val tracks: MutableMap<Int, TrackBinding>,
        private val samples: MutableList<PendingSample>
    ) : TrackOutput {
        private val buffer = ByteArrayOutputStream(64 * 1024)
        private var trackId = -1
        private var formatSeen = false

        override fun format(format: Format) {
            if (trackId >= 0) return
            if (type != C.TRACK_TYPE_VIDEO && type != C.TRACK_TYPE_AUDIO) {
                throw UnsupportedSource("Unsupported track type $type")
            }
            val sampleMime = format.sampleMimeType ?: throw UnsupportedSource("Track has no MIME type")
            val existing = tracks[id]
            trackId = if (existing != null) {
                if (existing.mime != sampleMime) throw UnsupportedSource("Track MIME change")
                if (type == C.TRACK_TYPE_VIDEO &&
                    (existing.width != format.width || existing.height != format.height)) {
                    throw UnsupportedSource("Video resolution change")
                }
                existing.trackId
            } else {
                val added = try {
                    muxer.addTrack(format)
                } catch (e: Exception) {
                    throw UnsupportedSource("Unsupported track format: ${e.message}")
                }
                tracks[id] = TrackBinding(added, sampleMime, format.width, format.height)
                added
            }
            formatSeen = true
        }

        override fun sampleData(input: androidx.media3.common.util.ParsableByteArray, length: Int, sampleDataPart: Int) {
            if (!formatSeen) throw UnsupportedSource("Sample before track format")
            val chunk = ByteArray(length)
            input.readBytes(chunk, 0, length)
            buffer.write(chunk)
        }

        override fun sampleData(input: DataReader, length: Int, allowEndOfInput: Boolean, sampleDataPart: Int): Int {
            if (!formatSeen) throw UnsupportedSource("Sample before track format")
            val chunk = ByteArray(length)
            var total = 0
            while (total < length) {
                val read = input.read(chunk, total, length - total)
                if (read == C.RESULT_END_OF_INPUT) {
                    if (total == 0 && allowEndOfInput) return C.RESULT_END_OF_INPUT
                    break
                }
                total += read
            }
            buffer.write(chunk, 0, total)
            return total
        }

        override fun sampleMetadata(timeUs: Long, flags: Int, size: Int, offset: Int, cryptoData: TrackOutput.CryptoData?) {
            if (!formatSeen || trackId < 0) throw UnsupportedSource("Sample before track format")
            val data = buffer.toByteArray()
            buffer.reset()
            if (data.size != size) throw UnsupportedSource("Sample size mismatch")
            samples += PendingSample(trackId, data, timeUs, flags)
        }

        override fun durationUs(durationUs: Long) {}
    }

    private class ByteArrayReader(private val bytes: ByteArray) : DataReader {
        private var position = 0
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= bytes.size) return C.RESULT_END_OF_INPUT
            val count = minOf(length, bytes.size - position)
            System.arraycopy(bytes, position, buffer, offset, count)
            position += count
            return count
        }
    }

    private fun trace(event: String, message: String? = null) {
        if (message != null) DebugTrace.event(event, "message" to message) else DebugTrace.event(event)
    }

    private const val MPEG_TS_SYNC_BYTE = 0x47.toByte()
    private const val RESET_TOLERANCE_US = 200_000L
    private const val FORWARD_GAP_LIMIT_US = 5_000_000L
}
