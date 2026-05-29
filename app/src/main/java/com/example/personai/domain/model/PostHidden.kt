package com.example.personai.domain.model

import androidx.room.Entity

@Entity(tableName = "post_hidden", primaryKeys = ["userId", "postId"])
data class PostHidden(
    val userId: String,
    val postId: String,
    val timestamp: Long = System.currentTimeMillis()
)