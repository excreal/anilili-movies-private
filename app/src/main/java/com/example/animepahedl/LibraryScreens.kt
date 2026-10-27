@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.animepahedl

import android.net.Uri
import android.provider.DocumentsContract
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import coil.ImageLoader
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal fun outputLabel(model: DownloaderViewModel): String = if (model.outputTree.isBlank())
    "Downloads" + model.outputFolder.takeIf { it.isNotEmpty() }?.let { "/$it" }.orEmpty()
    else runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(model.outputTree)).replace(':', '/') }.getOrDefault("Selected folder")

@Composable
internal fun DownloadsScreen(model: DownloaderViewModel, images: ImageLoader, onPlay: (DownloadRow) -> Unit, onRemove: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("All") }
    var query by rememberSaveable { mutableStateOf("") }
    var toggledShows by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val ready = model.downloads.count { it.complete }
    val active = model.downloads.count { !it.complete && !it.failed }
    val visible = model.downloads.filter { it.title.contains(query.trim(), ignoreCase = true) }.filter { when (filter) {
        "In progress" -> !it.complete && !it.failed
        "Completed" -> it.complete
        else -> true
    } }
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Your offline collection", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text("$ready saved · $active active", style = MaterialTheme.typography.labelSmall)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppSymbol(AppIcon.Folder)
                Column(Modifier.weight(1f)) {
                    Text("OUTPUT FOLDER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(outputLabel(model), style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = { model.showSettings = true }) { Text("Change") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "In progress", "Completed").forEach { label ->
                    FilterChip(selected = filter == label, onClick = { filter = label },
                        label = { Text(label, Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                        shape = CircleShape, border = null, modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer))
                }
            }
        }
        if (model.downloads.isNotEmpty()) item {
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("Find downloaded episodes") }, leadingIcon = { AppSymbol(AppIcon.Browse) },
                trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { query = "" }) { Text("Clear") } },
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
        }
        if (visible.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    AppSymbol(AppIcon.Download, modifier = Modifier.padding(22.dp).size(40.dp))
                }
                Text(if (query.isNotBlank()) "No matching downloads" else if (filter == "All") "Make room for a good story" else "Nothing here yet", style = MaterialTheme.typography.titleLarge)
                Text(if (query.isNotBlank()) "Try a show title or episode number." else if (filter == "All") "Choose a title and download a few episodes.\nThey’ll be waiting for you here." else "Your ${filter.lowercase()} downloads will appear here.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                if (filter == "All" && query.isBlank()) FilledTonalButton(onClick = { model.showDownloads = false }) { Text("Explore anime") }
            }
        }
        visible.groupBy { it.title.substringBefore(" · ") }.toSortedMap(String.CASE_INSENSITIVE_ORDER).forEach { (show, episodes) ->
            val defaultExpanded = query.isNotBlank() || episodes.size <= 5 || episodes.any { !it.complete }
            val expanded = defaultExpanded != (show in toggledShows)
            item(key = "show:$show") {
                Card(onClick = {
                    toggledShows = if (show in toggledShows) toggledShows - show else toggledShows + show
                }, modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${episodes.size} episode${if (episodes.size == 1) "" else "s"}",
                            Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(if (expanded) "Hide" else "Show", Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
            if (expanded) items(episodes.sortedWith(compareBy<DownloadRow> {
                it.title.split(" · ").getOrNull(1)?.removePrefix("Episode ")?.toDoubleOrNull() ?: Double.MAX_VALUE
            }.thenBy { it.title }), key = { it.id }) { row ->
                val parts = row.title.split(" · ")
                var menu by remember { mutableStateOf(false) }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(parts.first(), style = MaterialTheme.typography.titleSmall, maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (row.thumbnail.isNotEmpty()) Thumbnail(row.thumbnail, parts.first(), images,
                                Modifier.size(52.dp, 74.dp), cornerRadius = 8.dp)
                            else AppSymbol(if (row.complete) AppIcon.Check else AppIcon.Download, modifier = Modifier.size(32.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(listOfNotNull(parts.getOrNull(1), row.quality ?: parts.getOrNull(2),
                                    row.audioCode?.let(::audioTag) ?: parts.getOrNull(3)).joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium, maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text(listOf(if (row.complete) "Ready" else row.status,
                                    Formatter.formatShortFileSize(context, row.bytes) + if (row.totalBytes > 0 && !row.complete)
                                        " / ${Formatter.formatShortFileSize(context, row.totalBytes)}" else "",
                                    if (!row.complete && row.percent >= 0) "${row.percent.toInt()}%" else "")
                                    .filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    color = if (row.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (row.complete) IconButton(onClick = { onPlay(row) }) { AppSymbol(AppIcon.Play, "Play ${row.title}") }
                            Box {
                                IconButton(onClick = { menu = true }) { AppSymbol(AppIcon.More, "Actions for ${row.title}") }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    if (!row.id.startsWith("direct:") && !row.complete && !row.failed && row.status != "Waiting to save") {
                                        val paused = row.status == "Paused"
                                        DropdownMenuItem(text = { Text(if (paused) "Resume" else "Pause") }, onClick = {
                                            menu = false
                                            if (paused) model.resumeDownload(row.id) else model.pauseDownload(row.id)
                                        })
                                    }
                                    if (row.status.startsWith("Failed") && !row.id.startsWith("direct:"))
                                        DropdownMenuItem(text = { Text("Retry") }, onClick = { menu = false; model.retryOfflineDownload(row.id) })
                                    if (row.status.startsWith("Save failed"))
                                        DropdownMenuItem(text = { Text("Retry save") }, onClick = { menu = false; model.retryExport(row.id) })
                                    DropdownMenuItem(
                                        text = { Text(
                                            if (row.complete) "Remove saved video" else "Cancel download",
                                            color = MaterialTheme.colorScheme.error
                                        ) },
                                        onClick = { menu = false; onRemove(row.id) }
                                    )
                                }
                            }
                        }
                        if (!row.complete && !row.failed && row.status != "Paused") {
                            if (row.percent >= 0) LinearProgressIndicator(progress = { (row.percent / 100).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(3.dp))
                            else LinearProgressIndicator(Modifier.fillMaxWidth().height(3.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsScreen(model: DownloaderViewModel) {
    var showClear by remember { mutableStateOf(false) }
    var editFolder by remember { mutableStateOf(false) }
    var folder by remember(model.outputFolder) { mutableStateOf(model.outputFolder) }
    var invalid by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(model::chooseOutputTree) }
    val busy = model.resolving || model.searching || model.loadingDetails || model.loadingSources || model.clearingSession
    var choice by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        item { SettingsRow(AppIcon.Folder, "Download storage", outputLabel(model)) { choice = "storage" } }
        item { SettingsRow(AppIcon.Play, "Default audio", if (model.defaultAudio == "Original") "Source default" else audioLabel(model.defaultAudio)) { choice = "audio" } }
        item { SettingsRow(AppIcon.Download, "Download speed", "${model.connections} segment connections · 2 episodes") { choice = "speed" } }
        item { SettingsRow(AppIcon.Shield, "Browser session", "Verification and session data") { choice = "session" } }
        model.settingsMessage?.let { message -> item {
            Text(message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)
        } }
    }
    choice?.let { selected ->
        AlertDialog(onDismissRequest = { choice = null }, title = { Text(when (selected) {
            "storage" -> "Download storage"
            "audio" -> "Default audio"
            "speed" -> "Download speed"
            else -> "Browser session"
        }) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (selected) {
                    "storage" -> {
                        Text(outputLabel(model), style = MaterialTheme.typography.bodyMedium)
                        Text("New videos are grouped in show folders.", style = MaterialTheme.typography.bodySmall)
                        FilledTonalButton(onClick = { choice = null; picker.launch(model.outputTree.takeIf { it.isNotBlank() }?.let(Uri::parse)) }) { Text("Choose folder") }
                        TextButton(onClick = { choice = null; editFolder = true; invalid = false }) { Text("Use Downloads") }
                    }
                    "audio" -> {
                        Text("If unavailable, uses the source default. Audio can also be changed per show.", style = MaterialTheme.typography.bodySmall)
                        listOf("Original", "jpn", "eng").forEach { code ->
                            TextButton(onClick = { model.updateDefaultAudio(code); choice = null }, modifier = Modifier.fillMaxWidth()) {
                                RadioButton(selected = model.defaultAudio == code, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (code == "Original") "Source default" else audioLabel(code), Modifier.weight(1f))
                            }
                        }
                    }
                    "speed" -> {
                        Text("Parallel HLS segment requests shared across two active episodes.")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2, 4, 8, 12).forEach { count ->
                                FilterChip(selected = model.connections == count, onClick = { model.setDownloadConnections(count) }, label = { Text("$count") })
                            }
                        }
                    }
                    else -> {
                        Text("Saved cookies are shared with searches and downloads.")
                        FilledTonalButton(onClick = { choice = null; model.verifyManually() }, enabled = !busy) { Text("Open verification") }
                        OutlinedButton(onClick = { choice = null; showClear = true }, enabled = !busy) { Text(if (model.clearingSession) "Clearing…" else "Clear session data") }
                        if (busy) Text("Available when the current request finishes.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { choice = null }) { Text("Done") } })
    }
    if (showClear) AlertDialog(onDismissRequest = { showClear = false }, icon = { AppSymbol(AppIcon.Shield) },
        title = { Text("Clear your session?") }, text = { Text("You may need to verify again. This removes cookies, website data and browser cache without deleting your downloads.") },
        confirmButton = { TextButton(onClick = { showClear = false; model.clearSessionData() }) { Text("Clear session") } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("Cancel") } })
    if (editFolder) AlertDialog(onDismissRequest = { editFolder = false }, title = { Text("Save inside Downloads") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Choose a subfolder, or leave empty to use Downloads itself.")
            OutlinedTextField(value = folder, onValueChange = { folder = it; invalid = false }, singleLine = true,
                label = { Text("Subfolder") }, prefix = { Text("Downloads/") }, isError = invalid,
                supportingText = { if (invalid) Text("Use folder names without .. or special characters.") })
        } }, confirmButton = { TextButton(onClick = { if (model.saveOutputFolder(folder)) editFolder = false else invalid = true }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { editFolder = false }) { Text("Cancel") } })
}

@Composable
private fun SettingsRow(icon: AppIcon, title: String, value: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppSymbol(icon, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
