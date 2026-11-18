package com.example.animepahedl

import android.content.Context
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.os.Environment
import java.io.File
import java.io.IOException
import java.io.FileNotFoundException

private val outputSubtitleExtensions = setOf("srt", "vtt", "ass", "ssa", "sub", "idx", "ttml", "smi")

internal fun isOutputSubtitleSidecar(candidate: String, mediaFileName: String): Boolean {
    val mediaStem = mediaFileName.substringBeforeLast('.', mediaFileName)
    val candidateExtension = candidate.substringAfterLast('.', "").lowercase()
    val candidateStem = candidate.substringBeforeLast('.', candidate)
    val sidecarTag = candidateStem.takeIf { it.startsWith("$mediaStem.", ignoreCase = true) }
        ?.substring(mediaStem.length + 1)
    val matchingStem = candidateStem.equals(mediaStem, ignoreCase = true) ||
        sidecarTag?.matches(Regex("(?i)(?:[a-z]{2,3}(?:[-_][a-z0-9]{2,8})*|track-\\d+)")) == true
    return candidateExtension in outputSubtitleExtensions && matchingStem
}

/** Find exported captions next to a saved video. The player uses these as a recovery path when
 * older download records do not contain app-private subtitle references. */
internal fun findOutputSubtitleSidecars(context: Context, record: ExportRecord, requireMatchingVideo: Boolean = false): List<MediaSubtitle> {
    val location = record.outputLocation("mp4")
    val tree = record.tree.takeIf { it.isNotBlank() && hasOutputFolderAccess(context, it) }?.let(Uri::parse)
    val found = mutableListOf<Pair<String, Uri>>()
    if (tree != null) {
        runCatching {
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
            val rootName = context.contentResolver.query(root, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
            val parentId = if (rootName.equals(location.showFolder, ignoreCase = true)) rootId else {
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
                context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)
                    ?.use { cursor ->
                        var id: String? = null
                        while (cursor.moveToNext()) if (cursor.getString(1).equals(location.showFolder, ignoreCase = true) &&
                            cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                            id = cursor.getString(0)
                            break
                        }
                        id
                    } ?: return@runCatching
            }
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
            var matchedVideo = false
            context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(1).orEmpty()
                        val mime = cursor.getString(2).orEmpty()
                        val uri = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                        if (uri.toString() == record.uri && sameVideoFile(name, location.fileName)) matchedVideo = true
                        if (mime != DocumentsContract.Document.MIME_TYPE_DIR && isPlayableOutputSubtitle(name, location.fileName)) {
                            found += name to uri
                        }
                    }
                }
            if (requireMatchingVideo && !matchedVideo) found.clear()
        }.onFailure { if (requireMatchingVideo) found.clear() }
    } else if (!requireMatchingVideo || record.tree.isBlank()) {
        val relativePath = listOf(Environment.DIRECTORY_DOWNLOADS, record.folder, location.showFolder)
            .filter(String::isNotBlank).joinToString("/").trimEnd('/') + "/"
        runCatching {
            if (requireMatchingVideo) {
                val uri = Uri.parse(record.uri)
                if (uri.authority != MediaStore.AUTHORITY) return@runCatching
                val matches = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    cursor.moveToFirst() && cursor.getString(0) == relativePath && sameVideoFile(cursor.getString(1).orEmpty(), location.fileName)
                } == true
                if (!matches) return@runCatching
            }
            val files = MediaStore.Files.getContentUri("external")
            context.contentResolver.query(files, arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME),
                "${MediaStore.MediaColumns.RELATIVE_PATH}=?", arrayOf(relativePath), null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1).orEmpty()
                    if (isPlayableOutputSubtitle(name, location.fileName)) {
                        found += name to ContentUris.withAppendedId(files, cursor.getLong(0))
                    }
                }
            }
        }.onFailure { if (requireMatchingVideo) found.clear() }
    }
    return found.mapNotNull { (name, uri) -> outputSubtitleTrack(name, location.fileName, uri) }
        .sortedWith(compareBy<MediaSubtitle> { it.language == "und" }.thenBy { it.language }.thenBy { it.label })
}

private fun isPlayableOutputSubtitle(name: String, mediaFileName: String): Boolean =
    isOutputSubtitleSidecar(name, mediaFileName) && name.substringAfterLast('.', "").lowercase() in setOf("vtt", "srt", "ass", "ssa", "ttml")

private fun outputSubtitleTrack(name: String, mediaFileName: String, uri: Uri): MediaSubtitle? {
    val extension = name.substringAfterLast('.', "").lowercase()
    val mediaStem = mediaFileName.substringBeforeLast('.')
    val candidateStem = name.substringBeforeLast('.')
    val tag = candidateStem.takeIf { it.startsWith("$mediaStem.", ignoreCase = true) }
        ?.substring(mediaStem.length + 1).orEmpty()
    val language = tag.takeUnless { it.startsWith("track-", true) }.orEmpty().replace('_', '-').ifBlank { "und" }
    val label = if (language == "und") "Subtitle ${tag.removePrefix("track-").ifBlank { "track" }}"
        else java.util.Locale.forLanguageTag(language).getDisplayLanguage(java.util.Locale.ENGLISH).ifBlank { language }
    val mime = when (extension) {
        "vtt" -> "text/vtt"
        "srt" -> "application/x-subrip"
        "ass", "ssa" -> "text/x-ssa"
        "ttml" -> "application/ttml+xml"
        else -> return null
    }
    return MediaSubtitle(uri.toString(), label, language, mime)
}

internal fun localSubtitleUriExists(context: Context, value: String): Boolean = runCatching {
    val uri = Uri.parse(value)
    when (uri.scheme?.lowercase()) {
        "file" -> File(uri.path.orEmpty()).let { it.isFile && it.length() in 1..4_000_000 }
        "content" -> context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
            it.length < 0 || it.length in 1..4_000_000
        } == true
        else -> false
    }
}.getOrDefault(false)

/** Remove empty download directories from [start] upward, stopping before [boundary]. */
internal fun deleteEmptyDirectoriesUpTo(start: File, boundary: File) {
    val root = boundary.canonicalFile
    var current = start.canonicalFile
    while (current != root && current.toPath().startsWith(root.toPath())) {
        if (!current.isDirectory || current.list()?.isNotEmpty() != false || !current.delete()) return
        current = current.parentFile ?: return
    }
}

/** Unknown provider state is never treated as absence (especially after a revoked grant). */
internal fun deleteOwnedMedia(context: Context, uri: Uri) {
    val resolver = context.contentResolver
    val document = DocumentsContract.isDocumentUri(context, uri)
    if (uri.scheme != "content" || (DocumentsContract.isTreeUri(uri) && !document)) throw IOException("The saved video URI is invalid.")
    fun absent(): Boolean {
        val listings = externalStorageAncestorListings(uri)
        val present = try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.extras.getBoolean(DocumentsContract.EXTRA_LOADING)) null else it.moveToFirst()
            }
        } catch (_: FileNotFoundException) { null }
        catch (e: IllegalArgumentException) { if (listings.isEmpty()) throw e else null }
        if (present != null) return !present
        for ((children, childId) in listings) {
            val missing = try {
                resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use {
                    confirmedChildMissing(it, childId)
                }
            } catch (_: FileNotFoundException) { null }
            catch (_: IllegalArgumentException) { null }
            if (missing != null) return missing
        }
        return false
    }
    if (absent()) return
    if (resolver.getType(uri) == DocumentsContract.Document.MIME_TYPE_DIR) throw IOException("Only video files can be removed from Downloads.")
    val deleted = try {
        if (document) DocumentsContract.deleteDocument(resolver, uri) else resolver.delete(uri, null, null) > 0
    } catch (_: FileNotFoundException) { false }
    if (!deleted && !absent()) throw IOException("The storage provider refused deletion. Restore folder access and tap Delete again.")
}

/** ExternalStorageProvider encodes the volume and relative path in the document ID. */
internal fun externalStorageAncestors(treeId: String, documentId: String): List<Pair<String, String>> = buildList {
    val root = treeId.trimEnd('/')
    val separator = root.indexOf(':')
    if (separator <= 0) return@buildList
    val prefix = if (separator == root.lastIndex) root else "$root/"
    if (!documentId.startsWith(prefix)) return@buildList
    val relative = documentId.removePrefix(prefix).split('/')
    if (relative.any { it.isBlank() || it == "." || it == ".." }) return@buildList
    for (depth in relative.size downTo 1) {
        val child = prefix + relative.take(depth).joinToString("/")
        val parent = if (depth == 1) root else prefix + relative.take(depth - 1).joinToString("/")
        add(parent to child)
    }
}

/** A null document cursor is inconclusive. A readable directory's child list can prove absence. */
internal fun externalStorageAncestorListings(uri: Uri): List<Pair<Uri, String>> = runCatching {
    if (uri.authority != "com.android.externalstorage.documents" || !DocumentsContract.isTreeUri(uri)) return emptyList()
    externalStorageAncestors(DocumentsContract.getTreeDocumentId(uri), DocumentsContract.getDocumentId(uri)).map { (parent, child) ->
        DocumentsContract.buildChildDocumentsUriUsingTree(uri, parent) to child
    }
}.getOrDefault(emptyList())

internal fun confirmedChildMissing(cursor: Cursor, childId: String): Boolean {
    if (cursor.extras.getBoolean(DocumentsContract.EXTRA_LOADING)) return false
    while (cursor.moveToNext()) if (cursor.getString(0) == childId) return false
    return true
}

internal data class ItemResult(val id: String, val succeeded: Boolean, val message: String? = null)
