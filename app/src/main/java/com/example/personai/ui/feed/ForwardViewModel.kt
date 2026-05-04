package com.example.personai.ui.feed

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personai.domain.model.Persona
import com.example.personai.domain.model.Post
import com.example.personai.domain.repository.PersonaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForwardViewModel @Inject constructor(
    private val repository: PersonaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    // 帖子详情
    private val _post = MutableStateFlow<Post?>(null)
    val post = _post.asStateFlow()

    private val _chatPersonas = MutableStateFlow<List<Persona>>(emptyList())
    val chatPersonas = _chatPersonas.asStateFlow()

    // 已选中的 Persona ID 列表
    val selectedPersonaIds = mutableStateListOf<String>()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // 加载帖子
            _post.value = repository.getPostById(postId)

            val sessions = repository.getRecentChats()

            _chatPersonas.value = sessions.map { it.persona }
        }
    }

    fun toggleSelection(personaId: String) {
        if (selectedPersonaIds.contains(personaId)) {
            selectedPersonaIds.remove(personaId)
        } else {
            selectedPersonaIds.add(personaId)
        }
    }

    // 执行转发
    fun sendForward(comment: String, onSuccess: () -> Unit) {
        repository.forwardPostToPersonas(
            postId = postId,
            targetPersonaIds = selectedPersonaIds.toList(),
            comment = comment
        )
        onSuccess()
    }
}