package com.example.personai.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val authorId: String,
    // 作者类型
    val authorType: String,

    // 冗余存一下作者名和头像
    val authorName: String,
    val authorAvatar: String,
    val title: String,
    val content: String,
    val imageUrls: List<String>,
    val likeCount: Int = 0,
    val tags: List<String> = emptyList(),
    val commentCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)