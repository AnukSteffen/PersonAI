package com.example.personai.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personai.domain.model.SavedAccount
import com.example.personai.domain.repository.PersonaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountSwitchViewModel @Inject constructor(
    private val repository: PersonaRepository
) : ViewModel() {

    // 1. 获取所有保存的账号
    val savedAccounts = repository.getSavedAccounts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 2. 获取当前登录的 User
    val currentUser = repository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // 3. 切换账号
    fun switchAccount(account: SavedAccount, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // 直接用保存的密码登录
            val user = repository.login(account.phone, account.password)
            if (user != null) {
                // 刷新最后登录时间
                repository.saveAccount(user, account.password)
                onSuccess()
            }
        }
    }

    // 4. 删除账号记录
    fun removeAccount(phone: String) {
        viewModelScope.launch {
            repository.removeSavedAccount(phone)
        }
    }
}