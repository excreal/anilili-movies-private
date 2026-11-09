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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import com.example.animepahedl.CatalogItem

/** Home rails follow Anilili's carousel/grid and explicit load-more behavior. */
internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.catalogHomeSections(
    titles: List<CatalogItem>, images: ImageLoader, columns: Int, phoneCardWidth: androidx.compose.ui.unit.Dp, expanded: List<String>,
    toggle: (String) -> Unit, onOpen: (CatalogItem) -> Unit, onMore: () -> Unit, loading: Boolean, hasMore: Boolean,
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
                    rows[index].forEach { entry -> CatalogPosterCard(entry, images, { onOpen(entry) }, Modifier.weight(1f)) }
                    repeat(sectionColumns - rows[index].size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (name == "Discover" && hasMore) item(key = "more:$name", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }, contentType = "pagination-action") {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    OutlinedButton(onClick = onMore, enabled = !loading, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
                        if (loading) {
                            androidx.compose.material3.CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (loading) "Loading…" else "Load More Discover", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else item(key = "rail:$name", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
            contentType = { if (wideArtwork) "banner-carousel" else "poster-carousel" }) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries, key = { it.url }, contentType = { "catalog-card" }) { entry ->
                    CatalogPosterCard(entry, images, { onOpen(entry) }, Modifier.width(if (entry.wideArtwork) phoneCardWidth * (16f / 9f) / (2f / 3f) else phoneCardWidth))
                }
                if (name == "Discover" && hasMore) item(key = "more:$name", contentType = "pagination-action") {
                    Column(Modifier.width(92.dp).height(160.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f))
                        .clickable(enabled = !loading, role = Role.Button, onClickLabel = "Load more Discover", onClick = onMore),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        if (loading) androidx.compose.material3.CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.GridView, contentDescription = "Load more Discover")
                        Text(if (loading) "Loading…" else "Load More", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
