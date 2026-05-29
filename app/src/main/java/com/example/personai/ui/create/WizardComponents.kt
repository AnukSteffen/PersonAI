package com.example.personai.ui.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WizardStepTitle(title: String, subtitle: String = "") {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

/**
 * 核心组件：选择或输入框
 * @param label 标题
 * @param options 预设选项列表
 * @param currentValue 当前选中的值
 * @param customValue 如果选了自定义，这里填写的自定义内容
 * @param onValueChange 当选中预设值，或输入自定义值时回调
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectOrInputBox(
    label: String,
    options: List<String>,
    currentValue: String,
    customValue: String,
    onValueChange: (String, String) -> Unit
) {
    // 判断当前是否处于"自定义"模式
    // 初始状态：如果当前值不为空且不在选项中，默认展开
    var isCustomExpanded by remember { mutableStateOf(currentValue.isNotEmpty() && currentValue !in options) }

    // 状态同步：当外部数据变化时，自动修正本地状态
    LaunchedEffect(currentValue) {
        if (currentValue in options) {
            // 如果选中了固定选项，强制收起输入框
            isCustomExpanded = false
        } else if (currentValue.isNotEmpty()) {
            // 如果有自定义值，强制展开
            isCustomExpanded = true
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // 1. 选项 Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // A. 渲染预设选项
            options.forEach { option ->
                FilterChip(
                    selected = (currentValue == option),
                    onClick = {
                        isCustomExpanded = false // 收起输入框
                        // 选中预设值，清空自定义内容
                        onValueChange(option, "")
                    },
                    label = { Text(option) },
                    leadingIcon = if (currentValue == option) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }

            // B. 渲染"自定义"按钮
            FilterChip(
                selected = isCustomExpanded,
                onClick = {
                    // 点击时：强制展开输入框
                    isCustomExpanded = true
                    onValueChange("自定义", customValue)
                },
                label = { Text("自定义") },
                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
        }

        // 2. 自定义输入框
        AnimatedVisibility(visible = isCustomExpanded) {
            OutlinedTextField(
                value = customValue,
                onValueChange = { input ->
                    // 输入变动时，当前值设为"自定义"，并更新自定义内容
                    onValueChange("自定义", input)
                },
                placeholder = { Text("请输入$label...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp)) // 组间距
}