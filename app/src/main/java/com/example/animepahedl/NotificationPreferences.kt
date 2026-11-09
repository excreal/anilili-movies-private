package com.example.animepahedl

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun PlaybackPreference(key: String, title: String, description: String) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("app-settings", android.content.Context.MODE_PRIVATE) }
    var value by remember { mutableStateOf(preferences.getBoolean(key, true)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        val native = when (key) {
            "play-on-open" -> com.anilili.data.settings.SettingsStore.playOnOpen.collectAsState().value
            "auto-next" -> com.anilili.data.settings.SettingsStore.autoplay.collectAsState().value
            else -> null
        }
        Switch(native ?: value, {
            value = it; preferences.edit().putBoolean(key, it).apply()
            if (key == "play-on-open") com.anilili.data.settings.SettingsStore.setPlayOnOpen(it)
            if (key == "auto-next") com.anilili.data.settings.SettingsStore.setAutoplay(it)
        })
    }
}

@Composable
internal fun NotificationPreferences() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("app-settings", android.content.Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(preferences.getBoolean("announcements", true)) }
    val scope = rememberCoroutineScope()
    val checking by AnnouncementManager.checking.collectAsState()
    val status by AnnouncementManager.checkStatus.collectAsState()
    fun checkNow() { scope.launch { AnnouncementManager.check(context, force = true) } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) checkNow() else AnnouncementManager.checkStatus.value = "Notifications are blocked. Enable them in Notification settings."
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Announcements")
                Text("Rare notices from the developer, like an outage or a moved download", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(enabled, {
                enabled = it; preferences.edit().putBoolean("announcements", it).apply()
                if (it) { permission.launch(Manifest.permission.POST_NOTIFICATIONS); AnnouncementScheduler.checkNow(context) }
            })
        }
        TextButton(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Allow notifications") }
        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("Notification settings") }
        TextButton(onClick = { checkNow() }, enabled = !checking) { Text(if (checking) "Checking…" else "Check for announcements") }
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
