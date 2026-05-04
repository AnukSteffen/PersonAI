package com.example.personai.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.personai.domain.model.CommentWithTitle
import com.example.personai.ui.component.ManualToolbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onCommentClick: (String) -> Unit, // 跳转到帖子详情
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val sent by viewModel.sentComments.collectAsState()
    val received by viewModel.receivedReplies.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = { ManualToolbar(title = "消息通知", onBack = onBack) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("回复我的") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("我回复的") })
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val data = if (selectedTab == 0) received else sent

                if (data.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("暂无消息", color = Color.Gray)
                        }
                    }
                }

                items(data) { item ->
                    NotificationItem(
                        data = item, // 传入整个包装对象
                        isSentByMe = selectedTab == 1,
                        onClick = { onCommentClick(item.comment.postId) }
                    )
                    Divider(color = Color.LightGray.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    data: CommentWithTitle,
    isSentByMe: Boolean,
    onClick: () -> Unit
) {
    val comment = data.comment
    val postTitle = data.postTitle ?: "未知帖子"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 头像
        AsyncImage(
            model = comment.authorAvatar,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // 头部：名字 + 时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                val timeStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(comment.timestamp))
                Text(text = timeStr, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(4.dp))
            val actionText = buildAnnotatedString {
                append(if (isSentByMe) "回复了帖子 " else "回复了你 ")
                // 加粗显示标题
                withStyle(style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                ) {
                    append("[$postTitle]")
                }
                append(" :")
            }
            // 动作描述
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 内容
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}