package com.ljyh.mei.ui.component.player.component.classic

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.PlayerHorizontalPadding
import com.ljyh.mei.constants.ProgressBarStyle
import com.ljyh.mei.constants.ProgressBarStyleKey
import com.ljyh.mei.constants.TabletAnimationStyle
import com.ljyh.mei.constants.TabletAnimationStyleKey
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.player.OverlayState
import com.ljyh.mei.ui.component.player.component.FluidProgressSlider
import com.ljyh.mei.ui.component.player.component.PlayerControls
import com.ljyh.mei.ui.component.player.component.LyricScreen
import com.ljyh.mei.ui.component.player.component.PlayerActionToolbar
import com.ljyh.mei.ui.component.player.component.PlayerProgressSlider
import com.ljyh.mei.ui.component.player.component.PlayerTableControls
import com.ljyh.mei.ui.component.player.component.classic.component.Cover
import com.ljyh.mei.ui.component.player.component.classic.component.PlayerHeader
import com.ljyh.mei.ui.component.player.component.sheet.PlaylistContent
import com.ljyh.mei.ui.component.player.overlay.PlayerOverlayHandler
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.utils.rememberEnumPreference

@Composable
fun ClassicTabletLayout(
    stateContainer: PlayerStateContainer,
    overlayHandler: PlayerOverlayHandler
) {
    val context = LocalContext.current
    val mediaMetadata by stateContainer.mediaMetadata
    val isPlaying by stateContainer.isPlaying
    val playbackState by stateContainer.playbackState
    val sliderPosition by remember { derivedStateOf { stateContainer.sliderPosition } }
    val duration by remember { derivedStateOf { stateContainer.duration } }
    val lyricLine by remember { derivedStateOf { stateContainer.lyricLine } }
    val isLiked by stateContainer.isLiked

    var isShowingPlaylist by remember { mutableStateOf(false) }

    val (progressBarStyle, _) = rememberEnumPreference(
        key = ProgressBarStyleKey,
        defaultValue = ProgressBarStyle.LINEAR
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ){

        // 左侧播放控制与信息区（权重微调至 0.46f，为右侧让出更多车机横向屏幕空间）
        Column(
            modifier = Modifier
                .weight(0.46f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            mediaMetadata?.let {
                Cover(
                    playerConnection = stateContainer.playerConnection,
                    mediaMetadata = it,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth(0.85f) // 保持封面展示尺寸不变
                        .aspectRatio(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            mediaMetadata?.let {
                PlayerHeader(
                    mediaMetadata = it,
                    modifier = Modifier
                        .fillMaxWidth(0.98f)
                        .padding(horizontal = 2.dp),
                    onClick = {
                        overlayHandler.showAlbumArtist(it.album, it.artists, it.coverUrl)
                    },
                    onMoreClick = {
                        overlayHandler.showMoreAction()
                    },
                    isLiked = isLiked,
                    onLikeClick = {
                        stateContainer.playerViewModel.like(it.id.toString())
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            if (progressBarStyle == ProgressBarStyle.LINEAR) {
                FluidProgressSlider(
                    position = sliderPosition.toLong(),
                    duration = duration,
                    onPositionChange = { newPosition ->
                        stateContainer.playerConnection.player.seekTo(newPosition)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .padding(horizontal = 4.dp)
                )
            } else {
                PlayerProgressSlider(
                    position = sliderPosition.toLong(),
                    duration = duration,
                    isPlaying = isPlaying,
                    onPositionChange = { newPosition ->
                        stateContainer.playerConnection.player.seekTo(newPosition)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .padding(horizontal = 4.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            PlayerTableControls(
                playerConnection = stateContainer.playerConnection,
                canSkipPrevious = stateContainer.canSkipPrevious.value,
                canSkipNext = stateContainer.canSkipNext.value,
                isPlaying = isPlaying,
                playbackState = playbackState,
                modifier = Modifier.fillMaxWidth(0.96f),
                onPlaylistClick = {
                    isShowingPlaylist = !isShowingPlaylist
                }
            )

        }

        Spacer(Modifier.width(16.dp)) // 缩减左右大区之间的间隔，给歌词腾出更多余量

        val tabletAnimStyle by rememberEnumPreference(
            key = TabletAnimationStyleKey,
            defaultValue = TabletAnimationStyle.FLIP_3D
        )

        // 右侧歌词展示区（权重扩大至 0.54f，并将左右边距缩小到 4.dp，使歌词拉宽变长）
        Box(
            modifier = Modifier
                .weight(0.54f)
                .fillMaxHeight(0.98f)
                .align(Alignment.CenterVertically)
        ) {
            val lyricContent = @Composable {
                LyricScreen(
                    lyricData = lyricLine,
                    playerConnection = stateContainer.playerConnection,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp), // 👈 左右内边距压缩到 4.dp，让单行歌词拉长、空间最大化
                    onClick = {
                        mediaMetadata?.let {
                            if (overlayHandler.currentOverlayValue is OverlayState.None) {
                                stateContainer.playerViewModel.searchQQSong(it.title)
                                overlayHandler.showQQMusicSelection(
                                    mediaMetadata = it
                                )
                            }
                        }
                    },
                    onLongClick = { source ->
                        if (source == LyricSource.QQMusic && mediaMetadata != null) {
                            stateContainer.playerViewModel.deleteSongById(id = mediaMetadata!!.id.toString())
                            Toast.makeText(context, "已删除QQ音乐歌词", Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                    controlsVisible = stateContainer.controlsVisible,
                    onToggleControls = {},
                )
            }

            val playlistContent = @Composable {
                PlaylistContent(
                    onDismiss = { isShowingPlaylist = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = PlayerHorizontalPadding)
                )
            }

            when (tabletAnimStyle) {
                TabletAnimationStyle.FLIP_3D -> {
                    val rotation by animateFloatAsState(
                        targetValue = if (isShowingPlaylist) 180f else 0f,
                        animationSpec = tween(600, easing = FastOutSlowInEasing)
                    )
                    val density = LocalDensity.current

                    if (rotation < 90f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationY = rotation
                                    cameraDistance = 12f * density.density
                                }
                        ) {
                            lyricContent()
                        }
                    }

                    if (rotation > 90f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationY = rotation - 180f
                                    cameraDistance = 12f * density.density
                                }
                        ) {
                            playlistContent()
                        }
                    }
                }

                TabletAnimationStyle.SLIDE -> {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isShowingPlaylist,
                        enter = slideInHorizontally { -it } + fadeIn(),
                        exit = slideOutHorizontally { -it } + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        lyricContent()
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isShowingPlaylist,
                        enter = slideInHorizontally { it } + fadeIn(),
                        exit = slideOutHorizontally { -it } + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        playlistContent()
                    }
                }

                TabletAnimationStyle.CROSSFADE -> {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isShowingPlaylist,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        lyricContent()
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isShowingPlaylist,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        playlistContent()
                    }
                }

                TabletAnimationStyle.ZOOM -> {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isShowingPlaylist,
                        enter = scaleIn(initialScale = 0.92f) + fadeIn(),
                        exit = scaleOut(targetScale = 0.92f) + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        lyricContent()
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isShowingPlaylist,
                        enter = scaleIn(initialScale = 0.92f) + fadeIn(),
                        exit = scaleOut(targetScale = 0.92f) + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        playlistContent()
                    }
                }
            }
        }
    }
}



