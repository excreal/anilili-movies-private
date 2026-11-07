package com.example.animepahedl

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun PreparationStatusRow(status: PreparationStatus, remaining: Int, onPause: () -> Unit,
    onResume: () -> Unit, onDiscard: () -> Unit, onVerify: (() -> Unit)? = null, onChooseFolder: (() -> Unit)? = null) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (status.automatic) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        else AppSymbol(AppIcon.Download, Modifier.size(18.dp))
        Text("${status.label} · $remaining left", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (status.needsFolderAccess && onChooseFolder != null) {
            IconButton(onClick = onChooseFolder) { AppSymbol(AppIcon.Folder, description = "Choose existing downloads folder") }
        } else if (status.verificationUrl != null && onVerify != null) {
            IconButton(onClick = onVerify) { AppSymbol(AppIcon.Shield, description = "Verify session") }
        } else IconButton(onClick = if (status.automatic) onPause else onResume) {
            AppSymbol(if (status.automatic) AppIcon.Pause else AppIcon.Play,
                description = if (status.automatic) "Pause link preparation" else "Resume link preparation")
        }
        Box {
            IconButton(onClick = { menu = true }) { AppSymbol(AppIcon.More, description = "Preparation actions") }
            DropdownMenu(menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Discard remaining links") }, onClick = { menu = false; onDiscard() })
            }
        }
    }
}
