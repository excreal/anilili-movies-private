package com.example.animepahedl

import androidx.media3.datasource.DataSourceInputStream
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.offline.DownloadRequest
import java.io.File

/** Cache-only copy: holes fail rather than fetching or publishing a partial video. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal suspend fun copyCachedProgressive(cache: Cache, request: DownloadRequest, expectedLength: Long,
    destination: File, progress: (Long) -> Unit) {
    val factory = CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(null)
    val spec = DataSpec.Builder().setUri(request.uri).setKey(request.customCacheKey).apply {
        if (expectedLength > 0) setLength(expectedLength)
    }.build()
    val bytes = DataSourceInputStream(factory.createDataSource(), spec).use { input ->
        destination.outputStream().use { output -> copyCancellable(input, output, progress) }
    }
    require(bytes > 0 && (expectedLength < 0 || bytes == expectedLength)) {
        "Cached media is incomplete. Retry download; the source is retained."
    }
}
