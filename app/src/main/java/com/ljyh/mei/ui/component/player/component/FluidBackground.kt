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

    // 1. 将图片加载逻辑独立出来，提取 Bitmap
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

    // 👈 响应重低音鼓点的轻微呼吸缩放（1.0 ~ 1.06 随低音跳动）
    val bassScale by animateFloatAsState(
        targetValue = 1f + (bass * volumeScale * 0.15f).coerceIn(0f, 0.08f),
        animationSpec = tween(durationMillis = 100),
        label = "bassScale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 👈 1. 动态 Mesh 弥散渲染层（加上低音律动缩放与柔和模糊）
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
                .scale(bassScale) // 重低音呼吸伸缩
                .blur(8.dp)       // 柔和二次弥散，让色彩混色更通透
        )

        // 👈 2. 沉浸式暗色渐变遮罩（确保歌词与底部控件清晰可见）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f), // 顶部顶部标题栏遮罩
                            Color.Black.copy(alpha = 0.15f), // 中间歌词区域保持通透
                            Color.Black.copy(alpha = 0.50f)  // 底部进度条与控制区遮罩
                        )
                    )
                )
        )
    }
}
