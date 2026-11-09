package com.example.animepahedl

import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@Composable
internal fun AnnouncementDialog() {
    val message by AnnouncementManager.pending.collectAsState()
    val notice = message ?: return
    val context = LocalContext.current
    AlertDialog(onDismissRequest = AnnouncementManager::dismissPending,
        title = { Text(notice.title) }, text = { Text(notice.message + notice.url?.let { "\n\n$it" }.orEmpty()) },
        confirmButton = { TextButton(onClick = AnnouncementManager::dismissPending) { Text("Close") } },
        dismissButton = { notice.url?.let { url -> TextButton(onClick = {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
        }) { Text(notice.urlLabel ?: "Read more") } } })
}
