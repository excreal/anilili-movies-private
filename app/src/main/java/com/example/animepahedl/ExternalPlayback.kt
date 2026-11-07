package com.example.animepahedl

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** ACTION_VIEW respects Android's default player and grants access only to this saved video. */
internal suspend fun openSavedVideo(context: Context, row: DownloadRow): String? {
    if (!row.complete || row.savedUri.isBlank()) return "This video has not finished saving."
    val uri = row.savedUri.toUri()
    if (uri.scheme != "content") return "Relink this video to open it in your default player."
    val mime = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.getType(uri) }.getOrNull()?.takeIf { it.startsWith("video/") } ?: "video/mp4"
    }
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newRawUri("Saved video", uri)
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try { context.startActivity(intent); null }
    catch (_: ActivityNotFoundException) { "No video player is installed. Install one and try again." }
    catch (_: SecurityException) { "The player could not read this video. Relink the file and try again." }
}
