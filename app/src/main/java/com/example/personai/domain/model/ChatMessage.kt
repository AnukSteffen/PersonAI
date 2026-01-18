package com.example.personai.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class ChatMessage(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String,
    val personaId: String,
    val content: String,

    // 0: 纯文本
    // 1: 图片
    // 2: 视频
    // 3: 帖子分享
    // 4. 语音
    val type: Int = 0,

    val mediaPrompt: String? = null,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// 辅助类，用于“联系人页面”的显示
data class ChatSession(
    val persona: Persona,
    val lastMessage: String,
    val lastTime: Long
)