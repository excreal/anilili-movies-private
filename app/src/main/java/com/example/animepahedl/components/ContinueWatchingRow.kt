package com.example.animepahedl.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import coil.ImageLoader
import com.example.animepahedl.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContinueWatchingRow(
    series: List<SiteDownloadSeries>,
    images: ImageLoader,
    onOpen: (SiteDownloadSeries) -> Unit,
    onRemove: (SiteDownloadSeries) -> Unit = {},
    onMarkCompleted: (SiteDownloadSeries) -> Unit = {},
) {
    if (series.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text("Continue Watching", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(series, key = { it.provider.name + it.id }) { item ->
                val summary = rememberSeriesPlaybackSummary(item)
                var showActions by androidx.compose.runtime.remember(item.provider, item.id) { androidx.compose.runtime.mutableStateOf(false) }
                Column(Modifier.width(174.dp).combinedClickable(onClick = { onOpen(item) }, onLongClick = { showActions = true })) {
                    Box {
                        Thumbnail(item.posterThumbnail(), item.title, images, Modifier.fillMaxWidth().aspectRatio(16f / 9f), cornerRadius = 12.dp)
                        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .30f)))
                        AppSymbol(AppIcon.Play, Modifier.align(Alignment.Center))
                        IconButton(onClick = { showActions = true }, modifier = Modifier.align(Alignment.TopEnd)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More actions for ${item.title}", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                            DropdownMenuItem(text = { Text("Mark completed") }, onClick = { showActions = false; onMarkCompleted(item) })
                            DropdownMenuItem(text = { Text("Remove from Continue watching") }, onClick = { showActions = false; onRemove(item) })
                        }
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
