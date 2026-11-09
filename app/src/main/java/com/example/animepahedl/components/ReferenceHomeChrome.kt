@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.animepahedl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.animepahedl.*
import com.example.animepahedl.R
import dev.chrisbanes.haze.HazeState

/** HomeTopChrome + GenrePill geometry from Anilili; provider selection is the data adapter. */
@Composable
internal fun ReferenceHomeChrome(
    model: CatalogViewModel,
    backdrop: HazeState?,
    onLink: () -> Unit,
    onDownloads: () -> Unit,
    overHero: Boolean = true,
) {
    Column(Modifier.fillMaxWidth().chromeFade(overHero = overHero, backdrop = backdrop)) {
        TopAppBar(title = { androidx.compose.foundation.Image(painterResource(R.drawable.anilili_header_wordmark), "Home", Modifier.height(22.dp)) },
            actions = {
                IconButton(onClick = onDownloads) { AppSymbol(AppIcon.Download, description = "Downloads") }
                IconButton(onClick = onLink) { AppSymbol(AppIcon.More, description = "Open a video link") }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
        LazyRow(Modifier.selectableGroup(), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(model.availableSources, key = { it.name }) { source ->
                val selected = model.source == source
                val shape = RoundedCornerShape(50)
                val frosted = !selected && backdrop != null && backdropBlurAvailable
                Text(source.label, style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = .92f),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    modifier = Modifier.clip(shape).then(if (frosted) Modifier.backdropBlur(backdrop, Color.Black.copy(alpha = .30f)) else Modifier)
                        .background(if (selected) MaterialTheme.colorScheme.primary else if (frosted) Color.Transparent else Color.Black.copy(alpha = .52f))
                        .border(1.dp, if (selected) Color.Transparent else Color.White.copy(alpha = .18f), shape)
                        .selectable(selected = selected, role = Role.Tab, onClick = { model.select(source) })
                        .padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
