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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FluidBackground(
    imageUrl: String?,
    // 移除原先不需要的音频视觉化管理器和律动参数，保持纯净
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val context = LocalContext.current

    // 1. 异步从图片 URL 中提取出主色调
    var vibrantColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }
    var darkMutedColor by remember { mutableStateOf(Color(0xFF0F0F0F)) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrEmpty()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(256)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.image.toBitmap()
                    Palette.from(bitmap).generate().let { palette ->
                        val vibrant = palette.getVibrantColor(palette.getDominantColor(0xFF2C2C2C.toInt()))
                        val dark = palette.getDarkMutedColor(0xFF121212.toInt())
                        vibrantColor = Color(vibrant)
                        darkMutedColor = Color(dark)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 2. 核心：切歌时的平滑交叉渐变（Cross-fade），持续时间 1000ms（完全对标 QPlayer 的平滑过渡）
    const val CROSS_FADE_DURATION = 1000

    val animatedPrimary by animateColorAsState(
        targetValue = vibrantColor,
        animationSpec = tween(durationMillis = CROSS_FADE_DURATION),
        label = "QPlayerPrimaryColor"
    )

    val animatedSecondary by animateColorAsState(
        targetValue = darkMutedColor,
        animationSpec = tween(durationMillis = CROSS_FADE_DURATION),
        label = "QPlayerSecondaryColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                // 3. 仿照 QPlayer 的多层光晕与沉浸式暗色底调收尾
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = 0.6f),
                        animatedSecondary.copy(alpha = 0.8f),
                        Color(0xFF0A0A0A)
                    ),
                    radius = 1600f
                )
            )
    ) {
        content()
    }
}


