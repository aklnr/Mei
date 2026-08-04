package com.ljyh.mei.ui.component.player.component

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.extensions.togglePlayPause
import com.ljyh.mei.playback.PlayerConnection

@OptIn(UnstableApi::class)
@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    playerConnection: PlayerConnection,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    isPlaying: Boolean,
    playbackState: Int,
){
    // 👈 获取封面提取的动态主色调
    val accentColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier){
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {

            // previous (上一首)
            Box(modifier = Modifier.weight(1f)) {
                IconButton(
                    enabled = canSkipPrevious,
                    onClick = playerConnection::seekToPrevious,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (canSkipPrevious) 0.9f else 0.4f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                    )
                }
            }

            // play/pause (播放/暂停 - 动态取色圆形底座)
            Box(modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = {
                        if (playbackState == STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.player.togglePlayPause()
                        }
                    },
                    modifier = Modifier
                        .size(64.dp) // 👈 调整按钮总大小为 64.dp，视觉比例更精致
                        .align(Alignment.Center)
                        .background(accentColor, shape = CircleShape) // 👈 充当封面取色圆形底图
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = if (playbackState == STATE_ENDED) Icons.Rounded.Replay else if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White, // 👈 纯白图标，与取色底座形成精美对比
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp) // 👈 调整图标在圆圈内的合适尺寸
                    )
                }
            }

            // next (下一首)
            Box(modifier = Modifier.weight(1f)) {
                IconButton(
                    enabled = canSkipNext,
                    onClick = playerConnection::seekToNext,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (canSkipNext) 0.9f else 0.4f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                    )
                }
            }

        }
    }
}
