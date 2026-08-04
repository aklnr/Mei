
package com.ljyh.mei.ui.component.player.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ljyh.mei.utils.audio.AudioVisualizerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.max

// 原版 QPlayer 核心 SkSL 算法 (移植至 Android AGSL)
private const val AGSL_FLUID_SHADER = """
uniform float2 resolution;
uniform float  time;
uniform shader cover;

const float TEX_SIZE  = 32.0;
const float WARP_A    = 0.10;
const float WARP_B    = 0.06;
const float ROT_SPEED = 0.18;

half4 main(float2 fragCoord) {
    float2 screenUV = fragCoord / resolution;
    float2 corr = float2(min(1.0, resolution.x / resolution.y),
                         min(1.0, resolution.y / resolution.x));
    float2 uv = (screenUV - 0.5) * corr + 0.5;
    
    // 双波段水波扭曲
    float2 w1 = float2(sin(time * 0.42 + uv.y * 3.7), cos(time * 0.37 + uv.x * 3.1));
    float2 w2 = float2(cos(time * 0.29 + uv.x * 2.3), sin(time * 0.51 + uv.y * 4.4));
    float2 warpedUV = uv + WARP_A * w1 + WARP_B * w2;
    
    // UV 动态旋转
    float a  = time * ROT_SPEED;
    float cs = cos(a);
    float sn = sin(a);
    float2 c = warpedUV - 0.5;
    float2 rotUV = float2(c.x * cs - c.y * sn, c.x * sn + c.y * cs) + 0.5;
    rotUV = clamp(rotUV, float2(0.005), float2(0.995));
    
    half4 col = cover.eval(rotUV * resolution);
    
    // 暗角与抖动抗频闪处理
    float vignette = smoothstep(0.8, 0.3, distance(screenUV, float2(0.5)));
    half3 rgb = col.rgb * half(0.6 + vignette * 0.4);
    float dither = fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453);
    rgb += half3(dither - 0.5) / 255.0;
    
    return half4(rgb, 1.0);
}
"""

@Composable
fun FluidBackground(
    imageUrl: String?,
    audioVisualizerManager: AudioVisualizerManager? = null,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val context = LocalContext.current

    // 动态时间轴动画驱动（对应 QPlayer 的 Shader Time 变量）
    val infiniteTransition = rememberInfiniteTransition(label = "FluidTime")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var primaryColor by remember { mutableStateOf(Color(0xFF2C3E50)) }

    // CPU 端做原版 AMLL 算法预处理与色彩增强
    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrEmpty()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection().apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                }
                val inputStream = connection.getInputStream()
                val srcBitmap = BitmapFactory.decodeStream(inputStream)

                if (srcBitmap != null) {
                    // 1. 下采样至 32x32 纹理
                    val scaled = Bitmap.createScaledBitmap(srcBitmap, 32, 32, true)
                    
                    // 2. 执行 QPlayer 原版 AMLL 增强算法
                    val width = scaled.width
                    val height = scaled.height
                    val pixels = IntArray(width * height)
                    scaled.getPixels(pixels, 0, width, 0, 0, width, height)

                    for (i in pixels.indices) {
                        val p = pixels[i]
                        var r = (p shr 16 and 0xFF).toFloat()
                        var g = (p shr 8 and 0xFF).toFloat()
                        var b = (p and 0xFF).toFloat()

                        r = (r - 128f) * 0.4f + 128f
                        g = (g - 128f) * 0.4f + 128f
                        b = (b - 128f) * 0.4f + 128f

                        val gray = r * 0.3f + g * 0.59f + b * 0.11f
                        r = gray * -2f + r * 3f
                        g = gray * -2f + g * 3f
                        b = gray * -2f + b * 3f

                        r = (r - 128f) * 1.7f + 128f
                        g = (g - 128f) * 1.7f + 128f
                        b = (b - 128f) * 1.7f + 128f

                        r *= 0.75f
                        g *= 0.75f
                        b *= 0.75f

                        val cr = r.toInt().coerceIn(0, 255)
                        val cg = g.toInt().coerceIn(0, 255)
                        val cb = b.toInt().coerceIn(0, 255)
                        pixels[i] = (0xFF shl 24) or (cr shl 16) or (cg shl 8) or cb
                    }

                    val adjustedBitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                    processedBitmap = adjustedBitmap

                    Palette.from(adjustedBitmap).generate().let { palette ->
                        val vibrant = palette.getVibrantColor(0)
                        val dominant = palette.getDominantColor(0)
                        primaryColor = Color(if (vibrant != 0) vibrant else dominant)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
    ) {
        // 判断系统版本：Android 13 (API 33) 及以上直接启用原生 SkSL/AGSL 扭曲着色器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && processedBitmap != null) {
            val shader = remember(processedBitmap) {
                RuntimeShader(AGSL_FLUID_SHADER)
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        ShaderBrush(shader.apply {
                            setFloatUniform("resolution", 1080f, 2400f)
                            setFloatUniform("time", time)
                            // 传入下采样预处理后的封面 Shader
                            setInputShader(
                                "cover",
                                android.graphics.BitmapShader(
                                    processedBitmap!!,
                                    android.graphics.Shader.TileMode.CLAMP,
                                    android.graphics.Shader.TileMode.CLAMP
                                )
                            )
                        })
                    )
            )
        } else {
            // 低版本降级方案：经典高质感高斯模糊底
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.8f)
                        .blur(85.dp)
                )
            }
        }

        // 覆盖层遮罩，保证文字清晰
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
        )

        content()
    }
}
