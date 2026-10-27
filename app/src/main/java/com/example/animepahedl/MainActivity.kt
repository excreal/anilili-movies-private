@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.animepahedl

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import com.example.animepahedl.ui.theme.AnimepaheDLTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model = ViewModelProvider(this)[DownloaderViewModel::class.java]
        val images = ImageLoader.Builder(this).okHttpClient(AnimeNetwork.client).build()
        setContent { AnimepaheDLTheme { DownloaderApp(model, images) } }
    }
}

@Composable
private fun DownloaderApp(model: DownloaderViewModel, images: ImageLoader) {
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var playing by remember { mutableStateOf<String?>(null) }
    var errorDetails by remember { mutableStateOf(false) }
    var removeId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val anime = model.selectedAnime
    BackHandler(enabled = model.showSettings || model.showDownloads || anime != null) {
        if (model.showSettings) model.showSettings = false else if (model.showDownloads) model.showDownloads = false else model.back()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (model.showSettings) "Settings" else if (model.showDownloads) "Downloads" else if (anime != null) "Episodes" else "Explore", maxLines = 1) },
                navigationIcon = {
                    if (model.showSettings || model.showDownloads || anime != null) IconButton(onClick = {
                        if (model.showSettings) model.showSettings = false else if (model.showDownloads) model.showDownloads = false else model.back()
                    }, enabled = !model.resolving) { AppSymbol(AppIcon.Back, "Back") }
                },
                actions = { IconButton(onClick = model::verifyManually) { AppSymbol(AppIcon.Shield, "Verify session") } }

            )
        },
        bottomBar = {
            Column {
            if (anime != null && !model.showDownloads && !model.showSettings) Surface(tonalElevation = 3.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Save to ${outputLabel(model)}", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    if (model.resolving) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(model.resolvingLabel, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = model::cancelPreparation) { Text("Cancel") }
                        }
                    } else Button(onClick = {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        model.downloadSelected()
                    }, enabled = model.selectedEpisodes.isNotEmpty() && !model.loadingSources && model.availableSources.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Text("Download selected (${model.selectedEpisodes.size})")
                    }
                }
            }
            NavigationBar(modifier = Modifier.clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)), containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                NavigationBarItem(selected = !model.showDownloads && !model.showSettings,
                    onClick = { model.showDownloads = false; model.showSettings = false },
                    icon = { AppSymbol(AppIcon.Browse) }, label = { Text("Explore") })
                NavigationBarItem(selected = model.showDownloads && !model.showSettings,
                    onClick = { model.showDownloads = true; model.showSettings = false },
                    icon = { BadgedBox(badge = { if (model.downloads.isNotEmpty()) Badge { Text("${model.downloads.size}") } }) { AppSymbol(AppIcon.Download) } }, label = { Text("Downloads") })
                NavigationBarItem(selected = model.showSettings, onClick = { model.showSettings = true },
                    icon = { AppSymbol(AppIcon.Settings) }, label = { Text("Settings") })
            }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            model.error?.let { message ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Request couldn’t finish", style = MaterialTheme.typography.titleSmall)
                        Text(message, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        FlowRow {
                            TextButton(onClick = model::retry) { Text("Retry") }
                            TextButton(onClick = model::verifyManually) { Text("Verify session") }
                            TextButton(onClick = { errorDetails = true }) { Text("Details") }
                            TextButton(onClick = model::clearError) { Text("Dismiss") }
                        }
                    }
                }
            }
            when {
                model.showSettings -> SettingsScreen(model)
                model.showDownloads -> DownloadsScreen(model, images, onPlay = { row ->
                    if (row.savedUri.isNotEmpty()) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(android.net.Uri.parse(row.savedUri), "video/*")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                        } catch (_: android.content.ActivityNotFoundException) {
                            android.widget.Toast.makeText(context, "Install a video player to open this file.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else if (row.id.startsWith("direct:")) {
                        val manager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                        val uri = manager.getUriForDownloadedFile(row.id.removePrefix("direct:").toLong())
                        if (uri != null) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/*")
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            } catch (_: android.content.ActivityNotFoundException) {
                                android.widget.Toast.makeText(context, "Install a video player to open this file.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else playing = row.id
                }, onRemove = { removeId = it })
                anime != null -> DetailsScreen(model, anime, images)
                else -> SearchScreen(model, images)
            }
        }
    }
    if (errorDetails) AlertDialog(onDismissRequest = { errorDetails = false }, title = { Text("Request details") },
        text = { Text(model.error.orEmpty()) }, confirmButton = { TextButton(onClick = { errorDetails = false }) { Text("Close") } })
    model.verification?.let { url -> SessionBrowser(url, onClose = model::closeVerification) }
    playing?.let { id -> OfflinePlayer(id, onClose = { playing = null }) }
    removeId?.let { id ->
        val row = model.downloads.firstOrNull { it.id == id }
        AlertDialog(onDismissRequest = { removeId = null },
            title = { Text(if (row?.complete == true) "Remove saved video?" else "Cancel download?") },
            text = { Text("${row?.title.orEmpty()}\n\n${if (row?.complete == true) "This permanently deletes the saved video." else "This cancels the download and removes its partial files."}") },
            confirmButton = { TextButton(onClick = { model.removeDownload(id); removeId = null }) {
                Text(if (row?.complete == true) "Remove" else "Cancel download")
            } },
            dismissButton = { TextButton(onClick = { removeId = null }) { Text("Keep") } })
    }
}

@Composable
private fun SearchScreen(model: DownloaderViewModel, images: ImageLoader) {
    val keyboard = LocalSoftwareKeyboardController.current
    val visible = filterResults(model.results, model.year, model.type, model.sort)
    Column(Modifier.fillMaxSize()) {
        if (!model.searched && !model.searching) Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Animepahe", style = MaterialTheme.typography.displayMedium)
            Text("Find a story worth saving.", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextField(value = model.query, onValueChange = model::updateQuery,
            label = { Text("Search anime") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 64.dp),
            shape = CircleShape, leadingIcon = { AppSymbol(AppIcon.Browse) },
            trailingIcon = { Row(verticalAlignment = Alignment.CenterVertically) {
                if (model.query.isNotEmpty()) TextButton(onClick = { model.updateQuery("") }) { Text("Clear") }
                FilledTonalButton(onClick = { keyboard?.hide(); model.search() },
                enabled = model.query.isNotBlank(), modifier = Modifier.padding(end = 8.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) { Text("Search") } } },
            colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); model.search() }))
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceMenu(model.year, listOf("All years") + model.results.map { it.year }.filter { it.isNotBlank() }.distinct()
                .sortedDescending(), "Release year") { model.year = it }
            ChoiceMenu(model.type, listOf("All types") + model.results.map { it.type }.filter { it.isNotBlank() }.distinct().sorted(), "Type") {
                model.type = it
            }
            ChoiceMenu(model.sort.label, ResultSort.entries.map { it.label }, "Sort") { label ->
                model.sort = ResultSort.entries.first { it.label == label }
            }
        }
        when {
            model.searching -> Loading("Searching AnimePahe…")
            !model.searched -> EmptyState("Good stories, ready to go.", "Find an anime you love, choose your episodes, and keep them for later.")
            visible.isEmpty() -> EmptyState("No matching anime", "Try another title or clear the year and type filters.")
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("${visible.size} results · Filters apply to these search results", style = MaterialTheme.typography.bodySmall) }
                items(visible, key = { it.session }) { anime ->
                    Card(onClick = { model.open(anime) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Thumbnail(anime.poster, anime.title, images, Modifier.size(80.dp, 112.dp))
                            Column(Modifier.weight(1f).align(Alignment.CenterVertically), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(anime.title, style = MaterialTheme.typography.titleMedium)
                                Text(listOf(anime.year.ifBlank { "Year unknown" }, anime.type).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (anime.status.isNotBlank()) Text(anime.status, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsScreen(model: DownloaderViewModel, anime: Anime, images: ImageLoader) {
    val details = model.details
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Thumbnail(anime.poster, anime.title, images, Modifier.size(100.dp, 144.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(anime.title, style = MaterialTheme.typography.headlineSmall)
                    Text(listOf(anime.year, anime.type, anime.status).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (model.loadingDetails) item { Loading("Loading details and all episodes…") }
        if (details != null) {
            if (details.anime.synopsis.isNotBlank()) item {
                Text(details.anime.synopsis, style = MaterialTheme.typography.bodyMedium)
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceMenu(if (model.availableSources.isEmpty()) "—" else model.quality.ifBlank { "—" }, model.availableQualities, "Quality", !model.resolving && !model.loadingSources && model.availableQualities.isNotEmpty()) { model.quality = it }
                    ChoiceMenu(if (model.availableSources.isEmpty()) "—" else audioLabel(model.audio), model.availableAudio.map(::audioLabel), "Audio", !model.resolving && !model.loadingSources && model.availableAudio.isNotEmpty()) { label -> model.availableAudio.firstOrNull { audioLabel(it) == label }?.let(model::chooseAudio) }
                }
                if (model.loadingSources) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
                Text(model.sourceChoicesLabel,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${details.episodes.size} episodes", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = model::selectAll, enabled = details.episodes.isNotEmpty() && !model.resolving) {
                        Text(if (model.selectedEpisodes.size == details.episodes.size && details.episodes.isNotEmpty()) "Deselect all" else "Select all")
                    }
                }
            }
            if (details.episodes.isEmpty()) item { Text("No episodes have been released yet.") }
            items(details.episodes, key = { it.session }) { episode ->
                val selected = episode.session in model.selectedEpisodes
                Card(onClick = { model.toggle(episode) }, enabled = !model.resolving,
                    colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Checkbox(checked = selected, onCheckedChange = null)
                        Thumbnail(episode.snapshot, "Episode ${episode.number}", images, Modifier.size(88.dp, 50.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Episode ${episode.number}", fontWeight = FontWeight.Medium)
                            if (episode.duration.isNotBlank()) Text(episode.duration, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceMenu(selected: String, options: List<String>, label: String, enabled: Boolean = true, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(onClick = { expanded = true }, label = { Text("$label: $selected ▾") }, enabled = enabled,
            shape = CircleShape, border = null,
            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.heightIn(min = 44.dp))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@Composable
internal fun Thumbnail(url: String, description: String, images: ImageLoader, modifier: Modifier, cornerRadius: androidx.compose.ui.unit.Dp = 20.dp) {
    SubcomposeAsyncImage(model = url, imageLoader = images, contentDescription = description,
        contentScale = ContentScale.Crop, modifier = modifier.clip(RoundedCornerShape(cornerRadius)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp)) } },
        error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No image", style = MaterialTheme.typography.labelSmall) } })
}

@Composable
private fun Loading(message: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) { CircularProgressIndicator(); Text(message) }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Surface(Modifier.fillMaxWidth().padding(20.dp), shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                AppSymbol(AppIcon.Play, modifier = Modifier.padding(16.dp).size(28.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun OfflinePlayer(id: String, onClose: () -> Unit) {
    val context = LocalContext.current
    var failure by remember { mutableStateOf<String?>(null) }
    val player = remember(id) {
        val manager = OfflineDownloads.manager(context)
        val source = CacheDataSource.Factory().setCache(OfflineDownloads.cache)
            .setUpstreamDataSourceFactory(null).setCacheWriteDataSinkFactory(null)
        ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(source)).build().apply {
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) { failure = "Offline playback failed: ${error.errorCodeName}" }
            })
            manager.downloadIndex.getDownload(id)?.request?.let { setMediaItem(it.toMediaItem()); prepare(); playWhenReady = true }
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding()) {
                TextButton(onClick = onClose) { Text("Close player") }
                failure?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
                AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}
