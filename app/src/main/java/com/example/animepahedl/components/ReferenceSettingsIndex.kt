package com.example.animepahedl.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal data class SettingsSectionTile(val id: String, val title: String, val icon: ImageVector)
internal val settingsSectionTiles = listOf(
    SettingsSectionTile("player", "Player", Icons.Outlined.PlayCircleOutline),
    SettingsSectionTile("subtitles", "Subtitles", Icons.Outlined.ClosedCaption),
    SettingsSectionTile("downloads", "Downloads", Icons.Outlined.FileDownload),
    SettingsSectionTile("servers", "Servers", Icons.Outlined.Dns),
    SettingsSectionTile("accounts", "Accounts & sync", Icons.Outlined.SyncAlt),
    SettingsSectionTile("notifications", "Notifications", Icons.Outlined.Notifications),
    SettingsSectionTile("content", "Content", Icons.Outlined.VisibilityOff),
    SettingsSectionTile("appearance", "Appearance", Icons.Outlined.Palette),
    SettingsSectionTile("data", "Data", Icons.Outlined.Storage),
    SettingsSectionTile("app", "App", Icons.Outlined.Tune),
    SettingsSectionTile("about", "About", Icons.Outlined.Info),
)

/** Anilili SettingsSectionCard phone renderer. Television focus and badges are omitted. */
@Composable
private fun SettingsSectionCard(tile: SettingsSectionTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = LocalContentColor.current,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(tile.icon, contentDescription = null, modifier = Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
            Text(tile.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, color = LocalContentColor.current)
        }
    }
}

@Composable
internal fun ReferenceSettingsIndex(onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val tiles = settingsSectionTiles.filter { it.title.contains(query, ignoreCase = true) }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 108.dp)) {
        item {
            PageSearchField(query, { query = it }, "Search settings", "Clear search",
                Modifier.padding(bottom = 10.dp))
        }
        items(tiles.chunked(3), key = { it.first().id }) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tile -> SettingsSectionCard(tile, { onOpen(tile.id) }, Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (tiles.isEmpty()) item { Text("No matching settings", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
