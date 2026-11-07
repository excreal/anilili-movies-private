package com.example.animepahedl.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared surfaces preserve Material touch targets and animate changes in content size. */
@Composable
internal fun AppCard(modifier: Modifier = Modifier, colors: CardColors = CardDefaults.cardColors(), content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.animateContentSize(), shape = MaterialTheme.shapes.large, colors = colors, content = content)
}

@Composable
internal fun AppCard(onClick: () -> Unit, modifier: Modifier = Modifier, colors: CardColors = CardDefaults.cardColors(), content: @Composable ColumnScope.() -> Unit) {
    Card(onClick = onClick, modifier = modifier.animateContentSize(), shape = MaterialTheme.shapes.large, colors = colors, content = content)
}

@Composable
internal fun SectionHeading(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}
