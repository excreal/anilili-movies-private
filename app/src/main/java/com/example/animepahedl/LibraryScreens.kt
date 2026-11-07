@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.animepahedl

import com.example.animepahedl.components.*
import androidx.compose.animation.animateContentSize

import androidx.core.net.toUri
import android.net.Uri
import android.provider.DocumentsContract
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal fun friendlyDownloadStatus(status: String): String = when (status) {
    "Downloading HLS", "Downloading file" -> "Downloading"
    "Queued" -> "Waiting to download"
    "Waiting to save" -> "Waiting to save"
    "Preparing MP4", "Retrying MP4" -> "Preparing video"
    "Checking MP4" -> "Checking video"
    "Copying to folder" -> "Saving to folder"
    "Ready offline", "Saved to Downloads", "Saved to folder" -> "Ready"
    else -> if (status.startsWith("Save failed")) "Couldn’t save video" else if (status.startsWith("Failed")) "Download failed" else status
}

internal fun outputLabel(model: DownloaderViewModel): String = if (model.outputTree.isBlank())
    "Downloads" + model.outputFolder.takeIf { it.isNotEmpty() }?.let { "/$it" }.orEmpty()
    else runCatching { DocumentsContract.getTreeDocumentId((model.outputTree).toUri()).replace(':', '/') }.getOrDefault("Selected folder")

@Composable
internal fun DownloadsScreen(model: DownloaderViewModel, siteDownload: SiteDownloadViewModel, images: ImageLoader, onChooseFolder: () -> Unit, onPlay: (DownloadRow) -> Unit, onRemove: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var expandedGroups by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var relinkId by rememberSaveable { mutableStateOf<String?>(null) }
    var forgetId by rememberSaveable { mutableStateOf<String?>(null) }
    val relinkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = relinkId
        relinkId = null
        if (uri != null && id != null) model.relink(id, uri)
    }
    forgetId?.let { id -> AlertDialog(onDismissRequest = { forgetId = null }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_remove_library_record)) },
        text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_the_video_file_is_kept_you_can_add_it_again_by_downloading_or_rel)) },
        confirmButton = { TextButton(onClick = { model.forgetMissing(id); forgetId = null }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_remove_record)) } },
        dismissButton = { TextButton(onClick = { forgetId = null }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_keep)) } }) }
    var detailRow by remember { mutableStateOf<DownloadRow?>(null) }
    detailRow?.let { row -> AlertDialog(onDismissRequest = { detailRow = null }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_download_details)) },
        text = { Text("${row.title}\n\n${row.status}\n\n${outputLabel(model)}", Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { detailRow = null }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_close)) } }) }
    val listState = rememberLazyListState()
    var downloadSort by rememberSaveable { mutableStateOf("Show title") }
    val allRows = model.downloads
    val snapshot by produceState(DownloadLibrarySnapshot(), allRows, query, downloadSort) {
        val previous = value
        value = withContext(Dispatchers.Default) { downloadLibrarySnapshot(allRows, query, downloadSort, previous) }
    }
    val ready = snapshot.ready
    val active = snapshot.unfinished
    val visible = snapshot.rows
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().testTag("downloads-list"), state = listState, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (!model.outputAccessGranted || model.outputCheckRequired || model.checkingExistingFiles) item(key = "output-folder-access") {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                if (model.checkingExistingFiles) CircularProgressIndicator(Modifier.padding(end = 8.dp).size(16.dp), strokeWidth = 2.dp)
                Text(if (model.checkingExistingFiles) "Checking existing videos…" else "Link your downloads folder to reuse saved videos.",
                    Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                if (!model.checkingExistingFiles) TextButton(onClick = onChooseFolder) { Text("Choose folder") }
            }
        }
        model.pendingPreparation?.let { pending -> item(key = "anime-preparation") {
            PreparationStatusRow(model.preparationStatus, pending.episodes.size, model::cancelPreparation,
                model::resumePreparation, model::discardPreparation, model::verifyPreparation, onChooseFolder)
        } }
        siteDownload.pending?.let { pending -> item(key = "site-preparation") {
            PreparationStatusRow(siteDownload.preparationStatus, pending.episodes.size, siteDownload::pausePreparation,
                siteDownload::resumePreparation, siteDownload::discardPreparation, onChooseFolder = onChooseFolder)
        } }
        model.downloadNotice?.let { notice -> item {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(notice, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = model::dismissDownloadNotice) { AppSymbol(AppIcon.Close, description = "Dismiss notice") }
                }
            }
        } }
        item {
            Text("$ready saved · $active unfinished · ${Formatter.formatShortFileSize(context, snapshot.savedBytes)}" +
                if (model.temporaryBytes > 0) " · ${Formatter.formatShortFileSize(context, model.temporaryBytes)} temporary" else "",
                modifier = Modifier.padding(vertical = 4.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        if (model.downloads.isNotEmpty()) item {
            var sortMenu by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                verticalAlignment = Alignment.CenterVertically) {
                AppSymbol(AppIcon.Browse, Modifier.padding(start = 12.dp, end = 8.dp).size(20.dp))
                BasicTextField(query, { query = it; model.clearDownloadSelection() }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).padding(vertical = 12.dp).testTag("downloads-search").semantics { contentDescription = "Search downloads" },
                    decorationBox = { field -> Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text("Search downloads", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        field()
                    } })
                if (query.isNotEmpty()) IconButton(onClick = { query = ""; model.clearDownloadSelection() }) {
                    AppSymbol(AppIcon.Close, description = "Clear search")
                }
                Box {
                    IconButton(onClick = { sortMenu = true }) { AppSymbol(AppIcon.Sort, description = "Sort: $downloadSort") }
                    DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                        listOf("Show title", "Status", "Largest").forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { downloadSort = option; sortMenu = false },
                                trailingIcon = { if (downloadSort == option) AppSymbol(AppIcon.Check) })
                        }
                    }
                }
            }
        }
        if (visible.isEmpty() && snapshot.source === allRows) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    AppSymbol(AppIcon.Download, modifier = Modifier.padding(22.dp).size(40.dp))
                }
                Text(if (query.isNotBlank()) "No matching downloads" else "Make room for a good story", style = MaterialTheme.typography.titleLarge)
                Text(if (query.isNotBlank()) "Try a show title or episode number." else "Choose a title and download a few episodes.\nThey’ll be waiting for you here.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                if (query.isBlank()) FilledTonalButton(onClick = { model.showDownloads = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_explore_anime)) }
            }
        }
        snapshot.groups.forEach { (groupId, episodes) ->
            val show = episodes.first().showTitle.ifBlank { episodes.first().title.substringBefore(" · ") }
            val preparingLinks = (model.resolving && model.pendingPreparation?.anime?.session == groupId) ||
                (siteDownload.preparing && siteDownload.pending?.series?.let { "sites:${it.provider.name}:${it.id}" } == groupId)
            val groupProgress = downloadGroupProgress(episodes, preparingLinks)
            item(key = "group:$groupId") {
                Surface(Modifier.fillMaxWidth().padding(vertical = 2.dp), color = if (groupId !in expandedGroups) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.semantics { stateDescription = if (groupId !in expandedGroups) "Collapsed" else "Expanded" }.combinedClickable(onClickLabel = if (groupId !in expandedGroups) "Show episodes" else "Hide episodes", onClick = {
                        expandedGroups = if (groupId in expandedGroups) expandedGroups - groupId else expandedGroups + groupId
                    }, onLongClick = { model.selectDownloads(episodes.map { it.id }.toSet()) }).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (model.selectionMode) {
                            val count = episodes.count { it.id in model.selectedDownloadIds }
                            TriStateCheckbox(state = when (count) {
                                0 -> androidx.compose.ui.state.ToggleableState.Off
                                episodes.size -> androidx.compose.ui.state.ToggleableState.On
                                else -> androidx.compose.ui.state.ToggleableState.Indeterminate
                            }, onClick = {
                                val ids = episodes.map { it.id }.toSet()
                                model.selectDownloads(if (count == episodes.size) model.selectedDownloadIds - ids else model.selectedDownloadIds + ids)
                            })
                        }
                        episodes.firstOrNull { it.thumbnail.isNotBlank() }?.let { Thumbnail(it.thumbnail, show, images, Modifier.size(34.dp, 46.dp), cornerRadius = 6.dp) }
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(show, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(listOfNotNull(groupProgress.label.takeUnless { it == "Saved" }?.let { status ->
                                status + groupProgress.percent?.let { " ${it.toInt()}%" }.orEmpty()
                            }, "${episodes.count { it.complete }} saved",
                                episodes.count { it.failed }.takeIf { it > 0 && !groupProgress.failed }?.let { "$it failed" }).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (groupProgress.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        Box(Modifier.padding(end = 8.dp).size(26.dp), contentAlignment = Alignment.Center) {
                            when {
                                groupProgress.running && groupProgress.percent != null -> CircularProgressIndicator(
                                    progress = { groupProgress.percent / 100f }, modifier = Modifier.fillMaxSize(), strokeWidth = 2.5.dp)
                                groupProgress.running -> CircularProgressIndicator(Modifier.fillMaxSize(), strokeWidth = 2.5.dp)
                                groupProgress.failed -> AppSymbol(AppIcon.Info, description = groupProgress.label)
                                groupProgress.label == "Saved" -> AppSymbol(AppIcon.Check, description = "All videos saved")
                                groupProgress.label == "Paused" -> AppSymbol(AppIcon.Pause, description = "Paused")
                                else -> AppSymbol(AppIcon.Download, description = groupProgress.label)
                            }
                        }
                        val angle by animateFloatAsState(if (groupId !in expandedGroups) -90f else 180f, tween(160), label = "Group expansion")
                        AppSymbol(AppIcon.Expand, modifier = Modifier.rotate(angle).semantics { stateDescription = if (groupId !in expandedGroups) "Collapsed" else "Expanded" })
                    }
                }
            }
            if (groupId in expandedGroups) items(episodes, key = { it.id }) { row ->
                val parts = row.title.split(" · ")
                var menu by remember { mutableStateOf(false) }
                AppCard(Modifier.fillMaxWidth().padding(start = 6.dp).combinedClickable(
                    onClickLabel = if (model.selectionMode) "Toggle selection" else if (row.complete) "Play ${row.title}" else null,
                    onClick = { if (model.selectionMode) model.toggleDownload(row.id) else if (row.complete) onPlay(row) },
                    onLongClick = { model.enterSelection(row.id) }),
                    colors = CardDefaults.cardColors(containerColor = if (row.id in model.selectedDownloadIds) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(start = 12.dp, end = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(Modifier.heightIn(min = 48.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (model.selectionMode) Checkbox(checked = row.id in model.selectedDownloadIds,
                                onCheckedChange = { model.toggleDownload(row.id) })
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
                                    Text(if (row.showId.startsWith("sites:") && row.episodeNumber == "1" && !row.title.contains(" · Episode ")) row.showTitle.ifBlank { parts.first() } else parts.getOrNull(1) ?: parts.first(), style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(listOfNotNull(row.quality ?: parts.getOrNull(2),
                                        row.audioCode?.let(::audioTag) ?: parts.getOrNull(3),
                                        if (row.complete) Formatter.formatShortFileSize(context, row.bytes) else null).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!row.complete) Text(listOf(friendlyDownloadStatus(row.status),
                                    Formatter.formatShortFileSize(context, row.bytes) + if (row.totalBytes > 0 && !row.complete)
                                        " / ${Formatter.formatShortFileSize(context, row.totalBytes)}" else "",
                                    if (!row.complete && row.percent >= 0) "${row.percent.toInt()}%" else "")
                                    .filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    color = if (row.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (!model.selectionMode && row.complete) AppSymbol(AppIcon.Play, modifier = Modifier.size(18.dp))
                            if (!model.selectionMode) Box {
                                IconButton(onClick = { menu = true }) { AppSymbol(AppIcon.More, description = "Actions for ${row.title}") }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_details)) }, onClick = { menu = false; detailRow = row })
                                    if (row.complete) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_relink_video)) }, onClick = { menu = false; relinkId = row.id; relinkPicker.launch(arrayOf("video/*")) })
                                    if (row.complete) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_remove_record_keep_file)) }, onClick = { menu = false; forgetId = row.id })
                                    if (!row.complete) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_move_to_front)) }, onClick = { menu = false; model.prioritize(row.id) })
                                    if (row.complete) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_play)) }, onClick = { menu = false; onPlay(row) })
                                    if (DownloadAction.PAUSE in row.actions || DownloadAction.RESUME in row.actions) {
                                        val paused = DownloadAction.RESUME in row.actions
                                        DropdownMenuItem(text = { Text(if (paused) "Resume" else "Pause") }, onClick = {
                                            menu = false
                                            if (paused) model.resumeDownload(row.id) else model.pauseDownload(row.id)
                                        })
                                    }
                                    if (DownloadAction.VERIFY in row.actions) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_verify)) }, onClick = { menu = false; model.verifyDownload(row.id) })
                                    if (DownloadAction.RETRY in row.actions) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_retry)) }, onClick = { menu = false; model.retryDownload(row.id) })
                                    if (row.failure in setOf(FailureReason.FOLDER_ACCESS, FailureReason.STORAGE_FULL)) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_change_folder)) }, onClick = { menu = false; model.showSettings = true })
                                    if (DownloadAction.RETRY_SAVE in row.actions) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_retry_save)) }, onClick = { menu = false; model.retryExport(row.id) })
                                    DropdownMenuItem(
                                        text = { Text(
                                            if (row.complete) "Delete saved video" else "Cancel download",
                                            color = MaterialTheme.colorScheme.error
                                        ) },
                                        onClick = { menu = false; onRemove(row.id) }
                                    )
                                }
                            }
                        }
                        if (!row.complete && !row.failed && row.transfer != TransferState.PAUSED) {
                            if (row.percent >= 0) key(row.status) {
                                val progress by animateFloatAsState(
                                    targetValue = (row.percent / 100).coerceIn(0f, 1f),
                                    animationSpec = tween(durationMillis = 250, easing = LinearEasing),
                                    label = "Download progress")
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp))
                            }
                            else LinearProgressIndicator(Modifier.fillMaxWidth().height(3.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsScreen(model: DownloaderViewModel, onAbout: () -> Unit) {
    var showClear by remember { mutableStateOf(false) }
    var editFolder by remember { mutableStateOf(false) }
    var folder by remember(model.outputFolder) { mutableStateOf(model.outputFolder) }
    var invalid by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { model.chooseOutputTree(it) } }
    val busy = model.resolving || model.searching || model.loadingDetails || model.loadingSources || model.clearingSession
    var showHealth by remember { mutableStateOf(false) }
    if (showHealth) AlertDialog(onDismissRequest = { showHealth = false }, title = { Text("Source health") }, text = { Text(model.sourceHealthHistory() + "\n\nBrowser verification does not guarantee HTTP acceptance.", Modifier.verticalScroll(rememberScrollState())) }, confirmButton = { TextButton(onClick = { showHealth = false; model.verifyManually() }) { Text("Verify") } }, dismissButton = { TextButton(onClick = { showHealth = false }) { Text("Close") } })
    var showRecovery by remember { mutableStateOf(false) }
    var diagnosticPreview by remember { mutableStateOf<String?>(null) }
    var exportingDiagnostic by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val diagnosticPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val contents = exportingDiagnostic
        exportingDiagnostic = null
        if (uri != null && contents != null) scope.launch {
            try { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(contents.toByteArray()) } ?: error("Cannot write report") } }
            catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; android.widget.Toast.makeText(context, "Could not export diagnostics", android.widget.Toast.LENGTH_LONG).show() }
        }
    }
    if (showRecovery) AlertDialog(onDismissRequest = { showRecovery = false }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_storage_and_recovery)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val report = model.storageReport
            Text(if (report == null) "Checking storage…" else "App storage available: ${Formatter.formatShortFileSize(context, report.available)}\nTemporary media: ${Formatter.formatShortFileSize(context, report.retained)}\nFailed saves: ${report.failed}\nInaccessible saved files: ${report.inaccessible}")
            Text(androidx.compose.ui.res.stringResource(R.string.ui_saving_hls_needs_space_for_cached_segments_an_assembled_video_and))
            TextButton(onClick = model::retryFailedSaves) { Text(androidx.compose.ui.res.stringResource(R.string.ui_retry_failed_saves)) }
            TextButton(onClick = { showRecovery = false; model.showSettings = false; model.showDownloads = true }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_repair_relink_or_delete_in_downloads)) }
            TextButton(onClick = model::inspectLibrary) { Text(androidx.compose.ui.res.stringResource(R.string.ui_refresh_storage_check)) }
        }
    }, confirmButton = { TextButton(onClick = { showRecovery = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_done)) } })
    diagnosticPreview?.let { preview -> AlertDialog(onDismissRequest = { diagnosticPreview = null }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_preview_diagnostics)) },
        text = { Text(preview, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { exportingDiagnostic = preview; diagnosticPreview = null; diagnosticPicker.launch("animepahe-diagnostics.txt") }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_save_report)) } },
        dismissButton = { TextButton(onClick = { diagnosticPreview = null }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_cancel)) } }) }
    var choice by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppSymbol(AppIcon.Download, modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f)) { Text(androidx.compose.ui.res.stringResource(R.string.ui_unmetered_downloads), style = MaterialTheme.typography.titleSmall); Text(androidx.compose.ui.res.stringResource(R.string.ui_hls_uses_unmetered_networks_new_direct_files_use_wi_fi), style = MaterialTheme.typography.bodySmall) }
                Switch(checked = model.unmeteredOnly, onCheckedChange = model::setUnmetered)
            }
        }
        item { Text("Temporary storage: ${Formatter.formatShortFileSize(LocalContext.current, model.temporaryBytes)}", Modifier.padding(start = 36.dp, top = 4.dp, bottom = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SettingsRow(AppIcon.Folder, "Download storage", outputLabel(model)) { choice = "storage" } }
        item { SettingsRow(AppIcon.Play, "Default audio", if (model.defaultAudio == "Original") "Source default" else audioLabel(model.defaultAudio)) { choice = "audio" } }
        item { SettingsRow(AppIcon.Download, "Download speed", "${model.simultaneousDownloads} at a time") { choice = "speed" } }
        item { SettingsRow(AppIcon.Shield, "Browser session", "Verification and session data") { choice = "session" } }
        item { SettingsRow(AppIcon.Folder, "Storage and recovery", "Check free space, retained media, and missing files") { model.inspectLibrary(); showRecovery = true } }
        item { SettingsRow(AppIcon.Shield, "Source health", model.sourceHealth) { showHealth = true } }
        item { SettingsRow(AppIcon.Info, "Export diagnostics", "Preview a report without titles, paths, or cookies") { diagnosticPreview = diagnostics(context) } }
        item { SettingsRow(AppIcon.Info, "About", "Version and what’s new", onAbout) }
        model.settingsMessage?.let { message -> item {
            Text(message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
                color = if (message.startsWith("Cannot") || message.startsWith("Could")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        } }
    }
    choice?.let { selected ->
        AlertDialog(onDismissRequest = { choice = null }, title = { Text(when (selected) {
            "storage" -> "Download storage"
            "audio" -> "Default audio"
            "speed" -> "Download speed"
            else -> "Browser session"
        }) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (selected) {
                    "storage" -> {
                        DestinationSummary(outputLabel(model))
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_new_videos_are_grouped_in_show_folders), style = MaterialTheme.typography.bodySmall)
                        FilledTonalButton(onClick = { choice = null; picker.launch(model.outputTree.takeIf { it.isNotBlank() }?.let(Uri::parse)) }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_choose_folder)) }
                        TextButton(onClick = { choice = null; editFolder = true; invalid = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_use_downloads)) }
                    }
                    "audio" -> {
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_if_unavailable_uses_the_source_default_audio_can_also_be_changed_), style = MaterialTheme.typography.bodySmall)
                        listOf("Original", "jpn", "eng").forEach { code ->
                            TextButton(onClick = { model.updateDefaultAudio(code); choice = null }, modifier = Modifier.fillMaxWidth()) {
                                RadioButton(selected = model.defaultAudio == code, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (code == "Original") "Source default" else audioLabel(code), Modifier.weight(1f))
                            }
                        }
                    }
                    "speed" -> {
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_episodes_at_a_time), style = MaterialTheme.typography.titleSmall)
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_other_episodes_wait_their_turn_direct_file_downloads_are_managed_), style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3, 4, 6).forEach { count ->
                                FilterChip(selected = model.simultaneousDownloads == count, onClick = { model.updateSimultaneousDownloads(count) }, label = { Text("$count") })
                            }
                        }
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_download_connections), style = MaterialTheme.typography.titleSmall)
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_more_connections_may_speed_up_downloads_but_use_more_network_reso), style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2, 4, 8, 12).forEach { count ->
                                FilterChip(selected = model.connections == count, onClick = { model.setDownloadConnections(count) }, label = { Text("$count") })
                            }
                        }
                    }
                    else -> {
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_saved_cookies_are_shared_with_searches_and_downloads))
                        FilledTonalButton(onClick = { choice = null; model.verifyManually() }, enabled = !busy) { Text(androidx.compose.ui.res.stringResource(R.string.ui_open_verification)) }
                        OutlinedButton(onClick = { choice = null; showClear = true }, enabled = !busy) { Text(if (model.clearingSession) "Clearing…" else "Clear session data") }
                        if (busy) Text(androidx.compose.ui.res.stringResource(R.string.ui_available_when_the_current_request_finishes), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { choice = null }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_done)) } })
    }
    if (showClear) AlertDialog(onDismissRequest = { showClear = false }, icon = { AppSymbol(AppIcon.Shield) },
        title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_clear_your_session)) }, text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_you_may_need_to_verify_again_this_removes_cookies_website_data_an)) },
        confirmButton = { TextButton(onClick = { showClear = false; model.clearSessionData() }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_clear_session)) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_cancel)) } })
    if (editFolder) AlertDialog(onDismissRequest = { editFolder = false }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_save_inside_downloads)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(androidx.compose.ui.res.stringResource(R.string.ui_choose_a_subfolder_or_leave_empty_to_use_downloads_itself))
            OutlinedTextField(value = folder, onValueChange = { folder = it; invalid = false }, singleLine = true,
                label = { Text(androidx.compose.ui.res.stringResource(R.string.ui_subfolder)) }, prefix = { Text("Downloads/") }, isError = invalid,
                supportingText = { if (invalid) Text(androidx.compose.ui.res.stringResource(R.string.ui_use_folder_names_without_or_special_characters)) })
        } }, confirmButton = { TextButton(onClick = { if (model.saveOutputFolder(folder)) editFolder = false else invalid = true }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_save)) } },
        dismissButton = { TextButton(onClick = { editFolder = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_cancel)) } })
}

@Composable
internal fun AboutScreen() {
    val context = LocalContext.current
    val info = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(androidx.compose.ui.res.stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text("Version ${info.versionName ?: "Unknown"} · Build ${info.longVersionCode}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text(androidx.compose.ui.res.stringResource(R.string.ui_what_s_new), style = MaterialTheme.typography.titleLarge)
            Text("Version ${info.versionName ?: "Unknown"}", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
        }
        items(listOf(
            "Faster video saving and smoother progress updates.",
            "Choose how many episodes download at once.",
            "Clearer download statuses and a simpler downloads list.",
            "Session and playback improvements."
        )) { change ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("•", color = MaterialTheme.colorScheme.primary)
                Text(change, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
