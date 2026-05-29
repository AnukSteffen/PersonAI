package com.example.personai.ui.post

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.personai.domain.model.Comment
import com.example.personai.domain.model.Post
import com.example.personai.ui.component.FullScreenMediaDialog
import com.example.personai.ui.component.ManualToolbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCommentTime(timestamp: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onNavigateToForward: (String) -> Unit,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val post by viewModel.post.collectAsState()
    val commentTree by viewModel.commentTree.collectAsState()
    val totalCount by viewModel.totalCommentCount.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()
    var commentText by remember { mutableStateOf("") }
    val replyingTo by viewModel.replyingTo.collectAsState() // 监听回复状态
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ManualToolbar(
                title = "详情",
                onBack = onBack,
                actions = {
                    // 转发按钮
                    IconButton(onClick = { onNavigateToForward(post!!.id) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Forward"
                        )
                    }
                }
            )
        },
        bottomBar = {
            // 底部评论输入框
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Column {
                    // 回复提示条
                    if (replyingTo != null) {
                        val targetName = replyingTo!!.authorName
                        val targetContent = replyingTo!!.content

                        val displayContent = if (targetContent.length > 20) {
                            "${targetContent.take(20)}..."
                        } else {
                            targetContent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                // 格式：回复 @Name : 内容...
                                text = "回复 @$targetName : $displayContent",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.onCancelReply() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("写评论...") },
                            maxLines = 3
                        )
                        IconButton(onClick = {
                            if (commentText.isNotBlank()) {
                                viewModel.sendComment(commentText)
                                commentText = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (post != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. 帖子主体内容
                    item {
                        PostDetailContent(
                            post = post!!,
                            isLiked = isLiked,
                            onLikeClick = { viewModel.toggleLike() },
                            onAuthorClick = { onAuthorClick(post!!.authorId) }
                        )
                    }

                    // 评论数
                    item {
                        Divider(
                            thickness = 8.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            "评论 ($totalCount)",
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 3. 评论列表，树状渲染
                    items(commentTree) { node ->
                        // 渲染一个完整的对话树
                        CommentTreeItem(
                            node = node,
                            onReplyClick = { targetComment -> viewModel.onReplyClick(targetComment) }
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.3f))
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            } else {

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun PostDetailContent(
    post: Post,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onAuthorClick: () -> Unit
) {
    // 控制全屏查看的状态
    var showFullScreen by remember { mutableStateOf(false) }
    var initialImageIndex by remember { mutableStateOf(0) }

    val canClickProfile = post.authorType != "user"
    Column(modifier = Modifier.padding(16.dp)) {
        // 作者信息
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                if (canClickProfile) {
                    Modifier.clickable { onAuthorClick() }
                } else {
                    Modifier
                }
            )
        ) {
            AsyncImage(
                model = post.authorAvatar,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(post.authorName, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 标题与正文
        Text(post.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(post.content, style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(12.dp))

        // 图片
        if (post.imageUrls.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { post.imageUrls.size })

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                // 1. 轮播图
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    AsyncImage(
                        model = post.imageUrls[page],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                // 点击查看大图
                                initialImageIndex = page
                                showFullScreen = true
                            }
                    )
                }

                // 2. 圆点导航器
                if (post.imageUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(post.imageUrls.size) { iteration ->
                            val color =
                                if (pagerState.currentPage == iteration) Color.White else Color.White.copy(
                                    alpha = 0.5f
                                )
                            Box(
                                modifier = Modifier
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // tag展示区
        if (post.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp)) // 与图片保持间距

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                post.tags.forEach { tag ->
                    SuggestionChip(
                        onClick = { /* 未来可点击跳转搜索 */ },
                        label = {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.height(32.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 操作栏 (点赞)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    tint = if (isLiked) Color.Red else Color.Gray,
                    contentDescription = "Like"
                )
            }
            Text("${post.likeCount} 赞")

            Spacer(modifier = Modifier.weight(1f))

            Text(
                SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(post.timestamp)),
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    // 图片全屏查看器
    if (showFullScreen) {
        FullScreenMediaDialog(
            mediaUrls = post.imageUrls,
            initialPage = initialImageIndex,
            onDismiss = { showFullScreen = false }
        )
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .clickable { onClick() }
            .fillMaxWidth()) {
        AsyncImage(
            model = comment.authorAvatar,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            // 1. 名字
            Text(
                text = comment.authorName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            // 2. 内容 (正常黑色)
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // 3. 时间
            Text(
                text = formatCommentTime(comment.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray
            )
        }
    }
}

//评论树条目
@Composable
fun CommentTreeItem(
    node: CommentNode,
    onReplyClick: (Comment) -> Unit
) {
    // 展开状态：如果子评论超过 1 条，默认收起；如果不超过 1 条，默认展开
    var isExpanded by remember { mutableStateOf(node.children.size <= 1) }

    Column {
        // 父评论
        CommentItem(
            comment = node.comment,
            onClick = { onReplyClick(node.comment) }
        )

        // 子评论区域
        if (node.children.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(start = 48.dp)
                    .fillMaxWidth()
            ) {
                // 如果是展开状态 ，显示所有子评论
                if (isExpanded) {
                    node.children.forEach { childNode ->
                        ChildCommentItem(
                            comment = childNode.comment,
                            onClick = { onReplyClick(childNode.comment) }
                        )
                    }
                }
                // B. 如果是收起状态 (且 > 1条)，显示折叠提示
                else {
                    //始终显示第1条子评论
                    ChildCommentItem(
                        comment = node.children.first().comment,
                        onClick = { onReplyClick(node.children.first().comment) }
                    )
                    // 显示展开提示
                    val remainingCount = node.children.size - 1
                    if (remainingCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 装饰横线
                            Box(
                                modifier = Modifier
                                    .size(20.dp, 1.dp)
                                    .background(Color.LightGray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // 提示文字 (例如：展开 2 条回复)
                            Text(
                                text = "展开 $remainingCount 条回复",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(4.dp))
                            // 向下箭头
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 子评论样式
@Composable
fun ChildCommentItem(
    comment: Comment,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 0.dp)
    ) {
        // 小头像
        AsyncImage(
            model = comment.authorAvatar,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            // 1. 名字
            Text(
                text = comment.authorName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // 2. 内容
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 1.dp)
            )

            // 3. 时间
            Text(
                text = formatCommentTime(comment.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray
            )
        }
    }
}