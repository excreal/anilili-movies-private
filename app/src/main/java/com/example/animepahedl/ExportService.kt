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
    val identity: DownloadIdentity? = null, val pendingUri: String? = null)

internal data class ExportProgress(val stage: String, val percent: Float, val bytes: Long = 0)

internal object Exports {
    val progress = java.util.concurrent.ConcurrentHashMap<String, ExportProgress>()
    private val progressEvents = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    /** Emits whenever an export stage or percentage changes. */
    val updates = progressEvents

    fun setProgress(id: String, value: ExportProgress) {
        if (progress.put(id, value) != value) progressEvents.tryEmit(Unit)
    }

    fun clearProgress(id: String) {
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
    fun cancel(context: Context, id: String) {
        jobs[id]?.cancel()
        runCatching {
            context.startService(Intent(context, ExportService::class.java)
                .setAction(ExportService.ACTION_CANCEL).putExtra(ExportService.EXTRA_ID, id))
        }.onFailure { DebugTrace.event("export_cancel_failed", "reason" to it.message) }
    }
    fun register(id: String, job: Job) { jobs[id] = job }
    fun unregister(id: String) { jobs.remove(id) }
    fun start(context: Context) {
        runCatching { context.startForegroundService(Intent(context, ExportService::class.java)) }
            .onFailure { failure ->
                all(context).filter { (id, record) -> record.uri.isEmpty() && !deleting(id) }.forEach { (id, record) ->
                    put(context, id, record.copy(error = "Saving could not start. Reopen the app and tap Retry save. ${failure.javaClass.simpleName}"))
                }
            }
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
        val percent = progress?.percent?.takeIf { it >= 0 }?.toInt()?.coerceIn(0, 100)
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
        if (intent?.action == ACTION_CANCEL) {
            if (activeExportId == intent.getStringExtra(EXTRA_ID)) activeExport?.cancel()
            return START_NOT_STICKY
        }
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel("exports", "Saving videos", NotificationManager.IMPORTANCE_LOW))
        startForeground(2, savingNotification())
        if (running?.isActive != true) running = scope.launch {
            DownloadStore.awaitReady()
            try {
                var idleChecks = 0
                while (isActive) {
                    DownloadStore.flush()
                    val pending = Exports.all(this@ExportService).filter { (id, record) -> record.uri.isEmpty() && record.error.isEmpty() && !Exports.deleting(id) }
                    val next = withContext(Dispatchers.IO) { QueueOrder.ordered(this@ExportService, pending.keys.toList()).firstOrNull { ready(it) }?.let { pending.entries.first { entry -> entry.key == it } } }
                    if (next != null) {
                        idleChecks = 0
                        activeExportId = next.key
                        activeExport = scope.launch(start = CoroutineStart.LAZY) {
                            Exports.withItemLock(next.key) {
                                if (!Exports.deleting(next.key)) export(next.key, Exports.all(this@ExportService)[next.key] ?: return@withItemLock)
                            }
                        }
                        Exports.register(next.key, activeExport!!)
                        activeExport?.start()
                        activeExport?.join()
                        Exports.unregister(next.key)
                        activeExport = null
                        activeExportId = null
                    } else if (pending.isNotEmpty() && idleChecks++ < 5) {
                        delay(1000)
                    } else break
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { DebugTrace.event("export_service_deferred", "type" to e.javaClass.simpleName) }
            finally { stopSelf() }
        }
        return START_NOT_STICKY
    }
    private fun ready(id: String): Boolean {
        if (!id.startsWith("direct:")) return OfflineDownloads.manager(this).downloadIndex.getDownload(id)?.state == Download.STATE_COMPLETED
        val manager = getSystemService(DownloadManager::class.java)
        return manager.query(DownloadManager.Query().setFilterById(id.removePrefix("direct:").toLong())).use {
            it.moveToFirst() && it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
        }
    }
    private fun downloading(id: String): Boolean {
        if (!id.startsWith("direct:")) return OfflineDownloads.manager(this).downloadIndex.getDownload(id)?.state in
            listOf(Download.STATE_QUEUED, Download.STATE_DOWNLOADING, Download.STATE_RESTARTING)
        return getSystemService(DownloadManager::class.java).query(DownloadManager.Query().setFilterById(id.removePrefix("direct:").toLong())).use {
            it.moveToFirst() && it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) in
                listOf(DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED)
        }
    }
    private suspend fun export(id: String, record: ExportRecord) {
        // A foreground notification alone does not keep the CPU running with the screen off.
        wakeLock.acquire(95 * 60_000L)
        val temp = File(getExternalFilesDir(null) ?: filesDir, "export-temp").apply { mkdirs() }
            .resolve("${id.hashCode().toUInt()}.mp4")
        var pendingOutput: OutputDocument? = null
        var outputPublished = false
        try {
            record.pendingUri?.takeIf { it.isNotEmpty() }?.let { old ->
                withContext(Dispatchers.IO) { deleteOwnedMedia(this@ExportService, (old).toUri()) }
                Exports.put(this, id, record.copy(pendingUri = null))
            }
            val estimated = if (id.startsWith("direct:")) 0L else withContext(Dispatchers.IO) { OfflineDownloads.manager(this@ExportService).downloadIndex.getDownload(id)?.bytesDownloaded ?: 0L }
            val available = android.os.StatFs(temp.parentFile!!.absolutePath).availableBytes
            require(estimated <= 0 || available > estimated * if (record.tree.isBlank()) 2 else 1) { "Not enough space for assembly and final copy. Free storage and Retry save." }
            val direct = id.startsWith("direct:")
            if (direct) {
                val source = getSystemService(DownloadManager::class.java)
                    .getUriForDownloadedFile(id.removePrefix("direct:").toLong()) ?: error("Downloaded file is unavailable")
                val extension = if (contentResolver.getType(source) == "video/webm") "webm" else "mp4"
                Exports.setProgress(id, ExportProgress("Copying to folder", 0f))
                val destination = withExportProgressDeadline(id) { withContext(Dispatchers.IO) { publish(source, record, extension, id) { ensureActive() } } }
                if (!Exports.all(this).containsKey(id) || Exports.deleting(id)) {
                    withContext(Dispatchers.IO) { deleteOwnedMedia(this@ExportService, destination.first) }
                    throw CancellationException("Download was removed")
                }
                Exports.put(this, id, record.copy(uri = destination.first.toString(), bytes = destination.second, pendingUri = null))
                DownloadStore.flush()
                withContext(Dispatchers.IO) { getSystemService(DownloadManager::class.java).remove(id.removePrefix("direct:").toLong()) }
            } else {
                temp.delete()
                val destination = withContext(Dispatchers.IO) { createOutputDocument(record, "mp4") }
                Exports.put(this, id, record.copy(pendingUri = destination.uri.toString()))
                DownloadStore.flush()
                pendingOutput = destination
                Exports.setProgress(id, ExportProgress("Preparing MP4", -1f))
                val update: (Int) -> Unit = { Exports.setProgress(id, ExportProgress("Preparing MP4", it.toFloat())) }
                val started = android.os.SystemClock.elapsedRealtime()
                withTimeout(30 * 60_000L) { ffmpegRemuxCachedHls(this@ExportService, id, temp, update) }
                withTimeout(2 * 60_000L) {
                    Exports.setProgress(id, ExportProgress("Checking MP4", -1f))
                    runInterruptible(Dispatchers.IO) { validateCachedHlsExport(this@ExportService, id, temp) }
                }
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                withExportProgressDeadline(id) {
                    Exports.setProgress(id, ExportProgress("Copying to folder", -1f))
                    withContext(Dispatchers.IO) {
                        java.security.DigestInputStream(temp.inputStream(), digest).use { input -> destination.openOutput().use { output ->
                            val total = temp.length().coerceAtLeast(1)
                            copyCancellable(input, output) { copied ->
                                Exports.setProgress(id, ExportProgress("Copying to folder", copied * 100f / total, copied))
                            }
                        } }
                    }
                }
                val bytes = temp.length()
                withExportProgressDeadline(id) { withContext(Dispatchers.IO) {
                    Exports.setProgress(id, ExportProgress("Checking saved video", 0f))
                    verifyCopy(this@ExportService, destination.uri, bytes, digest.digest()) { checked -> Exports.setProgress(id, ExportProgress("Checking saved video", checked * 100f / bytes, checked)) }
                } }
                if (!Exports.all(this).containsKey(id) || Exports.deleting(id)) throw CancellationException("Download was removed")
                withContext(Dispatchers.IO) { destination.publish() }
                Exports.put(this, id, record.copy(uri = destination.uri.toString(), bytes = bytes, pendingUri = null))
                DownloadStore.flush()
                outputPublished = true
                DebugTrace.event("export_assembled", "elapsed_ms" to android.os.SystemClock.elapsedRealtime() - started, "bytes" to bytes)
                OfflineDownloads.manager(this).removeDownload(id)
            }
        } catch (e: TimeoutCancellationException) {
            Exports.all(this)[id]?.takeUnless { Exports.deleting(id) }?.let { Exports.put(this, id, it.copy(error = "Saving exceeded the stage time limit. Retry save; the source is retained.")) }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            Exports.all(this)[id]?.takeUnless { Exports.deleting(id) }?.let { Exports.put(this, id, it.copy(error = e.message ?: "Could not save video")) }
        } finally {
            Exports.clearProgress(id)
            withContext(NonCancellable + Dispatchers.IO) {
                if (!outputPublished && pendingOutput != null) {
                    runCatching { deleteOwnedMedia(this@ExportService, pendingOutput.uri) }.onSuccess {
                        Exports.all(this@ExportService)[id]?.let { Exports.put(this@ExportService, id, it.copy(pendingUri = null)) }
                    }
                }
                temp.delete()
                if (wakeLock.isHeld) wakeLock.release()
            }
        }
    }

    private data class OutputDocument(val uri: Uri, val mediaStore: Boolean)

    private fun createOutputDocument(record: ExportRecord, extension: String): OutputDocument {
        val location = record.outputLocation(extension)
        val mime = if (extension == "webm") "video/webm" else "video/mp4"
        val tree = record.tree.takeIf { it.isNotEmpty() }?.let(Uri::parse)
        val uri = if (tree != null) {
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
            var showDirectory: Uri? = null
            contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == location.showFolder && cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        showDirectory = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)); break
                    }
                }
            }
            val parent = showDirectory ?: DocumentsContract.createDocument(contentResolver, root,
                DocumentsContract.Document.MIME_TYPE_DIR, location.showFolder) ?: error("Cannot create the show folder")
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
        val location = record.outputLocation(extension)
        val name = location.fileName
        val mime = if (extension == "webm") "video/webm" else "video/mp4"
        val tree = record.tree.takeIf { it.isNotEmpty() }?.let(Uri::parse)
        val uri = if (tree != null) {
            val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            var showDirectory: Uri? = null
            contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == location.showFolder && cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        showDirectory = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)); break
                    }
                }
            }
            val parent = showDirectory ?: DocumentsContract.createDocument(contentResolver, root,
                DocumentsContract.Document.MIME_TYPE_DIR, location.showFolder) ?: error("Cannot create the show folder")
            DocumentsContract.createDocument(contentResolver, parent, mime, name) ?: error("Cannot create file in this folder. Choose the folder again in Settings.")
        } else {
            contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, listOf(Environment.DIRECTORY_DOWNLOADS, record.folder, location.showFolder).filter { it.isNotEmpty() }.joinToString("/"))
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }) ?: error("Cannot create output file")
        }
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
                        Exports.setProgress(id, ExportProgress("Copying to folder", if (total > 0) copied * 100f / total else -1f, copied))
                    }
                }
            }
            checkActive()
            require(bytes > 0) { "Output is empty" }
            Exports.setProgress(id, ExportProgress("Checking saved video", 0f))
            verifyCopy(this@ExportService, uri, bytes, digest.digest()) { checked -> Exports.setProgress(id, ExportProgress("Checking saved video", checked * 100f / bytes, checked)) }
            if (tree == null) require(contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) > 0) { "Output publication failed" }
            return uri to bytes
        } catch (e: Exception) {
            runCatching { deleteOwnedMedia(this@ExportService, uri) }.onSuccess {
                Exports.all(this)[id]?.let { Exports.put(this, id, it.copy(pendingUri = null)) }
            }
            throw e
        }
    }
    companion object {
        const val ACTION_CANCEL = "com.example.animepahedl.CANCEL_EXPORT"
        const val EXTRA_ID = "export_id"
    }
    override fun onTimeout(startId: Int, fgsType: Int) {
        activeExportId?.let { id -> Exports.all(this)[id]?.let { Exports.put(this, id, it.copy(error = "Android stopped saving after its background time limit. Reopen and tap Retry save; the source is retained.")) } }
        activeExport?.cancel(); running?.cancel(); stopSelf()
    }
    override fun onDestroy() { activeExport?.cancel(); scope.cancel(); if (wakeLock.isHeld) wakeLock.release(); super.onDestroy() }
}
