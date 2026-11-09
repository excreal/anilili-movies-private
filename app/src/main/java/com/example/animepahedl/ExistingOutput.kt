package com.example.animepahedl

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import java.io.IOException
import java.text.Normalizer
import java.util.Locale

internal class OutputFolderAccessRequired : IOException("Choose the downloads folder so existing videos can be checked.")
internal data class ExistingVideo(val uri: Uri, val bytes: Long)

/** Avoid adopting empty/tiny/truncated files when a provider output happens to reuse the name. */
internal fun isPlausibleExistingVideo(bytes: Long, header: ByteArray): Boolean {
    if (bytes < 1024L || header.isEmpty()) return false
    val sample = header.take(16).toByteArray()
    val mp4 = sample.size >= 8 && sample[4] == 'f'.code.toByte() && sample[5] == 't'.code.toByte() &&
        sample[6] == 'y'.code.toByte() && sample[7] == 'p'.code.toByte()
    val webm = sample.size >= 4 && sample[0] == 0x1a.toByte() && sample[1] == 0x45.toByte() &&
        sample[2] == 0xdf.toByte() && sample[3] == 0xa3.toByte()
    // Some valid MP4s use a large `free` box before ftyp; accept recognizable ISO BMFF boxes.
    val bmff = sample.size >= 8 && (sample[4] == 'm'.code.toByte() && sample[5] == 'o'.code.toByte() &&
        sample[6] == 'o'.code.toByte() && sample[7] == 'v'.code.toByte() || mp4)
    return bmff || webm
}

internal fun hasOutputFolderAccess(context: Context, tree: String = AppSettings(context).outputTree): Boolean =
    tree.isNotBlank() && context.contentResolver.persistedUriPermissions.any {
        it.uri.toString() == tree && it.isReadPermission && it.isWritePermission
    }

internal fun existingVideoName(name: String): String = Normalizer.normalize(name, Normalizer.Form.NFC).lowercase(Locale.ROOT)

/** Compare the complete episode/quality/language name; allow only a trailing Android copy number. */
internal fun sameVideoFile(actual: String, expected: String): Boolean {
    val name = existingVideoName(actual)
    val target = existingVideoName(expected)
    if (name.substringAfterLast('.', "") !in setOf("mp4", "m4v", "webm")) return false
    val base = name.substringBeforeLast('.')
    val expectedBase = target.substringBeforeLast('.')
    return base == expectedBase || base.replace(Regex(""" \(\d+\)$"""), "") == expectedBase
}

/** One folder listing per show and preparation pass, rather than rescanning for every episode. */
internal class ExistingOutputIndex(private val context: Context, private val tree: String) {
    private data class Document(val id: String, val name: String, val mime: String, val bytes: Long)
    private val resolver = context.contentResolver
    private val root = tree.toUri()
    private var shows: List<Document>? = null
    private val files = mutableMapOf<String, List<Document>>()
    private fun children(id: String): List<Document> {
        if (!hasOutputFolderAccess(context, tree)) throw OutputFolderAccessRequired()
        try {
            return resolver.query(DocumentsContract.buildChildDocumentsUriUsingTree(root, id), arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE), null, null, null)?.use { cursor ->
                buildList<Document> {
                    while (cursor.moveToNext()) add(Document(cursor.getString(0), cursor.getString(1).orEmpty(),
                        cursor.getString(2).orEmpty(), if (cursor.isNull(3)) -1 else cursor.getLong(3)))
                }
            } ?: throw IOException("The selected folder could not be listed.")
        } catch (_: SecurityException) { throw OutputFolderAccessRequired() }
    }
    fun find(location: DownloadLocation): ExistingVideo? {
        if (!hasOutputFolderAccess(context, tree)) throw OutputFolderAccessRequired()
        val directories = shows ?: children(DocumentsContract.getTreeDocumentId(root)).also { shows = it }
        val show = directories.firstOrNull { existingVideoName(it.name) == existingVideoName(location.showFolder) && it.mime == DocumentsContract.Document.MIME_TYPE_DIR }
        // Also support linking a single show folder (Downloads itself cannot always be selected).
        val entries = (show?.let { files.getOrPut(it.id) { children(it.id) } }.orEmpty() +
            directories.filter { it.mime != DocumentsContract.Document.MIME_TYPE_DIR })
        val matches = entries.filter {
            it.mime != DocumentsContract.Document.MIME_TYPE_DIR && it.bytes != 0L && sameVideoFile(it.name, location.fileName)
        }.sortedBy { if (it.name == location.fileName) 0 else 1 }
        for (match in matches) {
            val uri = DocumentsContract.buildDocumentUriUsingTree(root, match.id)
            try {
                resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    val observedSize = descriptor.statSize
                    val size = if (observedSize >= 0) observedSize else match.bytes
                    if (size >= 1024L) {
                        resolver.openInputStream(uri)?.use { stream ->
                            val header = stream.readNBytes(16)
                            if (isPlausibleExistingVideo(size, header)) return ExistingVideo(uri, size)
                        }
                    }
                }
            } catch (_: java.io.FileNotFoundException) { /* Deleted since the listing; do not adopt it. */
            } catch (_: SecurityException) { throw OutputFolderAccessRequired() }
        }
        return null
    }
}

/** A missing MediaStore row cannot prove absence after reinstall, so only a granted tree is authoritative. */
internal fun findExistingOutput(context: Context, tree: String, location: DownloadLocation): ExistingVideo? =
    ExistingOutputIndex(context, tree).find(location)

internal fun existingOutput(context: Context, tree: String, folder: String, location: DownloadLocation): Boolean =
    findExistingOutput(context, tree, location) != null
