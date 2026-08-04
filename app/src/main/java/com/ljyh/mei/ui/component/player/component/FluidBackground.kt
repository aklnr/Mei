

package com.ljyh.mei.ui.component.player.component

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
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
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
    val context = LocalContext.current

    var vibrantColor by remember { mutableStateOf(Color(0xFF4A90E2)) }
    var darkMutedColor by remember { mutableStateOf(Color(0xFF2C3E50)) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrEmpty()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(256)
                    .build()
                
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.image.toBitmap()
                    Palette.from(bitmap).generate().let { palette ->
                        val dominant = palette.getDominantColor(0xFF3F51B5.toInt())
                        val vibrant = palette.getVibrantColor(dominant)
                        val muted = palette.getMutedColor(dominant)

                        vibrantColor = Color(vibrant)
                        darkMutedColor = Color(muted)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val animatedPrimary by animateColorAsState(
        targetValue = vibrantColor,
        animationSpec = tween(durationMillis = 800),
        label = "QPlayerPrimaryColor"
    )

    val animatedSecondary by animateColorAsState(
        targetValue = darkMutedColor,
        animationSpec = tween(durationMillis = 800),
        label = "QPlayerSecondaryColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = 0.95f),
                        animatedSecondary.copy(alpha = 0.85f),
                        Color(0xFF0A0A0A)
                    ),
                    radius = 2200f
                )
            )
    ) {
        content()
    }
}
