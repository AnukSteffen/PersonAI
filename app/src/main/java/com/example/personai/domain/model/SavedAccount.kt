package com.example.personai.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_accounts")
data class SavedAccount(
    @PrimaryKey val phone: String, // 手机号作为主键
    val password: String,          // 密码
    val nickname: String,          // 用于列表展示
    val avatarUrl: String,         // 用于列表展示
    val lastLoginTime: Long = System.currentTimeMillis()
)