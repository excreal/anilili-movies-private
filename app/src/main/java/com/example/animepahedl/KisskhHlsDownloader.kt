package com.example.animepahedl

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.Downloader
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

internal fun kisskhHlsOutputFile(context: Context, id: String): File =
    File(context.getExternalFilesDir(null) ?: context.filesDir, "hls-downloads/${id.hashCode().toUInt()}.mp4")

/** Downloads the provider's HLS directly with FFmpeg. The verified final file is exposed to
 * ExportService only after FFmpeg has closed and the source duration has been checked. */
@androidx.annotation.OptIn(UnstableApi::class)
internal class KisskhHlsDownloader(
    private val context: Context,
    private val request: DownloadRequest,
    private val media: ResolvedMedia,
    private val cookieHeader: (String) -> String,
) : Downloader {
    private val process = AtomicReference<Process?>(null)
    private val call = AtomicReference<okhttp3.Call?>(null)
    @Volatile private var cancelled = false

    override fun download(progressListener: Downloader.ProgressListener?) {
        cancelled = false
        lastReportedBytes = 0L
        val output = kisskhHlsOutputFile(context, request.id)
        val part = File(output.parentFile, "${output.nameWithoutExtension}.part.mp4")
        output.parentFile?.mkdirs()
        // MP4 containers cannot safely resume by appending to a previous partial mux.
        // Keep the .part on failure for inspection, and restart it cleanly on Retry.
        part.delete()
        val errors = File(context.cacheDir, "ffmpeg-download-${request.id.hashCode().toUInt()}.log")
        // FFmpeg applies one header block to every CDN segment host. Keep the browser-style
        // headers used by KissKH's working fetch and exclude cookies/auth that must be host-scoped.
        val ffmpegHeaders = mediaRequestHeaders(media, media.url).filterKeys {
            it.equals("User-Agent", true) || it.equals("Referer", true) || it.equals("Origin", true) ||
                it.equals("Accept", true) || it.equals("Accept-Language", true)
        }
        val headers = ffmpegHeaders.entries.joinToString("\r\n", postfix = "\r\n") { (name, value) ->
            "${name.replace(Regex("[\\r\\n:]"), "")}: ${value.replace("\r", "").replace("\n", "")}".trimEnd()
        }
        val expectedDurationUs = probeDurationUs()
        DebugTrace.event("hls_download_started", "id" to request.id.hashCode().toUInt().toString(),
            "provider" to "KISSKH", "source" to DebugTrace.safeUrl(media.url),
            "expectedDurationUs" to expectedDurationUs,
            "headerNames" to ffmpegHeaders.keys)
        val args = listOf(File(context.applicationInfo.nativeLibraryDir, "libffmpeg_exec.so").absolutePath,
            "-nostdin", "-hide_banner", "-loglevel", "error", "-y", "-rw_timeout", "60000000",
            "-reconnect", "1", "-reconnect_streamed", "1", "-reconnect_delay_max", "10",
            "-headers", headers, "-stats_period", "0.25", "-progress", "pipe:1",
            "-protocol_whitelist", "http,https,tcp,tls,crypto,data", "-allowed_extensions", "ALL", "-extension_picky", "0",
            "-f", "hls", "-i", media.url, "-map", "0:v:0", "-map", "0:a:0?", "-c", "copy", "-f", "mp4", part.absolutePath)
        try {
            require(expectedDurationUs > 0L) { "Could not read a complete HLS playlist to verify this download" }
            if (cancelled || Thread.currentThread().isInterrupted) throw InterruptedException("HLS download cancelled")
            val running = ProcessBuilder(args).redirectError(errors).start()
            process.set(running)
            var lastPercent = -1
            running.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (cancelled || Thread.currentThread().isInterrupted) {
                        running.destroyForcibly()
                        throw InterruptedException("HLS download cancelled")
                    }
                    if (line.startsWith("out_time_us=")) {
                        val elapsedUs = line.substringAfter('=').toLongOrNull() ?: 0L
                        val percent = if (expectedDurationUs > 0) (elapsedUs * 100f / expectedDurationUs).coerceIn(0f, 99f) else -1f
                        if (percent < 0 || percent.toInt() > lastPercent) {
                            if (percent >= 0) lastPercent = percent.toInt()
                            val bytes = part.length()
                            DownloadSpeed.add((bytes - lastReportedBytes).coerceAtLeast(0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                            lastReportedBytes = bytes
                            progressListener?.onProgress(-1L, bytes, percent)
                        }
                    }
                }
            }
            val exit = running.waitFor()
            process.compareAndSet(running, null)
            if (cancelled || Thread.currentThread().isInterrupted) throw InterruptedException("HLS download cancelled")
            if (exit != 0) throw IOException("FFmpeg HLS download failed (exit $exit); partial file retained")
            require(part.isFile && part.length() > 0L) { "FFmpeg produced no video data" }
            val actualDurationMs = android.media.MediaMetadataRetriever().run {
                try {
                    setDataSource(part.absolutePath)
                    extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                } finally { release() }
            }
            require(actualDurationMs > 0L) { "FFmpeg output is not a readable video; partial file retained" }
            if (expectedDurationUs > 0) require(isExportDurationComplete(expectedDurationUs / 1_000L, actualDurationMs)) {
                "Downloaded video is shorter than the HLS playlist; partial file retained"
            }
            output.delete()
            require(part.renameTo(output)) { "Could not finalize verified HLS .part file" }
            progressListener?.onProgress(output.length(), output.length(), 100f)
            DebugTrace.event("hls_download_verified", "id" to request.id.hashCode().toUInt().toString(),
                "expectedMs" to expectedDurationUs / 1_000L, "actualMs" to actualDurationMs,
                "bytes" to output.length())
        } catch (error: InterruptedException) {
            DebugTrace.event("hls_download_cancelled", "id" to request.id.hashCode().toUInt().toString(), "bytes" to part.length())
            process.getAndSet(null)?.destroyForcibly()
            throw error
        } catch (error: Exception) {
            DebugTrace.event("hls_download_failed", "id" to request.id.hashCode().toUInt().toString(),
                "type" to error.javaClass.simpleName, "message" to (error.message ?: "HLS download failed"),
                "bytes" to part.length())
            throw if (error is IOException) error else IOException(error.message ?: "HLS download failed", error)
        } finally {
            call.set(null)
            process.getAndSet(null)?.let { if (it.isAlive) it.destroyForcibly() }
            if (errors.exists()) {
                errors.bufferedReader().useLines { lines -> lines.take(200).forEach { line ->
                    DebugTrace.event("hls_download_ffmpeg", "id" to request.id.hashCode().toUInt().toString(), "message" to line)
                } }
                errors.delete()
            }
        }
    }

    @Volatile private var lastReportedBytes = 0L

    private fun probeDurationUs(): Long = runCatching {
        val client = MediaRequests.client(media, cookieHeader)
        fun fetch(url: String): Pair<String, String> {
            val headers = mediaRequestHeaders(media, url)
            val request = Request.Builder().url(url).apply { headers.forEach { (name, value) -> header(name, value) } }.build()
            val networkCall = client.newCall(request)
            call.set(networkCall)
            return try {
                networkCall.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Playlist request returned HTTP ${response.code}")
                    response.request.url.toString() to (response.body?.string() ?: throw IOException("Empty HLS playlist"))
                }
            } finally { call.compareAndSet(networkCall, null) }
        }
        val (masterUrl, masterText) = fetch(media.url)
        val master = HlsPlaylistParser().parse(Uri.parse(masterUrl), ByteArrayInputStream(masterText.toByteArray()))
        val mediaPlaylist = when (master) {
            is HlsMediaPlaylist -> master
            is HlsMultivariantPlaylist -> {
                // Rendition durations should match; the first provides a cheap completion bound.
                val relative = master.mediaPlaylistUrls.firstOrNull()?.toString()
                    ?: throw IOException("No media playlist in HLS master")
                val playlistUrl = masterUrl.toHttpUrl().resolve(relative)?.toString()
                    ?: throw IOException("Invalid HLS media playlist URL")
                val (finalUrl, text) = fetch(playlistUrl)
                HlsPlaylistParser().parse(Uri.parse(finalUrl), ByteArrayInputStream(text.toByteArray())) as? HlsMediaPlaylist
                    ?: throw IOException("HLS media playlist is invalid")
            }
            else -> throw IOException("Unsupported HLS playlist type")
        }
        require(mediaPlaylist.hasEndTag && mediaPlaylist.segments.isNotEmpty()) { "HLS media playlist is incomplete" }
        mediaPlaylist.durationUs
    }.onFailure { error ->
        DebugTrace.event("hls_duration_probe_failed", "id" to request.id.hashCode().toUInt().toString(),
            "type" to error.javaClass.simpleName, "message" to (error.message ?: "Playlist probe failed"))
    }.getOrDefault(0L)

    override fun cancel() {
        cancelled = true
        call.getAndSet(null)?.cancel()
        process.getAndSet(null)?.destroyForcibly()
    }

    override fun remove() {
        cancel()
        val output = kisskhHlsOutputFile(context, request.id)
        File(output.parentFile, "${output.nameWithoutExtension}.part.mp4").delete()
        output.delete()
    }
}
