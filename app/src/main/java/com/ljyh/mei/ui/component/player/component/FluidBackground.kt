
package com.ljyh.mei.ui.component.player.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.ljyh.mei.utils.audio.AudioVisualizerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private const val FLUID_SHADER_SRC = """
uniform vec2 iResolution;
uniform float iTime;
uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;

vec3 permute(vec3 x) { return mod(((x*34.0)+1.0)*x, 289.0); }

float snoise(vec2 v){
  const vec4 C = vec4(0.211324865405187, 0.366025403784439,
                     -0.577350269189626, 0.024390243902439);
  vec2 i  = floor(v + dot(v, C.yy) );
  vec2 x0 = v -   i + dot(i, C.xx);
  vec2 i1;
  i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
  vec4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod(i, 289.0);
  vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))
  + i.x + vec3(0.0, i1.x, 1.0 ) );
  vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy), dot(x12.zw,x12.zw)), 0.0);
  m = m*m;
  m = m*m;
  vec3 x = 2.0 * fract(p * C.www) - 1.0;
  vec3 h = abs(x) - 0.5;
  vec3 ox = floor(x + 0.5);
  vec3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );
  vec3 g;
  g.x  = a0.x  * x0.x  + h.x  * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}

half4 main(vec2 fragCoord) {
    vec2 uv = fragCoord.xy / iResolution.xy;
    float t = iTime * 0.12;
    float n1 = snoise(uv * 1.5 + vec2(t * 0.2, t * 0.1));
    float n2 = snoise(uv * 2.2 - vec2(t * 0.1, t * 0.25));
    
    vec3 col = mix(color1.rgb, color2.rgb, clamp(n1 + 0.5, 0.0, 1.0));
    col = mix(col, color3.rgb, clamp(n2 * 0.8 + 0.4, 0.0, 1.0));
    col *= 0.65;
    
    return vec4(col, 1.0);
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
    var primaryColor by remember { mutableStateOf(Color(0xFF2C3E50)) }
    var secondaryColor by remember { mutableStateOf(Color(0xFF1F120B)) }
    var tertiaryColor by remember { mutableStateOf(Color(0xFF0F0F13)) }

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
                val bitmap = BitmapFactory.decodeStream(inputStream)

                if (bitmap != null) {
                    Palette.from(bitmap).generate().let { palette ->
                        val vibrant = palette.getVibrantColor(0xFFFF9800.toInt())
                        val dominant = palette.getDominantColor(0xFF3F51B5.toInt())
                        val darkMuted = palette.getDarkMutedColor(0xFF121218.toInt())

                        primaryColor = Color(vibrant)
                        secondaryColor = Color(dominant)
                        tertiaryColor = Color(darkMuted)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val infiniteTransition = rememberInfiniteTransition(label = "FluidTime")
            val time by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(100000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "Time"
            )

            val shader = remember { RuntimeShader(FLUID_SHADER_SRC) }

            Canvas(modifier = Modifier.fillMaxSize()) {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", time)

                val c1 = primaryColor.toArgb()
                val c2 = secondaryColor.toArgb()
                val c3 = tertiaryColor.toArgb()

                shader.setFloatUniform(
                    "color1",
                    android.graphics.Color.red(c1) / 255f,
                    android.graphics.Color.green(c1) / 255f,
                    android.graphics.Color.blue(c1) / 255f,
                    1f
                )
                shader.setFloatUniform(
                    "color2",
                    android.graphics.Color.red(c2) / 255f,
                    android.graphics.Color.green(c2) / 255f,
                    android.graphics.Color.blue(c2) / 255f,
                    1f
                )
                shader.setFloatUniform(
                    "color3",
                    android.graphics.Color.red(c3) / 255f,
                    android.graphics.Color.green(c3) / 255f,
                    android.graphics.Color.blue(c3) / 255f,
                    1f
                )

                drawRect(brush = ShaderBrush(shader))
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = primaryColor.copy(alpha = 0.85f))
            }
        }

        content()
    }
}
