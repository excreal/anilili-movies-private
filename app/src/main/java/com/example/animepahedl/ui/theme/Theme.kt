package com.example.animepahedl.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.anilili.data.settings.SettingsStore
import com.anilili.ui.theme.LocalAppColors
import com.anilili.ui.theme.LocalMidnightAccent
import com.anilili.ui.theme.ThemeSpec
import com.anilili.ui.theme.appColors
import com.anilili.ui.theme.wallpaperSeed

@Composable
fun AnimepaheDLTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val background by SettingsStore.themeBackground.collectAsState()
    val accent by SettingsStore.themeAccent.collectAsState()
    val style by SettingsStore.themeStyle.collectAsState()
    val contrast by SettingsStore.themeContrast.collectAsState()
    val playerAccent by SettingsStore.playerAccent.collectAsState()
    val customSeed by SettingsStore.themeCustomSeed.collectAsState()
    val wallpaper = remember(context) { wallpaperSeed(context) }
    val colors = remember(background, accent, style, contrast, playerAccent, customSeed, wallpaper) {
        appColors(ThemeSpec(background, accent, style, contrast, playerAccent, customSeed), wallpaper)
    }
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalMidnightAccent provides colors.scheme.primary,
    ) {
        MaterialTheme(colorScheme = colors.scheme, typography = Typography, content = content)
    }
}
