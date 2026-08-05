package com.ljyh.mei.ui.component.player.component

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint as NativePaint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.R
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.coroutines.delay
import kotlin.math.exp
import kotlin.math.sqrt

private const val LIFT_PEAK_PX = 8.0f
private const val LIFT_OMEGA0 = 3.7416574
private const val LIFT_ZETA = 0.935414

@OptIn(UnstableApi::class)
@Composable
fun LyricScreen(
    lyricData: LyricData,
    modifier: Modifier = Modifier,
    playerConnection: PlayerConnection,
    onClick: (LyricSource) -> Unit,
    onLongClick: (LyricSource) -> Unit,
    controlsVisible: Boolean,
    onToggleControls: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var animatedPosition by remember { mutableLongStateOf(0L) }

    val pingFangTypeface = remember(context) {
        try {
            ResourcesCompat.getFont(context, R.font.pingfang) ?: Typeface.DEFAULT
        } catch (e: Exception) {
            e.printStackTrace()
            Typeface.DEFAULT
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            onToggleControls(false)
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    if (delta < -10) {
                        onToggleControls(false)
                    } else if (delta > 10) {
                        onToggleControls(true)
                    }
                }
                return Offset.Zero
            }
        }
    }

    // 🌟 60FPS 无缝平滑刷帧
    LaunchedEffect(playerConnection.isPlaying) {
        if (playerConnection.isPlaying.value) {
            while (true) {
                animatedPosition = playerConnection.player.currentPosition
                delay(16)
            }
        } else {
            animatedPosition = playerConnection.player.currentPosition
        }
    }

    val lines = remember(lyricData) { lyricData.lyricLine.lines }

    // 基础底层 Paint（未唱到的部分，保持 35% 白半透明）
    val basePaint = remember(pingFangTypeface) {
        NativePaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            alpha = (255 * 0.35f).toInt()
            textSize = with(density) { 28.sp.toPx() }
            typeface = Typeface.create(pingFangTypeface, Typeface.BOLD)
        }
    }

    // 高亮发光 Paint
    val glowPaint = remember(pingFangTypeface) {
        NativePaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = with(density) { 28.sp.toPx() }
            typeface = Typeface.create(pingFangTypeface, Typeface.BOLD)
            maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
        }
    }

    // 非焦点行 Paint
    val dimPaint = remember(pingFangTypeface) {
        NativePaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            alpha = (255 * 0.25f).toInt()
            textSize = with(density) { 22.sp.toPx() }
            typeface = Typeface.create(pingFangTypeface, Typeface.NORMAL)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggleControls(true) }
    ) {
        if (lines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "暂无歌词",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 16.sp
                )
            }
        } else {
            val currentIndex = remember(animatedPosition, lines) {
                val idx = lines.indexOfLast { line ->
                    val st: Long = when (line) {
                        is KaraokeLine -> line.start.toLong()
                        is SyncedLine -> line.start.toLong()
                        else -> 0L
                    }
                    st <= animatedPosition
                }
                if (idx == -1) 0 else idx
            }

            var scrollYOffset by remember { mutableFloatStateOf(0f) }
            val targetScrollY = currentIndex * with(density) { 72.dp.toPx() }

            LaunchedEffect(targetScrollY) {
                scrollYOffset += (targetScrollY - scrollYOffset) * 0.12f
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasHeight = size.height
                val startX = 80f
                val centerY = canvasHeight * 0.38f
                val lineSpacingPx = 72.dp.toPx()

                lines.forEachIndexed { index, line ->
                    val lineTopY = centerY + (index * lineSpacingPx) - scrollYOffset
                    val isSelected = index == currentIndex

                    if (lineTopY > -100f && lineTopY < canvasHeight + 100f) {
                        val contentText = when (line) {
                            is KaraokeLine -> line.syllables.joinToString("") { it.content }
                            is SyncedLine -> line.content
                            else -> ""
                        }

                        if (isSelected) {
                            val lineStart: Long = when (line) {
                                is KaraokeLine -> line.start.toLong()
                                is SyncedLine -> line.start.toLong()
                                else -> 0L
                            }

                            val nextLineStart: Long = if (index < lines.size - 1) {
                                when (val next = lines[index + 1]) {
                                    is KaraokeLine -> next.start.toLong()
                                    is SyncedLine -> next.start.toLong()
                                    else -> lineStart + 4000L
                                }
                            } else lineStart + 4000L

                            val duration: Long = (nextLineStart - lineStart).coerceAtLeast(1000L)
                            val progress = ((animatedPosition - lineStart).toFloat() / duration.toFloat()).coerceIn(0f, 1f)

                            val totalWidth = basePaint.measureText(contentText)

                            // 🌟 1. 绘制底层 35% 白半透明暗字
                            drawContext.canvas.nativeCanvas.drawText(contentText, startX, lineTopY, basePaint)

                            // 🌟 2. 核心：创建 Apple Music 风格的线性渐变扫字 Shader
                            val activeWidth = totalWidth * progress
                            val gradientWidth = 30f // 渐变边缘羽化宽度

                            if (activeWidth > 0f) {
                                val sweepPaint = NativePaint(basePaint).apply {
                                    alpha = 255
                                    shader = LinearGradient(
                                        startX + activeWidth - gradientWidth,
                                        lineTopY,
                                        startX + activeWidth + gradientWidth,
                                        lineTopY,
                                        intArrayOf(
                                            android.graphics.Color.WHITE,
                                            android.graphics.Color.WHITE,
                                            android.graphics.Color.TRANSPARENT
                                        ),
                                        floatArrayOf(0f, 0.5f, 1f),
                                        Shader.TileMode.CLAMP
                                    )
                                }

                                // 绘制高亮扫字
                                drawContext.canvas.nativeCanvas.drawText(contentText, startX, lineTopY, sweepPaint)

                                // 🌟 3. 在当前正在唱到的字符位置，叠加 Glow 发光 + 物理微抬升
                                var curX = startX
                                val charCount = contentText.length
                                for (i in 0 until charCount) {
                                    val ch = contentText[i].toString()
                                    val charWidth = basePaint.measureText(ch)
                                    val charProgressStart = i.toFloat() / charCount.toFloat()

                                    if (progress >= charProgressStart) {
                                        val charTau = (progress - charProgressStart) * (duration / 1000.0)
                                        val liftK = computeLiftSpringK(charTau)
                                        val offsetY = -LIFT_PEAK_PX * liftK

                                        // Glow 发光 Pass
                                        glowPaint.alpha = (255 * 0.45f * (1f - liftK * 0.3f)).toInt()
                                        drawContext.canvas.nativeCanvas.drawText(
                                            ch,
                                            curX,
                                            lineTopY + offsetY,
                                            glowPaint
                                        )
                                    }
                                    curX += charWidth
                                }
                            }
                        } else {
                            // 非焦点行
                            drawContext.canvas.nativeCanvas.drawText(contentText, startX, lineTopY, dimPaint)
                        }
                    }
                }
            }
        }

        LyricSourceBadge(
            source = lyricData.source,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onClick = onClick,
            onLongClick = onLongClick
        )
    }
}

private fun computeLiftSpringK(tau: Double): Float {
    if (tau <= 0.0 || tau > 1.2) return 0f
    val zw = LIFT_ZETA * LIFT_OMEGA0
    val wd = LIFT_OMEGA0 * sqrt(1.0 - LIFT_ZETA * LIFT_ZETA)
    val env = exp(-zw * tau)
    var y = 1.0 - env * (kotlin.math.cos(wd * tau) + (zw / wd) * kotlin.math.sin(wd * tau))
    if (y < 0.0) y = 0.0
    return y.toFloat()
}

@Composable
private fun LyricSourceBadge(
    source: LyricSource,
    modifier: Modifier = Modifier,
    onClick: (LyricSource) -> Unit,
    onLongClick: (LyricSource) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .combinedClickable(
                onClick = { onClick(source) },
                onLongClick = { onLongClick(source) }
            )
    ) {
        Icon(
            painter = painterResource(
                when (source) {
                    LyricSource.Empty, LyricSource.Loading -> R.drawable.empty
                    LyricSource.NetEaseCloudMusic -> R.drawable.netease
                    LyricSource.QQMusic -> R.drawable.qq
                    LyricSource.AM -> R.drawable.am
                }
            ),
            modifier = Modifier.size(16.dp),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f)
        )
    }
}
