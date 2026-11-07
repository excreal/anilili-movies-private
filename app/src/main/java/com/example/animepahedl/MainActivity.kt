@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.animepahedl

import com.example.animepahedl.components.*
import androidx.compose.animation.animateContentSize

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
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import coil.imageLoader
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import com.example.animepahedl.ui.theme.AnimepaheDLTheme

class MainActivity : ComponentActivity() {
    private lateinit var model: DownloaderViewModel
    private lateinit var siteDownload: SiteDownloadViewModel
    override fun onResume() {
        super.onResume()
        if (::model.isInitialized) model.setForeground(true)
    }
    override fun onPause() {
        if (::model.isInitialized) model.setForeground(false)
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
        siteDownload = ViewModelProvider(this)[SiteDownloadViewModel::class.java]
        openFromIntent(intent)
        val images = imageLoader
        setContent { AnimepaheDLTheme { DownloaderApp(model, siteDownload, images) } }
    }
}

@Composable
private fun DownloaderApp(model: DownloaderViewModel, siteDownload: SiteDownloadViewModel, images: ImageLoader) {
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val playbackScope = rememberCoroutineScope()
    var openingPlayer by remember { mutableStateOf(false) }
    var errorDetails by remember { mutableStateOf(false) }
    var confirmEnqueue by remember { mutableStateOf(false) }
    var removeId by remember { mutableStateOf<String?>(null) }
    var confirmBulkDelete by remember { mutableStateOf(false) }
    var bulkMenu by remember { mutableStateOf(false) }
    var enqueueAfterFolder by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val outputPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val action = enqueueAfterFolder
        enqueueAfterFolder = null
        if (uri != null) model.chooseOutputTree(uri) {
            if (action == "anime") model.downloadSelected()
            if (action == "sites") siteDownload.downloadSelected()
        }
    }
    fun chooseOutputFolder(action: String? = null) {
        enqueueAfterFolder = action
        val initial = model.outputTree.takeIf { it.isNotBlank() }?.toUri() ?: android.provider.DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents", "primary:Download/${model.outputFolder}")
        outputPicker.launch(initial)
    }
    val screenState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val anime = model.selectedAnime.takeUnless { model.siteDownloadSelected }
    BackHandler(enabled = model.selectionMode && model.showDownloads) { model.exitSelection() }
    BackHandler(enabled = !model.selectionMode && (model.showAbout || model.showSettings || model.showDownloads || anime != null)) {
        if (model.selectionMode && model.showDownloads) model.exitSelection() else if (model.showAbout) model.showAbout = false else if (model.showSettings) model.showSettings = false else if (model.showDownloads) model.showDownloads = false else model.back()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 56.dp,
                title = { Text(if (model.showDownloads && model.selectionMode) "${model.selectedDownloadIds.size} selected" else if (model.showAbout) "About" else if (model.showSettings) "Settings" else if (model.showDownloads) "Downloads" else if (anime != null) "Episodes" else "Explore", maxLines = 1) },
                navigationIcon = {
                    if (model.showDownloads && model.selectionMode) IconButton(onClick = model::exitSelection) {
                        AppSymbol(AppIcon.Close, description = "Exit selection")
                    } else if (model.showAbout || (anime != null && !model.showDownloads && !model.showSettings)) IconButton(onClick = {
                        if (model.showAbout) model.showAbout = false else model.back()
                    }) { AppSymbol(AppIcon.Back, description = "Back") }
                },
                actions = {
                    if (model.showDownloads && model.selectionMode) {
                        IconButton(onClick = {
                            val all = model.downloads.map { it.id }.toSet()
                            if (model.selectedDownloadIds.containsAll(all)) model.clearDownloadSelection() else model.selectDownloads(all)
                        }) {
                            AppSymbol(AppIcon.Check, description = "Select all downloads")
                        }
                        IconButton(onClick = { confirmBulkDelete = true }, enabled = model.selectedDownloadIds.isNotEmpty() && !model.deletingDownloads) {
                            AppSymbol(AppIcon.Delete, description = "Delete selected downloads")
                        }
                        Box {
                            IconButton(onClick = { bulkMenu = true }) { AppSymbol(AppIcon.More, description = "More selection actions") }
                            DropdownMenu(bulkMenu, onDismissRequest = { bulkMenu = false }) {
                                val selected = model.downloads.filter { it.id in model.selectedDownloadIds }
                                listOf(DownloadAction.PAUSE to "Pause", DownloadAction.RESUME to "Resume",
                                    DownloadAction.RETRY to "Retry failed", DownloadAction.RETRY_SAVE to "Retry saves").forEach { (action, label) ->
                                    if (selected.any { action in it.actions }) DropdownMenuItem(text = { Text(label) }, onClick = {
                                        bulkMenu = false; model.performSelectionAction(action)
                                    })
                                }
                                DropdownMenuItem(text = { Text("Clear selection") }, enabled = selected.isNotEmpty(), onClick = {
                                    bulkMenu = false; model.clearDownloadSelection()
                                })
                            }
                        }
                    } else if (model.showDownloads) {
                        IconButton(onClick = { model.enterSelection() }, enabled = model.downloads.isNotEmpty()) {
                            AppSymbol(AppIcon.Check, description = "Select downloads")
                        }
                    } else if (!model.siteDownloadSelected && !model.showSettings && !model.showAbout) IconButton(onClick = model::verifyManually) {
                        AppSymbol(AppIcon.Shield, description = "Verify session")
                    }
                }

            )
        },
        bottomBar = {
            Column {
            if (anime != null && !model.showDownloads && !model.showSettings) Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { DestinationSummary(outputLabel(model)) }
                    if (!model.resolving) Button(onClick = { confirmEnqueue = true },
                        enabled = !model.checkingExistingFiles && model.selectedEpisodes.isNotEmpty() && !model.loadingSources && model.availableSources.isNotEmpty(),
                        modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Download (${model.selectedEpisodes.size})")
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
        val noticeHeight = 88.dp
        Column(Modifier.fillMaxSize()) {
        Column(Modifier.heightIn(max = noticeHeight).verticalScroll(rememberScrollState())) {
            if (model.downloadVerificationCount > 0) Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${model.downloadVerificationCount} download(s) need session verification", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { model.showAbout = false; model.showSettings = false; model.showDownloads = true }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_view)) }
                }
            }
            model.error?.let { message ->
                AppCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
            if (!model.showDownloads && !model.showSettings && !model.showAbout && anime == null) {
                SecondaryTabRow(selectedTabIndex = if (model.siteDownloadSelected) 1 else 0) {
                    Tab(selected = !model.siteDownloadSelected, onClick = { model.selectExploreSource(false) }, text = { Text("AnimePahe") })
                    Tab(selected = model.siteDownloadSelected, onClick = { model.selectExploreSource(true) }, text = { Text("Video links") })
                }
            }
            when {
                model.showAbout -> AboutScreen()
                model.showSettings -> SettingsScreen(model, onAbout = { model.showAbout = true })
                model.showDownloads -> screenState.SaveableStateProvider("downloads") { DownloadsScreen(model, siteDownload, images, onChooseFolder = { chooseOutputFolder() }, onPlay = { row ->
                    if (!openingPlayer) {
                        openingPlayer = true
                        playbackScope.launch {
                            try {
                                openSavedVideo(context, row)?.let { message -> android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show() }
                            } finally { openingPlayer = false }
                        }
                    }
                }, onRemove = { removeId = it }) }
                model.siteDownloadSelected -> screenState.SaveableStateProvider("sites") {
                    SiteDownloadScreen(siteDownload, model, images, onEnqueue = {
                        if (model.outputAccessGranted && !model.outputCheckRequired) { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS); true }
                        else { chooseOutputFolder("sites"); false }
                    })
                }
                anime != null -> DetailsScreen(model, anime, images)
                else -> SearchScreen(model, images)
            }
        }
    }
    }
    if (confirmEnqueue) AlertDialog(onDismissRequest = { confirmEnqueue = false }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_download_selected_episodes)) },
        text = { Text("${model.selectedEpisodes.size} episodes · ${model.quality} · ${audioLabel(model.audio)}\nSave to ${outputLabel(model)}" + if (!model.outputAccessGranted) "\nChoose the existing downloads folder so matching videos can be reused." else "") },
        confirmButton = { TextButton(onClick = { confirmEnqueue = false
            if (model.outputAccessGranted && !model.outputCheckRequired) { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS); model.downloadSelected() }
            else chooseOutputFolder("anime") }) { Text(if (!model.outputAccessGranted || model.outputCheckRequired) "Choose folder" else androidx.compose.ui.res.stringResource(R.string.ui_download)) } },
        dismissButton = { TextButton(onClick = { confirmEnqueue = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_cancel)) } })
    if (confirmBulkDelete) {
        val selected = model.downloads.filter { it.id in model.selectedDownloadIds }
        AlertDialog(onDismissRequest = { confirmBulkDelete = false }, title = { Text("Delete ${selected.size} items?") },
            text = { Text("This deletes ${selected.count { it.complete }} saved videos and cancels ${selected.count { !it.complete }} unfinished downloads.") },
            confirmButton = { TextButton(onClick = { confirmBulkDelete = false; model.removeDownloads(selected.map { it.id }.toSet(), selected.filter { it.complete }.map { it.id }.toSet()) }, enabled = selected.isNotEmpty()) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmBulkDelete = false }) { Text("Keep") } })
    }
    if (errorDetails) AlertDialog(onDismissRequest = { errorDetails = false }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_request_details)) },
        text = { Text(model.error.orEmpty(), Modifier.verticalScroll(rememberScrollState())) }, confirmButton = { TextButton(onClick = { errorDetails = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_close)) } })
    model.verification?.let { url -> SessionBrowser(url, onClose = model::closeVerification) }
    removeId?.let { id ->
        val row = model.downloads.firstOrNull { it.id == id }
        AlertDialog(onDismissRequest = { removeId = null },
            title = { Text(if (row?.complete == true) "Delete saved video?" else "Cancel download?") },
            text = { Text("${row?.title.orEmpty()}\n\n${if (row?.complete == true) "This permanently deletes the saved video." else "This cancels the download and removes its partial files."}") },
            confirmButton = { TextButton(onClick = { model.removeDownload(id, deleteSavedFile = row?.complete == true); removeId = null }) {
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
        if (model.query.isBlank() && !model.searched && !model.searching) {
            Spacer(Modifier.fillParentMaxHeight(0.12f))
            Text("Find your next watch", Modifier.padding(horizontal = 20.dp, vertical = 12.dp), style = MaterialTheme.typography.headlineSmall)
        }
        OutlinedTextField(value = model.query, onValueChange = model::updateQuery,
            label = { Text(androidx.compose.ui.res.stringResource(R.string.ui_search_anime)) }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.medium, leadingIcon = { IconButton(onClick = { keyboard?.hide(); model.search() }, enabled = model.query.isNotBlank()) { AppSymbol(AppIcon.Browse, description = "Search") } },
            trailingIcon = { if (model.query.isNotEmpty()) IconButton(onClick = { model.updateQuery("") }) { AppSymbol(AppIcon.Close, description = "Clear search") } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); model.search() }))
        if (model.searched) FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    model.recentSearches.filter { it.length > 2 }.take(4).forEach { term -> AssistChip(onClick = { model.updateQuery(term) }, label = { Text(term) }) }
                    if (model.recentSearches.isNotEmpty()) TextButton(onClick = model::clearRecentSearches) { Text(androidx.compose.ui.res.stringResource(R.string.ui_clear_history)) }
                }
            }
            items(model.bookmarks, key = { "bookmark:${it.session}" }) { saved ->
                TextButton(onClick = { model.open(saved) }, modifier = Modifier.fillMaxWidth()) { Text("Saved show · ${saved.title}") }
            }
        }
        when {
            model.searching && model.results.isEmpty() -> item { Loading("Searching AnimePahe…") }
            !model.searched -> Unit
            visible.isEmpty() -> item { EmptyState("No matching anime", "Try another anime title.") }
            else -> {
                item { Text("${visible.size} results", style = MaterialTheme.typography.bodySmall) }
                items(visible, key = { it.session }) { anime ->
                    AppCard(onClick = { model.open(anime) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Thumbnail(anime.poster, anime.title, images, Modifier.size(56.dp, 80.dp))
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
    var synopsisExpanded by androidx.compose.runtime.saveable.rememberSaveable(anime.session) { mutableStateOf(false) }
    var rangeOpen by androidx.compose.runtime.saveable.rememberSaveable(anime.session) { mutableStateOf(false) }
    var rangeStart by androidx.compose.runtime.saveable.rememberSaveable(anime.session) { mutableStateOf("") }
    var rangeEnd by androidx.compose.runtime.saveable.rememberSaveable(anime.session) { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Thumbnail(anime.poster, anime.title, images, Modifier.size(72.dp, 104.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(anime.title, style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { model.toggleBookmark(anime) }) { Text(if (model.bookmarks.any { it.session == anime.session }) "Remove bookmark" else "Bookmark show") }
                    Text(listOf(anime.year, anime.type, anime.status).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (model.loadingDetails) item { Loading("Loading details and all episodes…") }
        if (details != null) {
            if (details.anime.synopsis.isNotBlank()) item {
                Column(Modifier.animateContentSize()) {
                    Text(details.anime.synopsis, style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    TextButton(onClick = { synopsisExpanded = !synopsisExpanded }) { Text(if (synopsisExpanded) "Show less" else "Read more") }
                }
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { rangeOpen = !rangeOpen }) { Text(if (rangeOpen) "Hide range" else "Select range") }
                    TextButton(onClick = model::selectMissingEpisodes) { Text("Select missing") }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = model::clearEpisodes) { AppSymbol(AppIcon.Close, description = "Clear selection") }
                }
                androidx.compose.animation.AnimatedVisibility(rangeOpen) { Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(rangeStart, { rangeStart = it }, label = { Text(androidx.compose.ui.res.stringResource(R.string.ui_from)) }, singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f))
                    OutlinedTextField(rangeEnd, { rangeEnd = it }, label = { Text(androidx.compose.ui.res.stringResource(R.string.ui_to)) }, singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f))
                }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { model.selectEpisodeRange(rangeStart, rangeEnd) }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_select_range)) }
                }
                }
            }
            }
            if (details.episodes.isEmpty()) item { Text(androidx.compose.ui.res.stringResource(R.string.ui_no_episodes_have_been_released_yet)) }
            items(details.episodes, key = { it.session }) { episode ->
                val selected = episode.session in model.selectedEpisodes
                AppCard(onClick = { model.toggle(episode) }, modifier = Modifier.semantics { this.selected = selected; role = androidx.compose.ui.semantics.Role.Checkbox },
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
