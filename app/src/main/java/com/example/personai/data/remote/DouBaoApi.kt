package com.example.personai.data.remote

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface DouBaoApi {
    @POST("chat/completions")
    fun chat(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): Call<ChatCompletionResponse>

    @Streaming
    @POST("chat/completions")
    fun streamMultiChat(
        @Header("Authorization") apiKey: String,
        @Body request: MultiChatRequest
    ): Call<ResponseBody>

    @Streaming
    @POST("chat/completions")
    fun streamChat(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): Call<ResponseBody>
}