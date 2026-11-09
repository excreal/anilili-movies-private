package com.example.animepahedl.components

import androidx.compose.ui.draw.drawBehind
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// Exact role mapping used by Anilili's Midnight and FloatingNavigationBar.
internal object Midnight {
    val Bg: Color @Composable get() = MaterialTheme.colorScheme.background
    val Bg2: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerLowest
    val Panel2: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainer
    val Line: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val Line2: Color @Composable get() = MaterialTheme.colorScheme.outline
    val Text2: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val OnAccent: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
}
internal val LocalMidnightAccent = compositionLocalOf { Color.White }

@Composable
internal fun androidx.compose.ui.Modifier.appCanvasFade(alpha: () -> Float, brush: (Color) -> androidx.compose.ui.graphics.Brush): androidx.compose.ui.Modifier {
    val canvas = MaterialTheme.colorScheme.background
    return this.then(androidx.compose.ui.Modifier.drawCanvasFade(brush(canvas), alpha))
}
private fun androidx.compose.ui.Modifier.drawCanvasFade(brush: androidx.compose.ui.graphics.Brush, alpha: () -> Float): androidx.compose.ui.Modifier =
    drawBehind { drawRect(brush, alpha = alpha()) }
