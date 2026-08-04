package com.ljyh.mei.ui.component.player.component

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import coil3.Bitmap
import com.ljyh.mei.constants.MeshFlowSpeedKey
import com.ljyh.mei.constants.MeshLowFreqVolumeKey
import com.ljyh.mei.constants.MeshPlayingKey
import com.ljyh.mei.constants.MeshRenderScaleKey
import com.ljyh.mei.constants.MeshStaticModeKey
import com.ljyh.mei.constants.MeshSubdivisionKey
import com.ljyh.mei.ui.component.player.component.mesh.MeshBackgroundView
import com.ljyh.mei.utils.audio.AudioVisualizerManager
import com.ljyh.mei.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FluidBackground(
    imageUrl: String?,
    audioVisualizerManager: AudioVisualizerManager,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bass by audioVisualizerManager.bassValue.collectAsState()

    val (flowSpeed) = rememberPreference(MeshFlowSpeedKey, defaultValue = 0.25f)
    val (renderScale) = rememberPreference(MeshRenderScaleKey, defaultValue = 0.75f)
    val (staticMode) = rememberPreference(MeshStaticModeKey, defaultValue = false)
    val (meshPlaying) = rememberPreference(MeshPlayingKey, defaultValue = true)
    val (volumeScale) = rememberPreference(MeshLowFreqVolumeKey, defaultValue = 0.1f)
    val (subdivision) = rememberPreference(MeshSubdivisionKey, defaultValue = 50)

    val albumBitmap by produceState<Bitmap?>(null, imageUrl) {
        if (imageUrl.isNullOrEmpty()) {
            value = null
            return@produceState
        }
        withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .size(256)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                value = result.image.toBitmap()
            }
        }
    }

    val shouldAnimate = !meshPlaying || isPlaying

    val bassScale by animateFloatAsState(
        targetValue = 1f + (bass * volumeScale * 0.15f).coerceIn(0f, 0.08f),
        animationSpec = tween(durationMillis = 100),
        label = "bassScale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 👈 1. 关键修复：blur 调回 28.dp！抹平网格噪点，让色彩混合像水墨一样均匀自然！
        AndroidView(
            factory = { ctx ->
                MeshBackgroundView(ctx).apply {
                    setFlowSpeed(flowSpeed)
                    setRenderScale(renderScale)
                    setSubdivision(subdivision)
                    setStaticMode(staticMode)
                    setPlaying(shouldAnimate)
                    setPreserveEGLContextOnPause(true)
                }
            },
            update = { view ->
                albumBitmap?.let { bmp ->
                    view.setAlbum(bmp)
                }

                view.updateVolume(bass * volumeScale)
                view.setFlowSpeed(flowSpeed)
                view.setRenderScale(renderScale)
                view.setSubdivision(subdivision)
                view.setStaticMode(staticMode)
                view.setPlaying(shouldAnimate)
            },
            modifier = Modifier
                .fillMaxSize()
                .scale(bassScale)
                .blur(28.dp) // 👈 28.dp 柔化网格，恢复均匀平滑的弥散色彩
        )

        // 👈 2. 优雅渐变保护层（微暗度，保证文字清晰的同时颜色依旧亮丽）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.28f)
                        )
                    )
                )
        )
    }
}


