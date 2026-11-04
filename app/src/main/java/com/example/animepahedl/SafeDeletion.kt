package com.example.animepahedl

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.IOException

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

internal data class ItemResult(val id: String, val succeeded: Boolean, val message: String? = null)
