package com.example.animepahedl.components

import com.example.animepahedl.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@Composable
internal fun Thumbnail(url: String, description: String, images: ImageLoader, modifier: Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 0.dp, imageRequest: ImageRequest? = null) {
    SubcomposeAsyncImage(model = imageRequest ?: url, imageLoader = images, contentDescription = description,
        contentScale = ContentScale.Crop, modifier = modifier.clip(RoundedCornerShape(cornerRadius)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp)) } },
        error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppSymbol(AppIcon.Browse, modifier = Modifier.size(20.dp)) } })
}
