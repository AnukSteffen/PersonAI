package com.example.personai.domain.model

import androidx.room.Entity

@Entity(tableName = "post_likes", primaryKeys = ["postId", "userId"])
data class PostLike(
    val postId: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)