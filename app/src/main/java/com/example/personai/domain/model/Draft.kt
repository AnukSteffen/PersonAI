package com.example.personai.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class Draft(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,

    // 草稿类型: 1 = 创作 Persona, 2 = 发布帖子 Post
    val type: Int,

    // 核心数据：将表单内容转为 JSON 字符串存储
    val contentJson: String,

    val lastModified: Long = System.currentTimeMillis()
)