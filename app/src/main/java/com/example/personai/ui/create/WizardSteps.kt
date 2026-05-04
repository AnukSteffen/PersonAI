package com.example.personai.ui.create

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.personai.domain.model.VoiceConstants

// Step 1: 基本信息
@Composable
fun Step1BasicInfo(
    state: PersonaCreationState,
    onUpdate: (PersonaCreationState) -> Unit
) {
    WizardStepTitle(
        title = "基本信息",
        subtitle = "这是角色对外展示的名片"
    )

    // 1. 昵称
    OutlinedTextField(
        value = state.name,
        onValueChange = { onUpdate(state.copy(name = it)) },
        label = { Text("角色昵称 (必填)") },
        placeholder = { Text("给你的角色命名") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 2. 简介
    OutlinedTextField(
        value = state.description,
        onValueChange = { onUpdate(state.copy(description = it)) },
        label = { Text("简介 (必填)") },
        placeholder = { Text("简单介绍一下你的角色或剧情") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 5
    )
}

// Step 2: 详细设定
@Composable
fun Step2DetailedSettings(
    state: PersonaCreationState,
    onUpdate: (PersonaCreationState) -> Unit
) {
    WizardStepTitle(
        title = "详细设定",
        subtitle = "丰富的人设能让 AI 的回复更具灵魂。所有内容均可留空。"
    )

    // --- 1. 性别 ---
    val genderOptions = listOf("男", "女", "无性别")
    SelectOrInputBox(
        label = "性别",
        options = genderOptions,
        currentValue = if (state.gender in genderOptions) state.gender else if (state.gender.isNotEmpty()) "自定义" else "",
        customValue = if (state.gender in genderOptions) "" else state.gender,
        onValueChange = { type, input ->
            val newValue = if (type == "自定义") input else type
            onUpdate(state.copy(gender = newValue))
        }
    )

    // --- 2. 年龄  ---
    Text(text = "年龄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.ageValue,
            onValueChange = { input ->
                if (input.all { it.isDigit() } && input.length <= 9) {
                    onUpdate(state.copy(ageValue = input))
                }
            },
            placeholder = { Text("数字") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        Spacer(modifier = Modifier.width(8.dp))

        var expanded by remember { mutableStateOf(false) }
        val units = listOf("纪元", "万年", "世纪", "岁", "月", "天", "小时")
        Box(modifier = Modifier.width(100.dp)) {
            OutlinedTextField(
                value = state.ageUnit,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
            // 透明遮罩点击
            Box(modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                units.forEach { unit ->
                    DropdownMenuItem(text = { Text(unit) }, onClick = { onUpdate(state.copy(ageUnit = unit)); expanded = false })
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))

    // --- 3. 身份/职业 ---
    val identityOptions = listOf("学生", "社畜", "医生", "杀手", "黑客", "魔法师", "骑士", "总裁")
    SelectOrInputBox(
        label = "身份 / 职业",
        options = identityOptions,
        currentValue = if (state.identity in identityOptions) state.identity else if (state.identity.isNotEmpty()) "自定义" else "",
        customValue = if (state.identity in identityOptions) "" else state.identity,
        onValueChange = { type, input ->
            onUpdate(state.copy(identity = if (type == "自定义") input else type))
        }
    )

    // --- 4. 性格特点 ---
    val personalityOptions = listOf("冷静果断", "温柔善良", "腹黑毒舌", "傲娇", "热血笨蛋", "社恐", "病娇")
    SelectOrInputBox(
        label = "性格特点",
        options = personalityOptions,
        currentValue = if (state.personality in personalityOptions) state.personality else if (state.personality.isNotEmpty()) "自定义" else "",
        customValue = if (state.personality in personalityOptions) "" else state.personality,
        onValueChange = { type, input ->
            onUpdate(state.copy(personality = if (type == "自定义") input else type))
        }
    )

    // --- 5. 外貌特征 ---
    val appearanceOptions = listOf("银发红瞳", "黑长直", "机械义肢", "兽耳娘/郎", "总是穿着风衣", "肌肉猛男")
    SelectOrInputBox(
        label = "外貌特征",
        options = appearanceOptions,
        currentValue = if (state.appearance in appearanceOptions) state.appearance else if (state.appearance.isNotEmpty()) "自定义" else "",
        customValue = if (state.appearance in appearanceOptions) "" else state.appearance,
        onValueChange = { type, input ->
            onUpdate(state.copy(appearance = if (type == "自定义") input else type))
        }
    )

    // --- 6. 身世与逻辑 (长文本) ---
    OutlinedTextField(
        value = state.backgroundStory,
        onValueChange = { onUpdate(state.copy(backgroundStory = it)) },
        label = { Text("身世背景") },
        placeholder = { Text("TA 的过去发生了什么？") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.behaviorLogic,
        onValueChange = { onUpdate(state.copy(behaviorLogic = it)) },
        label = { Text("行为逻辑") },
        placeholder = { Text("TA 遵循怎样的行为逻辑？\n例: 绝对理性，永远都不会发生情绪波动") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.dialogueStyle,
        onValueChange = { onUpdate(state.copy(dialogueStyle = it)) },
        label = { Text("对话风格 ") },
        placeholder = { Text("User: ...\nTA: ...") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )

    // 底部留白，防止被按钮遮挡
    Spacer(modifier = Modifier.height(80.dp))
}

// Step 3: 世界观设定 (选填)
@Composable
fun Step3WorldSetting(
    state: PersonaCreationState,
    onUpdate: (PersonaCreationState) -> Unit
) {
    WizardStepTitle(
        title = "世界观设定",
        subtitle = "TA 生活在一个怎样的世界？这将决定 AI 的常识范围。"
    )

    // --- 1. 世界观类型 ---
    val worldOptions = listOf(
        "现代现实", "魔法中世纪", "赛博朋克", "末日废土",
        "古代武侠", "克苏鲁神话", "太空科幻", "校园日常"
    )
    SelectOrInputBox(
        label = "世界观",
        options = worldOptions,
        currentValue = if (state.worldView in worldOptions) state.worldView else if (state.worldView.isNotEmpty()) "自定义" else "",
        customValue = if (state.worldView in worldOptions) "" else state.worldView,
        onValueChange = { type, input ->
            onUpdate(state.copy(worldView = if (type == "自定义") input else type))
        }
    )

    // --- 2. 历史背景 ---
    OutlinedTextField(
        value = state.history,
        onValueChange = { onUpdate(state.copy(history = it)) },
        label = { Text("历史背景") },
        placeholder = { Text("例: \n-21世纪，人类在奥尔特云发现了一个小型虫洞，但是这个虫洞及其不稳定，被联合国赞助的尤利西斯计划随之展开。\n-23世纪初，人类联邦正式成为星际文明，并开始寻找当年六艘方舟中秋菊号的姊妹舰，预计在3年后到达并支援发展的‵风信子号‘。") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
    )

    Spacer(modifier = Modifier.height(16.dp))

    // --- 3. 设定地图/地点 ---
    OutlinedTextField(
        value = state.location,
        onValueChange = { onUpdate(state.copy(location = it)) },
        label = { Text("地图/地点") },
        placeholder = { Text("例: \n-虚拟平台『PersonaAI』: 存在于虚拟云端的社交平台\n-空中都市『阿斯塔洛特』: 科技与财权的中心") },
        modifier = Modifier.fillMaxWidth()
    )
}


// Step 4: 形象与标签
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step4Appearance(
    state: PersonaCreationState,
    isGenerating: Boolean,
    presetAvatars: List<String>,
    onUpdate: (PersonaCreationState) -> Unit,
    onUploadAvatar: (android.net.Uri) -> Unit,
    onAiGenerate: () -> Unit,
    onSelectPreset: (String) -> Unit
) {
    WizardStepTitle(
        title = "形象与标签",
        subtitle = "一个好看的皮囊和精准的标签，能让 TA 在广场中脱颖而出。"
    )
    var showPresetDialog by remember { mutableStateOf(false) }
    // --- 1. 头像设定 ---
    Text("头像设定", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        // 头像预览
            Surface(
                shape = CircleShape,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(100.dp)
                    .border(1.dp, Color.LightGray, CircleShape)
            ) {
                if (isGenerating) {
                    // Loading 状态
                    Box(modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    }
                } else
                {
                    AsyncImage(
                        model = state.avatarUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

        Spacer(modifier = Modifier.width(24.dp))

        // 操作按钮区
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            val buttonWidth = 160.dp
            // A. 上传图片 Launcher
            val photoPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) onUploadAvatar(uri)
            }

            Button(
                onClick = {
                    photoPicker.launch("image/*")
                },
                modifier = Modifier.width(buttonWidth),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("上传图片")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // B. AI 生成
            OutlinedButton(
                onClick = onAiGenerate,
                enabled = !isGenerating, // 生成中禁用
                modifier = Modifier.width(buttonWidth),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("生成中...", fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("AI 生成", fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 3. 选择预设
            OutlinedButton(
                onClick = { showPresetDialog = true },
                modifier = Modifier.width(buttonWidth),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Face, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("选择预设", fontSize = 13.sp)
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(24.dp))

    // --- 2. 标签设定 (Tags) ---
    Text("添加标签 (Tags)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text("选择或输入标签，方便在广场被搜索到。", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    Spacer(modifier = Modifier.height(16.dp))

    // 预设标签
    val presetTags = listOf("赛博朋克", "古风", "现代", "悬疑", "治愈", "恋爱", "搞笑", "战斗", "助手")

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presetTags.forEach { tag ->
            val isSelected = state.tags.contains(tag)
            FilterChip(
                selected = isSelected,
                onClick = {
                    val newTags = if (isSelected) state.tags - tag else state.tags + tag
                    onUpdate(state.copy(tags = newTags))
                },
                label = { Text(tag) },
                leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null) } } else null
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 自定义标签输入
    Row(verticalAlignment = Alignment.CenterVertically) {
        var tempTag by remember { mutableStateOf("") }

        OutlinedTextField(
            value = tempTag,
            onValueChange = { tempTag = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("自定义标签...") },
            singleLine = true
        )
        IconButton(onClick = {
            if (tempTag.isNotBlank() && !state.tags.contains(tempTag)) {
                onUpdate(state.copy(tags = state.tags + tempTag))
                tempTag = ""
            }
        }) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }

    // 显示已选的自定义标签 (如果不在预设里)
    val customTags = state.tags.filter { it !in presetTags }
    if (customTags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            customTags.forEach { tag ->
                InputChip(
                    selected = true,
                    onClick = { onUpdate(state.copy(tags = state.tags - tag)) },
                    label = { Text(tag) },
                    trailingIcon = { Icon(Icons.Default.Close, null) }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(80.dp))
    val voiceNames = VoiceConstants.VOICE_PRESETS.values.toList()
    val currentVoiceName = VoiceConstants.VOICE_PRESETS[state.voiceId] ?: "未知"
    Text(text = "声音音色", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VoiceConstants.VOICE_PRESETS.forEach { (id, name) ->
            FilterChip(
                selected = (state.voiceId == id),
                onClick = {
                    onUpdate(state.copy(voiceId = id))
                },
                label = { Text(name) },
                leadingIcon = if (state.voiceId == id) {
                    { Icon(Icons.Default.Check, null) }
                } else null
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))


    // --- 弹窗：预设头像选择 ---
    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text("选择预设头像") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(250.dp) // 限制高度
                ) {
                    items(presetAvatars) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (state.avatarUri == uri) 3.dp else 0.dp,
                                    color = if (state.avatarUri == uri) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onSelectPreset(uri)
                                    showPresetDialog = false
                                }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetDialog = false }) { Text("取消") }
            }
        )
    }
}

// Step 5: 预览与发布
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step5Preview(state: PersonaCreationState) {
    WizardStepTitle(
        title = "确认信息",
        subtitle = "最后检查一遍，准备赋予 TA 生命！"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 头像
                val displayAvatar = if (state.avatarUri.isNotBlank()) state.avatarUri
                else "https://api.dicebear.com/9.x/bottts/png?seed=${state.name}"
                AsyncImage(
                    model = displayAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(state.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(state.description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Tags
            if (state.tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.tags.forEach {
                        Text("#$it", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 详细设定预览
            PreviewItem("身份", state.identity)
            PreviewItem("性别", state.gender)
            PreviewItem("年龄", if(state.ageValue.isNotBlank()) "${state.ageValue} ${state.ageUnit}" else "")
            PreviewItem("世界观", state.worldView)
            PreviewItem("说话风格", state.dialogueStyle)
            val voiceName = VoiceConstants.VOICE_PRESETS[state.voiceId] ?: "默认音色"
            PreviewItem("音色", voiceName)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 提示
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("发布后，TA 将出现在你的个人主页", style = MaterialTheme.typography.bodyMedium)
    }

    Spacer(modifier = Modifier.height(80.dp))
}

// 简单的预览条目组件
@Composable
fun PreviewItem(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(modifier = Modifier.padding(vertical = 4.dp)) {
            Text("$label：", fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
            Text(value)
        }
    }
}