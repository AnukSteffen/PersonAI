package com.example.personai.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.personai.ui.component.ManualToolbar

@Composable
fun RelationshipScreen(
    initialTab: Int = 0, // 0=关注, 1=粉丝
    onBack: () -> Unit,
    onPersonaClick: (String) -> Unit,
    viewModel: RelationshipViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    val following by viewModel.following.collectAsState()
    val followers by viewModel.followers.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = { ManualToolbar(title = "人脉关系", onBack = onBack) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("我的关注") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("我的粉丝") })
            }

            LazyColumn {
                if (selectedTab == 0) {
                    // 关注列表
                    items(following) { persona ->
                        RelationshipItem(
                            avatar = persona.avatarUrl,
                            name = persona.name,
                            description = persona.description,
                            isFollowing = true,
                            onActionClick = { viewModel.unfollow(persona.id) },
                            onItemClick = { onPersonaClick(persona.id) }
                        )
                    }
                } else {
                    // 粉丝列表
                    items(followers) { user ->
                        RelationshipItem(
                            avatar = user.avatarUrl,
                            name = user.nickname,
                            description = "用户",
                            isFollowing = false,
                            showActionButton = false,
                            onItemClick = { /* 用户暂无主页 */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RelationshipItem(
    avatar: String,
    name: String,
    description: String,
    isFollowing: Boolean,
    showActionButton: Boolean = true,
    onActionClick: () -> Unit = {},
    onItemClick: () -> Unit = {}
) {
    ListItem(
        modifier = Modifier.clickable { onItemClick() },
        leadingContent = {
            AsyncImage(
                model = avatar, contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        headlineContent = { Text(name) },
        supportingContent = { Text(description, maxLines = 1) },
        trailingContent = {
            if (showActionButton) {
                Button(
                    onClick = onActionClick,
                    colors = if(isFollowing) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors()
                ) {
                    Text(if(isFollowing) "已关注" else "关注")
                }
            }
        }
    )
}