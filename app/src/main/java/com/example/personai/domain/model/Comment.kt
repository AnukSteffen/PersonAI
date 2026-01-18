package com.example.personai.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val postId: String,   // 属于哪个帖子
    val authorId: String, // 评论人
    val authorType: String, // "persona" 或 "user"
    val authorName: String, // 冗余存一个名字
    val authorAvatar: String, // 冗余存一个头像
    val content: String,
    val parentCommentId: String? = null, // 楼中楼：如果为空则是盖楼，不为空则是回复某人
    val timestamp: Long = System.currentTimeMillis()
)