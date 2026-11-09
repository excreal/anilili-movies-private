@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.animepahedl.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.animepahedl.DownloadRow
import com.example.animepahedl.DownloadStore
import com.example.animepahedl.SavedShowsStore
import com.example.animepahedl.savedSeriesFromDownloads

/** Saved files use the same controls and locator-based progress as their online episodes. */
@Composable
internal fun ShortDramaPlayer(rows: List<DownloadRow>, initialId: String, onClose: () -> Unit, onExternal: (DownloadRow) -> Unit) {
    LaunchedEffect(Unit) { com.anilili.playback.PlaybackService.stopActivePlayback() }
    val context = LocalContext.current
    val playlist = remember(rows) { rows.filter { it.complete && !it.deleting && it.savedUri.isNotBlank() }.distinctBy { it.id } }
    val savedShows = remember(context) { SavedShowsStore(context) }
    val savedSeries = remember(playlist) { savedSeriesFromDownloads(playlist) }
    val history = remember(context) { com.example.animepahedl.PlaybackHistory(context) }
    LaunchedEffect(savedSeries) { savedSeries?.let(history::registerLocal) }
    val savedRevision by SavedShowsStore.changes.collectAsState()
    val isSaved = remember(savedRevision, savedSeries) { savedSeries?.let(savedShows::contains) == true }
    val records = remember(playlist) { DownloadStore.all() }
    val episodes = remember(playlist, records) { playlist.map { row ->
        DramaPlayerEpisode(row.id, records[row.id]?.identity?.locator?.takeIf(String::isNotBlank) ?: row.savedUri,
            row.episodeNumber.takeIf(String::isNotBlank)?.let { "Episode $it" } ?: row.title)
    } }
    val first = playlist.firstOrNull()
    val identity = first?.let { records[it.id]?.identity }
    val seriesKey = identity?.anime?.session?.removePrefix("sites:")?.let { "series:$it" }
        ?: "saved:${first?.showId.orEmpty().ifBlank { first?.showTitle.orEmpty() }}"
    DramaPlayerSurface(first?.let { it.showTitle.ifBlank { it.title } }.orEmpty(), seriesKey, episodes, initialId.takeIf(String::isNotBlank), onClose,
        saved = isSaved, onToggleSave = savedSeries?.let { series -> { savedShows.toggle(series) } },
        resolve = { index ->
            val uri = playlist[index].savedUri.toUri()
            require(uri.scheme == "content") { "Relink this saved video before playing it." }
            DefaultMediaSourceFactory(context.applicationContext).createMediaSource(MediaItem.fromUri(uri))
        }, external = { onExternal(playlist[it]) })
}
