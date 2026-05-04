package com.example.personai.ui.chat

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personai.data.manager.MediaStorageManager
import com.example.personai.domain.model.ChatMessage
import com.example.personai.domain.model.Persona
import com.example.personai.domain.model.Post
import com.example.personai.domain.repository.PersonaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: PersonaRepository,
    private val mediaManager: MediaStorageManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    var replyLengthLevel by mutableStateOf(1)
    private val personaId: String = checkNotNull(savedStateHandle["personaId"])
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()
    val generatingStatus = repository.generatingStatus
    private val _currentPersona = MutableStateFlow<Persona?>(null)
    val currentPersona = _currentPersona.asStateFlow()
    private val _pendingMediaPath = MutableStateFlow<String?>(null)
    val pendingMediaPath = _pendingMediaPath.asStateFlow()
    init {
        loadPersona()
        observeMessages()
    }

    private fun loadPersona() {
        viewModelScope.launch {
            _currentPersona.value = repository.getPersonaById(personaId)
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            repository.getMessagesFlow(personaId).collect { dbMessages ->
                _messages.value = dbMessages
            }
        }
    }

    fun sendMessage(text: String) { //处理用户发送的文本消息并触发 AI 回复
        if (text.isBlank()) return
        android.util.Log.e("AI_DEBUG", "ViewModel: sendMessage 被调用: $text")
        viewModelScope.launch {
            val currentUser = repository.getCurrentUser().first() ?: return@launch
            val userId = currentUser.id
            repository.chatStream(personaId, text , lengthLevel = 0).collect { partialResponse -> //获取 AI 流式回复
                updateStreamingMessage(partialResponse, userId)
            }
            val totalCount = repository.getMessageCount(personaId)

            if (totalCount > 0 && totalCount % 7 == 0) {
                repository.updatePersonaEvolution(personaId) //每 7 条消息触发一次 AI 角色的进化
            }
            else if (totalCount > 0 && totalCount % 3 == 0) {
                repository.updatePersonaStatus(personaId) //每 3 条消息更新一次 AI 角色的状态
            }
        }
    }

    private fun updateStreamingMessage(newContent: String, userId: String) {
        _messages.update { currentList ->
            val newList = currentList.toMutableList()

            val lastMsg = newList.lastOrNull()

            if (lastMsg != null && !lastMsg.isUser && lastMsg.type == 0) {
                // 情况 A: 更新内容
                newList[newList.lastIndex] = lastMsg.copy(content = newContent)
            } else {
                // 情况 B: 新增消息
                newList.add(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        personaId = personaId,
                        content = newContent,
                        isUser = false,
                        type = 0
                    )
                )
            }

            newList
        }
    }

    // 暂存多媒体
    fun onMediaPicked(uri: Uri) {
        viewModelScope.launch {
            val path = mediaManager.saveMediaToInternalStorage(uri)
            _pendingMediaPath.value = path
        }
    }

    fun cancelMediaSend() {
        _pendingMediaPath.value = null
    }

    fun confirmMediaSend(text: String) {
        val path = _pendingMediaPath.value ?: return
        _pendingMediaPath.value = null

        viewModelScope.launch {
            // A. 先发送媒体消息 存库
            val isVideo = mediaManager.isVideo(path)
            val msgType = if (isVideo) 2 else 1
            repository.sendMediaMessage(personaId, path, type = msgType)

            val triggerText = text.ifBlank { "(发送了一张图片/视频)" }

            if (text.isNotBlank()) {
                // chatStream 内部会存入 text 消息
                repository.chatStream(personaId, text, lengthLevel = replyLengthLevel).collect { partial ->
                    updateStreamingMessage(partial, repository.getCurrentUser().first()?.id ?: "")
                }
            } else {

                repository.chatStream(personaId, "（用户发送了媒体文件）", lengthLevel = replyLengthLevel).collect { partial ->
                    updateStreamingMessage(partial, repository.getCurrentUser().first()?.id ?: "")
                }
            }

            // 进化计数
            val totalCount = repository.getMessageCount(personaId)
            if (totalCount > 0 && totalCount % 7 == 0) {
                repository.updatePersonaEvolution(personaId)
            }
            else if (totalCount > 0 && totalCount % 3 == 0) {
                repository.updatePersonaStatus(personaId)
            }
        }
    }

    // 发送多媒体
    fun sendMedia(uri: Uri) {
        viewModelScope.launch {
            // 1. 保存到私有目录
            val path = mediaManager.saveMediaToInternalStorage(uri)
            val isVideo = mediaManager.isVideo(path)
            val msgType = if (isVideo) 2 else 1
            // 2. 乐观更新 UI
            val currentUser = repository.getCurrentUser().first() ?: return@launch

            // 3. 存入数据库
            repository.sendMediaMessage(personaId, path, type = msgType)

        }
    }

    // 获取帖子详情
    private val _postCache = mutableStateMapOf<String, Post>()

    suspend fun getPostDetails(postId: String): Post? {
        if (_postCache.containsKey(postId)) {
            return _postCache[postId]
        }
        val post = repository.getPostById(postId)
        if (post != null) {
            _postCache[postId] = post
        }
        return post
    }
}