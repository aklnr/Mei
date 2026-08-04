
package com.ljyh.mei.ui.component.player.component

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.R
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.utils.setClipboard
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import kotlinx.coroutines.delay

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
    val listState = rememberLazyListState()
    val context = LocalContext.current
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

    LaunchedEffect(playerConnection.isPlaying) {
        if (playerConnection.isPlaying.value) {
            while (true) {
                animatedPosition = (playerConnection.player.currentPosition).coerceAtMost(
                    playerConnection.player.duration
                )
                delay(30) // 更高频的刷新，让逐字高亮像 QPlayer 一样丝滑
            }
        } else {
            animatedPosition = playerConnection.player.currentPosition
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
        if (lyricData.lyricLine.lines.isNotEmpty()) {
            key(System.identityHashCode(lyricData.lyricLine)) {
                KaraokeLyricsView(
                    listState = listState,
                    lyrics = lyricData.lyricLine,
                    currentPosition = { animatedPosition.toInt() },
                    onLineClicked = { line ->
                        playerConnection.player.seekTo(line.start.toLong())
                        onToggleControls(true)
                    },
                    onLinePressed = { line ->
                        val result = when (line) {
                            is KaraokeLine -> "${line.syllables.joinToString("") { it.content }}\n${line.translation}"
                            is SyncedLine -> "${line.content}\n${line.translation}"
                            else -> null
                        }
                        result?.let { setClipboard(context, it, "lyric") }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),

                    // 🌟 QPlayer 核心效果：当前播放行（绝对纯净高亮白光、微放大、发光晕染）
                    normalLineTextStyle = LocalTextStyle.current.copy(
                        fontSize = 22.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textMotion = TextMotion.Animated,
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.White.copy(alpha = 0.6f),
                            offset = Offset(0f, 0f),
                            blurRadius = 16f
                        )
                    ),

                    // 🌟 QPlayer 核心效果：非当前行（深度压暗至 0.25 透明度，拒绝喧宾夺主）
                    accompanimentLineTextStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Normal,
                        textMotion = TextMotion.Animated,
                        color = Color.White.copy(alpha = 0.25f),
                        shadow = Shadow(
                            color = Color.Transparent,
                            offset = Offset(0f, 0f),
                            blurRadius = 0f
                        )
                    )
                )
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
