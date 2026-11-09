package com.example.animepahedl

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

internal data class SeriesPlaybackSummary(
    val resumeLabel: String?, val fraction: Float, val downloaded: Int, val total: Int, val watched: Int,
) {
    val downloadLabel get() = "$downloaded of $total downloaded"
    val watchLabel get() = if (watched > 0) "$watched of $total watched" else "Not started"
}

@Composable
internal fun rememberSeriesPlaybackSummary(series: SiteDownloadSeries): SeriesPlaybackSummary {
    val context = LocalContext.current
    var playbackRevision by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val preferences = context.getSharedPreferences("playback-history", android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> playbackRevision++ }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    // Refresh after the initial asynchronous Room snapshot, even if no download changes.
    LaunchedEffect(series.provider, series.id) {
        DownloadStore.awaitReady()
        playbackRevision++
    }
    val downloadRevision by DownloadStore.revisions.collectAsState()
    return remember(series, playbackRevision, downloadRevision) {
        val history = PlaybackHistory(context)
        val episodes = (history.metadata(series)?.episodes.orEmpty() + series.episodes).distinctBy { it.id }
        val last = history.lastEpisode("series:${series.provider.name}:${series.id}")
        val resume = episodes.firstOrNull { history.matches(last, it.url) && history.position(it.url) > 0L }
        val label = resume?.let {
            val seconds = history.position(it.url) / 1000
            "Resume episode ${it.displayNumber} · ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
        }
        val downloaded = DownloadStore.all().values.filter { record ->
            record.uri.isNotBlank() && record.identity?.isSiteDownload == true &&
                record.identity.anime.session == "sites:${series.provider.name}:${series.id}"
        }.mapNotNull { it.identity?.episode?.session }.distinct().size
        SeriesPlaybackSummary(label, resume?.let { history.fraction(it.url) } ?: 0f,
            downloaded, episodes.size, episodes.count { history.watched(it.url) })
    }
}
