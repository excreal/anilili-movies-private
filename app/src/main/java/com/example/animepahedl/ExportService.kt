package com.example.animepahedl

import androidx.core.net.toUri
import android.app.*
import android.content.*
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.media3.exoplayer.offline.Download
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.File

internal data class ExportRecord(val title: String, val tree: String, val folder: String,
    val uri: String = "", val error: String = "", val bytes: Long = 0,
    val thumbnail: String? = null, val audio: String? = null, val quality: String? = null,
    val showTitle: String? = null, val episodeNumber: String? = null,
    val identity: DownloadIdentity? = null, val pendingUri: String? = null,
    val media: ResolvedMedia? = null)

internal data class ExportProgress(val stage: String, val percent: Float, val bytes: Long = 0, val totalBytes: Long = -1) {
    companion object {
        fun forBytes(stage: String, bytes: Long, total: Long): ExportProgress {
            val knownTotal = displayTransferTotal(bytes, total, hls = false)
            return ExportProgress(stage, byteProgress(bytes, knownTotal), bytes, knownTotal)
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun exportTransferReady(context: Context, index: androidx.media3.exoplayer.offline.DownloadIndex, id: String): Boolean {
    if (!id.startsWith("direct:")) return index.getDownload(id)?.state == Download.STATE_COMPLETED
    val number = id.removePrefix("direct:").toLongOrNull() ?: return false
    // A legacy provider failure must not block exports of other completed transfers.
    return runCatching {
        context.getSystemService(DownloadManager::class.java).query(DownloadManager.Query().setFilterById(number))?.use {
            it.moveToFirst() && it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
        } == true
    }.getOrDefault(false)
}

internal object Exports {
    val progress = java.util.concurrent.ConcurrentHashMap<String, ExportProgress>()
    private val progressEvents = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    /** Emits whenever an export stage or percentage changes. */
    val updates = progressEvents

    private val lastProgressEvents = java.util.concurrent.ConcurrentHashMap<String, Long>()
    fun setProgress(id: String, value: ExportProgress) {
        val before = progress.put(id, value)
        if (before == value) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (before?.stage != value.stage || value.percent >= 100 || now - (lastProgressEvents[id] ?: 0L) >= 500) {
            lastProgressEvents[id] = now
            progressEvents.tryEmit(Unit)
        }
    }

    fun clearProgress(id: String) {
        lastProgressEvents.remove(id)
        if (progress.remove(id) != null) progressEvents.tryEmit(Unit)
    }
    private val locks = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.sync.Mutex>()
    private val jobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    suspend fun <T> withItemLock(id: String, block: suspend () -> T): T {
        val lock = locks.getOrPut(id) { kotlinx.coroutines.sync.Mutex() }
        lock.lock()
        try { return block() } finally { lock.unlock() }
    }
    fun deleting(id: String) = id in DownloadStore.operations("delete")
    fun all(context: Context): Map<String, ExportRecord> = DownloadStore.all()
    fun put(context: Context, id: String, value: ExportRecord) { DownloadStore.put(id, value); progressEvents.tryEmit(Unit) }
    fun prepare(context: Context, id: String, title: String, thumbnail: String? = null, audio: String? = null, quality: String? = null, showTitle: String? = null, episodeNumber: String? = null, identity: DownloadIdentity? = null) {
        if (deleting(id) || all(context).containsKey(id)) return
        val settings = AppSettings(context)
        put(context, id, ExportRecord(title, settings.outputTree, settings.outputFolder, thumbnail = thumbnail, audio = audio, quality = quality, showTitle = showTitle, episodeNumber = episodeNumber, identity = identity))
    }
    fun remove(context: Context, id: String) { DownloadStore.remove(id); progressEvents.tryEmit(Unit) }
    fun cancel(context: Context, id: String) { jobs[id]?.cancel() }
    fun register(id: String, job: Job) { jobs[id] = job }
    fun unregister(id: String) { jobs.remove(id) }
    fun start(context: Context): Boolean {
        if (!hasOutputFolderAccess(context) || DownloadStore.operationValue("transfers", "folder-access") != null) return false
        return try {
            context.startForegroundService(Intent(context, ExportService::class.java))
            true
        } catch (failure: Exception) {
            // Android may deny a background start. A foreground retry can still save the cache.
            DebugTrace.event("export_start_deferred", "type" to failure.javaClass.simpleName)
            false
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    suspend fun resumeReady(context: Context) {
        if (!hasOutputFolderAccess(context) || DownloadStore.operationValue("transfers", "folder-access") != null) return
        val pending = all(context).filter { (id, record) -> record.uri.isEmpty() && record.error.isEmpty() && !deleting(id) }
        if (pending.isEmpty()) return
        val index = withContext(Dispatchers.Main.immediate) { OfflineDownloads.manager(context).downloadIndex }
        if (withContext(Dispatchers.IO) { pending.keys.any { exportTransferReady(context, index, it) } }) start(context)
    }
}

class DownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = "direct:${intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)}"
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(8_000) {
                    DownloadStore.awaitReady()
                    reconcileDirectEnqueues(context)
                    if (Exports.all(context).containsKey(id)) Exports.start(context)
                }
            } catch (e: Exception) { DebugTrace.event("completion_deferred", "type" to e.javaClass.simpleName) } finally { pending.finish() }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ExportService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var running: Job? = null
    private var activeExport: Job? = null
    private var activeExportId: String? = null
    private var latestStartId = 0
    private var lastNotificationUpdate = 0L
    private var lastNotificationStage = ""

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            Exports.updates.collect {
                val id = activeExportId ?: return@collect
                val progress = Exports.progress[id] ?: return@collect
                val now = android.os.SystemClock.elapsedRealtime()
                if (progress.stage != lastNotificationStage || now - lastNotificationUpdate >= 500 || progress.percent >= 100f) {
                    lastNotificationStage = progress.stage
                    lastNotificationUpdate = now
                    getSystemService(NotificationManager::class.java).notify(2, savingNotification(progress))
                }
            }
        }
    }

    private fun savingNotification(progress: ExportProgress? = null): Notification {
        val percent = progress?.percent?.let(::normalizedProgress)?.takeIf { it >= 0 }?.toInt()
        val stage = progress?.stage?.let(::friendlyDownloadStatus) ?: "Waiting to save"
        return NotificationCompat.Builder(this, "exports")
            .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(activeExportId?.let { Exports.all(this)[it]?.title } ?: "Saving video")
            .setContentText(if (percent == null) stage else "$stage · $percent%")
            .setContentIntent(downloadsPendingIntent(this)).setOnlyAlertOnce(true).setOngoing(true)
            .setProgress(100, percent ?: 0, percent == null).build()
    }
    private val wakeLock by lazy {
        getSystemService(android.os.PowerManager::class.java)
            .newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "$packageName:mp4-export")
            .apply { setReferenceCounted(false) }
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel("exports", "Saving videos", NotificationManager.IMPORTANCE_LOW))
        startForeground(2, savingNotification())
        if (running?.isActive != true) running = scope.launch {
            try {
                DownloadStore.awaitReady()
                val index = OfflineDownloads.manager(this@ExportService).downloadIndex
                var idleChecks = 0
                while (isActive) {
                    val checkedStartId = latestStartId
                    DownloadStore.flush()
                    val pending = (if (DownloadStore.operations("folder-access")["transfers"] != null) emptyMap() else Exports.all(this@ExportService)).filter { (id, record) -> record.uri.isEmpty() && record.error.isEmpty() && !Exports.deleting(id) }
                    val next = withContext(Dispatchers.IO) {
                        QueueOrder.ordered(this@ExportService, pending.keys.toList()).firstOrNull { id ->
                            id in pending && exportTransferReady(this@ExportService, index, id)
                        }
                    }
                    if (next != null) {
                        idleChecks = 0
                        activeExportId = next
                        val job = scope.launch(start = CoroutineStart.LAZY) {
                            Exports.withItemLock(next) {
                                val record = Exports.all(this@ExportService)[next] ?: return@withItemLock
                                if (record.uri.isEmpty() && record.error.isEmpty() && !Exports.deleting(next)) export(next, record)
                            }
                        }
                        activeExport = job
                        Exports.register(next, job)
                        try {
                            job.start()
                            job.join()
                        } finally {
                            Exports.unregister(next)
                            activeExport = null
                            activeExportId = null
                        }
                    } else if (checkedStartId != latestStartId) {
                        idleChecks = 0
                    } else if (pending.isNotEmpty() && idleChecks++ < 5) {
                        delay(1000)
                    } else break
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { DebugTrace.event("export_service_deferred", "type" to e.javaClass.simpleName) }
            finally { stopSelfResult(latestStartId) }
        }
        return START_NOT_STICKY
    }
    private suspend fun export(id: String, original: ExportRecord) {
        var record = original
        var tempFile: File? = null
        var acquiredWakeLock = false
        var pendingOutput: OutputDocument? = null
        var outputPublished = false
        var createdSubtitleSidecars = emptyList<Uri>()
        try {
            wakeLock.acquire(95 * 60_000L)
            acquiredWakeLock = true
            val temp = withContext(Dispatchers.IO) {
                File(getExternalFilesDir(null) ?: filesDir, "export-temp").apply {
                    check(isDirectory || mkdirs() || isDirectory) { "Could not create temporary video directory" }
                }.resolve("${id.hashCode().toUInt()}.mp4")
            }
            tempFile = temp
            val tree = record.tree.takeIf { hasOutputFolderAccess(this, it) } ?: AppSettings(this).outputTree
            if (!hasOutputFolderAccess(this, tree)) throw OutputFolderAccessRequired()
            record = record.copy(tree = tree)
            // Older queued jobs may have resolved video before captions were available.
            // Recover captions for this exact episode before publishing its saved files.
            val identity = record.identity
            if (identity?.anime?.session?.startsWith("sites:KISSKH:") == true) {
                val fresh = try {
                    KisskhApi(cookies = SiteDownloadCookieStore(this)::headerFor).subtitles(identity.episode.session)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { emptyList() }
                if (fresh.isNotEmpty()) {
                    val media = record.media
                    if (media != null) record = record.copy(media = media.copy(
                        subtitles = mergeKisskhSubtitleTracks(media.subtitles.orEmpty(), fresh)))
                }
            }
            record.pendingUri?.takeIf { it.isNotEmpty() }?.let { old ->
                withContext(Dispatchers.IO) { deleteOwnedMedia(this@ExportService, (old).toUri()) }
                record = record.copy(pendingUri = null)
                Exports.put(this, id, record)
                DownloadStore.flush()
            }
            val existing = withContext(Dispatchers.IO) { findExistingOutput(this@ExportService, record.tree, record.outputLocation("mp4")) }
            if (existing != null) throw ExistingVideoFound(existing)
            val direct = id.startsWith("direct:")
            val progressive = !direct && record.identity?.isSiteDownload == true && record.media?.isHls() == false
            val index = OfflineDownloads.manager(this).downloadIndex
            val hlsSource = if (direct || progressive) null else withContext(Dispatchers.IO) {
                requireCachedHlsSegments(this@ExportService, id)
            }
            val stagedBytes = when {
                hlsSource != null -> hlsSource.mediaBytes
                direct -> 0L
                else -> withContext(Dispatchers.IO) { index.getDownload(id)?.bytesDownloaded ?: 0L }
            }
            val available = withContext(Dispatchers.IO) { android.os.StatFs(temp.parentFile!!.absolutePath).availableBytes }
            require(stagedBytes <= available / if (record.tree.isBlank()) 2 else 1) { "Not enough space for assembly and final copy. Free storage and Retry save." }
            if (direct || progressive) {
                val source = when {
                    direct -> getSystemService(DownloadManager::class.java)
                        .getUriForDownloadedFile(id.removePrefix("direct:").toLong()) ?: error("Downloaded file is unavailable")
                    else -> {
                        Exports.setProgress(id, ExportProgress("Preparing video", -1f))
                        withExportProgressDeadline(id) { withContext(Dispatchers.IO) {
                            copyCachedProgressive(id, temp) { copied ->
                                ensureActive()
                                Exports.setProgress(id, ExportProgress.forBytes("Preparing video", copied, stagedBytes))
                            }
                        } }
                        Uri.fromFile(temp)
                    }
                }
                val extension = if (record.media?.mimeType == "video/webm" ||
                    record.media?.url?.toUri()?.path.orEmpty().endsWith(".webm", true) ||
                    (direct && contentResolver.getType(source) == "video/webm")) "webm" else "mp4"
                Exports.setProgress(id, ExportProgress("Copying to folder", 0f))
                val destination = withExportProgressDeadline(id) { withContext(Dispatchers.IO) { publish(source, record, extension, id) { ensureActive() } } }
                if (!Exports.all(this).containsKey(id) || Exports.deleting(id)) {
                    withContext(Dispatchers.IO) { deleteOwnedMedia(this@ExportService, destination.first) }
                    throw CancellationException("Download was removed")
                }
                Exports.put(this, id, record.copy(uri = destination.first.toString(), bytes = destination.second, pendingUri = null))
                DownloadStore.flush()
                if (direct) withContext(Dispatchers.IO) {
                    val number = id.removePrefix("direct:")
                    getSystemService(DownloadManager::class.java).remove(number.toLong())
                    DownloadStore.operation(number, "direct", null)
                    DownloadStore.flush()
                } else OfflineDownloads.manager(this).removeDownload(id)
            } else {
                temp.delete()
                val destination = withContext(Dispatchers.IO) { createOutputDocument(record, "mp4") }
                Exports.put(this, id, record.copy(pendingUri = destination.uri.toString()))
                DownloadStore.flush()
                pendingOutput = destination
                Exports.setProgress(id, ExportProgress("Preparing MP4", -1f))
                val started = android.os.SystemClock.elapsedRealtime()
                withTimeout(30 * 60_000L) {
                    ffmpegRemuxCachedHls(this@ExportService, id, temp, hlsSource) {
                        Exports.setProgress(id, ExportProgress("Preparing MP4", it.percent.toFloat(), it.bytes))
                    }
                }
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                withExportProgressDeadline(id) {
                    Exports.setProgress(id, ExportProgress("Copying to folder", -1f))
                    withContext(Dispatchers.IO) {
                        java.security.DigestInputStream(temp.inputStream(), digest).use { input -> destination.openOutput().use { output ->
                            val total = temp.length()
                            copyCancellable(input, output) { copied ->
                                Exports.setProgress(id, ExportProgress.forBytes("Copying to folder", copied, total))
                            }
                        } }
                    }
                }
                val bytes = temp.length()
                withExportProgressDeadline(id) { withContext(Dispatchers.IO) {
                    Exports.setProgress(id, ExportProgress("Checking saved video", 0f))
                    verifyCopy(this@ExportService, destination.uri, bytes, digest.digest()) { checked ->
                        Exports.setProgress(id, ExportProgress.forBytes("Checking saved video", checked, bytes))
                    }
                } }
                if (!Exports.all(this).containsKey(id) || Exports.deleting(id)) throw CancellationException("Download was removed")
                createdSubtitleSidecars = publishSubtitleSidecars(record) { if (Exports.deleting(id)) throw CancellationException("Download was removed") }
                withContext(Dispatchers.IO) { destination.publish() }
                Exports.put(this, id, record.copy(uri = destination.uri.toString(), bytes = bytes, pendingUri = null))
                DownloadStore.flush()
                outputPublished = true
                DebugTrace.event("export_assembled", "elapsed_ms" to android.os.SystemClock.elapsedRealtime() - started, "bytes" to bytes)
                OfflineDownloads.manager(this).removeDownload(id)
            }
        } catch (found: ExistingVideoFound) {
            if (Exports.all(this).containsKey(id) && !Exports.deleting(id)) {
                createdSubtitleSidecars = publishSubtitleSidecars(record) { if (Exports.deleting(id)) throw CancellationException("Download was removed") }
                Exports.put(this, id, record.copy(uri = found.video.uri.toString(), bytes = found.video.bytes, pendingUri = null, error = ""))
                DownloadStore.flush()
                outputPublished = true
                if (id.startsWith("direct:")) withContext(Dispatchers.IO) {
                    val number = id.removePrefix("direct:")
                    getSystemService(DownloadManager::class.java).remove(number.toLong())
                    DownloadStore.operation(number, "direct", null)
                    DownloadStore.flush()
                } else OfflineDownloads.manager(this).removeDownload(id)
            }
        } catch (e: TimeoutCancellationException) {
            DebugTrace.event("export_failed", "id" to id.hashCode().toUInt().toString(),
                "stage" to (Exports.progress[id]?.stage ?: "unknown"), "type" to e.javaClass.simpleName,
                "message" to "Export stage timed out")
            Exports.all(this)[id]?.takeUnless { Exports.deleting(id) }?.let { Exports.put(this, id, it.copy(error = "Saving exceeded the stage time limit. Retry save; the source is retained.")) }
        } catch (e: CancellationException) {
            DebugTrace.event("export_cancelled", "id" to id.hashCode().toUInt().toString(),
                "stage" to (Exports.progress[id]?.stage ?: "unknown"), "deleting" to Exports.deleting(id))
            throw e
        } catch (e: Exception) {
            DebugTrace.event("export_failed", "id" to id.hashCode().toUInt().toString(),
                "stage" to (Exports.progress[id]?.stage ?: "unknown"), "type" to e.javaClass.simpleName,
                "message" to (e.message ?: "Could not save video"))
            Exports.all(this)[id]?.takeUnless { Exports.deleting(id) }?.let { Exports.put(this, id, it.copy(error = e.message ?: "Could not save video")) }
        } finally {
            Exports.clearProgress(id)
            try { withContext(NonCancellable + Dispatchers.IO) {
                if (!outputPublished) createdSubtitleSidecars.asReversed().forEach { sidecar ->
                    runCatching { deleteOwnedMedia(this@ExportService, sidecar) }
                }
                if (!outputPublished && pendingOutput != null) {
                    runCatching { deleteOwnedMedia(this@ExportService, pendingOutput.uri) }.onSuccess {
                        Exports.all(this@ExportService)[id]?.let { Exports.put(this@ExportService, id, it.copy(pendingUri = null)) }
                    }
                }
                tempFile?.delete()
            } } finally {
                if (acquiredWakeLock && wakeLock.isHeld) wakeLock.release()
            }
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private suspend fun copyCachedProgressive(id: String, destination: File, progress: (Long) -> Unit) {
        val download = OfflineDownloads.manager(this).downloadIndex.getDownload(id)
            ?: error("Cached media is unavailable. Retry download.")
        require(download.state == Download.STATE_COMPLETED) { "Download is not complete" }
        copyCachedProgressive(OfflineDownloads.cache, download.request, download.contentLength, destination, progress)
    }

    private class ExistingVideoFound(val video: ExistingVideo) : java.io.IOException("The requested video is already saved.")
    private data class OutputDocument(val uri: Uri, val mediaStore: Boolean)

    private fun verifyCreatedName(uri: Uri, record: ExportRecord, extension: String) {
        val expected = record.outputLocation(extension).fileName
        val actual = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
        if (actual != expected) {
            // This is our new, empty file; never keep an automatically numbered duplicate.
            deleteOwnedMedia(this, uri)
            val found = findExistingOutput(this, record.tree, record.outputLocation(extension))
            if (found != null) throw ExistingVideoFound(found)
            throw java.io.IOException("A conflicting file exists. Choose the original downloads folder before retrying.")
        }
    }

    private fun outputParent(tree: Uri, location: DownloadLocation): Uri {
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
        val rootName = contentResolver.query(root, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
        if (rootName != null && existingVideoName(rootName) == existingVideoName(location.showFolder)) return root
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
        val cursor = contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)
            ?: throw java.io.IOException("Cannot list the selected folder.")
        cursor.use {
            while (it.moveToNext()) if (existingVideoName(it.getString(1).orEmpty()) == existingVideoName(location.showFolder) &&
                it.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR)
                return DocumentsContract.buildDocumentUriUsingTree(tree, it.getString(0))
        }
        return DocumentsContract.createDocument(contentResolver, root, DocumentsContract.Document.MIME_TYPE_DIR, location.showFolder)
            ?: error("Cannot create the show folder")
    }

    private fun createOutputDocument(record: ExportRecord, extension: String): OutputDocument {
        findExistingOutput(this, record.tree, record.outputLocation(extension))?.let { throw ExistingVideoFound(it) }
        val location = record.outputLocation(extension)
        val mime = if (extension == "webm") "video/webm" else "video/mp4"
        val tree = record.tree.takeIf { it.isNotEmpty() }?.let(Uri::parse)
        val uri = if (tree != null) {
            val parent = outputParent(tree, location)
            DocumentsContract.createDocument(contentResolver, parent, mime, location.fileName)
                ?: error("Cannot create file in this folder. Choose the folder again in Settings.")
        } else {
            contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, location.fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, listOf(Environment.DIRECTORY_DOWNLOADS, record.folder, location.showFolder).filter { it.isNotEmpty() }.joinToString("/"))
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }) ?: error("Cannot create output file")
        }
        verifyCreatedName(uri, record, extension)
        return OutputDocument(uri, mediaStore = tree == null)
    }

    private fun OutputDocument.openOutput(): java.io.FileOutputStream {
        val descriptor = contentResolver.openFileDescriptor(uri, "wt") ?: error("Cannot write to output folder")
        val closed = java.util.concurrent.atomic.AtomicBoolean(false)
        return object : java.io.FileOutputStream(descriptor.fileDescriptor) {
            override fun close() {
                if (closed.compareAndSet(false, true)) {
                    try { super.close() } finally { descriptor.close() }
                }
            }
        }
    }

    private fun OutputDocument.clear() { openOutput().close() }

    private fun OutputDocument.size(): Long {
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0).takeIf { it >= 0 }?.let { return it }
        }
        return contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }?.coerceAtLeast(0) ?: 0
    }

    private fun OutputDocument.publish() {
        if (mediaStore) require(contentResolver.update(uri, ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }, null, null) > 0) { "Output publication failed" }
    }

    private fun OutputDocument.delete() { runCatching { deleteOwnedMedia(this@ExportService, uri) } }

    private suspend fun publish(source: Uri, record: ExportRecord, extension: String, id: String, checkActive: () -> Unit): Pair<Uri, Long> {
        findExistingOutput(this, record.tree, record.outputLocation(extension))?.let { throw ExistingVideoFound(it) }
        val location = record.outputLocation(extension)
        val name = location.fileName
        val mime = if (extension == "webm") "video/webm" else "video/mp4"
        val tree = record.tree.takeIf { it.isNotEmpty() }?.let(Uri::parse)
        val uri = if (tree != null) {
            val parent = outputParent(tree, location)
            DocumentsContract.createDocument(contentResolver, parent, mime, name) ?: error("Cannot create file in this folder. Choose the folder again in Settings.")
        } else {
            contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, listOf(Environment.DIRECTORY_DOWNLOADS, record.folder, location.showFolder).filter { it.isNotEmpty() }.joinToString("/"))
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }) ?: error("Cannot create output file")
        }
        verifyCreatedName(uri, record, extension)
        var createdSidecars = emptyList<Uri>()
        try {
            Exports.put(this, id, record.copy(pendingUri = uri.toString()))
            DownloadStore.flush()
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = contentResolver.openInputStream(source)?.let { java.security.DigestInputStream(it, digest) }.use { input ->
                requireNotNull(input) { "Cannot open temporary media" }
                contentResolver.openOutputStream(uri, "w").use { output ->
                    requireNotNull(output) { "Cannot write to output folder" }
                    val total = contentResolver.openAssetFileDescriptor(source, "r")?.use { it.length } ?: -1L
                    copyCancellable(input, output) { copied ->
                        checkActive()
                        Exports.setProgress(id, ExportProgress.forBytes("Copying to folder", copied, total))
                    }
                }
            }
            checkActive()
            require(bytes > 0) { "Output is empty" }
            Exports.setProgress(id, ExportProgress("Checking saved video", 0f))
            verifyCopy(this@ExportService, uri, bytes, digest.digest()) { checked ->
                Exports.setProgress(id, ExportProgress.forBytes("Checking saved video", checked, bytes))
            }
            createdSidecars = publishSubtitleSidecars(record, checkActive)
            if (tree == null) require(contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) > 0) { "Output publication failed" }
            return uri to bytes
        } catch (e: Exception) {
            createdSidecars.asReversed().forEach { sidecar ->
                runCatching { deleteOwnedMedia(this@ExportService, sidecar) }
            }
            runCatching { deleteOwnedMedia(this@ExportService, uri) }.onSuccess {
                Exports.all(this)[id]?.let { Exports.put(this, id, it.copy(pendingUri = null)) }
            }
            throw e
        }
    }

    /** Export normalized, app-owned caption files next to the completed video. */
    private suspend fun publishSubtitleSidecars(record: ExportRecord, checkActive: () -> Unit): List<Uri> = withContext(Dispatchers.IO) {
        val location = record.outputLocation("mp4")
        val tracks = record.media?.subtitles.orEmpty()
        if (tracks.isEmpty()) return@withContext emptyList()
        val tree = record.tree.takeIf(String::isNotBlank)?.let(Uri::parse)
        val usedNames = mutableSetOf<String>()
        val created = mutableListOf<Uri>()
        try {
            tracks.forEachIndexed { index, track ->
                currentCoroutineContext().ensureActive()
                val source = ProviderSubtitles.managedFile(track) ?: return@forEachIndexed
                var name = subtitleSidecarName(location.fileName, track.language, index)
                if (!usedNames.add(name.lowercase())) {
                    name = subtitleSidecarName(location.fileName, "", index)
                    if (!usedNames.add(name.lowercase())) return@forEachIndexed
                }
                var output: Uri? = null
                try {
                    checkActive()
                    val sidecar = if (tree != null) {
                        val parent = outputParent(tree, location)
                        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent))
                        val exists = contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
                            ?.use { cursor -> generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.any { it.equals(name, true) } } == true
                        if (exists) return@forEachIndexed
                        DocumentsContract.createDocument(contentResolver, parent, "text/vtt", name)
                            ?: throw java.io.IOException("Could not create subtitle sidecar.")
                    } else {
                        val relativePath = listOf(Environment.DIRECTORY_DOWNLOADS, record.folder, location.showFolder)
                            .filter(String::isNotBlank).joinToString("/").trimEnd('/') + "/"
                        val files = MediaStore.Files.getContentUri("external")
                        val exists = contentResolver.query(files, arrayOf(MediaStore.MediaColumns._ID),
                            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                            arrayOf(relativePath, name), null)?.use { it.moveToFirst() } == true
                        if (exists) return@forEachIndexed
                        contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                            put(MediaStore.MediaColumns.MIME_TYPE, "text/vtt")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }) ?: throw java.io.IOException("Could not create subtitle sidecar.")
                    }
                    output = sidecar
                    created += sidecar
                    source.inputStream().use { input ->
                        contentResolver.openOutputStream(sidecar, "w")?.use { target ->
                            val buffer = ByteArray(16 * 1024)
                            var copied = 0L
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                copied += count
                                require(copied <= 4_000_000L) { "Subtitle sidecar exceeds the size limit." }
                                checkActive()
                                target.write(buffer, 0, count)
                            }
                            require(copied > 0) { "Subtitle sidecar is empty." }
                        } ?: throw java.io.IOException("Could not write subtitle sidecar.")
                    }
                    if (tree == null) require(contentResolver.update(sidecar, ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }, null, null) > 0) { "Could not publish subtitle sidecar." }
                    DebugTrace.event("subtitle_sidecar_exported", "language" to track.language.take(20), "bytes" to source.length())
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    output?.let { runCatching { deleteOwnedMedia(this@ExportService, it) }; created.remove(it) }
                    DebugTrace.event("subtitle_sidecar_export_failed", "language" to track.language.take(20), "reason" to (error.message ?: "unknown").take(120))
                }
            }
        } catch (cancelled: CancellationException) {
            created.asReversed().forEach { runCatching { deleteOwnedMedia(this@ExportService, it) } }
            throw cancelled
        }
        created.toList()
    }
    override fun onTimeout(startId: Int, fgsType: Int) {
        activeExportId?.let { id -> Exports.all(this)[id]?.let { Exports.put(this, id, it.copy(error = "Android stopped saving after its background time limit. Reopen and tap Retry save; the source is retained.")) } }
        activeExport?.cancel(); running?.cancel(); stopSelf()
    }
    override fun onDestroy() { activeExport?.cancel(); scope.cancel(); if (wakeLock.isHeld) wakeLock.release(); super.onDestroy() }
}
