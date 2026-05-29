package com.example.personai.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val phone: String, // 登录账号
    val password: String,
    val nickname: String,
    val avatarUrl: String = "android.resource://com.example.personai/drawable/default_offline_avatar"
)