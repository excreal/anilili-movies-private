package com.example.animepahedl

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class DownloadRow(val id: String, val title: String, val status: String, val percent: Float,
    val bytes: Long = 0, val totalBytes: Long = -1, val savedUri: String = "",
    val thumbnail: String = "", val audioCode: String? = null, val quality: String? = null) {
    val complete get() = status == "Ready offline" || status == "Saved to Downloads" || status == "Saved to folder"
    val saving get() = status == "Preparing MP4" || status == "Checking MP4" || status == "Retrying MP4" || status == "Copying to folder"
    val failed get() = status.startsWith("Failed") || status.startsWith("Save failed")
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class DownloaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AnimeRepository()
    var query by mutableStateOf("")
        private set
    var results by mutableStateOf(emptyList<Anime>())
        private set
    var searched by mutableStateOf(false)
        private set
    var searching by mutableStateOf(false)
        private set
    var selectedAnime by mutableStateOf<Anime?>(null)
        private set
    var details by mutableStateOf<AnimeDetails?>(null)
        private set
    var loadingDetails by mutableStateOf(false)
        private set
    var selectedEpisodes by mutableStateOf(emptySet<String>())
        private set
    var year by mutableStateOf("All years")
    var type by mutableStateOf("All types")
    var sort by mutableStateOf(ResultSort.RELEVANCE)
    var quality by mutableStateOf("Best")
    var audio by mutableStateOf("Original")
    var availableSources by mutableStateOf(emptyList<EpisodeSource>())
        private set
    var loadingSources by mutableStateOf(false)
        private set
    var sourceChoicesLabel by mutableStateOf("")
        private set
    val availableAudio get() = availableSources.map { it.audio }.distinct()
    val availableQualities get() = availableSources.filter { it.audio == audio }.map { it.quality }.distinct()
        .sortedByDescending { it.removeSuffix("p").toInt() }
    private val sourceCache = mutableMapOf<String, List<EpisodeSource>>()
    private var sourceJob: Job? = null
    private var sourceGeneration = 0
    private var artworkJob: Job? = null
    private val attemptedArtwork = mutableSetOf<String>()
    var error by mutableStateOf<String?>(null)
        private set
    var verification by mutableStateOf<String?>(null)
        private set
    var resolving by mutableStateOf(false)
        private set
    var resolvingLabel by mutableStateOf("")
        private set
    var downloads by mutableStateOf(emptyList<DownloadRow>())
        private set
    var showDownloads by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    private val settings = AppSettings(application)
    var defaultAudio by mutableStateOf(settings.defaultAudio)
        private set
    var connections by mutableStateOf(settings.connections)
        private set
    var outputFolder by mutableStateOf(settings.outputFolder)
        private set
    var outputTree by mutableStateOf(settings.outputTree)
        private set
    var clearingSession by mutableStateOf(false)
        private set
    var settingsMessage by mutableStateOf<String?>(null)
        private set
    private var searchJob: Job? = null
    private var searchGeneration = 0
    private var detailJob: Job? = null
    private var resolveJob: Job? = null
    private var retryAction: (() -> Unit)? = null
    private var failedUrl: String? = null
    private var failedBrowserUrl: String? = null
    private val verifiedRequests = mutableSetOf<String>()
    private var retryingAfterBrowser = false
    private val handledMediaEvents = mutableSetOf<String>()
    private val app get() = getApplication<Application>()
    private val directPreferences = application.getSharedPreferences("direct-downloads", Context.MODE_PRIVATE)

    init {
        viewModelScope.launch {
            val unfinished = OfflineDownloads.manager(app).downloadIndex.getDownloads(
                Download.STATE_QUEUED, Download.STATE_DOWNLOADING, Download.STATE_RESTARTING, Download.STATE_FAILED
            ).use { it.moveToNext() }
            if (unfinished) DownloadService.startForeground(app, OfflineDownloadService::class.java)
            OfflineDownloads.manager(app).downloadIndex.getDownloads(Download.STATE_COMPLETED).use { cursor ->
                while (cursor.moveToNext()) {
                    val request = cursor.download.request
                    if (!Exports.all(app).containsKey(request.id)) Exports.prepare(app, request.id, String(request.data))
                }
            }
            Exports.start(app)
            refreshDownloads()
            checkMediaVerification()
            viewModelScope.launch { Exports.updates.collect { refreshDownloads(); checkMediaVerification() } }
            viewModelScope.launch { OfflineDownloads.updates.collect { refreshDownloads(); checkMediaVerification() } }
            // System DownloadManager exposes no progress callbacks for direct MP4/WebM files.
            viewModelScope.launch {
                while (true) {
                    delay(1000)
                    if (hasActiveDirectDownload()) refreshDownloads()
                }
            }
        }
    }

    private fun hasActiveDirectDownload(): Boolean {
        if (directPreferences.all.isEmpty()) return false
        val system = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return directPreferences.all.keys.any { id ->
            system.query(DownloadManager.Query().setFilterById(id.toLong())).use { cursor ->
                cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) in
                    listOf(DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED)
            }
        }
    }

    fun updateQuery(value: String) {
        query = value
        startSearch(400)
    }

    fun search() = startSearch(0)

    private fun startSearch(debounceMs: Long) {
        val generation = ++searchGeneration
        searchJob?.cancel()
        results = emptyList()
        searched = false
        searching = false
        clearFailure()
        year = "All years"
        type = "All types"
        val term = query.trim()
        if (term.isEmpty()) return
        beginOperation()
        searching = true
        searchJob = viewModelScope.launch {
            try {
                delay(debounceMs)
                val found = repository.search(term)
                ensureActive()
                if (generation == searchGeneration) {
                    results = found
                    searched = true
                    clearFailure()
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                if (generation == searchGeneration) fail(e) { search() }
            } finally { if (generation == searchGeneration) searching = false }
        }
    }

    fun open(anime: Anime) {
        beginOperation()
        detailJob?.cancel()
        sourceGeneration++
        sourceJob?.cancel()
        loadingSources = false
        sourceCache.clear()
        availableSources = emptyList()
        app.getSharedPreferences("download-artwork", Context.MODE_PRIVATE).edit().putString(anime.session, anime.poster).apply()
        audio = defaultAudio
        selectedAnime = anime
        details = null
        selectedEpisodes = emptySet()
        error = null
        detailJob = viewModelScope.launch {
            loadingDetails = true
            try { details = repository.details(anime); clearFailure(); updateSourceChoices()
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { fail(e) { open(anime) }
            } finally { loadingDetails = false }
        }
    }

    fun back() {
        if (resolving) return
        detailJob?.cancel()
        sourceGeneration++
        sourceJob?.cancel()
        loadingSources = false
        selectedAnime = null
        details = null
        error = null
    }

    fun toggle(episode: Episode) {
        selectedEpisodes = if (episode.session in selectedEpisodes) selectedEpisodes - episode.session
            else selectedEpisodes + episode.session
        updateSourceChoices()
    }

    fun selectAll() {
        val ids = details?.episodes.orEmpty().map { it.session }.toSet()
        selectedEpisodes = if (selectedEpisodes == ids) emptySet() else ids
        updateSourceChoices()
    }

    fun chooseAudio(code: String) {
        audio = code
        if (quality !in availableQualities) quality = availableQualities.firstOrNull().orEmpty()
    }

    private fun updateSourceChoices() {
        val generation = ++sourceGeneration
        sourceJob?.cancel()
        val data = details ?: return
        val episodes = data.episodes.filter { it.session in selectedEpisodes }.ifEmpty { data.episodes.take(1) }
        availableSources = emptyList()
        loadingSources = episodes.isNotEmpty()
        sourceChoicesLabel = "Loading available sources…"
        sourceJob = viewModelScope.launch {
            try {
                delay(200)
                val limit = Semaphore(3)
                var checked = 0
                val options = coroutineScope {
                    episodes.map { episode -> async {
                        limit.withPermit {
                            val sources = sourceCache[episode.session] ?: repository.sources(data.anime, episode)
                            ensureActive()
                            sourceCache[episode.session] = sources
                            checked++
                            sourceChoicesLabel = "Checking sources $checked/${episodes.size}"
                            sources
                        }
                    } }.awaitAll()
                }
                availableSources = commonSourceOptions(options)
                if (audio !in availableAudio) audio = availableAudio.firstOrNull { it == defaultAudio }
                    ?: availableSources.firstOrNull { it.preferred }?.audio ?: availableAudio.firstOrNull().orEmpty()
                if (quality !in availableQualities) quality = availableQualities.firstOrNull().orEmpty()
                sourceChoicesLabel = if (availableSources.isEmpty()) "No shared quality and audio options for these episodes. Select fewer episodes."
                    else if (selectedEpisodes.isEmpty()) "Available for episode ${episodes.first().number}. Select episodes to check their options."
                    else "Available for all ${episodes.size} selected episode${if (episodes.size == 1) "" else "s"}."
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { sourceChoicesLabel = "Could not load available sources."; fail(e) { updateSourceChoices() }
            } finally { if (generation == sourceGeneration) loadingSources = false }
        }
    }

    fun downloadSelected() {
        val data = details ?: return
        val episodes = data.episodes.filter { it.session in selectedEpisodes }
        if (episodes.isEmpty() || resolving || loadingSources || availableSources.isEmpty()) return
        beginOperation()
        val chosenQuality = quality
        val chosenAudio = audio
        resolveJob = viewModelScope.launch {
            resolving = true
            error = null
            try {
                episodes.forEachIndexed { index, episode ->
                    resolvingLabel = "Preparing ${index + 1}/${episodes.size} · Episode ${episode.number}"
                    val media = repository.playlist(data.anime, episode, chosenQuality, chosenAudio)
                    val uri = media.url
                    repository.checkMedia(uri)
                    val title = "${data.anime.title} · Episode ${episode.number} · ${media.quality} · ${audioTag(media.audio)}"
                    val id = "${data.anime.session}-${episode.session}-$chosenQuality-$chosenAudio"
                    if (Uri.parse(uri).path.orEmpty().endsWith(".m3u8", true)) {
                        val request = DownloadRequest.Builder(id, Uri.parse(uri))
                            .setMimeType(MimeTypes.APPLICATION_M3U8).setData(title.toByteArray()).build()
                        Exports.prepare(app, id, title, data.anime.poster, media.audio, media.quality, data.anime.title, episode.number)
                        DownloadService.sendAddDownload(app, OfflineDownloadService::class.java, request, true)
                        Exports.start(app)
                    } else {
                        val extension = if (Uri.parse(uri).path.orEmpty().endsWith(".webm", true)) ".webm" else ".mp4"
                        val name = title.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_") + extension
                        val request = DownloadManager.Request(Uri.parse(uri)).setTitle(title)
                            .setDestinationInExternalFilesDir(app, "direct-temp", "${System.currentTimeMillis()}-$name")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .addRequestHeader("User-Agent", AnimeNetwork.userAgent)
                            .addRequestHeader("Referer", "https://kwik.cx/")
                        CookieManager.getInstance().getCookie(uri)?.let { request.addRequestHeader("Cookie", it) }
                        val downloadId = (app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                        Exports.prepare(app, "direct:$downloadId", title, data.anime.poster, media.audio, media.quality, data.anime.title, episode.number)
                        directPreferences.edit().putString(downloadId.toString(), title).apply()
                        Exports.start(app)
                    }
                    selectedEpisodes = selectedEpisodes - episode.session
                }
                showDownloads = true
                clearFailure()
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { fail(e) { downloadSelected() }
            } finally { resolving = false; resolvingLabel = ""; refreshDownloads() }
        }
    }

    fun cancelPreparation() { resolveJob?.cancel() }

    private fun fail(e: Exception, retry: () -> Unit) {
        error = e.message ?: "The request failed. Please try again."
        retryAction = retry
        failedUrl = when (e) { is VerificationRequired -> e.url; is HttpFailure -> e.url; else -> null }
        failedBrowserUrl = when (e) { is VerificationRequired -> e.browserUrl; is HttpFailure -> e.browserUrl; else -> null }
        DebugTrace.event("operation_failed", "type" to e.javaClass.simpleName,
            "url" to failedUrl?.let(DebugTrace::safeUrl))
        if (e is VerificationRequired) {
            if (e.url in verifiedRequests) {
                error = "Browser verification finished, but ${Uri.parse(e.url).host} rejected the HTTP session. " +
                    "The browser and HTTP client can be treated differently. Stopped after one retry; tap Retry to try again."
            } else verification = e.browserUrl
        }
    }

    private fun beginOperation() {
        if (!retryingAfterBrowser) verifiedRequests.clear()
        retryingAfterBrowser = false
    }

    private fun clearFailure() {
        retryAction = null
        failedUrl = null
        failedBrowserUrl = null
        error = null
    }

    fun verifyManually() { verification = failedBrowserUrl ?: AnimeNetwork.HOST }
    fun closeVerification(retry: Boolean) {
        verification = null
        CookieManager.getInstance().flush()
        val action = retryAction
        if (retry && action != null) {
            failedUrl?.let { verifiedRequests += it }
            retryingAfterBrowser = true
            action()
        } else retryingAfterBrowser = false
    }
    fun retry() { retryAction?.invoke() }
    fun clearError() { error = null }

    private fun refreshDownloads() {
        val rows = mutableListOf<DownloadRow>()
        OfflineDownloads.manager(app).downloadIndex.getDownloads().use { cursor ->
            while (cursor.moveToNext()) {
                val download = cursor.download
                val status = when (download.state) {
                    Download.STATE_COMPLETED -> "Ready offline"
                    Download.STATE_FAILED -> "Failed · Tap Retry"
                    Download.STATE_DOWNLOADING -> "Downloading HLS"
                    Download.STATE_QUEUED -> when {
                        OfflineDownloads.manager(app).downloadsPaused -> "Paused"
                        OfflineDownloads.manager(app).notMetRequirements != 0 -> "Waiting for internet"
                        else -> "Queued"
                    }
                    Download.STATE_STOPPED -> "Paused"
                    Download.STATE_REMOVING -> "Removing"
                    else -> "Queued"
                }
                rows += DownloadRow(download.request.id, String(download.request.data), status, download.percentDownloaded,
                    download.bytesDownloaded, download.contentLength)
            }
        }
        val system = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        directPreferences.all.forEach { (id, title) ->
            system.query(DownloadManager.Query().setFilterById(id.toLong())).use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    rows += DownloadRow("direct:$id", title.toString(), when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> "Saved to Downloads"
                        DownloadManager.STATUS_FAILED -> "Failed · Remove and retry"
                        DownloadManager.STATUS_PAUSED -> "Paused"
                        DownloadManager.STATUS_RUNNING -> "Downloading file"
                        else -> "Queued"
                    }, if (total > 0) bytes * 100f / total else -1f, bytes, total)
                }
            }
        }
        val exports = Exports.all(app)
        val artwork = app.getSharedPreferences("download-artwork", Context.MODE_PRIVATE)
        downloads = rows.filter { exports[it.id]?.uri.isNullOrEmpty() }.map { row ->
            val record = exports[row.id]
            val metadata = row.copy(thumbnail = artwork.getString(row.id.take(36), null) ?: record?.thumbnail.orEmpty(),
                audioCode = record?.audio, quality = record?.quality)
            when {
                record?.error?.isNotEmpty() == true -> metadata.copy(status = "Save failed: ${record.error}")
                row.complete && record != null -> Exports.progress[row.id]?.let { metadata.copy(status = it.stage, percent = it.percent) }
                    ?: metadata.copy(status = "Waiting to save", percent = -1f)
                else -> metadata
            }
        } + exports.filterValues { it.uri.isNotEmpty() }.map { (id, record) ->
            DownloadRow(id, record.title, "Saved to folder", 100f, record.bytes, record.bytes, record.uri,
                artwork.getString(id.take(36), null) ?: record.thumbnail.orEmpty(), record.audio, record.quality)
        }
        loadMissingArtwork()
    }

    fun pauseDownload(id: String) {
        DownloadService.sendSetStopReason(app, OfflineDownloadService::class.java, id, 1, true)
    }

    fun resumeDownload(id: String) {
        DownloadService.sendSetStopReason(app, OfflineDownloadService::class.java, id, Download.STOP_REASON_NONE, true)
        DownloadService.sendResumeDownloads(app, OfflineDownloadService::class.java, true)
    }

    fun retryOfflineDownload(id: String) {
        beginOperation()
        val request = OfflineDownloads.manager(app).downloadIndex.getDownload(id)?.request ?: return
        app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit().remove(id).apply()
        error = null
        DownloadService.sendAddDownload(app, OfflineDownloadService::class.java, request, true)
        DownloadService.sendResumeDownloads(app, OfflineDownloadService::class.java, true)
    }

    private fun checkMediaVerification() {
        if (resolving || searching || loadingDetails || verification != null) return
        app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).all.values.mapNotNull { value ->
            runCatching { com.google.gson.Gson().fromJson(value.toString(), MediaVerification::class.java) }.getOrNull()
        }.firstOrNull { "${it.id}:${it.time}" !in handledMediaEvents }?.let { event ->
            handledMediaEvents += "${event.id}:${event.time}"
            fail(VerificationRequired(event.url, event.browserUrl)) { retryOfflineDownload(event.id) }
        }
    }

    private fun loadMissingArtwork() {
        if (artworkJob?.isActive == true) return
        val artwork = app.getSharedPreferences("download-artwork", Context.MODE_PRIVATE)
        val missing = downloads.filter { row ->
            val poster = artwork.getString(row.id.take(36), null)
            poster.isNullOrEmpty() && row.id.matches(Regex("^[a-fA-F0-9-]{36}-.*")) && row.id.take(36) !in attemptedArtwork
        }.distinctBy { it.id.take(36) }
        artworkJob = viewModelScope.launch {
            missing.forEach { row ->
                val session = row.id.take(36)
                attemptedArtwork += session
                try {
                    val poster = repository.poster(session)
                    if (poster.isNotBlank()) artwork.edit().putString(session, poster).apply()
                } catch (e: CancellationException) { throw e
                } catch (_: Exception) { /* Existing downloads remain usable if artwork is unavailable. */ }
            }
        }
    }

    fun removeDownload(id: String) {
        Exports.cancel(app, id)
        app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit().remove(id).apply()
        val record = Exports.all(app)[id]
        if (record?.uri?.isNotEmpty() == true) {
            try { app.contentResolver.delete(Uri.parse(record.uri), null, null) }
            catch (e: Exception) { error = "Cannot delete saved file: ${e.message}"; return }
        }
        Exports.remove(app, id)
        if (id.startsWith("direct:")) {
            val number = id.removePrefix("direct:")
            (app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).remove(number.toLong())
            directPreferences.edit().remove(number).apply()
        } else DownloadService.sendRemoveDownload(app, OfflineDownloadService::class.java, id, true)
        refreshDownloads()
    }

    fun updateDefaultAudio(code: String) {
        settings.defaultAudio = code
        defaultAudio = code
        settingsMessage = "Default audio saved. Applies when opening a show; unavailable languages use the source default."
    }

    fun setDownloadConnections(count: Int) {
        settings.connections = count
        connections = settings.connections
        OfflineDownloads.setConnections(connections)
        settingsMessage = "Using up to $connections HLS segment connections."
    }

    fun saveOutputFolder(input: String): Boolean {
        val folder = normalizeOutputFolder(input) ?: return false
        settings.outputTree = ""
        outputTree = ""
        settings.outputFolder = folder
        outputFolder = folder
        settingsMessage = "Output folder saved. Applies to new downloads."
        return true
    }

    fun chooseOutputTree(uri: Uri) {
        try {
            app.contentResolver.takePersistableUriPermission(uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            settings.outputTree = uri.toString()
            outputTree = uri.toString()
            settingsMessage = "Output folder saved. Applies to new downloads."
        } catch (e: Exception) { settingsMessage = "Cannot use folder: ${e.message}" }
    }

    fun retryExport(id: String) {
        Exports.all(app)[id]?.let { Exports.put(app, id, it.copy(error = "", tree = settings.outputTree, folder = settings.outputFolder)) }
        Exports.start(app)
    }

    fun clearSessionData() {
        if (clearingSession || resolving || searching || loadingDetails || loadingSources || verification != null) return
        clearingSession = true
        settingsMessage = null
        android.webkit.WebStorage.getInstance().deleteAllData()
        android.webkit.WebView(app).apply { clearCache(true); clearHistory(); clearFormData(); destroy() }
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            verifiedRequests.clear()
            retryingAfterBrowser = false
            clearFailure()
            settingsMessage = "Session cleared. Verify again the next time the site asks."
            clearingSession = false
        }
    }
}
