package com.example.animepahedl.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.animepahedl.AppIcon
import kotlin.math.roundToInt

private val navigationPaths = listOf(
    "M12,3 2,12h3v9h6v-6h2v6h6v-9h3z",
    AppIcon.Browse.path,
    AppIcon.Download.path,
    "M4,6H2v14a2,2 0,0 0,2 2h16v-2H4V6M20,2H8a2,2 0,0 0,-2 2v12a2,2 0,0 0,2 2h12a2,2 0,0 0,2 -2V4a2,2 0,0 0,-2 -2M12,6l6,4 -6,4V6z",
    AppIcon.Settings.path,
)
private val navigationLabels = listOf("Home", "Search", "Downloads", "Library", "Settings")
private val navigationIcons = navigationPaths.mapIndexed { index, path ->
    ImageVector.Builder(navigationLabels[index], 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes(path), fill = SolidColor(Color.Black)).build()
}

/** The original reference renderer; only destinations differ from the anime app. */
@Composable
internal fun DownloaderNavigation(selected: Int, backdrop: dev.chrisbanes.haze.HazeState? = null, onSelect: (Int) -> Unit) {
    val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    FloatingNavigationBar(
        items = navigationLabels.mapIndexed { index, label -> FloatingNavItem(index.toString(), label, navigationIcons[index]) },
        selectedRoute = selected.toString(), onSelect = { onSelect(it.toInt()) }, bottomInset = inset, backdrop = backdrop)
}
