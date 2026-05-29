package com.example.personai.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personai.domain.repository.PersonaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val repository: PersonaRepository
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    private val _currentPage = MutableStateFlow(0)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()
    @OptIn(ExperimentalCoroutinesApi::class)
    val personas = combine(_searchQuery, _currentPage) { query, page ->
        Pair(query, page)
    }.flatMapLatest { (query, page) ->
        if (query.isBlank()) {
            flow {
                // 1. 获取第 page 页的数据
                var list = repository.getRecommendedPersonas(page = page, pageSize = 10)
                if (list.isEmpty() && page > 0) {
                    // 重置回第 0 页
                    _currentPage.value = 0
                    // 重新获取第 0 页数据
                    list = repository.getRecommendedPersonas(page = 0, pageSize = 10)
                }
                emit(list)
            }
        } else {
            // 有搜索时，走搜索逻辑
            repository.searchPersonas(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun onSearch(query: String) {
        _searchQuery.value = query
        _currentPage.value = 0
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // 模拟一点点延迟，让转圈看得到
            delay(500)
            _currentPage.value += 1
            _isRefreshing.value = false
        }
    }
}