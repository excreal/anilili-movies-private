package com.example.animepahedl

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.animepahedl.components.CatalogPosterCard
import com.example.animepahedl.components.ContinueWatchingRow

@Composable
internal fun LibraryScreen(store: SavedShowsStore, history: List<SiteDownloadSeries>, images: ImageLoader,
    onDetails: (SiteDownloadSeries) -> Unit, onResume: (SiteDownloadSeries) -> Unit,
    onRemoveFromContinueWatching: (SiteDownloadSeries) -> Unit = {},
    onMarkCompleted: (SiteDownloadSeries) -> Unit = {}) {
    val revision by SavedShowsStore.changes.collectAsState()
    val saved = remember(revision) { store.all() }
    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Library", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black) }
        item {
            if (history.isNotEmpty()) ContinueWatchingRow(history, images, onResume,
                onRemove = onRemoveFromContinueWatching, onMarkCompleted = onMarkCompleted)
            else Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Continue watching", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Your unfinished videos appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Saved shows", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${saved.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (saved.isEmpty()) item {
            Text("Save a show from its details page or while watching.", Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(saved, key = { it.provider.name + it.id }) { series ->
                    val summary = rememberSeriesPlaybackSummary(series)
                    Column(Modifier.width(108.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box {
                            CatalogPosterCard(CatalogItem(series.title, series.url, series.posterThumbnail(), wideArtwork = series.provider == SiteDownloadProvider.KISSKH), images,
                                onClick = { onDetails(series) })
                            IconButton(onClick = { store.toggle(series) }, modifier = Modifier.align(Alignment.TopEnd)) {
                                AppSymbol(AppIcon.BookmarkFilled, description = "Unsave ${series.title}")
                            }
                        }
                        Text(summary.resumeLabel ?: summary.watchLabel, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(summary.downloadLabel, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
