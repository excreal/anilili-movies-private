package com.example.animepahedl

import androidx.core.net.toUri
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.IOException

/** Find a published file at the configured destination without creating folders. */
internal fun existingOutput(context: Context, tree: String, folder: String, location: DownloadLocation): Boolean {
    val resolver = context.contentResolver
    if (tree.isBlank()) {
        val path = listOf(Environment.DIRECTORY_DOWNLOADS, folder, location.showFolder)
            .filter { it.isNotEmpty() }.joinToString("/", postfix = "/")
        val cursor = resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.IS_PENDING} = 0",
            arrayOf(path, location.fileName), null) ?: throw IOException("Could not check the download folder")
        return cursor.use { it.moveToFirst() }
    }
    val root = (tree).toUri()
    fun children(id: String) = DocumentsContract.buildChildDocumentsUriUsingTree(root, id)
    val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE)
    val rootCursor = resolver.query(children(DocumentsContract.getTreeDocumentId(root)), columns, null, null, null)
        ?: throw IOException("Could not check the selected folder")
    val showId = rootCursor.use { cursor ->
        var found: String? = null
        while (cursor.moveToNext()) if (cursor.getString(1) == location.showFolder &&
            cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) { found = cursor.getString(0); break }
        found
    } ?: return false
    val files = resolver.query(children(showId), columns, null, null, null)
        ?: throw IOException("Could not check the show folder")
    return files.use { cursor ->
        var found = false
        while (cursor.moveToNext()) if (cursor.getString(1) == location.fileName &&
            cursor.getString(2) != DocumentsContract.Document.MIME_TYPE_DIR) { found = true; break }
        found
    }
}
