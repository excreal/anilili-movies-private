package com.example.animepahedl

import android.app.DownloadManager
import android.content.Context
import androidx.media3.exoplayer.offline.DownloadService
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal suspend fun clearPendingVideo(context: Context, id: String) {
    val before = Exports.all(context)[id] ?: return
    if (before.uri.isNotEmpty()) return
    Exports.cancel(context, id)
    Exports.withItemLock(id) {
        val record = Exports.all(context)[id] ?: return@withItemLock
        if (record.uri.isEmpty() && !record.pendingUri.isNullOrBlank()) {
            withContext(Dispatchers.IO) { deleteOwnedMedia(context, android.net.Uri.parse(record.pendingUri)) }
            Exports.put(context, id, record.copy(pendingUri = null))
            DownloadStore.flush()
        }
    }
}

/** Reuse a readable completed video without deleting it or starting a new network transfer. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal suspend fun adoptExistingVideo(context: Context, identity: DownloadIdentity, video: ExistingVideo, tree: String): String {
    val matches = Exports.all(context).filter { (id, record) -> record.identity?.key == identity.key || id == identity.key }
    val id = matches.keys.firstOrNull() ?: identity.key
    if (Exports.deleting(id) || matches.keys.any(Exports::deleting)) return id
    var adopted = false
    for ((key, record) in matches) if (record.uri.isEmpty()) Exports.cancel(context, key)
    Exports.withItemLock(id) {
        currentCoroutineContext().ensureActive()
        if (Exports.deleting(id) || matches.keys.any(Exports::deleting)) return@withItemLock
        val old = Exports.all(context)[id]
        val title = identity.anime.title + (if (identity.isSingleVideo) "" else " · Episode ${identity.episode.number}") + " · ${identity.quality} · ${audioTag(identity.audio)}"
        Exports.put(context, id, ExportRecord(title, tree, AppSettings(context).outputFolder,
            uri = video.uri.toString(), bytes = video.bytes, thumbnail = identity.anime.poster,
            audio = identity.audio, quality = identity.quality, showTitle = identity.anime.title,
            episodeNumber = identity.episode.number, identity = identity, pendingUri = old?.pendingUri, media = old?.media))
        DownloadStore.flush()
        adopted = true
    }
    if (!adopted) return id
    for ((key, record) in matches) if (record.uri.isEmpty()) {
        suspend fun removeTransfer() {
            if (key.startsWith("direct:")) withContext(Dispatchers.IO) {
                val number = key.removePrefix("direct:")
                context.getSystemService(DownloadManager::class.java).remove(number.toLong())
                DownloadStore.operation(number, "direct", null)
            } else withContext(Dispatchers.Main.immediate) { OfflineDownloads.manager(context).removeDownload(key) }
            if (key != id) Exports.remove(context, key)
        }
        if (key == id) removeTransfer() else Exports.withItemLock(key) { removeTransfer() }
    }
    DownloadStore.flush()
    return id
}

/** Existing queues are paused until old-install files can be checked through the user's folder grant. */
private val existingRecoveryLock = Mutex()
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal suspend fun reconcileExistingVideos(context: Context) = existingRecoveryLock.withLock {
    val settings = AppSettings(context)
    if (!hasOutputFolderAccess(context, settings.outputTree)) throw OutputFolderAccessRequired()
    DownloadStore.awaitReady()
    val (manager, resumeAfter) = withContext(Dispatchers.Main.immediate) {
        val manager = OfflineDownloads.manager(context)
        val resumeAfter = !manager.downloadsPaused || DownloadStore.operations("folder-access")["transfers"] != null
        manager.pauseDownloads()
        manager to resumeAfter
    }
    DownloadStore.operation("transfers", "folder-access", "1")
    DownloadStore.flush()
    val indices = mutableMapOf<String, ExistingOutputIndex>()
    val pending = Exports.all(context).filterValues { it.uri.isEmpty() && it.identity != null }
    for ((id, original) in pending) {
        currentCoroutineContext().ensureActive()
        if (Exports.deleting(id)) continue
        clearPendingVideo(context, id)
        val identity = original.identity ?: continue
        val tree = original.tree.takeIf { hasOutputFolderAccess(context, it) } ?: settings.outputTree
        val extension = if (original.media?.mimeType == "video/webm" || original.media?.url?.substringBefore('?')?.endsWith(".webm", true) == true) "webm" else "mp4"
        val found = withContext(Dispatchers.IO) { indices.getOrPut(tree) { ExistingOutputIndex(context, tree) }.find(original.outputLocation(extension)) }
        if (found != null) adoptExistingVideo(context, identity, found, tree)
        else Exports.withItemLock(id) {
            val current = Exports.all(context)[id]
            if (current != null && current.uri.isEmpty()) Exports.put(context, id, current.copy(tree = tree,
                error = current.error.takeUnless { it.contains("Choose the downloads folder") }.orEmpty()))
        }
    }
    DownloadStore.flush()
    DownloadStore.operation("transfers", "folder-access", null)
    DownloadStore.flush()
    if (resumeAfter) withContext(Dispatchers.Main.immediate) { manager.resumeDownloads() }
    if (Exports.all(context).any { (_, record) -> record.uri.isEmpty() && record.error.isEmpty() }) Exports.start(context)
    PreparationJobs.folderAccessRestored(context)
}
