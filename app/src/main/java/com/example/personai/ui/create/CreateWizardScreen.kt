package com.example.personai.ui.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.personai.ui.component.ManualToolbar
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CreateWizardScreen(
    onSaveSuccess: () -> Unit,
    onSuccessClick: (String) -> Unit,
    viewModel: CreateViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 5 }) //5页的填写内容
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDraftDialog by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }

    val canGoNext = remember(pagerState.currentPage, viewModel.uiState) { //是否可以填下一页
        if (pagerState.currentPage == 0) {
            viewModel.uiState.name.isNotBlank() && viewModel.uiState.description.isNotBlank()
        } else true
    }

    // ---  返回拦截逻辑 ---
    val handleBack: () -> Unit = {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        } else {
            if (viewModel.hasContent()) showDraftDialog = true else onSaveSuccess()
        }
    }

    // 拦截物理返回键
    BackHandler { handleBack() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // 标题根据步骤动态变化
            val stepTitle = when (pagerState.currentPage) {
                0 -> "基本信息"
                1 -> "详细设定"
                2 -> "世界观"
                3 -> "形象与标签"
                4 -> "确认信息"
                else -> ""
            }
            ManualToolbar(
                title = "$stepTitle (${pagerState.currentPage + 1}/5)",
                onBack = null
            )
        },
        bottomBar = {
            WizardBottomBar(
                currentPage = pagerState.currentPage,
                nextEnabled = canGoNext,
                onBack = handleBack,
                // 点击下一步
                onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                // 点击完成
                onFinish = {
                    viewModel.publishPersona(
                        onSuccess = { newId -> onSuccessClick(newId) },
                        onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    )
                }
            )
        },
        floatingActionButton = {
            if (pagerState.currentPage == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        isGenerating = true
                        viewModel.autoGeneratePersona {
                            isGenerating = false
                            // 生成完直接跳到预览页 (Page 4)
                            scope.launch { pagerState.animateScrollToPage(4) }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    icon = {
                        if (isGenerating) CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        else Icon(Icons.Default.AutoAwesome, null)
                    },
                    text = { Text(if (isGenerating) "生成中..." else "AI 一键生成") }
                )
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            userScrollEnabled = false,
            beyondViewportPageCount = 4
        ) { page ->
            // 给每个页面加一个垂直滚动容器，防止小屏幕显示不全
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // 根据页码渲染对应的 Step 组件 (来自 WizardSteps.kt)
                when (page) {
                    0 -> Step1BasicInfo(viewModel.uiState) { viewModel.updateState { _ -> it } }
                    1 -> Step2DetailedSettings(viewModel.uiState) { viewModel.updateState { _ -> it } }
                    2 -> Step3WorldSetting(viewModel.uiState) { viewModel.updateState { _ -> it } }
                    3 -> Step4Appearance(
                        state = viewModel.uiState,
                        isGenerating = viewModel.isGeneratingAvatar, // 传入状态
                        presetAvatars = viewModel.presetAvatars,     // 传入列表
                        onUpdate = { viewModel.updateState { _ -> it } },
                        onUploadAvatar = { uri -> viewModel.onAvatarSelected(uri) },
                        onAiGenerate = { viewModel.generateAiAvatar() }, // 绑定生成方法
                        onSelectPreset = { uri -> viewModel.selectPresetAvatar(uri) } // 绑定选择方法
                    )

                    4 -> Step5Preview(viewModel.uiState)
                }
            }
        }
    }

    // --- 草稿弹窗 ---
    if (showDraftDialog) {
        AlertDialog(
            onDismissRequest = { showDraftDialog = false },
            title = { Text("保存草稿？") },
            text = { Text("未发布的角色将丢失，是否保存为草稿以便下次继续编辑？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveDraft(onSaved = {
                        showDraftDialog = false
                        onSaveSuccess()
                    })
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.discardDraft()
                    showDraftDialog = false
                    onSaveSuccess() // 直接退出，不保存
                }) {
                    Text("不保存", color = Color.Red)
                }
            }
        )
    }
}

// --- 底部导航栏组件 ---
@Composable
fun WizardBottomBar(
    currentPage: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    nextEnabled: Boolean = true
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧按钮：上一步 / 退出
            OutlinedButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (currentPage == 0) "退出" else "上一步")
            }

            // 右侧按钮：下一步 / 完成
            if (currentPage < 4) {
                Button(
                    onClick = onNext,
                    enabled = nextEnabled
                ) {
                    Text("下一步")
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Button(onClick = onFinish) {
                    Text("发布")
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}