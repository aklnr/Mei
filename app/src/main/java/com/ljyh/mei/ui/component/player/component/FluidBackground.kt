package com.ljyh.mei.ui.component.player.component

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.ljyh.mei.utils.audio.AudioVisualizerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FluidBackground(
    imageUrl: String?,
    audioVisualizerManager: AudioVisualizerManager? = null,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    var primaryColor by remember { mutableStateOf(Color(0xFF2C3E50)) }
    var secondaryColor by remember { mutableStateOf(Color(0xFF1A1A1A)) }

    val painter = rememberAsyncImagePainter(model = imageUrl)
    val painterState = painter.state

    LaunchedEffect(painterState) {
        if (painterState is AsyncImagePainter.State.Success) {
            withContext(Dispatchers.IO) {
                try {
                    val drawable = painterState.result.drawable
                    val bitmap: Bitmap = drawable.toBitmap(128, 128, Bitmap.Config.ARGB_8888)
                    Palette.from(bitmap).generate().let { palette ->
                        val dominant = palette.getDominantColor(0xFF3F51B5.toInt())
                        val vibrant = palette.getVibrantColor(dominant)
                        val darkMuted = palette.getDarkMutedColor(0xFF1E1E1E.toInt())

                        primaryColor = Color(vibrant)
                        secondaryColor = Color(darkMuted)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val animatedPrimary by animateColorAsState(
        targetValue = primaryColor,
        animationSpec = tween(durationMillis = 800),
        label = "BgPrimary"
    )

    val animatedSecondary by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(durationMillis = 800),
        label = "BgSecondary"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = 0.85f),
                        animatedSecondary.copy(alpha = 0.95f),
                        Color(0xFF050505)
                    ),
                    radius = 2200f
                )
            )
    ) {
        content()
    }
}
