package com.example.animepahedl.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import com.example.animepahedl.CatalogItem

/** Same MediaRail layout, with expanded rows owned by the outer lazy feed. */
internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.catalogHomeSections(
    titles: List<CatalogItem>, images: ImageLoader, columns: Int, expanded: List<String>,
    toggle: (String) -> Unit, onOpen: (String) -> Unit, onMore: () -> Unit, loading: Boolean, hasMore: Boolean,
) {
    val sections = listOf("Popular" to titles.take(10), "Discover" to titles.drop(10))
    sections.filter { it.second.isNotEmpty() }.forEach { (name, entries) ->
        val wideArtwork = entries.firstOrNull()?.wideArtwork == true
        val sectionColumns = columns
        item(key = "section:$name", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }, contentType = "section-header") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { toggle(name) }) {
                    Icon(if (name in expanded) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (name in expanded) "Carousel" else "Expand")
                }
            }
        }
        if (name in expanded) {
            val rows = entries.chunked(sectionColumns)
            items(rows.size, key = { "section:$name:${rows[it].first().url}" }, span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
                contentType = { if (wideArtwork) "banner-grid-row" else "poster-grid-row" }) { index ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    rows[index].forEach { entry -> CatalogPosterCard(entry, images, { onOpen(entry.url) }, Modifier.weight(1f)) }
                    repeat(sectionColumns - rows[index].size) { Spacer(Modifier.weight(1f)) }
                }
            }
        } else item(key = "rail:$name", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
            contentType = { if (wideArtwork) "banner-carousel" else "poster-carousel" }) {
            val rail = rememberLazyListState()
            LaunchedEffect(rail, entries.size, loading, hasMore) {
                snapshotFlow { name == "Discover" && hasMore && !loading && (rail.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) >= entries.size - 3 }
                    .distinctUntilChanged().collect { if (it) onMore() }
            }
            LazyRow(state = rail, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries, key = { it.url }) { entry -> CatalogPosterCard(entry, images, { onOpen(entry.url) }, Modifier.width(108.dp)) }
            }
        }
        if (name == "Discover" && name in expanded && hasMore) item(key = "more:$name", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }, contentType = "pagination-trigger") {
            LaunchedEffect(entries.size, loading) { if (!loading) onMore() }
        }
    }
}
