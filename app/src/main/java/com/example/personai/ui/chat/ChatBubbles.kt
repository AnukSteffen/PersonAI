package com.example.personai.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.personai.domain.model.ChatMessage
import com.example.personai.domain.model.Post
import com.example.personai.ui.component.FullScreenMediaDialog
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material3.Material3RichText

@Composable
fun ChatBubbleDispatcher(
    msg: ChatMessage,
    viewModel: ChatViewModel,
    onPostClick: (String) -> Unit
) {
    val align = if (msg.isUser) Alignment.End else Alignment.Start
    val bgColor = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val shape = if (msg.isUser) RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp) else RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Surface(
            color = bgColor,
            shape = shape,
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .widthIn(max = 300.dp)
        ) {
            when (msg.type) {
                0 -> TextBubble(msg) // 纯文本
                1 -> ImageBubble(msg) // 图片
                2 -> VideoBubble(msg) // 视频
                3 -> PostCardBubble(msg, viewModel, onPostClick) // 帖子卡片
                4 -> AudioBubble(msg) // 音频
                else -> TextBubble(msg.copy(content = "[不支持的消息类型]"))
            }
        }
    }
}

@Composable
fun TextBubble(msg: ChatMessage) {
    Box(modifier = Modifier.padding(12.dp)) {
        if (msg.isUser) {
            Text(text = msg.content, color = MaterialTheme.colorScheme.onPrimaryContainer)
        } else {
            Material3RichText { Markdown(content = msg.content) }
        }
    }
}

@Composable
fun ImageBubble(msg: ChatMessage) {
    var showFullScreen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.clickable { showFullScreen = true }) {
        AsyncImage(
            model = msg.content, // 这里是本地路径
            contentDescription = "Image",
            modifier = Modifier.width(200.dp).heightIn(max = 300.dp),
            contentScale = ContentScale.Crop
        )
    }

    if (showFullScreen) {
        FullScreenMediaDialog(
            mediaUrls = listOf(msg.content),
            onDismiss = { showFullScreen = false }
        )
    }
}

@Composable
fun VideoBubble(msg: ChatMessage) {
    var showFullScreen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.clickable { showFullScreen = true }) {
        // 显示视频缩略图 (Coil Video 自动处理)
        AsyncImage(
            model = msg.content, // Coil 会自动解码视频第一帧
            contentDescription = "Video",
            modifier = Modifier
                .width(200.dp)
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        // 叠加一个播放图标
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }

    if (showFullScreen) {
        FullScreenMediaDialog(
            mediaUrls = listOf(msg.content),
            onDismiss = { showFullScreen = false }
        )
    }
}

@Composable
fun PostCardBubble(
    msg: ChatMessage,
    viewModel: ChatViewModel,
    onPostClick: (String) -> Unit
) {
    var post by remember { mutableStateOf<Post?>(null) }

    // 异步加载帖子信息
    LaunchedEffect(msg.content) {
        post = viewModel.getPostDetails(msg.content) // msg.content 里存的是 postId
    }

    if (post != null) {
        Column(
            modifier = Modifier
                .width(240.dp)
                .clickable { onPostClick(post!!.id) }
                .padding(8.dp)
        ) {
            // 封面图
            if (post!!.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = post!!.imageUrls.first(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            // 标题
            Text(
                text = post!!.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                maxLines = 1
            )
            // 摘要
            Text(
                text = post!!.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "来自动态",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        Text("帖子已删除或无法加载", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AudioBubble(msg: ChatMessage) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    // 初始化播放器
    val player = remember {
        ExoPlayer.Builder(context).build().apply{
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // 资源清理
    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    // 监听播放状态
    DisposableEffect(player) {
        // 定义监听器
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                    player.pause()   // 停止播放
                    player.seekTo(0) // 进度条回滚到开头
                }
            }
        }

        // 添加监听
        player.addListener(listener)

        // 组件销毁时：释放资源，移除监听
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable {
                if (isPlaying) {
                    player.pause()
                    isPlaying = false
                } else {
                    val file = java.io.File(msg.content)
                    if (file.exists()) {
                        player.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file)))
                        player.prepare()
                        player.play()
                        isPlaying = true
                    }
                }
            }
            .padding(12.dp)
            .width(120.dp) // 固定宽度的语音条
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = "Play/Pause",
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 显示文字
        Text(
            text = if (isPlaying) "播放中..." else "语音消息",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}