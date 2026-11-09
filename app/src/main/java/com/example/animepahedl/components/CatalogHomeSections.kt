package com.example.animepahedl.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import com.example.animepahedl.CatalogItem

/** Material's grid-view treatment without pulling an icon package into this component. */
@Composable
private fun CatalogExpandGridIcon() {
    val tint = LocalContentColor.current
    Canvas(Modifier.size(18.dp).clearAndSetSemantics { }) {
        val unit = size.minDimension / 24f
        val tile = Size(7f * unit, 7f * unit)
        for (x in listOf(3f, 14f)) for (y in listOf(3f, 14f)) {
            drawRoundRect(tint, Offset(x * unit, y * unit), tile, CornerRadius(unit, unit))
        }
    }
}

/** Same MediaRail layout, with expanded rows owned by the outer lazy feed. */
internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.catalogHomeSections(
    titles: List<CatalogItem>, images: ImageLoader, columns: Int, expanded: List<String>,
    toggle: (String) -> Unit, onOpen: (String) -> Unit, onMore: () -> Unit, loading: Boolean, hasMore: Boolean,
) {
    val sections = listOf("Popular" to titles.take(10), "Discover" to titles.drop(10))
    sections.filter { it.second.isNotEmpty() }.forEach { (name, entries) ->
        item(key = "section:$name", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { toggle(name) }) { CatalogExpandGridIcon(); Spacer(Modifier.width(5.dp)); Text(if (name in expanded) "Carousel" else "Expand") }
            }
        }
        if (name in expanded) {
            val rows = entries.chunked(columns)
            items(rows.size, key = { "section:$name:${rows[it].first().url}" }, span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { index ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    rows[index].forEach { entry -> CatalogPosterCard(entry, images, { onOpen(entry.url) }, Modifier.weight(1f)) }
                    repeat(columns - rows[index].size) { Spacer(Modifier.weight(1f)) }
                }
            }
        } else item(key = "rail:$name", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            val rail = rememberLazyListState()
            LaunchedEffect(rail, entries.size, loading, hasMore) {
                snapshotFlow { name == "Discover" && hasMore && !loading && (rail.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) >= entries.size - 3 }
                    .distinctUntilChanged().collect { if (it) onMore() }
            }
            LazyRow(state = rail, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries, key = { it.url }) { entry -> CatalogPosterCard(entry, images, { onOpen(entry.url) }, Modifier.width(108.dp)) }
            }
        }
        if (name == "Discover" && name in expanded && hasMore) item(key = "more:$name", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            LaunchedEffect(entries.size, loading) { if (!loading) onMore() }
        }
    }
}
