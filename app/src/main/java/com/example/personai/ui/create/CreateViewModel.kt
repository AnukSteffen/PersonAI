package com.example.personai.ui.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personai.data.manager.MediaStorageManager
import com.example.personai.domain.model.Persona
import com.example.personai.domain.model.VoiceConstants
import com.example.personai.domain.repository.PersonaRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateViewModel @Inject constructor(
    private val repository: PersonaRepository,
    private val mediaManager: MediaStorageManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    var uiState by mutableStateOf(PersonaCreationState())
        private set

    fun updateState(transform: (PersonaCreationState) -> PersonaCreationState) {
        uiState = transform(uiState)
    }

    // 头像生成loading状态
    var isGeneratingAvatar by mutableStateOf(false)
        private set

    val presetAvatars = listOf(
        "android.resource://${context.packageName}/drawable/default_offline_avatar",
        "android.resource://${context.packageName}/drawable/default_offline_female",
        "android.resource://${context.packageName}/drawable/default_offline_male",
        "android.resource://${context.packageName}/drawable/default_offline_geek",
        "android.resource://${context.packageName}/drawable/default_offline_giant",
        "android.resource://${context.packageName}/drawable/default_offline_beast",
        "android.resource://${context.packageName}/drawable/default_offline_robot",
        "android.resource://${context.packageName}/drawable/default_offline_thief",
        "android.resource://${context.packageName}/drawable/default_offline_wizard",
    )

    init {
        loadDraft()
    }

    private fun loadDraft() {
        viewModelScope.launch {
            val json = repository.getDraft(type = 1) // 1 = Persona Draft
            if (json != null) {
                try {
                    // 直接反序列化回 State 对象，恢复所有填写内容
                    uiState = Gson().fromJson(json, PersonaCreationState::class.java)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    fun saveDraft(onSaved: () -> Unit) {
        viewModelScope.launch {
            val json = Gson().toJson(uiState)
            repository.saveDraft(type = 1, contentJson = json)
            clearData()
            onSaved()
        }
    }

    fun hasContent(): Boolean {
        return uiState.name.isNotBlank() || uiState.description.isNotBlank()
    }

    // --- 图片处理 ---
    fun onAvatarSelected(uri: android.net.Uri) {
        viewModelScope.launch {
            val savedPath = mediaManager.saveImageToInternalStorage(uri)
            updateState { it.copy(avatarUri = savedPath) }
        }
    }

    fun selectPresetAvatar(uri: String) {
        updateState { it.copy(avatarUri = uri) }
    }

    fun generateAiAvatar() {
        if (uiState.description.isBlank() && uiState.name.isBlank()) return // 没素材不生成

        viewModelScope.launch {
            isGeneratingAvatar = true

            // 用名字和简介作为提示词
            val prompt = "Name: ${uiState.name}, Description: ${uiState.description}"
            val path = repository.generateAvatarImage(prompt)

            if (path != null) {
                updateState { it.copy(avatarUri = path) }
            } else {
            }

            isGeneratingAvatar = false
        }
    }

    // 拼装 System Prompt
    private fun generateSystemPrompt(): String {
        val s = uiState
        val sb = StringBuilder()

        // 有内容才拼接
        fun appendIfNotEmpty(label: String, value: String) {
            if (value.isNotBlank()) sb.append("<$label> $value\n")
        }

        // 1. 世界观部分
        if (s.worldView.isNotBlank() || s.history.isNotBlank() || s.location.isNotBlank()) {
            sb.append("【世界观设定】\n")
            appendIfNotEmpty("世界观", s.worldView)
            appendIfNotEmpty("历史背景", s.history)
            appendIfNotEmpty("当前地点/地图", s.location)
            sb.append("\n")
        }

        // 2. 角色设定部分
        sb.append("【角色设定：${s.name}】\n")

        val finalGender = s.gender
        if (finalGender.isNotBlank()) sb.append("<性别> $finalGender\n")

        if (s.ageValue.isNotBlank()) {
            sb.append("<年龄> ${s.ageValue}${s.ageUnit}\n")
        }
        appendIfNotEmpty("身份", s.identity)
        appendIfNotEmpty("性格特点", s.personality)
        appendIfNotEmpty("外貌特征", s.appearance)
        appendIfNotEmpty("身世背景", s.backgroundStory)
        appendIfNotEmpty("行为逻辑", s.behaviorLogic)
        appendIfNotEmpty("对话风格", s.dialogueStyle)
        val voiceName = VoiceConstants.VOICE_PRESETS[s.voiceId] ?: "默认音色"
        sb.append("<音色设定> $voiceName\n")

        if (sb.isEmpty()) sb.append("无详细设定。")
        return sb.toString().trim()
    }

    // --- 发布逻辑 ---
    fun publishPersona(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        if (uiState.name.isBlank()) { onError("昵称不能为空"); return }
        if (uiState.description.isBlank()) { onError("简介不能为空"); return }

        viewModelScope.launch {
            val currentUser = repository.getCurrentUser().first()
            if (currentUser == null) {
                onError("未登录，无法发布")
                return@launch
            }

            // 拼装最终实体
            val newPersona = Persona(
                id = UUID.randomUUID().toString(),
                creatorId = currentUser.id,
                name = uiState.name,
                description = uiState.description,

                systemPrompt = generateSystemPrompt(),
                dialogueStyle = uiState.dialogueStyle,
                tags = uiState.tags,
                avatarUrl = uiState.avatarUri,
                voiceId = uiState.voiceId
            )

            // 存库 & 清草稿
            repository.addPersona(newPersona)
            repository.clearDraft(type = 1)
            clearData()
            uiState = PersonaCreationState()
            launch(Dispatchers.IO) {
                repository.generateAndSavePersonaEmbedding(newPersona.id) //后台线程生成并保存角色的嵌入向量 ，用于后续的语义搜索和推荐功能
            }
            onSuccess(newPersona.id)
        }
    }

    fun autoGeneratePersona(onGenerated: () -> Unit) {
        val currentName = uiState.name
        isGeneratingAvatar = true //显示加载状态

        viewModelScope.launch {
            val voiceOptionsStr = VoiceConstants.VOICE_PRESETS.values.joinToString(", ") //1.音色选项
            val attrs = repository.generatePersonaAttributes(currentName, voiceOptionsStr) //2.AI去生成角色属性
            val newName = attrs["昵称"] ?: currentName
            val newDesc = attrs["简介"] ?: ""

            val prompt = "Character: $newName, $newDesc"
            val generatedAvatarPath = repository.generateAvatarImage(prompt) //3.依据角色属性让AI去生成头像
            val finalAvatar = generatedAvatarPath
                ?: "android.resource://${context.packageName}/drawable/default_offline_avatar"// 离线默认
            val aiVoiceName = attrs["音色"] ?: ""
            val matchedVoiceId = VoiceConstants.getIdByName(aiVoiceName)//4.依据角色属性中的音色去匹配音色列表
            updateState { currentState -> //5.更新状态
                currentState.copy(
                    name = attrs["昵称"] ?: currentName,
                    description = attrs["简介"] ?: "AI 还没想好简介...",
                    gender = attrs["性别"] ?: "自定义",
                    ageValue = attrs["年龄"]?.filter { it.isDigit() } ?: "18",
                    ageUnit = attrs["年龄"]?.filter { !it.isDigit() }?.trim() ?: "岁",
                    identity = attrs["身份"] ?: "",
                    personality = attrs["性格"] ?: "",
                    appearance = attrs["外貌"] ?: "",
                    backgroundStory = attrs["身世背景"] ?: "",
                    behaviorLogic = attrs["行为逻辑"] ?: "",
                    dialogueStyle = attrs["对话风格"] ?: "",
                    worldView = attrs["世界观"] ?: "",
                    history = attrs["历史背景"] ?: "",
                    location = attrs["地图"] ?: "",
                    tags = attrs["标签"]?.split(",", "，", " ","、")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                    voiceId = matchedVoiceId,
                    avatarUri = finalAvatar
                )
            }
            isGeneratingAvatar = false
            onGenerated()
        }
    }

    fun clearData() {
        // 重置为初始空状态
        uiState = PersonaCreationState()
        isGeneratingAvatar = false
    }

    fun discardDraft() {
        viewModelScope.launch {
            // 1. 从数据库物理删除草稿
            repository.clearDraft(type = 1)

            // 2. 清空内存状态
            clearData()
        }
    }
}