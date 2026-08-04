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

    // 给默认色一个稍微带点高级感的深色，防止初始空白
    var vibrantColor by remember { mutableStateOf(Color(0xFF2C3E50)) }
    var darkMutedColor by remember { mutableStateOf(Color(0xFF1A1A1A)) }

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
                        // 优化的取色策略：依次尝试 Vibrant -> Dominant -> Muted，确保总能拿到有色彩的值
                        val dominant = palette.getDominantColor(0xFF3F51B5.toInt())
                        val vibrant = palette.getVibrantColor(dominant)
                        val darkMuted = palette.getDarkMutedColor(0xFF1E1E1E.toInt())

                        vibrantColor = Color(vibrant)
                        darkMutedColor = Color(darkMuted)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val crossFadeDuration = 1000

    val animatedPrimary by animateColorAsState(
        targetValue = vibrantColor,
        animationSpec = tween(durationMillis = crossFadeDuration),
        label = "QPlayerPrimaryColor"
    )

    val animatedSecondary by animateColorAsState(
        targetValue = darkMutedColor,
        animationSpec = tween(durationMillis = crossFadeDuration),
        label = "QPlayerSecondaryColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                // 优化渐变权重：提高主色的透明度和光晕半径，让色彩饱满地透出来，告别灰蒙蒙
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = 0.85f),   // 提高主色浓度
                        animatedSecondary.copy(alpha = 0.9f),  // 辅助色支撑层次
                        Color(0xFF0F0F0F)                    // 边缘暗色收尾
                    ),
                    radius = 1800f
                )
            )
    ) {
        content()
    }
}

