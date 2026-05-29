package com.example.personai.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.personai.domain.model.Persona
import com.example.personai.ui.component.ManualToolbar
import com.example.personai.ui.feed.PostItem
import kotlinx.coroutines.launch


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonaProfileScreen(
    onBack: () -> Unit,
    onStartChat: () -> Unit, // 点击私聊
    onPostClick: (String) -> Unit,
    viewModel: PersonaProfileViewModel = hiltViewModel() // 复用 ProfileViewModel 查数据
) {
    val persona by viewModel.persona.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val isFollowing by viewModel.isFollowing.collectAsState()
    val likedIds by viewModel.likedPostIds.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val creatorName by viewModel.creatorName.collectAsState()
    var showAiPostMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsState()
    val isForceOffline by viewModel.isForceOffline.collectAsState()
    val generatingStatus by viewModel.generatingStatus.collectAsState()

    val isOnlineAvailable = isDeviceOnline && !isForceOffline
    if (persona == null) return

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ManualToolbar(
                title = "Persona主页",
                onBack = onBack,
                actions = {
                    val currentUser = user
                    val currentPersona = persona
                    if (currentUser != null && currentPersona != null && currentPersona.creatorId == currentUser.id) {
                        Box {
                            // 发帖图标
                            IconButton(onClick = { showAiPostMenu = true }) {
                                if (generatingStatus != null) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.Yellow
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Auto Post",
                                        tint = Color.Green
                                    )
                                }
                            }

                            // 下拉菜单
                            DropdownMenu(
                                expanded = showAiPostMenu,
                                onDismissRequest = { showAiPostMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("生成纯文字动态") },
                                    onClick = {
                                        showAiPostMenu = false
                                        scope.launch {
                                            viewModel.triggerAutoPost(mode = 0)
                                            snackbarHostState.showSnackbar("图文动态发布成功！")
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("生成图文动态")
                                            if (!isOnlineAvailable) Text(
                                                "(需在线)",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    enabled = isOnlineAvailable,
                                    onClick = {
                                        showAiPostMenu = false
                                        viewModel.triggerAutoPost(mode = 1)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("生成视频动态")
                                            if (!isOnlineAvailable) Text(
                                                "(需在线)",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    enabled = isOnlineAvailable,
                                    onClick = {
                                        showAiPostMenu = false
                                        viewModel.triggerAutoPost(mode = 2)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            ProfileBottomBar(
                isFollowing = isFollowing,
                onFollowClick = { viewModel.toggleFollow() },
                onChatClick = onStartChat
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // --- 1. 顶部背景区 ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    // 背景图
                    AsyncImage(
                        model = persona!!.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 黑色渐变遮罩
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.6f), // 上部遮罩
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.9f)  // 下部遮罩
                                    )
                                )
                            )
                    )

                    // 信息展示 (位于背景图底部)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        // 头像
                        AsyncImage(
                            model = persona!!.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 昵称 白字黑底
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = persona!!.name,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    drawStyle = Stroke(
                                        miter = 10f,
                                        width = 6f,
                                        join = StrokeJoin.Round
                                    )
                                ),
                                color = Color.Black
                            )

                            Text(
                                text = persona!!.name,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White // 白色实体
                            )
                        }
                        // ID & Creator
                        Text(
                            text = "ID: ${persona!!.id.take(8)}         创作者: @$creatorName",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        // 数据统计
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            StatItem("对话", "${persona!!.interactionCount}")
                            Spacer(modifier = Modifier.width(24.dp))
                            StatItem("粉丝", "${persona!!.followerCount}")
                            Spacer(modifier = Modifier.width(24.dp))
                            if (persona!!.currentStatus.isNotBlank()){
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 2.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 一个小圆点装饰
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color.Green, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = persona!!.currentStatus,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 2. Tab 栏 ---
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("关于 TA") })
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("TA 的动态") })
                }

                // --- 3. 内容区 ---
                if (selectedTab == 0) {
                    AboutTabContent(
                        persona = persona!!,
                        currentUser = user
                    )
                } else {
                    // 帖子列表
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        if (posts.isEmpty()) {
                            Text("暂无动态", modifier = Modifier.padding(24.dp), color = Color.Gray)
                        } else {
                            posts.forEach { post ->
                                val isLiked = likedIds.contains(post.id)

                                PostItem(
                                    post = post,
                                    isLiked = isLiked,
                                    onLikeClick = { viewModel.toggleLike(post.id) },
                                    onItemClick = { onPostClick(post.id) },
                                    showMenu = false,
                                    onForwardClick = null,
                                    onHideClick = null
                                )
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = generatingStatus != null,
                enter = expandVertically(),
                exit = shrinkVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = generatingStatus ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(value, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutTabContent(
    persona: Persona,
    currentUser: com.example.personai.domain.model.User?
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Tags
        if (persona.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                persona.tags.forEach { tag ->
                    SuggestionChip(onClick = {}, label = { Text("#$tag") })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 简介 (Static)
        Text("简介", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(persona.description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))
        val isCreator = currentUser != null && persona.creatorId == currentUser.id
        // 动态设定 (Evolved)
        if (isCreator) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("共生记录 (仅你可见)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "随着你们的对话，TA 产生了以下变化：",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = persona.evolvedPersonality,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileBottomBar(
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Surface(shadowElevation = 16.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 关注按钮
            Button(
                onClick = onFollowClick,
                modifier = Modifier.weight(1f),
                colors = if (isFollowing)
                    ButtonDefaults.buttonColors(containerColor = Color.Gray)
                else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(if (isFollowing) Icons.Default.Check else Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text(if (isFollowing) "已关注" else "关注")
            }

            // 聊天按钮
            Button(
                onClick = onChatClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ChatBubble, null)
                Spacer(Modifier.width(4.dp))
                Text("开始聊天")
            }
        }
    }
}