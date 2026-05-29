package com.example.personai.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personai.data.manager.MediaStorageManager
import com.example.personai.domain.repository.PersonaRepository
import com.example.personai.ui.theme.AppThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: PersonaRepository,
    private val mediaManager: MediaStorageManager
) : ViewModel() {

    // 1. 当前用户信息
    val currentUser = repository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // 2. 统计数据
    private val _followerCount = MutableStateFlow(0)
    val followerCount = _followerCount.asStateFlow()

    val followingPersonas = repository.getFollowedPersonas()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val followingCount = followingPersonas.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // 3. 三大数据列表
    val myCreations = repository.getUserPersonas()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val myPosts = repository.getMyPosts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val historyItems = repository.getBrowsingHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 监听设备联网状态
    val isDeviceOnline = repository.isDeviceOnline()
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // 2. 监听用户设置：是否强制离线
    val isForceOffline = repository.isForceOffline()
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // 3. 切换模式
    fun setForceOffline(enable: Boolean) {
        viewModelScope.launch {
            repository.setForceOffline(enable)
        }
    }

    init {
        refreshStats()
    }

    private fun refreshStats() {
        viewModelScope.launch {
            val user = repository.getCurrentUser().first() ?: return@launch
            _followerCount.value = repository.getUserFollowerCount(user.id)
        }
    }

    // --- 交互逻辑 ---
    fun updateNickname(newName: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.updateNickname(user.id, newName)
        }
    }

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            // 1. 保存到沙盒
            val path = mediaManager.saveMediaToInternalStorage(uri)
            // 2. 更新数据库
            repository.updateUserAvatar(user.id, path)
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onLogoutSuccess()
        }
    }

    fun switchTheme(mode: AppThemeMode) {
        viewModelScope.launch {
            repository.setAppTheme(mode)
        }
    }
}