package com.example.animepahedl

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
    val showTitle: String? = null, val episodeNumber: String? = null)

internal data class ExportProgress(val stage: String, val percent: Float)

internal object Exports {
    val progress = java.util.concurrent.ConcurrentHashMap<String, ExportProgress>()
    private val progressEvents = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    /** Emits whenever an export stage or percentage changes. */
    val updates = progressEvents

    fun setProgress(id: String, value: ExportProgress) {
        progress[id] = value
        progressEvents.tryEmit(Unit)
    }

    fun clearProgress(id: String) {
        if (progress.remove(id) != null) progressEvents.tryEmit(Unit)
    }
    private val gson = Gson()
    private fun prefs(context: Context) = context.getSharedPreferences("exports", Context.MODE_PRIVATE)
    fun all(context: Context): Map<String, ExportRecord> = prefs(context).all.mapNotNull { (id, value) ->
        runCatching { id to gson.fromJson(value as String, ExportRecord::class.java) }.getOrNull()
    }.toMap()
    fun put(context: Context, id: String, value: ExportRecord) { prefs(context).edit().putString(id, gson.toJson(value)).apply() }
    fun prepare(context: Context, id: String, title: String, thumbnail: String? = null, audio: String? = null, quality: String? = null, showTitle: String? = null, episodeNumber: String? = null) {
        val settings = AppSettings(context)
        put(context, id, ExportRecord(title, settings.outputTree, settings.outputFolder, thumbnail = thumbnail, audio = audio, quality = quality, showTitle = showTitle, episodeNumber = episodeNumber))
    }
    fun remove(context: Context, id: String) { prefs(context).edit().remove(id).apply() }
    fun cancel(context: Context, id: String) {
        runCatching {
            context.startService(Intent(context, ExportService::class.java)
                .setAction(ExportService.ACTION_CANCEL).putExtra(ExportService.EXTRA_ID, id))
        }.onFailure { DebugTrace.event("export_cancel_failed", "reason" to it.message) }
    }
    fun start(context: Context) {
        runCatching { context.startForegroundService(Intent(context, ExportService::class.java)) }
            .onFailure { DebugTrace.event("export_start_failed", "reason" to it.message) }
    }
}

class DownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = "direct:${intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)}"
        if (Exports.all(context).containsKey(id)) Exports.start(context)
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ExportService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var running: Job? = null
    private var activeExport: Job? = null
    private var activeExportId: String? = null
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
        startForeground(2, NotificationCompat.Builder(this, "exports")
            .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("Saving downloads")
            .setContentText("Downloading and saving videos to your output folder").setOngoing(true).build())
        if (running?.isActive != true) running = scope.launch {
            try {
                var idleChecks = 0
                while (isActive) {
                    val pending = Exports.all(this@ExportService).filterValues { it.uri.isEmpty() && it.error.isEmpty() }
                    val next = pending.entries.firstOrNull { ready(it.key) }
                    if (next != null) {
                        idleChecks = 0
                        activeExportId = next.key
                        activeExport = scope.launch { export(next.key, next.value) }
                        activeExport?.join()
                        activeExport = null
                        activeExportId = null
                    } else if (pending.keys.any { downloading(it) } || (pending.isNotEmpty() && idleChecks++ < 5)) {
                        delay(1000)
                    } else break
                }
            } finally { stopSelf() }
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
        wakeLock.acquire(30 * 60_000L)
        val temp = File(getExternalFilesDir(null) ?: filesDir, "export-temp").apply { mkdirs() }
            .resolve("${id.hashCode().toUInt()}.mp4")
        var pendingOutput: OutputDocument? = null
        var outputPublished = false
        try {
            val direct = id.startsWith("direct:")
            if (direct) {
                val source = getSystemService(DownloadManager::class.java)
                    .getUriForDownloadedFile(id.removePrefix("direct:").toLong()) ?: error("Downloaded file is unavailable")
                val extension = if (contentResolver.getType(source) == "video/webm") "webm" else "mp4"
                Exports.setProgress(id, ExportProgress("Copying to folder", 0f))
                val destination = withContext(Dispatchers.IO) { publish(source, record, extension, id) { ensureActive() } }
                Exports.put(this, id, record.copy(uri = destination.first.toString(), bytes = destination.second))
                getSystemService(DownloadManager::class.java).remove(id.removePrefix("direct:").toLong())
            } else {
                temp.delete()
                val destination = createOutputDocument(record, "mp4")
                pendingOutput = destination
                Exports.setProgress(id, ExportProgress("Preparing MP4", -1f))
                var stage = "Preparing MP4"
                val update: (Int) -> Unit = { Exports.setProgress(id, ExportProgress(stage, it.toFloat())) }
                val started = android.os.SystemClock.elapsedRealtime()
                assembleCachedHlsToDocument(this, id, temp.absolutePath,
                    openOutput = { destination.openOutput() }, clearOutput = { destination.clear() },
                    outputBytes = { destination.size() },
                    validate = { validateCachedHlsExport(this, id, destination.uri) }, onStage = {
                    stage = it
                    Exports.setProgress(id, ExportProgress(it, -1f))
                }, onProgress = update)
                val bytes = destination.size()
                destination.publish()
                Exports.put(this, id, record.copy(uri = destination.uri.toString(), bytes = bytes))
                outputPublished = true
                DebugTrace.event("export_assembled", "elapsed_ms" to android.os.SystemClock.elapsedRealtime() - started, "bytes" to bytes)
                OfflineDownloads.manager(this).removeDownload(id)
            }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            if (Exports.all(this).containsKey(id)) Exports.put(this, id, record.copy(error = e.message ?: "Could not save video"))
        } finally {
            Exports.clearProgress(id)
            withContext(NonCancellable + Dispatchers.IO) {
                if (!outputPublished) pendingOutput?.delete()
                temp.delete()
            }
        }
    }

    private data class OutputDocument(val uri: Uri, val mediaStore: Boolean)

    private fun createOutputDocument(record: ExportRecord, extension: String): OutputDocument {
        val parts = record.title.split(" · ")
        val location = downloadLocation(record.showTitle ?: parts.first(), record.episodeNumber
            ?: parts.getOrNull(1)?.removePrefix("Episode "), record.quality ?: parts.getOrNull(2)?.takeIf { it != "Best" }, extension)
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
        if (mediaStore) contentResolver.update(uri, ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }, null, null)
    }

    private fun OutputDocument.delete() { runCatching { contentResolver.delete(uri, null, null) } }

    private fun publish(source: Uri, record: ExportRecord, extension: String, id: String, checkActive: () -> Unit): Pair<Uri, Long> {
        val parts = record.title.split(" · ")
        val location = downloadLocation(record.showTitle ?: parts.first(), record.episodeNumber
            ?: parts.getOrNull(1)?.removePrefix("Episode "), record.quality ?: parts.getOrNull(2)?.takeIf { it != "Best" }, extension)
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
            val bytes = contentResolver.openInputStream(source).use { input ->
                requireNotNull(input) { "Cannot open temporary media" }
                contentResolver.openOutputStream(uri, "w").use { output ->
                    requireNotNull(output) { "Cannot write to output folder" }
                    val total = contentResolver.openAssetFileDescriptor(source, "r")?.use { it.length } ?: -1L
                    val buffer = ByteArray(256 * 1024)
                    var copied = 0L
                    var lastUpdate = 0L
                    while (true) {
                        checkActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        copied += count
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (now - lastUpdate > 250) {
                            Exports.setProgress(id, ExportProgress("Copying to folder", if (total > 0) copied * 100f / total else -1f))
                            lastUpdate = now
                        }
                    }
                    copied
                }
            }
            checkActive()
            if (tree == null) contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return uri to bytes
        } catch (e: Exception) { runCatching { contentResolver.delete(uri, null, null) }; throw e }
    }
    companion object {
        const val ACTION_CANCEL = "com.example.animepahedl.CANCEL_EXPORT"
        const val EXTRA_ID = "export_id"
    }
    override fun onTimeout(startId: Int, fgsType: Int) { activeExport?.cancel(); running?.cancel(); stopSelf() }
    override fun onDestroy() { activeExport?.cancel(); scope.cancel(); if (wakeLock.isHeld) wakeLock.release(); super.onDestroy() }
}
