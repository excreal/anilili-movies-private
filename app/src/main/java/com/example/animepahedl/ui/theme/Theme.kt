package com.example.animepahedl.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Anilili's default AMOLED monochrome theme: black canvas, neutral elevated planes.
private val DownloaderColors = darkColorScheme(
    primary = Color.White, onPrimary = Color(0xFF1B1B1B),
    primaryContainer = Color(0xFFD4D4D4), onPrimaryContainer = Color.Black,
    secondary = Color(0xFFC6C6C6), onSecondary = Color(0xFF303030),
    secondaryContainer = Color(0xFF474747), onSecondaryContainer = Color(0xFFE2E2E2),
    tertiary = Color(0xFFE2E2E2), onTertiary = Color(0xFF303030),
    tertiaryContainer = Color(0xFF474747), onTertiaryContainer = Color.White,
    background = Color.Black, surface = Color.Black,
    surfaceVariant = Color(0xFF1B1B1B),
    onBackground = Color(0xFFE2E2E2), onSurface = Color(0xFFE2E2E2),
    onSurfaceVariant = Color(0xFFC6C6C6), surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0E0E0E), surfaceContainer = Color(0xFF131313),
    surfaceContainerHigh = Color(0xFF1B1B1B), surfaceContainerHighest = Color(0xFF242424),
    outline = Color(0xFF393939), outlineVariant = Color(0xFF262626), surfaceTint = Color.White
)

@Composable
fun AnimepaheDLTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DownloaderColors, typography = Typography, content = content)
}
