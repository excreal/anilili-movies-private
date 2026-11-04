package com.example.animepahedl

import androidx.core.net.toUri
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.google.gson.Gson

/** Adopt only destinations named by our durable pre-enqueue journal. */
internal suspend fun reconcileDirectEnqueues(context: Context) {
    val journal = DownloadStore.operations("direct-enqueue")
    if (journal.isEmpty()) return
    val manager = context.getSystemService(DownloadManager::class.java)
    manager.query(DownloadManager.Query()).use { cursor ->
        while (cursor.moveToNext()) {
            val local = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)) ?: continue
            val uri = (local).toUri()
            val filename = uri.lastPathSegment ?: continue
            val payload = journal[filename] ?: continue
            val expected = java.io.File(context.getExternalFilesDir("direct-temp"), filename).canonicalFile
            if (uri.scheme != "file" || java.io.File(uri.path ?: continue).canonicalFile != expected) continue
            val number = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
            val id = "direct:$number"
            if (!Exports.all(context).containsKey(id)) {
                val record = Gson().fromJson(payload, ExportRecord::class.java)
                Exports.put(context, id, record)
                DownloadStore.operation(number.toString(), "direct", record.title)
                DownloadStore.flush()
            }
            DownloadStore.operation(filename, "direct-enqueue", null)
        }
    }
    DownloadStore.flush()
}
