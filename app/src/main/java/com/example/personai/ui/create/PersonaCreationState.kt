package com.example.personai.ui.create

import com.example.personai.domain.model.VoiceConstants

data class PersonaCreationState(
    // --- Step 1: 基础信息 (必填) ---
    val name: String = "",
    val description: String = "",

    // --- Step 2: 详细设定 (选填) ---
    val gender: String = "",
    val ageValue: String = "",
    val ageUnit: String = "岁",
    val identity: String = "",
    val personality: String = "",
    val appearance: String = "",
    val backgroundStory: String = "",
    val behaviorLogic: String = "",
    val dialogueStyle: String = "",
    val voiceId: String = VoiceConstants.DEFAULT_VOICE_ID,
    // --- Step 3: 世界观 (选填) ---
    val worldView: String = "", // 世界观
    val history: String = "", // 历史背景
    val location: String = "", // 地图/地点

    // --- Step 4: 形象与标签 ---
    val avatarUri: String = "android.resource://com.example.personai/drawable/default_offline_avatar",
    val tags: List<String> = emptyList(), // 显性 Tag

)
