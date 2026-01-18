package com.example.personai.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personas")
data class Persona(
    @PrimaryKey val id: String,
    val name: String,
    val description: String, // 角色描述
    val systemPrompt: String, // 角色人设，出场设定
    val dialogueStyle: String, //消息示例
    val avatarUrl: String,
    val createdAt: Long = System.currentTimeMillis(),
    val voiceId: String = "zh_female_linjianvhai_moon_bigtts", //邻家女孩
    val evolvedPersonality: String = "", // 动态设定 (共生)
    val currentStatus: String = "", //当前状态
    val creatorId: String = "system",

    // 统计字段
    val followerCount: Int = 0,    // 粉丝数
    val followingCount: Int = 0,    // 关注数
    val interactionCount: Int = 0, // 会话总数
    val tags: List<String> = emptyList(),
    // 向量数据
    val embedding: List<Float> = emptyList(),
)
