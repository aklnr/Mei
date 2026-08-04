package com.ljyh.mei.ui.component.player.component.classic.component

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.MediaMetadata

@Composable
fun PlayerHeader(
    modifier: Modifier,
    mediaMetadata: MediaMetadata,
    isLiked: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onLikeClick: () -> Unit,
    iconColor: Color = Color.White
) {

    val shadowStyle = Shadow(
        color = Color.Black.copy(alpha = 0.5f),
        offset = Offset(2f, 2f),
        blurRadius = 8f
    )

    Row(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .clickable(onClick = { onClick() }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：标题和副标题（给予最大的占用权重，充分延伸横向空间）
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .weight(1f) // 占据所有可用剩余空间
                .padding(end = 2.dp) // 极小的右间距，把空间最大化留给歌名
        ) {
            // --- 标题部分 (字重由 Bold 改为 Normal，实现不加粗) ---
            Text(
                text = mediaMetadata.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    shadow = shadowStyle,
                    fontWeight = FontWeight.Normal // 👈 调为不加粗
                ),
                maxLines = 1,
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
            )

            // --- 副标题部分 (歌手 & 专辑，同样确保不加粗) ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE)
            ) {
                val subTitleStyle = MaterialTheme.typography.bodyMedium.copy(
                    shadow = shadowStyle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Normal // 👈 副标题同步调为不加粗
                )

                if (mediaMetadata.artists.isNotEmpty()) {
                    Text(
                        text = mediaMetadata.artists.joinToString(", ") { it.name },
                        style = subTitleStyle,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    )
                }

                // 分隔符
                if (mediaMetadata.artists.isNotEmpty() && mediaMetadata.album.title.isNotEmpty()) {
                    Text(
                        text = " - ",
                        style = subTitleStyle,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }

                // 专辑部分
                if (mediaMetadata.album.title.isNotEmpty()) {
                    Text(
                        text = mediaMetadata.album.title,
                        style = subTitleStyle,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // 右侧：红心与更多按钮（紧靠最右端）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ){
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Like",
                    // 👈 收藏后展示鲜艳的 Apple 标准红，未收藏时保持图标默认色
                    tint = if (isLiked) Color(0xFFFF2D55) else iconColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // 右侧：更多按钮
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多选项",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
