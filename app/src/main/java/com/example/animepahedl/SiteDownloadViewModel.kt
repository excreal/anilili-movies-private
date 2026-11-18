package com.example.animepahedl

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.animepahedl.metadata.SiteMetadataApis
import com.example.animepahedl.metadata.toAppMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SiteDownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val cookies = SiteDownloadCookieStore(application)
    private val repository = appSiteDownloadRepository(cookieHeader = cookies::headerFor)
    private val metadataApis by lazy { SiteMetadataApis.forApp(application) }
    private var inspectJob: Job? = null
    private var metadataJob: Job? = null
    private var generation = 0
    private var submissions = 0
    private val submissionLock = Mutex()
    var input by mutableStateOf("")
        private set
    var series by mutableStateOf<SiteDownloadSeries?>(null)
        private set
    var inspecting by mutableStateOf(false)
        private set
    var selection by mutableStateOf("")
        private set
    var selectedEpisodes by mutableStateOf(emptyList<SiteDownloadEpisode>())
        private set
    var selectionError by mutableStateOf<String?>(null)
        private set
    var quality by mutableStateOf("Best")
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var preparationError by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var hasCookies by mutableStateOf(cookies.hasCookies)
        private set
    var pending by mutableStateOf<SiteDownloadPreparation?>(null)
        private set
    var preparing by mutableStateOf(false)
        private set
    var discarding by mutableStateOf(false)
        private set
    var preparationStatus by mutableStateOf(PreparationStatus())
        private set
    var preparationLabel by mutableStateOf("")
        private set
    var ready by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            try {
                DownloadStore.awaitReady()
                pending = PreparationJobs.pendingSites()
                ready = true
                PreparationJobs.observe(app).collect { status ->
                    val remaining = PreparationJobs.pendingSites()
                    if (submissions > 0) return@collect
                    pending = remaining
                    preparationStatus = status
                    preparing = status.automatic
                    preparationLabel = status.label
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { preparationError = "Could not load pending downloads. Reopen the app to try again." }
        }
    }

    fun libraryRows(actual: List<DownloadRow>): List<DownloadRow> = mergePreparedQueueRows(actual, pending, preparationStatus)
    fun isPendingRow(id: String) = isPreparedQueueRow(id)
    fun pausePending(id: String) = changePending(id, resume = false)
    fun resumePending(id: String) = changePending(id, resume = true)
    private fun changePending(id: String, resume: Boolean) {
        if (!isPendingRow(id)) return
        viewModelScope.launch {
            preparationError = null
            try {
                if (resume) PreparationJobs.resume(app, PreparationJobs.itemSlot(id))
                else PreparationJobs.pause(app, PreparationJobs.itemSlot(id))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { preparationError = "Could not update queued episode. Try again." }
        }
    }
    fun removePending(ids: Set<String>) {
        if (ids.none(::isPreparedQueueRow)) return
        viewModelScope.launch {
            preparationError = null
            try { PreparationJobs.removeSiteEpisodes(app, ids) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { preparationError = "Could not remove queued episodes. Try again." }
        }
    }

    fun updateInput(value: String) {
        generation++
        inspectJob?.cancel()
        metadataJob?.cancel()
        inspecting = false
        input = value
        series = null
        selectedEpisodes = emptyList()
        selection = ""
        selectionError = null
        error = null
    }

    fun inspect(posterThumbnailOverride: String? = null, fallbackTitle: String? = null) {
        if (input.isBlank()) return
        val request = input.trim()
        val current = ++generation
        inspectJob?.cancel()
        metadataJob?.cancel()
        inspecting = true
        error = null
        series = null
        selectedEpisodes = emptyList()
        inspectJob = viewModelScope.launch {
            try {
                val result = repository.inspect(request, cookies.headerFor(request))
                ensureActive()
                if (generation == current) {
                    series = result.copy(posterThumbnailOverride = posterThumbnailOverride?.takeIf(String::isNotBlank))
                    if (result.provider != SiteDownloadProvider.DIRECT) metadataJob = viewModelScope.launch {
                        try {
                            val metadata = metadataApis.forProvider(result.provider).details(request).toAppMetadata()
                            ensureActive()
                            val latest = series
                            if (latest != null) series = mergeInspectionMetadata(latest, result.provider, result.id, current, generation, metadata)
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { /* A metadata challenge must not block playback or downloads. */ }
                    }
                    quality = "Best"
                    updateSelection("")
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                if (generation == current) {
                    val kisskhUrl = runCatching { siteDownloadInput(request) }.getOrNull()
                    val kisskhId = kisskhUrl?.queryParameter("id")?.toLongOrNull()?.takeIf { it > 0 }
                    if (e is KisskhContentUnavailableException && kisskhId != null && !fallbackTitle.isNullOrBlank() && !posterThumbnailOverride.isNullOrBlank()) {
                        val unavailableMessage = when (e) {
                            is KisskhRegionRestrictionException -> "Content is not available from KissKH in your region."
                            else -> "Content is not available from KissKH right now."
                        }
                        series = SiteDownloadSeries(
                            provider = SiteDownloadProvider.KISSKH,
                            id = kisskhId.toString(),
                            title = fallbackTitle,
                            poster = posterThumbnailOverride,
                            url = request,
                            episodes = emptyList(),
                            description = unavailableMessage,
                            posterThumbnailOverride = posterThumbnailOverride,
                            contentAvailable = false
                        )
                    } else {
                        error = when {
                            e is KisskhContentUnavailableException -> "Content unavailable: ${e.message}"
                            request.contains("kisskh.do", ignoreCase = true) -> "Could not load details from KissKH. Check your connection or try again later."
                            else -> "Could not inspect this link. Check the URL and connection, or import a session for this site."
                        }
                    }
                }
            } finally { if (generation == current) inspecting = false }
        }
    }

    fun cancelInspect() { generation++; inspectJob?.cancel(); metadataJob?.cancel(); inspecting = false }
    fun chooseQuality(value: String) { if (value in (series?.qualities.orEmpty() + "Best")) quality = value }
    fun updateSelection(value: String) {
        selection = value
        try {
            selectedEpisodes = parseSiteDownloadSelection(value, series?.episodes.orEmpty())
            selectionError = null
        } catch (_: Exception) {
            selectedEpisodes = emptyList()
            selectionError = "Use episode numbers and ranges, for example 1-5,9. Enter none to clear."
        }
    }
    fun toggleEpisode(episode: SiteDownloadEpisode) {
        val ids = selectedEpisodes.map { it.id }.toMutableSet()
        if (!ids.add(episode.id)) ids.remove(episode.id)
        val selected = series?.episodes.orEmpty().filter { it.id in ids }
        updateSelection(if (selected.isEmpty()) "none" else selected.joinToString(",") { it.displayNumber })
    }
    fun saveCookies(input: String): Boolean = try {
        cookies.save(input)
        hasCookies = cookies.hasCookies
        notice = if (hasCookies) "Session cookies saved privately." else "Session cookies removed."
        true
    } catch (_: IllegalArgumentException) { error = "Import a Netscape cookie file with domain information (up to 256 KiB)."; false }
    fun clearCookies() { cookies.clear(); hasCookies = false; notice = "Session cookies removed." }
    fun dismissNotice() { notice = null }
    fun dismissError() { error = null }
    fun dismissPreparationError() { preparationError = null }
    fun downloadEpisode(show: SiteDownloadSeries, episode: SiteDownloadEpisode, selectedQuality: String = "Best"): Boolean {
        if (!ready || discarding) return false
        runPreparation(SiteDownloadPreparation(show, listOf(episode), selectedQuality))
        return true
    }
    fun downloadSelected() {
        val preview = series ?: return
        if (!ready || discarding || !preview.contentAvailable || selectionError != null || selectedEpisodes.isEmpty()) return
        runPreparation(SiteDownloadPreparation(preview, chronologicalSiteEpisodes(selectedEpisodes), quality))
    }
    fun resumePreparation() { if (!discarding) viewModelScope.launch {
        try { PreparationJobs.resume(app, PreparationJobs.SITES) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { preparationError = "Could not resume preparation. Try again." }
    } }
    fun pausePreparation() { viewModelScope.launch {
        try { PreparationJobs.pause(app, PreparationJobs.SITES) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { preparationError = "Could not pause preparation. Try again." }
    } }
    fun discardPreparation() {
        if (discarding || !ready) return
        discarding = true
        viewModelScope.launch {
            try { PreparationJobs.discard(app, PreparationJobs.SITES) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { preparationError = "Could not discard preparation. Try again." }
            finally { discarding = false }
        }
    }
    private fun runPreparation(record: SiteDownloadPreparation) {
        if (discarding || !ready) return
        submissions++
        val current = pending
        pending = if (current == null) record else {
            val batches = current.batches()
            val matching = batches.filter {
                it.series.provider == record.series.provider && it.series.id == record.series.id && it.quality == record.quality
            }.flatMap { it.episodes }.mapTo(HashSet()) { it.id }
            val added = record.episodes.filterNot { it.id in matching }
            if (added.isEmpty()) current else current.withBatches(batches + SiteDownloadBatch(record.series, added, record.quality))
        }
        preparationStatus = preparationStatus.copy(pending = true,
            label = "Queued",
            totalSelected = pending?.batches()?.sumOf { it.episodes.size } ?: record.episodes.size)
        preparationError = null
        viewModelScope.launch {
            try { submissionLock.withLock { PreparationJobs.submit(app, PreparationJobs.SITES, record) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                preparing = false
                preparationError = "Could not schedule preparation. Your selection is kept."
            } finally {
                submissions--
                if (submissions == 0) {
                    val remaining = PreparationJobs.pendingSites()
                    if (submissions == 0) pending = remaining
                }
            }
        }
    }
}
