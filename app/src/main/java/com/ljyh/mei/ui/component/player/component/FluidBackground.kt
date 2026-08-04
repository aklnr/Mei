
package com.ljyh.mei.ui.component.player.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.ljyh.mei.utils.audio.AudioVisualizerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun FluidBackground(
    imageUrl: String?,
    audioVisualizerManager: AudioVisualizerManager? = null,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    // 给默认色一个充满质感的深暖色，拒绝冷灰
    var primaryColor by remember { mutableStateOf(Color(0xFF8C4A27)) }
    var secondaryColor by remember { mutableStateOf(Color(0xFF1F120B)) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrEmpty()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                // 直接使用原生 BitmapFactory 强行解码，绕过 Coil 缓存/硬件加速限制
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                val inputStream = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)

                if (bitmap != null) {
                    Palette.from(bitmap).generate().let { palette ->
                        // 优化的降级颜色算法：Vibrant -> Dominant -> Muted -> 提取中心像素色
                        val vibrant = palette.getVibrantColor(0)
                        val dominant = palette.getDominantColor(0)
                        val muted = palette.getMutedColor(0)

                        val targetRgb = when {
                            vibrant != 0 -> vibrant
                            dominant != 0 -> dominant
                            muted != 0 -> muted
                            else -> bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
                        }

                        primaryColor = Color(targetRgb)
                        secondaryColor = Color(targetRgb).copy(alpha = 0.3f)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val animatedPrimary by animateColorAsState(
        targetValue = primaryColor,
        animationSpec = tween(durationMillis = 1200),
        label = "BgPrimary"
    )

    val animatedSecondary by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(durationMillis = 1200),
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
                        Color(0xFF08080A)
                    ),
                    radius = 2400f
                )
            )
    ) {
        content()
    }
}
