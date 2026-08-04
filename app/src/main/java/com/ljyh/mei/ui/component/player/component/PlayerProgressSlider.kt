package com.ljyh.mei.ui.component.player.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.constants.MusicQualityKey
import com.ljyh.mei.utils.TimeUtils.makeTimeString
import com.ljyh.mei.utils.rememberEnumPreference
import kotlin.math.PI
import kotlin.math.roundToLong
import kotlin.math.sin


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProgressSlider(
    position: Long,
    duration: Long,
    isPlaying: Boolean, // 根据播放状态控制波浪滚动
    onPositionChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val musicQuality by rememberEnumPreference(MusicQualityKey, MusicQuality.EXHIGH)
    val isDurationValid = remember(duration) { duration > 0 }
    val valueRange = remember(duration) { 0f..(duration.takeIf { it > 0 } ?: 1).toFloat() }

    // 👈 核心取色逻辑：自动获取从当前封面中提取的主鲜艳色
    val activeWaveColor = MaterialTheme.colorScheme.primary

    // 交互状态
    val interactionSource = remember { MutableInteractionSource() }
    val isUserDragging by interactionSource.collectIsDraggedAsState()

    // 内部拖拽位置状态
    var rawDragPosition by remember { mutableFloatStateOf(0f) }

    // 计算当前显示的值
    val sliderPosition = if (isUserDragging) {
        rawDragPosition
    } else {
        position.toFloat()
    }.coerceIn(valueRange)

    // 计算进度百分比 (0.0 - 1.0)
    val progressFraction = if (isDurationValid) {
        (sliderPosition / valueRange.endInclusive).coerceIn(0f, 1f)
    } else 0f

    // 动画：波浪的相位 (Phase) - 让波浪动起来
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (2 * PI).toFloat() else 0f, // 只有播放时才滚动
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // 拖拽时波浪变直，播放时振幅正常
    val targetAmplitude = if (isUserDragging) 0f else if (isPlaying) 6f else 3f
    val amplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(300),
        label = "amplitude"
    )

    Column(modifier = modifier) {
        Slider(
            value = sliderPosition,
            onValueChange = {
                rawDragPosition = it
            },
            onValueChangeFinished = {
                if (isDurationValid) {
                    onPositionChange(rawDragPosition.roundToLong())
                }
            },
            valueRange = valueRange,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            enabled = isDurationValid,

            thumb = {
                Spacer(Modifier.size(1.dp))
            },

            // 自定义轨道绘制
            track = { sliderState ->
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2

                    // 1. 绘制未播放部分 (Inactive) - 半透明白色
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(0f, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // 2. 绘制已播放部分 (Active) - 动态取色波浪线
                    if (progressFraction > 0) {
                        val activeWidth = width * progressFraction

                        val path = Path()
                        path.moveTo(0f, centerY)

                        val step = 5f
                        var x = 0f
                        val frequency = 0.05f

                        while (x < activeWidth) {
                            val y = centerY + amplitude * sin(x * frequency - phase)
                            path.lineTo(x, y)
                            x += step
                        }

                        val finalY = centerY + amplitude * sin(activeWidth * frequency - phase)
                        path.lineTo(activeWidth, finalY)

                        // 绘制跟随封面变色的波浪线
                        drawPath(
                            path = path,
                            color = activeWaveColor, // 👈 动态取色
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // 3. 绘制末端光标 (小圆点) - 同样使用动态取色
                        drawCircle(
                            color = activeWaveColor, // 👈 动态取色
                            radius = 5.dp.toPx(),
                            center = Offset(activeWidth, finalY)
                        )
                    }
                }
            }
        )

        // 时间文字行
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            val timeTextStyle = MaterialTheme.typography.labelMedium.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = Offset(0f, 1f),
                    blurRadius = 4f
                )
            )

            Text(
                text = makeTimeString(sliderPosition.toLong()),
                style = timeTextStyle,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = musicQuality.explanation,
                style = timeTextStyle,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = if (duration > 0) makeTimeString(duration) else "-:--",
                style = timeTextStyle,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

