
package com.ljyh.mei.ui.component.player.component

import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
import android.graphics.Typeface
import androidx.annotation.OptIn
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.R
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.coroutines.delay
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// 🌟 原版 QPlayer 物理常量
private const val LIFT_PEAK_PX = 4.0f // 字符微抬升高度
private const val LIFT_OMEGA0 = 3.7416574 // Apple LiftSpring 物理参数
private const val LIFT_ZETA = 0.935414
private const val DARK_MASK_ALPHA = 0.36f // 未演唱字符透明度

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
    var animatedPosition by remember { mutableLongStateOf(0) }

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

    // 🌟 高频 60FPS 实时帧率更新，保证逐字扫字与字符弹簧流畅度
    LaunchedEffect(playerConnection.isPlaying) {
        if (playerConnection.isPlaying.value) {
            while (true) {
                animatedPosition = (playerConnection.player.currentPosition).coerceAtMost(
                    playerConnection.player.duration
                )
                delay(16)
            }
        } else {
            animatedPosition = playerConnection.player.currentPosition
        }
    }

    val lines = remember(lyricData) { lyricData.lyricLine.lines }

    // 原生 Paint 缓存初始化
    val textPaint = remember {
        NativePaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = with(density) { 24.sp.toPx() }
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    val glowPaint = remember {
        NativePaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = with(density) { 24.sp.toPx() }
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL) // QPlayer 原版发光 Filter
        }
    }

    val dimPaint = remember {
        NativePaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            alpha = (255 * 0.25f).toInt() // 非焦点行深度压暗
            textSize = with(density) { 20.sp.toPx() }
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
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
            // 计算当前焦点行索引
            val currentIndex = remember(animatedPosition, lines) {
                val index = lines.indexOfLast { line ->
                    val startTime = when (line) {
                        is KaraokeLine -> line.start
                        is SyncedLine -> line.start
                        else -> 0
                    }
                    startTime <= animatedPosition
                }
                if (index == -1) 0 else index
            }

            // 平滑滚动 Y 轴平移变量
            var scrollYOffset by remember { mutableFloatStateOf(0f) }
            val targetScrollY = currentIndex * with(density) { 64.dp.toPx() }

            // 弹簧平滑跟踪
            LaunchedEffect(targetScrollY) {
                scrollYOffset += (targetScrollY - scrollYOffset) * 0.18f
            }

            // 🌟 核心：使用 Canvas 进行全底层精准 QPlayer 风格绘制
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val startX = 80f // 靠左大字对齐
                val centerY = canvasHeight * 0.38f // 视线黄金焦点比例 (ALIGN_POSITION = 0.35)

                val lineSpacingPx = 64.dp.toPx()

                lines.forEachIndexed { index, line ->
                    val lineTopY = centerY + (index * lineSpacingPx) - scrollYOffset
                    val isSelected = index == currentIndex

                    // 视口裁剪：只绘制可见区域内的歌词，优化流畅度
                    if (lineTopY > -100f && lineTopY < canvasHeight + 100f) {
                        val contentText = when (line) {
                            is KaraokeLine -> line.syllables.joinToString("") { it.content }
                            is SyncedLine -> line.content
                            else -> ""
                        }

                        if (isSelected) {
                            // 🌟 当前焦点行：处理逐字 Lift 抬升波浪 + 发光 Pass
                            var curX = startX
                            val lineStartMs = when (line) {
                                is KaraokeLine -> line.start
                                is SyncedLine -> line.start
                                else -> 0
                            }

                            if (line is KaraokeLine && line.syllables.isNotEmpty()) {
                                // 卡拉OK 音节逐字抬升绘制
                                line.syllables.forEach { syllable ->
                                    val sylText = syllable.content
                                    val sylStart = syllable.start
                                    val tau = (animatedPosition - sylStart) / 1000.0

                                    // 计算 QPlayer liftSpringK 抬升位移
                                    val liftK = computeLiftSpringK(tau)
                                    val offsetY = -LIFT_PEAK_PX * liftK

                                    val isSung = animatedPosition >= sylStart

                                    // 1. 发光底层 Pass
                                    if (isSung) {
                                        glowPaint.alpha = (255 * 0.55f).toInt()
                                        drawContext.canvas.nativeCanvas.drawText(
                                            sylText,
                                            curX,
                                            lineTopY + offsetY,
                                            glowPaint
                                        )
                                    }

                                    // 2. 纯白高亮字符 Pass
                                    textPaint.alpha = if (isSung) 255 else (255 * DARK_MASK_ALPHA).toInt()
                                    drawContext.canvas.nativeCanvas.drawText(
                                        sylText,
                                        curX,
                                        lineTopY + offsetY,
                                        textPaint
                                    )

                                    curX += textPaint.measureText(sylText)
                                }
                            } else {
                                // 传统整句同步行绘制
                                glowPaint.alpha = (255 * 0.55f).toInt()
                                drawContext.canvas.nativeCanvas.drawText(contentText, startX, lineTopY, glowPaint)
                                textPaint.alpha = 255
                                drawContext.canvas.nativeCanvas.drawText(contentText, startX, lineTopY, textPaint)
                            }
                        } else {
                            // 🌟 非当前行：应用 0.25 深度压暗与缩小
                            drawContext.canvas.nativeCanvas.drawText(
                                contentText,
                                startX,
                                lineTopY,
                                dimPaint
                            )
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

/**
 * 🌟 移植自 QPlayer LyricRenderer.java 的 Apple liftSpring 物理算法
 */
private fun computeLiftSpringK(tau: Double): Float {
    if (tau <= 0.0) return 0f
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
