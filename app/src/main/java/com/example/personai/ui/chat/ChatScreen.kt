package com.example.personai.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.personai.domain.model.Persona
import com.example.personai.ui.component.ManualToolbar

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onPostClick: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val currentPersona by viewModel.currentPersona.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val generatingStatus by viewModel.generatingStatus.collectAsState()
    var showExtendedMenu by remember { mutableStateOf(false) }
    val pendingMediaPath by viewModel.pendingMediaPath.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        // 选中后不直接发，而是暂存
        if (uri != null) viewModel.onMediaPicked(uri)
    }


    val rotationAngle by animateFloatAsState(
        targetValue = if (showExtendedMenu) 45f else 0f,
        label = "Rotation"
    )

    Scaffold(
        topBar = {
            ManualToolbar(
                title = currentPersona?.name ?: "对话中...",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Profile")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // B. 输入行
                Surface(tonalElevation = 2.dp, shadowElevation = 4.dp) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged {
                                    // 如果输入框获得焦点
                                    if (it.isFocused) {
                                        showExtendedMenu = false
                                    }
                                },
                            placeholder = { Text("输入消息...") },
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // 发送按钮
                        IconButton(onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 旋转十字按钮
                        IconButton(
                            onClick = {
                                showExtendedMenu = !showExtendedMenu
                                if (showExtendedMenu) {
                                    keyboardController?.hide() // 展开菜单时收起键盘
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Toggle Menu",
                                modifier = Modifier.rotate(rotationAngle), // 应用旋转动画
                                tint = Color.Gray
                            )
                        }
                    }
                }

                // C. 扩展菜单
                AnimatedVisibility(
                    visible = showExtendedMenu,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp) // 菜单高度
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 1. 图片发送按钮
                        ChatMenuItem(
                            icon = Icons.Default.Image,
                            label = "相册",
                            onClick = {
                                mediaPicker.launch("*/*")
                            }
                        )

                        // 2. 回复长度设置按钮
                        ChatMenuItem(
                            icon = Icons.Default.Settings,
                            // 动态显示当前长度设置
                            label = when(viewModel.replyLengthLevel) {
                                0 -> "长度: 精简"
                                1 -> "长度: 标准"
                                2 -> "长度: 详细"
                                else -> "长度"
                            },
                            onClick = {
                                // 循环切换长度
                                viewModel.replyLengthLevel = (viewModel.replyLengthLevel + 1) % 3
                            }
                        )

                        // 预留位置
                        // todo ChatMenuItem(...)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusManager.clearFocus() // 清除焦点 -> 收起键盘
                            // showExtendedMenu = false
                        }
                    )
                }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                reverseLayout = true,
                contentPadding = padding

            ) {
                items(messages.reversed()) { msg ->
                    ChatBubbleDispatcher(
                        msg = msg,
                        viewModel = viewModel,
                        onPostClick = onPostClick
                    )
                }

                if (currentPersona != null) {
                    item {
                        SystemBioBubble(persona = currentPersona!!)
                    }
                }
            }

            AnimatedVisibility(
                visible = generatingStatus != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shadowElevation = 4.dp,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // 小转圈
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = generatingStatus ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (pendingMediaPath != null) {
        MediaPreviewDialog(
            mediaPath = pendingMediaPath!!,
            onDismiss = { viewModel.cancelMediaSend() },
            onSend = { text ->
                viewModel.confirmMediaSend(text)
            }
        )
    }
}

// --- 菜单项组件 ---
@Composable
fun ChatMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}
@Composable
fun SystemBioBubble(persona: Persona) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = persona.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = persona.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp)
        ) {

            Text(
                text = persona.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "跟TA说些什么吧！",
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
