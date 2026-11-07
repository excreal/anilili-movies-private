@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.animepahedl

import com.example.animepahedl.components.*
import androidx.compose.animation.animateContentSize

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SiteDownloadScreen(model: SiteDownloadViewModel, downloads: DownloaderViewModel, images: ImageLoader, onEnqueue: () -> Boolean) {
    val keyboard = LocalSoftwareKeyboardController.current
    var cookiesOpen by rememberSaveable { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Add a video link", style = MaterialTheme.typography.titleMedium)
            Text("Paste a supported video or series link, or a direct HLS/MP4/WebM URL.", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            OutlinedTextField(model.input, model::updateInput, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                label = { Text("Video or series URL") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { keyboard?.hide(); model.inspect() }))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { keyboard?.hide(); model.inspect() }, enabled = model.input.isNotBlank() && !model.inspecting) { Text("Inspect") }
                if (model.inspecting) TextButton(onClick = model::cancelInspect) { Text("Cancel") }
                TextButton(onClick = { cookiesOpen = true }) { Text(if (model.hasCookies) "Session cookies saved" else "Import session cookies") }
            }
            Text("Dramatip · Kiss18 · Narto Drama · EroticMV · direct video", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (model.inspecting) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        model.notice?.let { notice -> item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(notice, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = model::dismissNotice) { AppSymbol(AppIcon.Close, description = "Dismiss notice") }
            }
        } }
        model.error?.let { message -> item {
            AppCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(12.dp)) { Text(message); TextButton(onClick = model::dismissError) { Text("Dismiss") } }
            }
        } }
        model.series?.let { series ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (series.poster.isNotBlank()) Thumbnail(series.poster, series.title, images, Modifier.size(72.dp, 104.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(series.title, style = MaterialTheme.typography.titleLarge)
                        Text(if (series.isSingleVideo) "Single video" else "${series.episodes.size} episodes", style = MaterialTheme.typography.bodyMedium)
                        ChoiceMenu(model.quality, (listOf("Best") + series.qualities).distinct(), "Quality", !model.preparing, model::chooseQuality)
                    }
                }
            }
            item {
                if (!series.isSingleVideo) {
                    OutlinedTextField(model.selection, model::updateSelection, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Episodes, for example 1-5,9") }, singleLine = true, shape = MaterialTheme.shapes.medium,
                        isError = model.selectionError != null,
                        supportingText = { Text(model.selectionError ?: "Blank selects all. Enter none to select zero episodes.") })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { model.updateSelection("") }) { Text("Select all") }
                        TextButton(onClick = { model.updateSelection("none") }) { Text("Clear selection") }
                    }
                }
                DestinationSummary(outputLabel(downloads))
                Button(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth(),
                    enabled = !downloads.checkingExistingFiles && model.ready && !model.preparing && !model.discarding && model.pending == null && model.selectionError == null && model.selectedEpisodes.isNotEmpty()) {
                    Text(if (series.isSingleVideo) "Download video" else "Download ${model.selectedEpisodes.size} selected episode${if (model.selectedEpisodes.size == 1) "" else "s"}")
                }
                if (model.pending != null) Text("Links are being prepared in the background. Manage the batch in Downloads.", style = MaterialTheme.typography.bodySmall)
            }
            if (!series.isSingleVideo) items(series.episodes, key = { it.id }) { episode ->
                val selected = model.selectedEpisodes.any { it.id == episode.id }
                AppCard(onClick = { model.toggleEpisode(episode) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Checkbox(selected, null)
                        Column(Modifier.weight(1f)) {
                            Text("Episode ${episode.number}", style = MaterialTheme.typography.titleSmall)
                            if (episode.title.isNotBlank()) Text(episode.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
    if (cookiesOpen) SiteDownloadCookiesDialog(model, onClose = { cookiesOpen = false })
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(if (model.series?.isSingleVideo == true) "Download video?" else "Download selected episodes?") },
        text = { Text("${if (model.series?.isSingleVideo == true) "Video" else "${model.selectedEpisodes.size} episodes"} · ${model.quality}\nSave to ${outputLabel(downloads)}") },
        confirmButton = { TextButton(onClick = { confirm = false; if (onEnqueue()) model.downloadSelected() }) { Text("Download") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
}

@Composable
private fun SiteDownloadCookiesDialog(model: SiteDownloadViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readNBytes(262_145)
                        require(bytes.size <= 262_144)
                        bytes.toString(Charsets.UTF_8)
                    } ?: error("Cannot read cookie file")
                }
                if (model.saveCookies(text)) onClose() else error = "Invalid cookie format. Use a Netscape cookie file."
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: Exception) { error = "Could not import this file. Use a text cookie file no larger than 256 KiB." }
            finally { importing = false }
        }
    }
    AlertDialog(onDismissRequest = { if (!importing) onClose() }, title = { Text("Session cookies") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Import a Netscape cookie file with domain information, or paste its contents. Cookies stay in app-private storage.")
            OutlinedTextField(input, { input = it; error = null }, label = { Text("Netscape cookie text") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp), enabled = !importing)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (importing) LinearProgressIndicator(Modifier.fillMaxWidth())
            TextButton(onClick = { importer.launch(arrayOf("text/*", "application/octet-stream")) }, enabled = !importing) { Text("Import cookie file") }
            if (model.hasCookies) TextButton(onClick = { model.clearCookies(); onClose() }, enabled = !importing) { Text("Remove saved cookies") }
        } },
        confirmButton = { TextButton(onClick = {
            if (model.saveCookies(input)) onClose() else error = "Invalid cookie format. Use a Netscape cookie file."
        }, enabled = !importing && input.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onClose, enabled = !importing) { Text("Cancel") } })
}
