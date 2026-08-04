
package com.ljyh.mei.ui.component.player.component

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb

/**
 * 🌟 修复版 SkSL 流体背景 Shader
 * 彻底解决坐标越界导致的斜向黑块/撕裂问题，并加入 Slow Simplex Noise 慢速流体动画
 */
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
    // 1. 严格归一化坐标在 [0, 1] 范围，杜绝黑块与斜向切割
    vec2 uv = fragCoord.xy / iResolution.xy;
    
    // 2. 时间驱动的慢速液态流动
    float t = iTime * 0.12;
    float n1 = snoise(uv * 1.5 + vec2(t * 0.2, t * 0.1));
    float n2 = snoise(uv * 2.2 - vec2(t * 0.1, t * 0.25));
    
    // 3. 柔和颜色混合与暗化
    vec3 col = mix(color1.rgb, color2.rgb, clamp(n1 + 0.5, 0.0, 1.0));
    col = mix(col, color3.rgb, clamp(n2 * 0.8 + 0.4, 0.0, 1.0));
    
    // 适当暗化背景，凸显上层白色歌词
    col *= 0.65;
    
    return vec4(col, 1.0);
}
"""

@Composable
fun FluidBackground(
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // 🌟 驱动时间变量 iTime 持续更新，实现背景液态流动
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

        Canvas(modifier = modifier.fillMaxSize()) {
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
        // Android 13 以下版本降级渐变
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(color = primaryColor.copy(alpha = 0.85f))
        }
    }
}
