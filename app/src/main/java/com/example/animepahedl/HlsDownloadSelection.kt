@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
package com.example.animepahedl

import android.net.Uri
import androidx.media3.common.StreamKey
import androidx.media3.datasource.DataSourceInputStream
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.hls.offline.HlsDownloader
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.Downloader
import java.io.IOException
import java.util.concurrent.Executor

internal data class CachedHlsPlaylist(val uri: Uri, val text: String)

/** Read through the download cache, retaining the final URI for resolving relative resources. */
internal fun readHlsPlaylist(source: CacheDataSource, uri: Uri): CachedHlsPlaylist =
    DataSourceInputStream(source, DataSpec.Builder().setUri(uri).setFlags(DataSpec.FLAG_ALLOW_GZIP).build()).use { input ->
        input.open()
        val finalUri = source.uri ?: uri
        val limit = 4 * 1024 * 1024
        val bytes = input.readNBytes(limit + 1)
        if (bytes.size > limit) throw IOException("HLS playlist exceeds the size limit")
        CachedHlsPlaylist(finalUri, bytes.toString(Charsets.UTF_8))
    }

internal fun selectedHlsStreamKeys(playlist: CachedHlsPlaylist, quality: String): List<StreamKey> {
    val parser = HlsPlaylistParser()
    val original = parser.parse(playlist.uri, playlist.text.byteInputStream()) as? HlsMultivariantPlaylist ?: return emptyList()
    val selected = parser.parse(playlist.uri, selectCachedHlsVariant(playlist.text, quality).byteInputStream()) as HlsMultivariantPlaylist
    val video = selected.variants.singleOrNull() ?: throw IOException("HLS has no selectable video rendition")
    val videoIndex = original.variants.indexOfFirst { it.url == video.url }
    if (videoIndex < 0) throw IOException("Selected HLS video rendition is missing")
    return buildList {
        add(StreamKey(HlsMultivariantPlaylist.GROUP_INDEX_VARIANT, videoIndex))
        selected.audios.forEach { audio ->
            val audioIndex = original.audios.indexOfFirst { it.groupId == audio.groupId && it.name == audio.name && it.url == audio.url }
            if (audioIndex < 0) throw IOException("Selected HLS audio rendition is missing")
            add(StreamKey(HlsMultivariantPlaylist.GROUP_INDEX_AUDIO, audioIndex))
        }
    }
}

internal fun cachedHlsMediaPlaylists(root: CachedHlsPlaylist, keys: List<StreamKey>, cacheFactory: CacheDataSource.Factory): List<HlsMediaPlaylist> =
    when (val manifest = HlsPlaylistParser().parse(root.uri, root.text.byteInputStream())) {
        is HlsMediaPlaylist -> listOf(manifest)
        is HlsMultivariantPlaylist -> {
            val selected = if (keys.isEmpty()) manifest else manifest.copy(keys)
            val parser = HlsPlaylistParser(selected, null)
            selected.mediaPlaylistUrls.map {
                val child = readHlsPlaylist(cacheFactory.createDataSource(), it)
                parser.parse(child.uri, child.text.byteInputStream()) as HlsMediaPlaylist
            }
        }
        else -> throw IOException("Unsupported HLS playlist")
    }

/** Refresh signed segment URLs while keeping byte ranges from the same transfer available. */
internal fun resetCachedHlsPlaylists(cache: Cache, uri: Uri, prefix: String) {
    val children = runCatching {
        val root = readHlsPlaylist(offlineCacheFactory(cache, prefix).createDataSource(), uri)
        (HlsPlaylistParser().parse(root.uri, root.text.byteInputStream()) as? HlsMultivariantPlaylist)?.mediaPlaylistUrls.orEmpty()
    }.getOrDefault(emptyList())
    (children + uri).distinct().forEach { cache.removeResource(prefix + it) }
}

/** Called inside the download task. The first request fills the cache the downloader will read. */
internal fun selectedHlsDownloader(request: DownloadRequest, quality: String, factory: CacheDataSource.Factory, executor: Executor,
    cachePrefix: String = "", onSample: (HlsTransferSample) -> Unit = {}): Downloader {
    val root = readHlsPlaylist(factory.createDataSourceForDownloading(), request.uri)
    if (root.uri != request.uri) {
        val keyFactory = factory.cacheKeyFactory
        // Media3 can open the media playlist again by its canonical URI. Reuse the cached root
        // even if that redirected URL can only be opened from the network once.
        factory.setCacheKeyFactory { spec ->
            keyFactory.buildCacheKey(if (spec.uri == root.uri) spec.withUri(request.uri) else spec)
        }
    }
    val keys = request.streamKeys.ifEmpty { selectedHlsStreamKeys(root, quality) }
    val item = request.toMediaItem().buildUpon().setStreamKeys(keys).build()
    val delegate = HlsDownloader.Factory(factory).setExecutor(executor).create(item)
    val estimates = HlsSizeEstimator(requireNotNull(factory.cache), root, keys, cachePrefix)
    return object : Downloader by delegate {
        override fun download(progressListener: Downloader.ProgressListener?) {
            // Restored downloads may already have enough cached fragments to provide an estimate.
            runCatching { estimates.update() }.getOrNull()?.let(onSample)
            delegate.download { length, bytes, percent ->
                progressListener?.onProgress(length, bytes, percent)
                // Estimation is optional. An unreadable cache sample must not fail a transfer.
                runCatching { estimates.update() }.getOrNull()?.let(onSample)
            }
        }
    }
}
