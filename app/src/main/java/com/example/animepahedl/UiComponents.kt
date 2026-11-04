package com.example.animepahedl

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/** A consistent shortened destination with access to the complete path. */
@Composable
internal fun DestinationSummary(path: String) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(path, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        TextButton(onClick = { expanded = true }) { Text("View folder") }
    }
    if (expanded) AlertDialog(onDismissRequest = { expanded = false }, title = { Text("Download storage") },
        text = { Text(path, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { expanded = false }) { Text("Close") } })
}
