package com.example.personai.ui.contact

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.personai.ui.component.ManualToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    onChatClick: (String) -> Unit,
    viewModel: ContactViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadRecentChats()
    }

    Scaffold(
        topBar = {
            ManualToolbar(title = "聊天列表")
        }
    ) { padding ->
        // 内容区域
        if (sessions.isEmpty()) {
            // 处理空状态的 padding
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无聊天记录,去广场找人聊聊吧！", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(sessions) { session ->
                    ListItem(
                        modifier = Modifier.clickable { onChatClick(session.persona.id) },
                        leadingContent = {
                            AsyncImage(
                                model = session.persona.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        headlineContent = { Text(session.persona.name, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Text(session.lastMessage, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}