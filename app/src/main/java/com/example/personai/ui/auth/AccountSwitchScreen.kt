package com.example.personai.ui.auth

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.personai.domain.model.SavedAccount
import com.example.personai.ui.component.ManualToolbar

@Composable
fun AccountSwitchScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onAddAccount: () -> Unit, // 跳转去登录页添加新号
    viewModel: AccountSwitchViewModel = hiltViewModel()
) {
    val accounts by viewModel.savedAccounts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<SavedAccount?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = {
            ManualToolbar(title = "切换账号", onBack = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accounts) { account ->
                    val isCurrent = currentUser?.phone == account.phone
                    AccountItem(
                        account = account,
                        isCurrent = isCurrent,
                        onClick = {
                            if (!isCurrent) {
                                viewModel.switchAccount(account, onSuccess = onLoginSuccess)
                            }
                        },
                        onDelete = {
                            accountToDelete = account
                            showDeleteDialog = true
                        }
                    )
                }
            }

            // 底部：添加账号按钮
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = onAddAccount,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("添加 / 登录其他账号")
                    }
                }
            }
        }
    }
    if (showDeleteDialog && accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("移除账号") },
            text = {
                Text("确定要移除账号 \"${accountToDelete?.nickname}\" 吗？\n移除后将无法使用该账号快速登录。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 确认删除：调用 ViewModel
                        viewModel.removeAccount(accountToDelete!!.phone)
                        showDeleteDialog = false
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("移除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun AccountItem(
    account: SavedAccount,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = { if (!isCurrent) onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isCurrent) 0.dp else 2.dp),
        border = if (isCurrent) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = account.avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(account.nickname, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(account.phone, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }

            if (isCurrent) {
                // 当前账号显示对勾
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            } else {
                // 其他账号显示删除
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                }
            }
        }
    }
}