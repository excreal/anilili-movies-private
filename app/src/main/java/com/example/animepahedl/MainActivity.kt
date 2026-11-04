@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.animepahedl

import androidx.core.net.toUri
import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
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
    private lateinit var model: DownloaderViewModel
    private val savedFilesObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { if (::model.isInitialized) model.refreshSavedFiles() }
    }

    override fun onResume() {
        super.onResume()
        if (::model.isInitialized) {
            contentResolver.registerContentObserver(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, true, savedFilesObserver)
            val trees = Exports.all(this).values.map { it.tree }.filter { it.isNotBlank() }.toSet() + AppSettings(this).outputTree
            trees.filter { it.isNotBlank() }.forEach { tree ->
                runCatching { contentResolver.registerContentObserver((tree).toUri(), true, savedFilesObserver) }
            }
            model.setForeground(true)
            model.refreshSavedFiles()
        }
    }

    override fun onPause() {
        if (::model.isInitialized) model.setForeground(false)
        contentResolver.unregisterContentObserver(savedFilesObserver)
        super.onPause()
    }

    companion object { const val ACTION_DOWNLOADS = "com.example.animepahedl.OPEN_DOWNLOADS" }

    private fun openFromIntent(intent: Intent?) {
        if (intent?.action == ACTION_DOWNLOADS) {
            model.showDownloads = true
            model.showSettings = false
            model.showAbout = false
            intent.action = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Request the fastest same-resolution mode up to 120 Hz; system power policy still applies.
        display?.let { screen ->
            val current = screen.mode
            screen.supportedModes.filter { it.physicalWidth == current.physicalWidth &&
                it.physicalHeight == current.physicalHeight && it.refreshRate <= 120.1f }
                .maxByOrNull { it.refreshRate }?.let { mode ->
                    window.attributes = window.attributes.apply { preferredDisplayModeId = mode.modeId }
                }
        }
        model = ViewModelProvider(this)[DownloaderViewModel::class.java]
        openFromIntent(intent)
        val images = ImageLoader.Builder(this).okHttpClient(AnimeNetwork.client).build()
        setContent { AnimepaheDLTheme { DownloaderApp(model, images) } }
    }
}

@Composable
private fun DownloaderApp(model: DownloaderViewModel, images: ImageLoader) {
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var playing by remember { mutableStateOf<String?>(null) }
    var errorDetails by remember { mutableStateOf(false) }
    var confirmEnqueue by remember { mutableStateOf(false) }
    var removeId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val screenState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val anime = model.selectedAnime
    BackHandler(enabled = model.selectionMode && model.showDownloads) { model.exitSelection() }
    BackHandler(enabled = !model.selectionMode && (model.showAbout || model.showSettings || model.showDownloads || anime != null)) {
        if (model.selectionMode && model.showDownloads) model.exitSelection() else if (model.showAbout) model.showAbout = false else if (model.showSettings) model.showSettings = false else if (model.showDownloads) model.showDownloads = false else model.back()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (model.showAbout) "About" else if (model.showSettings) "Settings" else if (model.showDownloads) "Downloads" else if (anime != null) "Episodes" else "Explore", maxLines = 1) },
                navigationIcon = {
                    if (model.showAbout || model.showSettings || model.showDownloads || anime != null) IconButton(onClick = {
                        if (model.selectionMode && model.showDownloads) model.exitSelection() else if (model.showAbout) model.showAbout = false else if (model.showSettings) model.showSettings = false else if (model.showDownloads) model.showDownloads = false else model.back()
                    }) { AppSymbol(AppIcon.Back, description = "Back") }
                },
                actions = { IconButton(onClick = model::verifyManually) { AppSymbol(AppIcon.Shield, description = "Verify session") } }

            )
        },
        bottomBar = {
            Column {
            if (anime != null && !model.showDownloads && !model.showSettings) Surface(tonalElevation = 3.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DestinationSummary(outputLabel(model))
                    if (!model.resolving) Button(onClick = {
                        confirmEnqueue = true
                    }, enabled = model.selectedEpisodes.isNotEmpty() && !model.loadingSources && model.availableSources.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.episodes_selected, model.selectedEpisodes.size, model.selectedEpisodes.size))
                    }
                }
            }
            NavigationBar( containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                NavigationBarItem(selected = !model.showDownloads && !model.showSettings,
                    onClick = { model.showAbout = false; model.showDownloads = false; model.showSettings = false },
                    icon = { AppSymbol(AppIcon.Browse) }, label = { Text(androidx.compose.ui.res.stringResource(R.string.ui_explore)) })
                NavigationBarItem(selected = model.showDownloads && !model.showSettings,
                    onClick = { model.showAbout = false; model.showDownloads = true; model.showSettings = false },
                    icon = { AppSymbol(AppIcon.Download) }, label = { Text(androidx.compose.ui.res.stringResource(R.string.ui_downloads), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) })
                NavigationBarItem(selected = model.showSettings, onClick = { model.showAbout = false; model.showSettings = true },
                    icon = { AppSymbol(AppIcon.Settings) }, label = { Text(androidx.compose.ui.res.stringResource(R.string.ui_settings)) })
            }
            }
        }
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        val noticeHeight = maxHeight * 0.3f
        Column(Modifier.fillMaxSize()) {
        Column(Modifier.heightIn(max = noticeHeight).verticalScroll(rememberScrollState())) {
            model.pendingPreparation?.let { pending ->
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(if (model.resolving) model.resolvingLabel else "${pending.episodes.size} episodes waiting to prepare",
                            style = MaterialTheme.typography.bodyMedium)
                        if (model.resolving) {
                            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(androidx.compose.ui.res.stringResource(R.string.ui_you_can_keep_browsing), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = model::cancelPreparation) { Text(androidx.compose.ui.res.stringResource(R.string.ui_pause)) }
                            }
                        } else Row {
                            TextButton(onClick = model::resumePreparation) { Text(androidx.compose.ui.res.stringResource(R.string.ui_resume)) }
                            TextButton(onClick = model::discardPreparation) { Text(androidx.compose.ui.res.stringResource(R.string.ui_discard)) }
                        }
                    }
                }
            }
            if (model.downloadVerificationCount > 0) Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${model.downloadVerificationCount} download(s) need session verification", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { model.showAbout = false; model.showSettings = false; model.showDownloads = true }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_view)) }
                }
            }
            model.error?.let { message ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_request_couldn_t_finish), style = MaterialTheme.typography.titleSmall)
                        Text(message, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        FlowRow {
                            TextButton(onClick = model::retry) { Text(androidx.compose.ui.res.stringResource(R.string.ui_retry)) }
                            TextButton(onClick = model::verifyManually) { Text(androidx.compose.ui.res.stringResource(R.string.ui_verify_session)) }
                            TextButton(onClick = { errorDetails = true }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_details)) }
                            IconButton(onClick = model::clearError) { AppSymbol(AppIcon.Close, description = "Dismiss error") }
                        }
                    }
                }
            }
        }
            when {
                model.showAbout -> AboutScreen()
                model.showSettings -> SettingsScreen(model, onAbout = { model.showAbout = true })
                model.showDownloads -> screenState.SaveableStateProvider("downloads") { DownloadsScreen(model, images, onPlay = { row ->
                    playing = row.id
                }, onRemove = { removeId = it }) }
                anime != null -> DetailsScreen(model, anime, images)
                else -> SearchScreen(model, images)
            }
        }
    }
    }
    if (confirmEnqueue) AlertDialog(onDismissRequest = { confirmEnqueue = false }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_download_selected_episodes)) },
        text = { Text("${model.selectedEpisodes.size} episodes · ${model.quality} · ${audioLabel(model.audio)}\nSave to ${outputLabel(model)}") },
        confirmButton = { TextButton(onClick = { confirmEnqueue = false; notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS); model.downloadSelected() }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_download)) } },
        dismissButton = { TextButton(onClick = { confirmEnqueue = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_cancel)) } })
    if (errorDetails) AlertDialog(onDismissRequest = { errorDetails = false }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_request_details)) },
        text = { Text(model.error.orEmpty(), Modifier.verticalScroll(rememberScrollState())) }, confirmButton = { TextButton(onClick = { errorDetails = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_close)) } })
    model.verification?.let { url -> SessionBrowser(url, onClose = model::closeVerification) }
    playing?.let { id -> SavedPlayer(id, model.downloads, onClose = { playing = null }, onNext = { playing = it }) }
    removeId?.let { id ->
        val row = model.downloads.firstOrNull { it.id == id }
        AlertDialog(onDismissRequest = { removeId = null },
            title = { Text(if (row?.complete == true) "Delete saved video?" else "Cancel download?") },
            text = { Text("${row?.title.orEmpty()}\n\n${if (row?.complete == true) "This permanently deletes the saved video." else "This cancels the download and removes its partial files."}") },
            confirmButton = { TextButton(onClick = { model.removeDownload(id); removeId = null }) {
                Text(if (row?.complete == true) "Delete" else "Cancel download")
            } },
            dismissButton = { TextButton(onClick = { removeId = null }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_keep)) } })
    }
}

@Composable
private fun SearchScreen(model: DownloaderViewModel, images: ImageLoader) {
    val keyboard = LocalSoftwareKeyboardController.current
    val visible = sortResults(model.results, model.sort)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
        if (!model.searched && !model.searching) Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(androidx.compose.ui.res.stringResource(R.string.ui_animepahe), style = MaterialTheme.typography.displayMedium)
            Text(androidx.compose.ui.res.stringResource(R.string.ui_find_a_story_worth_saving), Modifier.padding(top = 6.dp, start = 16.dp, end = 16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextField(value = model.query, onValueChange = model::updateQuery,
            label = { Text(androidx.compose.ui.res.stringResource(R.string.ui_search_anime)) }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 64.dp),
            shape = CircleShape, leadingIcon = { IconButton(onClick = { keyboard?.hide(); model.search() }, enabled = model.query.isNotBlank()) { AppSymbol(AppIcon.Browse, description = "Search") } },
            trailingIcon = { if (model.query.isNotEmpty()) IconButton(onClick = { model.updateQuery("") }) { AppSymbol(AppIcon.Close, description = "Clear search") } },
            colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); model.search() }))
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceMenu(model.sort.label, ResultSort.entries.map { it.label }, "Sort") { label ->
                model.sort = ResultSort.entries.first { it.label == label }
            }
        }
        if (model.searching && model.results.isNotEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
        model.searchFailure?.let { failure ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(failure.message, style = MaterialTheme.typography.bodySmall)
                Row {
                    if (failure.verificationNeeded) TextButton(onClick = model::verifySearch) { Text(androidx.compose.ui.res.stringResource(R.string.ui_verify)) }
                    TextButton(onClick = model::retrySearch) { Text(androidx.compose.ui.res.stringResource(R.string.ui_retry)) }
                }
            }
        }
        }
        if (model.query.isBlank()) {
            item {
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    model.recentSearches.take(8).forEach { term -> AssistChip(onClick = { model.updateQuery(term) }, label = { Text(term) }) }
                    if (model.recentSearches.isNotEmpty()) TextButton(onClick = model::clearRecentSearches) { Text(androidx.compose.ui.res.stringResource(R.string.ui_clear_history)) }
                }
            }
            items(model.bookmarks, key = { "bookmark:${it.session}" }) { saved ->
                TextButton(onClick = { model.open(saved) }, modifier = Modifier.fillMaxWidth()) { Text("Saved show · ${saved.title}") }
            }
        }
        when {
            model.searching && model.results.isEmpty() -> item { Loading("Searching AnimePahe…") }
            !model.searched -> item { EmptyState("Good stories, ready to go.", "Find an anime you love, choose your episodes, and keep them for later.") }
            visible.isEmpty() -> item { EmptyState("No matching anime", "Try another anime title.") }
            else -> {
                item { Text("${visible.size} results", style = MaterialTheme.typography.bodySmall) }
                items(visible, key = { it.session }) { anime ->
                    Card(onClick = { model.open(anime) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
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
    var rangeStart by androidx.compose.runtime.saveable.rememberSaveable(anime.session) { mutableStateOf("") }
    var rangeEnd by androidx.compose.runtime.saveable.rememberSaveable(anime.session) { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Thumbnail(anime.poster, anime.title, images, Modifier.size(100.dp, 144.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(anime.title, style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = { model.toggleBookmark(anime) }) { Text(if (model.bookmarks.any { it.session == anime.session }) "Remove bookmark" else "Bookmark show") }
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
                    ChoiceMenu(if (model.availableSources.isEmpty()) "—" else model.quality.ifBlank { "—" }, model.availableQualities, "Quality", !model.loadingSources && model.availableQualities.isNotEmpty()) { model.quality = it }
                    ChoiceMenu(if (model.availableSources.isEmpty()) "—" else audioLabel(model.audio), model.availableAudio.map(::audioLabel), "Audio", !model.loadingSources && model.availableAudio.isNotEmpty()) { label -> model.availableAudio.firstOrNull { audioLabel(it) == label }?.let(model::chooseAudio) }
                }
                if (model.loadingSources) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
                Text(model.sourceChoicesLabel,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${details.episodes.size} episodes", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = model::selectAll, enabled = details.episodes.isNotEmpty()) {
                        Text(if (model.selectedEpisodes.size == details.episodes.size && details.episodes.isNotEmpty()) "Deselect all" else "Select all")
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(rangeStart, { rangeStart = it }, label = { Text(androidx.compose.ui.res.stringResource(R.string.ui_from)) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(rangeEnd, { rangeEnd = it }, label = { Text(androidx.compose.ui.res.stringResource(R.string.ui_to)) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { model.selectEpisodeRange(rangeStart, rangeEnd) }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_select_range)) }
                    TextButton(onClick = model::selectMissingEpisodes) { Text(androidx.compose.ui.res.stringResource(R.string.ui_select_missing)) }
                    IconButton(onClick = model::clearEpisodes) { AppSymbol(AppIcon.Close, description = "Clear selection") }
                }
            }
            if (details.episodes.isEmpty()) item { Text(androidx.compose.ui.res.stringResource(R.string.ui_no_episodes_have_been_released_yet)) }
            items(details.episodes, key = { it.session }) { episode ->
                val selected = episode.session in model.selectedEpisodes
                Card(onClick = { model.toggle(episode) }, modifier = Modifier.semantics { this.selected = selected; role = androidx.compose.ui.semantics.Role.Checkbox },
                    colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Checkbox(checked = selected, onCheckedChange = null)
                        Thumbnail(episode.snapshot, "Episode ${episode.number}", images, Modifier.size(64.dp, 40.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Episode ${episode.number}", style = MaterialTheme.typography.titleSmall)
                            model.episodeAvailability(episode)?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
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
        AssistChip(onClick = { expanded = true }, label = { Text("$label: $selected") }, trailingIcon = { AppSymbol(AppIcon.Expand) }, enabled = enabled,
            shape = CircleShape, border = null,
            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.heightIn(min = 44.dp))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@Composable
internal fun Thumbnail(url: String, description: String, images: ImageLoader, modifier: Modifier, cornerRadius: androidx.compose.ui.unit.Dp = 0.dp) {
    SubcomposeAsyncImage(model = url, imageLoader = images, contentDescription = description,
        contentScale = ContentScale.Crop, modifier = modifier.clip(RoundedCornerShape(cornerRadius)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp)) } },
        error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppSymbol(AppIcon.Browse, modifier = Modifier.size(20.dp)) } })
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
                TextButton(onClick = onClose) { Text(androidx.compose.ui.res.stringResource(R.string.ui_close_player)) }
                failure?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
                AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}
