package com.example.personai.ui.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personai.domain.repository.PersonaRepository
import com.example.personai.ui.theme.AppThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    repository: PersonaRepository
) : ViewModel() {
    // 监听主题变化
    val themeMode = repository.getAppTheme()
        .stateIn(viewModelScope, SharingStarted.Lazily, AppThemeMode.DEFAULT)
}