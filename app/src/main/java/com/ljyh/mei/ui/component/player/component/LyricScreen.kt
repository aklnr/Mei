package com.ljyh.mei.ui.component.player.component

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val PingFangFontFamily = FontFamily(
    Font(resId = R.font.pingfang, weight = FontWeight.Normal),
    Font(resId = R.font.pingfang, weight = FontWeight.Bold)
)

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
    val coroutineScope = rememberCoroutineScope()
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
                delay(50)
            }
        } else {
            animatedPosition = playerConnection.player.currentPosition
        }
    }

    // 提取歌词纯文本列表和时间戳
    val lines = remember(lyricData) {
        lyricData.lyricLine.lines
    }

    // 计算当前正在播放的歌词行索引
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

    // 自动平滑滚动到当前行并居中
    LaunchedEffect(currentIndex) {
        if (lines.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = maxOf(0, currentIndex - 2), // 保持焦点在上方黄金视线位
                    scrollOffset = 0
                )
            }
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
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 220.dp) // 上下留白，让首尾行也能滚到正中间
            ) {
                itemsIndexed(lines) { index, line ->
                    val isSelected = index == currentIndex

                    // 🌟 QPlayer 核心动效：当前行放大、高亮、非当前行深度压暗与缩小
                    val scaleAnim by animateFloatAsState(
                        targetValue = if (isSelected) 1.12f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 250f),
                        label = "Scale"
                    )

                    val alphaAnim by animateFloatAsState(
                        targetValue = if (isSelected) 1.0f else 0.3f, // 非当前行压暗到 0.3，实现极致纵深
                        animationSpec = spring(stiffness = 200f),
                        label = "Alpha"
                    )

                    val contentText = when (line) {
                        is KaraokeLine -> line.syllables.joinToString("") { it.content }
                        is SyncedLine -> line.content
                        else -> ""
                    }

                    val translationText = when (line) {
                        is KaraokeLine -> line.translation
                        is SyncedLine -> line.translation
                        else -> null
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp)
                            .scale(scaleAnim)
                            .alpha(alphaAnim)
                            .clickable {
                                val startTime = when (line) {
                                    is KaraokeLine -> line.start
                                    is SyncedLine -> line.start
                                    else -> 0
                                }
                                playerConnection.player.seekTo(startTime.toLong())
                                onToggleControls(true)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 主歌词
                        Text(
                            text = contentText,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )

                        // 翻译（如果有）
                        if (!translationText.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.padding(top = 4.dp))
                            Text(
                                text = translationText,
                                color = Color.White.copy(alpha = if (isSelected) 0.8f else 0.2f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
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


