package com.example.personai.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personai.domain.model.Persona
import com.example.personai.domain.repository.PersonaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonaProfileViewModel @Inject constructor(
    private val repository: PersonaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val personaId: String = checkNotNull(savedStateHandle["personaId"])

    val currentUser = repository.getCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )
    // 角色信息
    private val _persona = MutableStateFlow<Persona?>(null)
    val persona = _persona.asStateFlow()

    private val _creatorName = MutableStateFlow("")
    val creatorName = _creatorName.asStateFlow()

    // 发布的帖子
    val posts = repository.getPostsByAuthor(personaId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // 是否已关注
    private val _isFollowing = MutableStateFlow(false)
    val isFollowing = repository.isFollowing(personaId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    val likedPostIds = repository.getMyLikedPostIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val isDeviceOnline = repository.isDeviceOnline()
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val isForceOffline = repository.isForceOffline()
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val generatingStatus = repository.generatingStatus

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // 加载角色详情
            val fetchedPersona = repository.getPersonaById(personaId)
            _persona.value = fetchedPersona

            if (fetchedPersona != null) {
                if (fetchedPersona.creatorId == "system") {
                    _creatorName.value = "官方"
                } else {
                    // 去数据库查这个用户的昵称
                    val user = repository.getUserById(fetchedPersona.creatorId)
                    _creatorName.value = user?.nickname ?: "未知用户"
                }
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            if (isFollowing.value) {
                repository.unfollow(personaId)
            } else {
                repository.follow(personaId)
            }
            _persona.value = repository.getPersonaById(personaId)
        }
    }

    // 点赞操作
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId)
        }
    }

    fun triggerAutoPost(mode: Int) {
        viewModelScope.launch {
            repository.triggerPersonaPost(personaId, mode)
        }
    }
}