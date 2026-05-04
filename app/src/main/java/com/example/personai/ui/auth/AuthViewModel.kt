package com.example.personai.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personai.R
import com.example.personai.domain.model.SavedAccount
import com.example.personai.domain.model.User
import com.example.personai.domain.repository.PersonaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: PersonaRepository
) : ViewModel() {

    // --- 登录状态 ---
    var loginPhone by mutableStateOf("")
    var loginPassword by mutableStateOf("")
    var loginError by mutableStateOf<String?>(null)

    // --- 注册状态 ---
    var regNickname by mutableStateOf("")
    var regPhone by mutableStateOf("")
    var regPassword by mutableStateOf("")
    var regConfirmPassword by mutableStateOf("")
    var regError by mutableStateOf<String?>(null)

    // 是否记住密码
    var rememberPassword by mutableStateOf(false)

    // 已保存的账号列表
    val savedAccounts = repository.getSavedAccounts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    // 登录逻辑
    fun login(onLoginSuccess: () -> Unit, onError: (String) -> Unit) {
        if (loginPhone.isBlank() || loginPassword.isBlank()) {
            onError("账号或密码不能为空")
            return
        }

        if (loginPhone.length != 11) {
            onError("请输入有效的手机号码！")
            return
        }

        viewModelScope.launch {
            // 1. 先检查账号是否存在
            val exists = repository.checkUserExists(loginPhone)
            if (!exists) {
                onError("该账号未注册")
                return@launch
            }

            // 2. 登录
            val user = repository.login(loginPhone, loginPassword)
            if (user != null) {
                if (rememberPassword) {
                    try {
                        repository.saveAccount(user, loginPassword)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                onLoginSuccess()
            } else {
                onError("密码错误")
            }
        }
    }

    // 用于从注册页跳转回来时预填充
    fun fillCredentials(phone: String, pass: String) {
        loginPhone = phone
        loginPassword = pass
        loginError = null
    }

    // 注册逻辑
    fun register(onRegisterSuccess: (String, String) -> Unit, onError: (String) -> Unit) {
        // 1. 基础校验
        if (regNickname.isBlank() || regPhone.isBlank() || regPassword.isBlank()) {
            onError("所有字段均为必填") // 弹窗提示
            return
        }
        if (regPhone.length != 11) {
            onError("请输入有效的手机号码！")
            return
        }
        if (regPassword != regConfirmPassword) {
            onError("两次输入的密码不一致") // 弹窗提示
            return
        }

        viewModelScope.launch {
            // 2. 构建用户对象
            val newUser = User(
                phone = regPhone,
                password = regPassword,
                nickname = regNickname,
                avatarUrl = "android.resource://com.example.personai/${R.drawable.default_offline_avatar}"
            )

            // 3. 调用仓库
            val result = repository.register(newUser)

            if (result != null) {
                // 成功
                onRegisterSuccess(regPhone, regPassword)
            } else {
                // 失败：调用 onError 触发弹窗
                onError("注册失败：该手机号已被注册")
            }
        }
    }

    // 快速登录
    fun quickLogin(account: SavedAccount, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = repository.login(account.phone, account.password)
            if (user != null) {
                // 更新一下最后登录时间
                repository.saveAccount(user, account.password)
                onLoginSuccess()
            }
        }
    }

    fun removeSavedAccount(phone: String) {
        viewModelScope.launch { repository.removeSavedAccount(phone) }
    }
}