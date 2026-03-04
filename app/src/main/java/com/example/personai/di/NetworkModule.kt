package com.example.personai.di

import com.example.personai.data.remote.DouBaoApi
import com.example.personai.data.remote.TtsApi
import com.example.personai.data.remote.VolcengineApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    //sk-bd93831da66245c88292a651d95c6d9d
//    private const val DPSK_BASE_URL = "https://api.deepseek.com/"

    // 火山引擎 Base URL
    private const val VOLC_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3/"
    private const val TTS_BASE_URL = "https://openspeech.bytedance.com/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // 日志拦截器配置
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideDouBaoApi(client: OkHttpClient): DouBaoApi {
        return Retrofit.Builder()
            .baseUrl(VOLC_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DouBaoApi::class.java)
    }


    @Provides
    @Singleton
    fun provideVolcengineApi(okHttpClient: OkHttpClient): VolcengineApi {
        return Retrofit.Builder()
            .baseUrl(VOLC_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VolcengineApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTtsApi(okHttpClient: OkHttpClient): TtsApi {
        return Retrofit.Builder()
            .baseUrl(TTS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TtsApi::class.java)
    }
}