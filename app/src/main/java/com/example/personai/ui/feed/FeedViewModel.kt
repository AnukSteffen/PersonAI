package com.example.personai.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personai.domain.repository.PersonaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: PersonaRepository
) : ViewModel() {

    // 1. 搜索词状态
    private val _searchQuery = MutableStateFlow("")

    // 2. Tab 状态 (0 = 推荐, 1 = 关注)
    private val _currentTab = MutableStateFlow(0)
    val currentTab = _currentTab.asStateFlow()

    // 3. 刷新状态
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    // 4. 核心数据流管道
    @OptIn(ExperimentalCoroutinesApi::class)
    val posts = combine(_searchQuery, _currentTab) { query, tab ->
        Pair(query, tab)
    }.flatMapLatest { (query, tabIndex) ->
        if (query.isNotBlank()) {
            // 有搜索词时，根据 Tab 分流
            if (tabIndex == 0) {
                // Tab 0 (推荐): 全局搜索
                repository.searchPosts(query)
            } else {
                // Tab 1 (关注): 只搜关注
                repository.searchFollowedPosts(query)
            }
        } else {
            // 无搜索词，正常浏览分流
            val type = if (tabIndex == 0) "recommend" else "follow"
            repository.getFeedPosts(type)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // 监听点赞状态
    val likedPostIds = repository.getMyLikedPostIds()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    // 点赞/取消点赞
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId)
        }
    }

    fun switchTab(index: Int) {
        _currentTab.value = index
    }

    fun hidePost(postId: String) {
        viewModelScope.launch {
            repository.hidePost(postId)
        }
    }

    fun undoHidePost(postId: String) {
        viewModelScope.launch {
            repository.unhidePost(postId)
        }
    }

    fun refreshPosts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(500)
            _isRefreshing.value = false
        }
    }

    fun onSearch(query: String) {
        _searchQuery.value = query
    }
}