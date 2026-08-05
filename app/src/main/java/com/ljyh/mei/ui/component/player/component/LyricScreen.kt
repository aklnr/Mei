
package com.ljyh.mei.ui.component.player.component

import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
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

private const val LIFT_PEAK_PX = 6.0f
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

    val textPaint = remember(pingFangTypeface) {
        NativePaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = with(density) { 26.sp.toPx() }
            typeface = Typeface.create(pingFangTypeface, Typeface.BOLD)
        }
    }

    val glowPaint = remember(pingFangTypeface) {
        NativePaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = with(density) { 26.sp.toPx() }
            typeface = Typeface.create(pingFangTypeface, Typeface.BOLD)
            maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
        }
    }

    val dimPaint = remember(pingFangTypeface) {
        NativePaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            alpha = (255 * 0.30f).toInt()
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
            val targetScrollY = currentIndex * with(density) { 68.dp.toPx() }

            LaunchedEffect(targetScrollY) {
                scrollYOffset += (targetScrollY - scrollYOffset) * 0.15f
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasHeight = size.height
                val startX = 70f
                val centerY = canvasHeight * 0.38f
                val lineSpacingPx = 68.dp.toPx()

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
                            // 🌟 强转为 Long，彻底解决类型推导问题
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
                            } else {
                                lineStart + 4000L
                            }

                            val duration: Long = (nextLineStart - lineStart).coerceAtLeast(1000L)
                            val progress = ((animatedPosition - lineStart).toFloat() / duration.toFloat()).coerceIn(0f, 1f)

                            var curX = startX

                            if (line is KaraokeLine && line.syllables.isNotEmpty()) {
                                line.syllables.forEach { syllable ->
                                    val sylText = syllable.content
                                    val sylStart = syllable.start.toLong()
                                    val isSung = animatedPosition >= sylStart

                                    val tau = if (isSung) (animatedPosition - sylStart) / 1000.0 else 0.0
                                    val liftK = if (isSung) computeLiftSpringK(tau) else 0f
                                    val offsetY = -LIFT_PEAK_PX * liftK

                                    if (isSung) {
                                        glowPaint.alpha = (255 * 0.65f).toInt()
                                        drawContext.canvas.nativeCanvas.drawText(
                                            sylText,
                                            curX,
                                            lineTopY + offsetY,
                                            glowPaint
                                        )
                                    }

                                    textPaint.alpha = if (isSung) 255 else (255 * 0.35f).toInt()
                                    drawContext.canvas.nativeCanvas.drawText(
                                        sylText,
                                        curX,
                                        lineTopY + offsetY,
                                        textPaint
                                    )

                                    curX += textPaint.measureText(sylText)
                                }
                            } else {
                                val charCount = contentText.length
                                for (i in 0 until charCount) {
                                    val ch = contentText[i].toString()
                                    val charProgressStart = i.toFloat() / charCount.toFloat()
                                    val isCharSung = progress >= charProgressStart

                                    val charTau = if (isCharSung) (progress - charProgressStart) * (duration / 1000.0) else 0.0
                                    val liftK = if (isCharSung) computeLiftSpringK(charTau) else 0f
                                    val offsetY = -LIFT_PEAK_PX * liftK

                                    if (isCharSung) {
                                        glowPaint.alpha = (255 * 0.65f).toInt()
                                        drawContext.canvas.nativeCanvas.drawText(
                                            ch,
                                            curX,
                                            lineTopY + offsetY,
                                            glowPaint
                                        )
                                    }

                                    textPaint.alpha = if (isCharSung) 255 else (255 * 0.35f).toInt()
                                    drawContext.canvas.nativeCanvas.drawText(
                                        ch,
                                        curX,
                                        lineTopY + offsetY,
                                        textPaint
                                    )

                                    curX += textPaint.measureText(ch)
                                }
                            }
                        } else {
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
