package com.example.animepahedl

import androidx.core.content.edit
import androidx.core.net.toUri
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.withLock

internal interface DownloadRepository {
    val rows: StateFlow<List<DownloadRow>>
    suspend fun refresh()
    suspend fun pause(id: String)
    suspend fun resume(id: String)
    suspend fun retry(id: String)
    suspend fun retrySave(id: String)
    suspend fun delete(id: String, deleteSavedFile: Boolean = true): ItemResult
    suspend fun deleteBatch(ids: Set<String>, confirmedSaved: Set<String> = ids): List<ItemResult> = ids.map { delete(it, it in confirmedSaved) }
    suspend fun enqueue(identity: DownloadIdentity, previous: ExportRecord? = null): String
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class AndroidDownloadRepository(private val app: Context) : DownloadRepository {
    private val settings = AppSettings(app)
    override val rows = MutableStateFlow<List<DownloadRow>>(emptyList())
    override suspend fun refresh() {
        DownloadStore.awaitReady()
        val (manager, active, state) = withContext(Dispatchers.Main.immediate) {
            val manager = OfflineDownloads.manager(app)
            Triple(manager, manager.currentDownloads.associateBy { it.request.id }, manager.downloadsPaused to manager.notMetRequirements)
        }
        rows.value = withContext(Dispatchers.IO) { readDownloadRows(manager.downloadIndex, active, state.first, state.second) }
    }
    private fun readDownloadRows(index: androidx.media3.exoplayer.offline.DownloadIndex, activeDownloads: Map<String, Download>, paused: Boolean, requirements: Int): List<DownloadRow> {
        val rows = mutableListOf<DownloadRow>()
        val exports = Exports.all(app)
        val savedHls = exports.values.filter { it.uri.isNotBlank() && it.bytes > 0 && it.isHlsTransfer() }
            .groupBy { Triple(it.identity?.anime?.session, it.quality, it.audio) }
        index.getDownloads().use { cursor ->
            while (cursor.moveToNext()) {
                val stored = cursor.download
                val download = activeDownloads[stored.request.id] ?: stored
                val id = download.request.id
                val record = exports[id]
                val hls = if (record?.media != null) record.isHlsTransfer() else isHlsMimeType(download.request.mimeType)
                val status = when (download.state) {
                    Download.STATE_COMPLETED -> TransferState.COMPLETE
                    Download.STATE_FAILED -> TransferState.FAILED
                    Download.STATE_DOWNLOADING -> if (OfflineDownloads.isGettingLink(id)) TransferState.RESOLVING else TransferState.DOWNLOADING
                    Download.STATE_QUEUED -> when {
                        paused -> TransferState.PAUSED
                        requirements != 0 -> TransferState.WAITING_NETWORK
                        else -> TransferState.QUEUED
                    }
                    Download.STATE_STOPPED -> TransferState.PAUSED
                    Download.STATE_REMOVING -> TransferState.REMOVING
                    else -> TransferState.QUEUED
                }
                val bytes = download.bytesDownloaded.coerceAtLeast(0L)
                val active = status == TransferState.DOWNLOADING || status == TransferState.PAUSED
                val sample = if (active && hls) OfflineDownloads.hlsSample(id) else null
                val estimated = if (active && hls && record?.identity != null) {
                    val peers = savedHls[Triple(record.identity.anime.session, record.quality, record.audio)].orEmpty()
                    estimateSavedHlsSize(sample, peers)
                } else -1L
                val reported = if (active && !hls) download.percentDownloaded else -1f
                rows += DownloadRow(id, String(download.request.data), status, mediaCoverageProgress(reported, sample?.percent),
                    bytes, displayTransferTotal(bytes, download.contentLength, hls), estimatedTotalBytes = estimated)
            }
        }
        val system = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        DownloadStore.operations("direct").filterKeys { exports["direct:$it"]?.uri.isNullOrEmpty() }.forEach { (id, title) ->
            val number = id.toLongOrNull() ?: return@forEach
            system.query(DownloadManager.Query().setFilterById(number)).use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).coerceAtLeast(0L)
                    val knownTotal = displayTransferTotal(bytes, total, hls = false)
                    rows += DownloadRow("direct:$id", title, when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> TransferState.COMPLETE
                        DownloadManager.STATUS_FAILED -> TransferState.FAILED
                        DownloadManager.STATUS_PAUSED -> TransferState.PAUSED
                        DownloadManager.STATUS_RUNNING -> TransferState.DOWNLOADING
                        else -> TransferState.QUEUED
                    }, byteProgress(bytes, knownTotal), bytes, knownTotal)
                }
            }
        }
        val artwork = app.getSharedPreferences("download-artwork", Context.MODE_PRIVATE)
        // Keep app-owned rows visible even if an engine has already removed its staging entry.
        val engineIds = rows.mapTo(HashSet()) { it.id }
        exports.filter { (id, record) -> record.uri.isEmpty() && id !in engineIds }.forEach { (id, record) ->
            rows += DownloadRow(id, record.title, TransferState.FAILED, -1f, explanation = "Video unavailable")
        }
        val challenges = mediaVerificationEvents(app)
        val expired = DownloadStore.operations("expired-media")
        val deleting = DownloadStore.operations("delete")
        return rows.filter { exports[it.id]?.uri.isNullOrEmpty() }.map { row ->
            val record = exports[row.id]
            // Only site/direct-link downloads persist the context required by the current
            // downloader factory. Re-adding an orphaned engine request fails at that lookup.
            val eligibility = downloadRecoveryEligibility(record)
            val metadata = row.copy(title = record?.episodeTitle() ?: row.title, thumbnail = artwork.getString(row.id.take(36), null) ?: record?.thumbnail.orEmpty(),
                audioCode = record?.audio, quality = record?.quality, requestedQuality = record?.identity?.quality,
                showId = record?.identity?.anime?.session.orEmpty(), episodeId = record?.identity?.episode?.session.orEmpty(), showTitle = record?.identity?.anime?.title ?: record?.showTitle.orEmpty(), episodeNumber = record?.identity?.episode?.number ?: record?.episodeNumber.orEmpty(),
                retryable = eligibility.retryable, transferControllable = eligibility.transferControllable,
                explanation = if (row.transfer == TransferState.FAILED && !eligibility.retryable)
                    "Add video again" else row.explanation)
            when {
                row.id in deleting -> metadata.copy(failure = FailureReason.DELETION, explanation = if (deleting[row.id].isNullOrEmpty()) "Removing" else "Couldn’t remove", deleting = true)
                row.transfer == TransferState.FAILED && row.id in challenges -> metadata.copy(failure = FailureReason.VERIFICATION, explanation = null)
                row.transfer == TransferState.FAILED && row.id in expired -> metadata.copy(failure = FailureReason.EXPIRED_SOURCE, explanation = null)
                row.transfer == TransferState.COMPLETE && record?.error?.isNotEmpty() == true -> metadata.copy(save = SaveState.FAILED,
                    failure = classifyFailure(record.error))
                row.transfer == TransferState.COMPLETE && record != null -> Exports.progress[row.id]?.let {
                    metadata.copy(save = when (it.stage) {
                        "Preparing MP4", "Retrying MP4", "Preparing video" -> SaveState.PREPARING
                        "Checking MP4", "Checking saved video" -> SaveState.CHECKING
                        else -> SaveState.COPYING
                    }, percent = normalizedProgress(it.percent), bytes = it.bytes, totalBytes = it.totalBytes)
                } ?: metadata.copy(save = SaveState.WAITING, percent = -1f)
                else -> metadata
            }
        } + exports.filterValues { it.uri.isNotEmpty() }.map { (id, record) ->
            DownloadRow(id, record.episodeTitle(), TransferState.COMPLETE, 100f, record.bytes, record.bytes, record.uri,
                artwork.getString(id.take(36), null) ?: record.thumbnail.orEmpty(), record.audio, record.quality,
                requestedQuality = record.identity?.quality,
                availability = Availability.SAVED, failure = if (id in deleting) FailureReason.DELETION else if (record.error.isNotBlank()) FailureReason.FOLDER_ACCESS else null,
                explanation = deleting[id]?.let { if (it.isBlank()) "Removing" else "Couldn’t remove" }
                    ?: "Choose folder".takeIf { record.error.isNotBlank() }, deleting = id in deleting, showId = record.identity?.anime?.session.orEmpty(), episodeId = record.identity?.episode?.session.orEmpty(), showTitle = record.identity?.anime?.title ?: record.showTitle.orEmpty(), episodeNumber = record.identity?.episode?.number ?: record.episodeNumber.orEmpty())
        }
    }

    private fun ExportRecord.isHlsTransfer(): Boolean = runCatching { media?.isHls() == true }.getOrDefault(false)

    override suspend fun pause(id: String) {
        if (Exports.deleting(id)) return
        DownloadService.sendSetStopReason(app, OfflineDownloadService::class.java, id, 1, true)
    }

    override suspend fun resume(id: String) {
        require(downloadRecoveryEligibility(Exports.all(app)[id]).transferControllable) {
            "This download has no saved source context. Open the show and select the episode again."
        }
        if (!hasOutputFolderAccess(app) || DownloadStore.operations("folder-access")["transfers"] != null) throw OutputFolderAccessRequired()
        if (Exports.deleting(id)) return
        val manager = withContext(Dispatchers.Main.immediate) { OfflineDownloads.manager(app) }
        val current = withContext(Dispatchers.IO) { manager.downloadIndex.getDownload(id) }
        if (current == null || current.state == Download.STATE_FAILED) { retry(id); return }
        withContext(Dispatchers.Main.immediate) {
            DownloadService.startForeground(app, OfflineDownloadService::class.java)
            manager.setStopReason(id, Download.STOP_REASON_NONE)
            manager.resumeDownloads()
        }
    }

    override suspend fun delete(id: String, deleteSavedFile: Boolean): ItemResult {
        // Preserve the user's Cancel intent even if an existing file is adopted before deletion runs.
        if (!deleteSavedFile) DownloadStore.operation(id, "delete-keep-file", "1")
        val removePublished = deleteSavedFile && id !in DownloadStore.operations("delete-keep-file")
        DownloadStore.operation(id, "delete", "")
        return try {
            DownloadStore.flush()
            Exports.cancel(app, id)
            Exports.withItemLock(id) {
                val record = Exports.all(app)[id]
                val publishedUri = record?.uri?.takeIf { it.isNotBlank() && removePublished }
                val deletePublished = publishedUri != null && Exports.all(app).none { (otherId, other) ->
                    otherId != id && other.uri == publishedUri
                }
                withContext(Dispatchers.IO) {
                    val sidecars = if (deletePublished && record != null)
                        findOutputSubtitleSidecars(app, record, requireMatchingVideo = true) else emptyList()
                    listOfNotNull(publishedUri?.takeIf { deletePublished }, record?.pendingUri?.takeIf { it.isNotBlank() && it != record.uri }).distinct()
                        .forEach { deleteOwnedMedia(app, (it).toUri()) }
                    if (deletePublished && record != null) {
                        ProviderSubtitles.deleteUnreferenced(id, record.media?.subtitles.orEmpty())
                        sidecars.forEach { subtitle -> runCatching { deleteOwnedMedia(app, subtitle.url.toUri()) } }
                    }
                    if (id.startsWith("direct:")) {
                        val number = id.removePrefix("direct:")
                        number.toLongOrNull()?.let { (app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).remove(it) }
                        DownloadStore.operation(number, "direct", null)
                    }
                }
                enqueueLocks.getOrPut(id) { kotlinx.coroutines.sync.Mutex() }.withLock {
                    if (!id.startsWith("direct:")) {
                        // A transfer handoff must finish before we check the index for removal.
                        val manager = withContext(Dispatchers.Main.immediate) { OfflineDownloads.manager(app) }
                        if (withContext(Dispatchers.IO) { manager.downloadIndex.getDownload(id) != null }) {
                            withContext(Dispatchers.Main.immediate) { manager.removeDownload(id) }
                            withTimeoutOrNull(30_000) {
                                while (withContext(Dispatchers.IO) { manager.downloadIndex.getDownload(id) != null }) delay(100)
                            } ?: error("The download engine did not finish removing this item")
                        }
                    }
                    app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit { remove(id) }
                    app.getSharedPreferences("download-retries", Context.MODE_PRIVATE).edit { remove(id); remove("attempts:$id") }
                    Exports.remove(app, id)
                    DownloadStore.operation(id, "delete", null)
                    DownloadStore.operation(id, "delete-keep-file", null)
                    DownloadStore.operation(id, "expired-media", null)
                    DownloadStore.flush()
                    ItemResult(id, true)
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (e: Exception) {
            val message = "Couldn’t delete: ${e.message ?: "storage unavailable"}. Tap Delete to retry."
            DownloadStore.operation(id, "delete", message)
            ItemResult(id, false, message)
        }
    }

    private companion object {
        val enqueueLocks = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.sync.Mutex>()
    }
    override suspend fun enqueue(identity: DownloadIdentity, previous: ExportRecord?): String {
        require(identity.isSiteDownload) { "Only supported source downloads can be queued here." }
        return enqueueLocks.getOrPut(identity.key) { kotlinx.coroutines.sync.Mutex() }.withLock {
            enqueueLocked(identity, previous)
        }
    }
    private suspend fun enqueueLocked(identity: DownloadIdentity, previous: ExportRecord?): String {
        DownloadStore.awaitReady()
        if (!hasOutputFolderAccess(app) || DownloadStore.operationValue("transfers", "folder-access") != null) throw OutputFolderAccessRequired()
        val locator = requireNotNull(identity.locator?.takeIf(String::isNotBlank)) { "Video link is missing" }
        val id = identity.key
        if (Exports.deleting(id)) error("This episode is being removed. Try again after removal finishes.")
        val manager = withContext(Dispatchers.Main.immediate) { OfflineDownloads.manager(app) }
        if (previous == null) for ((existingId, record) in Exports.all(app)) {
            if (Exports.deleting(existingId) || !matchesQueuedVideo(identity, record, existingId)) continue
            if (record.uri.isNotEmpty()) {
                if (missingSavedFiles(app, mapOf(existingId to record)).isEmpty()) return existingId
                continue
            }
            val existing = withContext(Dispatchers.IO) { manager.downloadIndex.getDownload(existingId) }
            // A durable record alone does not prove the engine accepted the request. A process
            // may have died between flush() and sendAddDownload(), or the transfer may have failed.
            if (existing != null && existing.state != Download.STATE_FAILED) return existingId
        }
        val title = identity.anime.title + (if (identity.isSingleVideo) "" else " · Episode ${identity.episode.number}")
        val record = previous?.copy(title = title, showTitle = identity.anime.title, episodeNumber = identity.episode.number,
            error = "", uri = "", identity = identity, refreshMedia = true)
            ?: ExportRecord(title, settings.outputTree, settings.outputFolder, thumbnail = identity.anime.poster,
                audio = identity.audio, quality = identity.quality, showTitle = identity.anime.title,
                episodeNumber = identity.episode.number, identity = identity, scopedCache = true)
        // The index stores the stable locator. The actual media URL belongs to a running task.
        if (Exports.deleting(id)) error("This episode is being removed. Try again after removal finishes.")
        val request = DownloadRequest.Builder(id, locator.toUri()).setData(title.toByteArray()).build()
        val before = withContext(Dispatchers.IO) { manager.downloadIndex.getDownload(id)?.updateTimeMs }
        Exports.put(app, id, record)
        DownloadStore.flush()
        app.getSharedPreferences("download-retries", Context.MODE_PRIVATE).edit { remove(id); remove("attempts:$id") }
        app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit { remove(id) }
        DownloadService.sendAddDownload(app, OfflineDownloadService::class.java, request, true)
        // Keep the preparation journal until Media3 has persisted the handoff.
        withTimeoutOrNull(10_000) {
            withContext(Dispatchers.IO) {
                var queued = manager.downloadIndex.getDownload(id)
                while (queued?.request != request || queued.updateTimeMs == before) {
                    delay(100)
                    queued = manager.downloadIndex.getDownload(id)
                }
            }
        } ?: error("The transfer did not enter the download queue. Retry this episode.")
        return id
    }
    override suspend fun retry(id: String) {
        val stored = Exports.all(app)[id]
        stored?.identity?.takeIf { it.isSiteDownload }?.let { identity ->
            val tree = stored.tree.takeIf { hasOutputFolderAccess(app, it) } ?: settings.outputTree
            val existing = withContext(Dispatchers.IO) { findExistingOutput(app, tree, stored.outputLocation("mp4")) }
            if (existing != null) { adoptExistingVideo(app, identity, existing, tree); refresh(); return }
        }
        Exports.withItemLock(id) {
            if (Exports.deleting(id)) return@withItemLock
            val record = Exports.all(app)[id]
            val identity = record?.identity
            if (identity == null) {
                error("This download has no saved source context. Open the show and select the episode again.")
            }
            val newId = enqueue(identity, record)
            if (newId != id) {
                app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit { remove(id) }
                if (id.startsWith("direct:")) withContext(Dispatchers.IO) {
                    val number = id.removePrefix("direct:")
                    (app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).remove(number.toLong())
                    DownloadStore.operation(number, "direct", null)
                }
                Exports.remove(app, id)
            }
        }
        refresh()
    }
    override suspend fun retrySave(id: String) {
        val index = withContext(Dispatchers.Main.immediate) { OfflineDownloads.manager(app).downloadIndex }
        Exports.withItemLock(id) {
            if (Exports.deleting(id)) return@withItemLock
            val record = Exports.all(app)[id]?.takeIf { it.uri.isEmpty() } ?: return@withItemLock
            if (!hasOutputFolderAccess(app) || DownloadStore.operationValue("transfers", "folder-access") != null) throw OutputFolderAccessRequired()
            check(withContext(Dispatchers.IO) { exportTransferReady(app, index, id) }) { "Temporary media is unavailable. Retry the download." }
            Exports.put(app, id, record.copy(error = "", tree = settings.outputTree, folder = settings.outputFolder))
            DownloadStore.flush()
            check(Exports.start(app)) { "Saving could not start. Reopen Downloads to try again." }
        }
    }
}
