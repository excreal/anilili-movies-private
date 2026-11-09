package com.example.animepahedl.components
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*
val backdropBlurAvailable = android.os.Build.VERSION.SDK_INT >= 31
@OptIn(ExperimentalHazeApi::class)
fun Modifier.backdropBlur(state: HazeState?, tint: Color, radius: Dp = 16.dp): Modifier =
    if (state == null || !backdropBlurAvailable) {
        this
    } else {
        hazeEffect(
            state,
            HazeStyle(
                backgroundColor = Color.Black,
                tint = HazeTint(tint),
                blurRadius = radius,
                noiseFactor = 0f,
            ),
        ) {
            inputScale = HazeInputScale.Fixed(0.33f)
            canDrawArea = { true }
        }
    }
