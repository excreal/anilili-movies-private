package com.example.animepahedl

import android.content.Context
import android.content.ContentUris
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.os.Environment
import java.io.File
import java.io.IOException

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
internal fun findOutputSubtitleSidecars(context: Context, record: ExportRecord): List<MediaSubtitle> {
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
            context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(1).orEmpty()
                        val mime = cursor.getString(2).orEmpty()
                        if (mime != DocumentsContract.Document.MIME_TYPE_DIR && isPlayableOutputSubtitle(name, location.fileName)) {
                            found += name to DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                        }
                    }
                }
        }
    } else {
        val relativePath = listOf(Environment.DIRECTORY_DOWNLOADS, record.folder, location.showFolder)
            .filter(String::isNotBlank).joinToString("/").trimEnd('/') + "/"
        runCatching {
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
        }
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
    fun absent(): Boolean = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { !it.moveToFirst() } ?: false
    if (absent()) return
    val deleted = if (DocumentsContract.isDocumentUri(context, uri)) DocumentsContract.deleteDocument(resolver, uri)
        else resolver.delete(uri, null, null) > 0
    if (!deleted && !absent()) throw IOException("The storage provider refused deletion. Restore folder access and tap Delete again.")
}

/** Delete only subtitle sidecars for the media basename and an empty per-show directory. */
internal fun deleteOutputCompanions(context: Context, record: ExportRecord) {
    val location = record.outputLocation("mp4")
    val tree = record.tree.takeIf(String::isNotBlank)?.let(Uri::parse)
    if (tree != null) {
        runCatching {
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
            val rootName = context.contentResolver.query(root, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
            val folderIsRoot = rootName.equals(location.showFolder, ignoreCase = true)
            // A selected tree can itself be the show folder. Never delete the user's granted root.
            val folder = if (folderIsRoot) root else {
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
                context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)
                    ?.use { cursor ->
                        var found: Uri? = null
                        while (cursor.moveToNext()) if (cursor.getString(1).equals(location.showFolder, ignoreCase = true) &&
                            cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                            found = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)); break
                        }
                        found
                    } ?: return@runCatching
            }
            val folderId = DocumentsContract.getDocumentId(folder)
            val contents = DocumentsContract.buildChildDocumentsUriUsingTree(tree, folderId)
            context.contentResolver.query(contents, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
                val sidecars = mutableListOf<Uri>()
                var hasOtherFiles = false
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    if (isOutputSubtitleSidecar(cursor.getString(1), location.fileName)) sidecars += DocumentsContract.buildDocumentUriUsingTree(tree, id)
                    else hasOtherFiles = true
                }
                sidecars.forEach { runCatching { DocumentsContract.deleteDocument(context.contentResolver, it) } }
                if (!hasOtherFiles && !folderIsRoot) runCatching { DocumentsContract.deleteDocument(context.contentResolver, folder) }
            }
        }
    } else {
        val relativePath = listOf(Environment.DIRECTORY_DOWNLOADS, record.folder, location.showFolder)
            .filter(String::isNotBlank).joinToString("/").trimEnd('/') + "/"
        runCatching {
            val files = MediaStore.Files.getContentUri("external")
            val sidecars = mutableListOf<Uri>()
            context.contentResolver.query(files,
                arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME),
                "${MediaStore.MediaColumns.RELATIVE_PATH}=?", arrayOf(relativePath), null)?.use { cursor ->
                while (cursor.moveToNext()) if (isOutputSubtitleSidecar(cursor.getString(1), location.fileName)) {
                    sidecars += ContentUris.withAppendedId(files, cursor.getLong(0))
                }
            }
            sidecars.forEach { context.contentResolver.delete(it, null, null) }
        }
        runCatching {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).canonicalFile
            val folder = File(downloads, listOf(record.folder, location.showFolder).filter(String::isNotBlank).joinToString("/")).canonicalFile
            deleteEmptyDirectoriesUpTo(folder, downloads)
        }
    }
}

internal data class ItemResult(val id: String, val succeeded: Boolean, val message: String? = null)
