package com.example.personai.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "post_history")
data class PostHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val postId: String,
    val timestamp: Long = System.currentTimeMillis()
)