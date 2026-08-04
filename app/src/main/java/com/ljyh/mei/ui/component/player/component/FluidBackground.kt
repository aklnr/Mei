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

    // 🌟 给一个更有生机和亮度的默认渐变色，绝不显示单调的灰色
    var vibrantColor by remember { mutableStateOf(Color(0xFF4A90E2)) }
    var darkMutedColor by remember { mutableStateOf(Color(0xFF2C3E50)) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrEmpty()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // 必须关闭硬件加速才能成功转成 Bitmap 给 Palette
                    .size(256)
                    .build()
                
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.image.toBitmap()
                    Palette.from(bitmap).generate().let { palette ->
                        // 智能降级取色，确保无论图片是什么色调都能提取到艳丽的颜色
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

    val crossFadeDuration = 800

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
                // 🌟 大幅提高透明度和辐射半径，让色彩像 QPlayer 一样大面积暈染开，绝对告别灰色
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = 0.95f),   // 核心主色高强度晕染
                        animatedSecondary.copy(alpha = 0.85f), // 辅助色过渡
                        Color(0xFF0A0A0A)                    // 边缘深色收尾
                    ),
                    radius = 2200f
                )
            )
    ) {
        content()
    }
}

