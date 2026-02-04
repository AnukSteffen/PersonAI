package com.example.personai.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.personai.ui.theme.AppThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
val Context.dataStore by preferencesDataStore("user_prefs")

class UserStore @Inject constructor(@ApplicationContext private val context: Context ) {
    companion object {
        private val USER_ID_KEY = stringPreferencesKey("current_user_id")
        private val THEME_MODE_KEY = intPreferencesKey("app_theme_mode")
        private val FORCE_OFFLINE_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("force_offline")
    }

    // 获取当前登录的用户ID
    val userId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_ID_KEY]
    }
    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        val ordinal = prefs[THEME_MODE_KEY] ?: 0
        AppThemeMode.values().getOrElse(ordinal) { AppThemeMode.DEFAULT }
    }

    suspend fun saveThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[THEME_MODE_KEY] = mode.ordinal }
    }

    // 登录
    suspend fun saveUser(userId: String) {
        context.dataStore.edit { it[USER_ID_KEY] = userId }
    }

    // 登出
    suspend fun clearUser() {
        context.dataStore.edit { it.remove(USER_ID_KEY) }
    }

    val isForceOffline: Flow<Boolean> = context.dataStore.data.map { it[FORCE_OFFLINE_KEY] ?: false }

    suspend fun setForceOffline(isForce: Boolean) {
        context.dataStore.edit { it[FORCE_OFFLINE_KEY] = isForce }
    }
}