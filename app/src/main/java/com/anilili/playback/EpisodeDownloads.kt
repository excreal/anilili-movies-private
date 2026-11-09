package com.anilili.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import com.anilili.data.model.StreamItem

/** Bind the copied service to the host's existing download cache, retaining read-only playback. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal object EpisodeDownloads {
    fun buildMediaItem(context: Context, uri: String, builder: MediaItem.Builder): MediaItem = builder.build()
    fun readOnlyPlaybackFactory(context: Context, upstream: DataSource.Factory): DataSource.Factory {
        com.example.animepahedl.OfflineDownloads.prepareCache(context)
        return CacheDataSource.Factory().setCache(com.example.animepahedl.OfflineDownloads.cache)
            .setUpstreamDataSourceFactory(upstream).setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
    fun canDownload(stream: StreamItem?): Boolean = stream != null && !stream.isEmbed &&
        (stream.isHls || stream.isDash || stream.url.substringBefore('?').let { it.endsWith(".mp4", true) || it.endsWith(".webm", true) || it.startsWith("content:") || it.startsWith("file:") })
}
