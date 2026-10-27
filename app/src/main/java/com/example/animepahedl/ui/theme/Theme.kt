package com.example.animepahedl.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C7FA), onPrimary = Color(0xFF16335A),
    primaryContainer = Color(0xFF2A3B55), onPrimaryContainer = Color(0xFFD4E3FF),
    secondary = Color(0xFFBAC8DF), onSecondary = Color(0xFF263449),
    secondaryContainer = Color(0xFF3B465B), onSecondaryContainer = Color(0xFFDCE7FF),
    tertiary = Color(0xFFB8DCCB), tertiaryContainer = Color(0xFF2D493D), onTertiaryContainer = Color(0xFFD2F2E1),
    background = Color(0xFF22252B), onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF22252B), onSurface = Color(0xFFE8EAED), onSurfaceVariant = Color(0xFFBFC3CB),
    surfaceContainerLowest = Color(0xFF1C1F25), surfaceContainerLow = Color(0xFF272A30),
    surfaceContainer = Color(0xFF2B2F35), surfaceContainerHigh = Color(0xFF33373F), surfaceContainerHighest = Color(0xFF3D424C),
    outline = Color(0xFF858B96), outlineVariant = Color(0xFF42464E)
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF315E9B), onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E5FF), onPrimaryContainer = Color(0xFF183452),
    secondary = Color(0xFF52647F), secondaryContainer = Color(0xFFDFE7F6), onSecondaryContainer = Color(0xFF23344F),
    tertiary = Color(0xFF3F6B56), tertiaryContainer = Color(0xFFD0EFDE), onTertiaryContainer = Color(0xFF1B3B2C),
    background = Color(0xFFF8F9FD), surface = Color(0xFFF8F9FD),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF1F3FA),
    surfaceContainer = Color(0xFFEBEEF6), surfaceContainerHigh = Color(0xFFE4E8F1), surfaceContainerHighest = Color(0xFFDDE2ED)
)
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp), small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(28.dp), large = RoundedCornerShape(32.dp), extraLarge = RoundedCornerShape(40.dp)
)

@Composable
fun AnimepaheDLTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography, shapes = ExpressiveShapes, content = content)
}
