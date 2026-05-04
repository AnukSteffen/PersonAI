package com.example.personai.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.personai.domain.model.Persona
import com.example.personai.ui.component.ManualToolbar
import com.example.personai.ui.theme.AppThemeMode
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onPersonaClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToRelationships: (Int) -> Unit,
    onNavigateToSwitchAccount: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    val followerCount by viewModel.followerCount.collectAsState()
    val followingCount by viewModel.followingCount.collectAsState()
    val creations by viewModel.myCreations.collectAsState()
    val posts by viewModel.myPosts.collectAsState()
    val history by viewModel.historyItems.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0:创作, 1:动态, 2:历史
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsState()
    val isForceOffline by viewModel.isForceOffline.collectAsState()
    var showNetworkDialog by remember { mutableStateOf(false) }
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.updateAvatar(uri) }
    StatItem(label = "关注", value = "$followingCount", onClick = { onNavigateToRelationships(0) })
    Spacer(modifier = Modifier.width(48.dp))
    StatItem(label = "粉丝", value = "$followerCount", onClick = { onNavigateToRelationships(1) })

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ManualToolbar(
                title = "我的",
                actions = {
                    IconButton(onClick = { showNetworkDialog = true }) {
                        Icon(
                            // 如果强制离线或没网，显示 WifiOff；否则显示 Wifi
                            imageVector = if (isForceOffline || !isDeviceOnline) Icons.Default.WifiOff else Icons.Default.Wifi,
                            contentDescription = "Network Mode",
                            // 没网时显示灰色，有网且在线显示绿色，强制离线显示黄色
                            tint = if (!isDeviceOnline) Color.Gray else if (isForceOffline) Color.Yellow else Color.Green
                        )
                    }
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Email, "Notifications")
                    }
                    IconButton(onClick = { showThemeDialog = true }) {
                        Icon(Icons.Default.Palette, "Theme")
                    }
                }
            )
        },
        bottomBar = {
            // 底部操作栏
            Surface(shadowElevation = 16.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 切换账号
                    OutlinedButton(
                        onClick = onNavigateToSwitchAccount,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("切换账号")
                    }

                    // 退出登录
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("退出")
                    }
                }
            }
        }
    ) { padding ->
        if (user == null) return@Scaffold // Loading...

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // === Header  ===
            ProfileHeader(
                user = user!!,
                followerCount = followerCount,
                followingCount = followingCount,
                onAvatarClick = { avatarPicker.launch("image/*") },
                onNameClick = { showEditNameDialog = true },
                onFollowerClick = { onNavigateToRelationships(1) },
                onFollowingClick = { onNavigateToRelationships(0) }
            )

            // === TabRow  ===
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                listOf("我的创作", "我的动态", "浏览历史").forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) }
                    )
                }
            }

            // === Content  ===
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 2
            ) { page ->
                when (page) {
                    0 -> { // 创作 (网格)
                        if (creations.isEmpty()) EmptyView("暂无创作")
                        else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(creations, key = { it.id }) { persona ->
                                    MyPersonaCard(persona) { onPersonaClick(persona.id) }
                                }
                            }
                        }
                    }
                    1 -> { // 动态 (列表)
                        if (posts.isEmpty()) EmptyView("暂无动态")
                        else {
                            LazyColumn(
                                contentPadding = PaddingValues(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(posts, key = { it.id }) { post ->
                                    TextOnlyPostCard(post, post.timestamp) { onPostClick(post.id) }
                                }
                            }
                        }
                    }
                    2 -> { // 历史 (列表)
                        if (history.isEmpty()) EmptyView("暂无浏览记录")
                        else {
                            LazyColumn(
                                contentPadding = PaddingValues(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(history, key = { it.post.id }) { item ->
                                    TextOnlyPostCard(item.post, item.browsedTime) { onPostClick(item.post.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 弹窗逻辑 ---
    // 修改昵称
    if (showEditNameDialog) {
        var newName by remember { mutableStateOf(user!!.nickname) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("修改昵称") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) viewModel.updateNickname(newName)
                    showEditNameDialog = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditNameDialog = false
                }) { Text("取消") }
            }
        )
    }

    // 退出确认
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout { /* MainActivity 监听跳转 */ }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("退出") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("取消") } }
        )
    }

    // 主题弹窗
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题风格") },
            text = {
                Column {
                    // 选项列表
                    ThemeOptionItem(
                        "默认 (跟随系统)",
                        AppThemeMode.DEFAULT,
                        viewModel
                    ) { showThemeDialog = false }
                    ThemeOptionItem("明亮黄色", AppThemeMode.YELLOW, viewModel) {
                        showThemeDialog = false
                    }
                    ThemeOptionItem("清新绿色", AppThemeMode.GREEN, viewModel) {
                        showThemeDialog = false
                    }
                    ThemeOptionItem("夜间模式", AppThemeMode.DARK, viewModel) {
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("关闭") }
            }
        )
    }

    // 网络模式选择
    if (showNetworkDialog) {
        AlertDialog(
            onDismissRequest = { showNetworkDialog = false },
            title = { Text("选择网络模式") },
            text = {
                Column {
                    if (!isDeviceOnline) {
                        Text("当前设备未连接网络，仅可用离线模式", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 选项 1: 在线模式
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isDeviceOnline) { // 没网时不可点
                                viewModel.setForceOffline(false)
                                showNetworkDialog = false
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = !isForceOffline,
                            onClick = null,
                            enabled = isDeviceOnline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("在线模式(使用云端API)", color = if(isDeviceOnline) Color.Unspecified else Color.Gray)
                            if (!isDeviceOnline) Text("不可用", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }

                    // 选项 2: 离线模式
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setForceOffline(true)
                                showNetworkDialog = false
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = isForceOffline,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("离线模式(使用本地大模型，不可生成图片、视频、语音)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNetworkDialog = false }) { Text("关闭") }
            }
        )
    }
}

// 辅助组件
@Composable
fun StatItem(label: String, value: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun EmptyView(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.LightGray)
    }
}

@Composable
fun MyPersonaCard(persona: Persona, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            AsyncImage(
                model = persona.avatarUrl, contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Text(
                persona.name,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ThemeOptionItem(
    text: String,
    mode: AppThemeMode,
    viewModel: ProfileViewModel,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                viewModel.switchTheme(mode)
                onSelect()
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 显示一个小色块预览
        val color = when (mode) {
            AppThemeMode.YELLOW -> com.example.personai.ui.theme.YellowPrimary
            AppThemeMode.GREEN -> com.example.personai.ui.theme.GreenPrimary
            AppThemeMode.DARK -> Color.Black
            else -> Color.Gray
        }

        Box(modifier = Modifier
            .size(24.dp)
            .background(color, CircleShape))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text)
    }
}

@Composable
fun ProfileHeader(
    user: com.example.personai.domain.model.User,
    followerCount: Int,
    followingCount: Int,
    onAvatarClick: () -> Unit,
    onNameClick: () -> Unit,
    onFollowerClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .clickable { onAvatarClick() },
                contentScale = ContentScale.Crop
            )
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(4.dp)
                    .size(14.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onNameClick() }
        ) {
            Text(user.nickname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        }
        Text("ID: ${user.id}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            StatItem(label = "粉丝", value = "$followerCount", onClick = onFollowerClick)
            Spacer(modifier = Modifier.width(48.dp))
            StatItem(label = "关注", value = "$followingCount", onClick = onFollowingClick)
        }
    }
}