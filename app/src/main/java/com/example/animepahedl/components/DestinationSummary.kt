package com.example.animepahedl.components

import com.example.animepahedl.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** A consistent shortened destination with access to the complete path. */
@Composable
internal fun DestinationSummary(path: String) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        AppSymbol(AppIcon.Folder)
        Spacer(Modifier.width(8.dp))
        Text(path.substringAfterLast('/').ifBlank { "Download folder" }, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (expanded) AlertDialog(onDismissRequest = { expanded = false }, title = { Text("Download storage") },
        text = { Text(path, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { expanded = false }) { Text("Close") } })
}
