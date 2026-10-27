package com.example.animepahedl

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

internal enum class AppIcon(val path: String) {
    Browse("M9.5,3a6.5,6.5 0,1 0,4.1 11.55L20.5,21l1.5,-1.5 -6.45,-6.9A6.5,6.5 0,0 0,9.5 3M9.5,5a4.5,4.5 0,1 1,0 9,4.5 4.5,0 0,1 0,-9"),
    Download("M11,3h2v10l3.5,-3.5L18,11l-6,6 -6,-6 1.5,-1.5L11,13zM5,18v3h14v-3h2v5H3v-5z"),
    Settings("M12,8a4,4 0,1 0,0 8,4 4,0 0,0 0,-8M10,2h4l.6,3 2,.9 2.6,-1.5 2,3.4 -2.2,2v2.4l2.2,2 -2,3.4 -2.6,-1.5 -2,.9 -.6,3h-4l-.6,-3 -2,-.9 -2.6,1.5 -2,-3.4 2.2,-2V9.8l-2.2,-2 2,-3.4 2.6,1.5 2,-.9z"),
    Back("M20,11H7.8l5.6,-5.6L12,4l-8,8 8,8 1.4,-1.4L7.8,13H20z"),
    Shield("M12,2 3,6v6c0,5 4,9 9,10 5,-1 9,-5 9,-10V6zM11,16l-4,-4 1.4,-1.4 2.6,2.6 5.6,-5.6L18,9z"),
    Folder("M3,4h7l2,2h9v14H3zM5,8v10h14V8z"),
    Play("M8,5v14l11,-7z"),
    More("M10,3h4v4h-4zM10,10h4v4h-4zM10,17h4v4h-4z"),
    Delete("M6,7h12l-1,15H7zM9,2h6l1,2h4v2H4V4h4z"),
    Refresh("M18,7a8,8 0,1 0,2 9h-2.3a6,6 0,1 1,-1.1 -7.5L13,12h9V3z"),
    Close("M6,4 12,10 18,4 20,6 14,12 20,18 18,20 12,14 6,20 4,18 10,12 4,6z"),
    Check("M9,16.2 4.8,12 3.4,13.4 9,19 21,7 19.6,5.6z")
}

private val vectors = AppIcon.entries.associateWith {
    ImageVector.Builder(it.name, 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes(it.path), fill = SolidColor(Color.Black)).build()
}

@Composable
internal fun AppSymbol(icon: AppIcon, description: String? = null, modifier: Modifier = Modifier) {
    Icon(vectors.getValue(icon), description, modifier)
}
