package com.example.personai.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)