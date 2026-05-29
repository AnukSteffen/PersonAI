package com.example.personai.ui.post

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.personai.ui.component.FullScreenMediaDialog
import com.example.personai.ui.component.PublishToolbar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    onBack: () -> Unit,
    onPostCreated: (String) -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel()
) {

    val transparentColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )
    var showFullScreen by remember { mutableStateOf(false) }
    var initialViewIndex by remember { mutableStateOf(0) }
    var showDraftDialog by remember { mutableStateOf(false) }
    // 照片选择器
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onMediaSelected(uri)
        }
    }

    val handleBackPress = {
        if (viewModel.hasContent()) {
            showDraftDialog = true
        } else {
            onBack()
        }
    }

    // 拦截物理系统返回键
    BackHandler {
        handleBackPress()
    }

    Scaffold(
        topBar = {
            PublishToolbar(
                title = "发布动态",
                onCancel = handleBackPress,
                onPublish = {
                    viewModel.submitPost(
                        onSuccess = { newId ->
                            onPostCreated(newId)
                        })
                },
                isPublishEnabled = viewModel.title.isNotBlank() && viewModel.content.isNotBlank()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // --- 1. 标题输入区 ---
            TextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                placeholder = {
                    Text(
                        "请输入完整帖子标题",
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        fontSize = 20.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = transparentColors,
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )

            // --- 2. 正文输入区 ---
            TextField(
                value = viewModel.content,
                onValueChange = { viewModel.content = it },
                placeholder = { Text("分享你的想法...", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp), // 最小高度
                colors = transparentColors,
                textStyle = TextStyle(fontSize = 16.sp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )

            // 3. Tag添加区
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                TextField(
                    value = viewModel.currentTagInput,
                    onValueChange = { viewModel.currentTagInput = it },
                    placeholder = {
                        Text("# 添加标签 (如: 赛博朋克)", fontSize = 14.sp, color = Color.Gray)
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = transparentColors,
                    textStyle = TextStyle(fontSize = 14.sp)
                )
                IconButton(onClick = { viewModel.addTag() }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Tag")
                }
            }

            // B. 已选 Tag 展示
            if (viewModel.selectedTags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    viewModel.selectedTags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.removeTag(tag) }, // 点击删除
                            label = { Text("#$tag") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- 5. 图片选择区 ---
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // A. 已选图片
                itemsIndexed(viewModel.selectedImages) { index, imageUrl ->
                    Box(modifier = Modifier.size(100.dp)) {
                        // 图片本体
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    Color.LightGray.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    initialViewIndex = index
                                    showFullScreen = true
                                }
                        )

                        // 删除按钮
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp) // 距离图片边缘的间距
                                .size(16.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .clip(CircleShape)
                                .clickable { viewModel.removeImage(imageUrl) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }

                // B. 上传按钮
                item {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable {
                                mediaPickerLauncher.launch("*/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // 底部留白
            Spacer(modifier = Modifier.height(50.dp))
        }
    }

    if (showFullScreen) {
        FullScreenMediaDialog(
            mediaUrls = viewModel.selectedImages.toList(), // 转为 List
            initialPage = initialViewIndex,
            onDismiss = { showFullScreen = false }
        )
    }

    // 草稿保存弹窗
    if (showDraftDialog) {
        AlertDialog(
            onDismissRequest = { showDraftDialog = false },
            title = { Text("保存草稿？") },
            text = { Text("保存后，下次进入该页面可继续编辑。") },
            confirmButton = {
                TextButton(onClick = {
                    // 保存逻辑
                    viewModel.saveDraft(onSaved = {
                        showDraftDialog = false
                        onBack() // 退出页面
                    })
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.discardDraft()
                    // 不保存逻辑
                    showDraftDialog = false
                    onBack()
                }) {
                    Text("不保存", color = Color.Red)
                }
            }
        )
    }
}