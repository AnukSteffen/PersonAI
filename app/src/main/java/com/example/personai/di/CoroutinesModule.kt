package com.example.personai.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

// 全局协程作用域
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @ApplicationScope
    @Singleton
    @Provides
    fun providesApplicationScope(): CoroutineScope =
        // SupervisorJob 确保一个子任务失败不会导致整个 Scope 取消
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}