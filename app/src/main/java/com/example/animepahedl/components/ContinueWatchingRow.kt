package com.example.animepahedl.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import coil.ImageLoader
import com.example.animepahedl.*

@Composable
internal fun ContinueWatchingRow(series: List<SiteDownloadSeries>, images: ImageLoader, onOpen: (SiteDownloadSeries) -> Unit) {
    if (series.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text("Continue Watching", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(series, key = { it.provider.name + it.id }) { item ->
                val summary = rememberSeriesPlaybackSummary(item)
                Column(Modifier.width(174.dp).clickable { onOpen(item) }) {
                    Box {
                        Thumbnail(item.poster, item.title, images, Modifier.fillMaxWidth().aspectRatio(16f / 9f), cornerRadius = 12.dp)
                        AppSymbol(AppIcon.Play, Modifier.align(Alignment.Center))
                    }
                    Text(item.title, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelLarge, maxLines = 2)
                    WatchProgressBar(summary.fraction, Modifier.fillMaxWidth().padding(top = 4.dp))
                    Text(summary.resumeLabel ?: "Resume", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(summary.downloadLabel, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
