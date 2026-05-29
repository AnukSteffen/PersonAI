package com.example.personai.domain.model

import androidx.room.Entity

@Entity(tableName = "follows", primaryKeys = ["userId", "personaId"])
data class Follow(
    val userId: String,
    val personaId: String,
    val timestamp: Long = System.currentTimeMillis()
)