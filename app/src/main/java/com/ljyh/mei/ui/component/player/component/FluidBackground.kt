package com.ljyh.mei.ui.component.player.component

import android.graphics.Bitmap
import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint as NativePaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ljyh.mei.utils.audio.AudioVisualizerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random

@Composable
fun FluidBackground(
    imageUrl: String?,
    audioVisualizerManager: AudioVisualizerManager? = null,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    var blurredBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val noiseBitmap = remember { createNoiseTexture(256, 256) }

    // 🌟 异步处理：下采样 -> HSL 调和 -> 生成高斯模糊 Bitmap
    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrEmpty()) {
            blurredBitmap = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                
                val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
                val originalBitmap = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap

                if (originalBitmap != null) {
                    // 1. 极小下采样 (64x64)，丢弃杂乱细节
                    val scaled = Bitmap.createScaledBitmap(originalBitmap, 64, 64, true)
                    // 2. HSL 调和，防止高饱和脏色
                    val harmonized = harmonizeBitmapHSL(scaled)
                    // 3. 放大并模糊
                    val target = Bitmap.createScaledBitmap(harmonized, 300, 300, true)
                    blurredBitmap = applyGaussianBlur(target, radius = 25f)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF121216))) {
        // 1. 渲染高斯模糊核心背景
        blurredBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // Android 12+ 硬件级二次极其剧烈的模糊
                            renderEffect = RenderEffect.createBlurEffect(
                                80f, 80f, Shader.TileMode.MIRROR
                            )
                        }
                    }
            )
        }

        // 2. 盖上一层微弱黑色暗罩（20% 透明），增强文字对比度
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.20f))
        )

        // 3. 盖上 Film Grain 噪点纹理，消除渐变断层，极大提升通透细腻感
        Image(
            bitmap = noiseBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Repeated,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.04f)
        )

        content()
    }
}

// HSL 调和，避免高饱和与脏色
private fun harmonizeBitmapHSL(source: Bitmap): Bitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    val hsl = FloatArray(3)
    for (i in pixels.indices) {
        ColorUtils.colorToHSL(pixels[i], hsl)
        // 限制饱和度在 30% - 60% 之间
        hsl[1] = hsl[1].coerceIn(0.30f, 0.60f)
        // 提升明度到 35% - 55%
        hsl[2] = hsl[2].coerceIn(0.35f, 0.55f)
        pixels[i] = ColorUtils.HSLToColor(hsl)
    }

    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    out.setPixels(pixels, 0, width, 0, 0, width, height)
    return out
}

// 快速软件高斯模糊补丁
private fun applyGaussianBlur(src: Bitmap, radius: Float): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = NativeCanvas(out)
    val paint = NativePaint().apply {
        flags = NativePaint.FILTER_BITMAP_FLAG
    }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return out
}

// 生成细小噪点纹理 (Film Grain)
private fun createNoiseTexture(width: Int, height: Int): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    val random = Random()
    for (i in pixels.indices) {
        val g = random.nextInt(255)
        pixels[i] = android.graphics.Color.argb(255, g, g, g)
    }
    bmp.setPixels(pixels, 0, width, 0, 0, width, height)
    return bmp
}
