package com.ljyh.mei.ui.component.player.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.constants.MusicQualityKey
import com.ljyh.mei.utils.TimeUtils.makeTimeString
import com.ljyh.mei.utils.rememberEnumPreference
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FluidProgressSlider(
    position: Long,
    duration: Long,
    onPositionChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val musicQuality by rememberEnumPreference(MusicQualityKey, MusicQuality.EXHIGH)
    val isDurationValid = remember(duration) { duration > 0 }
    val valueRange = remember(duration) { 0f..(duration.takeIf { it > 0 } ?: 1).toFloat() }

    // 👈 从动态主题中获取从封面提取的主色调（如暗红、鲜艳色等）
    val activeColor = MaterialTheme.colorScheme.primary

    // 交互状态监听
    val interactionSource = remember { MutableInteractionSource() }
    val isUserDragging by interactionSource.collectIsDraggedAsState()
    val isUserPressed by interactionSource.collectIsPressedAsState()

    // 按住或拖拽时条变粗
    val isInteracting = isUserDragging || isUserPressed

    // 内部拖拽位置状态
    var rawDragPosition by remember { mutableFloatStateOf(0f) }

    // 计算当前显示的值
    val sliderPosition = if (isUserDragging) {
        rawDragPosition
    } else {
        position.toFloat()
    }.coerceIn(valueRange)

    // --- 动画核心 ---

    // 1. 轨道高度动画：平时 2dp，按住时 10dp
    val trackHeight by animateDpAsState(
        targetValue = if (isInteracting) 10.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "trackHeight"
    )

    // 2. 滑块大小动画：平时 0dp，按住时 8dp
    val thumbRadius by animateDpAsState(
        targetValue = if (isInteracting) 8.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumbRadius"
    )

    Column(modifier = modifier) {

        Box(
            modifier = Modifier.height(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = sliderPosition,
                onValueChange = { rawDragPosition = it },
                onValueChangeFinished = {
                    if (isDurationValid) {
                        onPositionChange(rawDragPosition.roundToLong())
                    }
                },
                valueRange = valueRange,
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
                enabled = isDurationValid,

                colors = SliderDefaults.colors(
                    thumbColor = activeColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),

                // 自定义 Thumb (圆球)
                thumb = {
                    Canvas(modifier = Modifier.size(thumbRadius * 2)) {
                        drawCircle(
                            color = activeColor, // 👈 拖拽圆球也跟随取色
                            radius = thumbRadius.toPx()
                        )
                    }
                },

                // 自定义 Track (轨道)
                track = { sliderState ->
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(trackHeight)
                    ) {
                        val width = size.width
                        val height = size.height

                        // 计算进度比例
                        val fraction = if (valueRange.endInclusive > 0) {
                            (sliderState.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                        } else 0f

                        val activeWidth = width * fraction

                        // 1. 绘制背景轨道 (未播放部分) - 半透明白色
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.3f),
                            topLeft = Offset(0f, 0f),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(height / 2, height / 2)
                        )

                        // 2. 绘制已播放轨道 - 使用动态提取的封面主色调（取色效果）
                        if (activeWidth > 0) {
                            drawRoundRect(
                                color = activeColor, // 👈 动态取色
                                topLeft = Offset(0f, 0f),
                                size = Size(activeWidth, height),
                                cornerRadius = CornerRadius(height / 2, height / 2)
                            )
                        }
                    }
                }
            )
        }

        // 时间文字行
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            val commonTextStyle = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.2f),
                    offset = Offset(0f, 1f),
                    blurRadius = 2f
                )
            )

            // 左侧：当前时间
            Text(
                text = makeTimeString(sliderPosition.toLong()),
                style = commonTextStyle,
                color = Color.White.copy(alpha = 0.9f),
            )

            // 中间：音质
            Text(
                text = musicQuality.explanation,
                style = commonTextStyle.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.6f),
            )

            // 右侧：总时长
            Text(
                text = if (duration > 0) makeTimeString(duration) else "-:--",
                style = commonTextStyle,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}
