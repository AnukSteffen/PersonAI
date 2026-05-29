package com.example.personai.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface VolcengineApi {
    // 1. 生图接口，兼容OpenAI
    @POST("images/generations")
    suspend fun generateImage(
        @Header("Authorization") apiKey: String,
        @Body request: ImageGenerationRequest
    ): ImageGenerationResponse

    // 2. 创建视频任务
    @POST("contents/generations/tasks")
    @Headers("Content-Type: application/json")
    suspend fun createVideoTask(
        @Header("Authorization") apiKey: String,
        @Body request: VideoTaskRequest
    ): VideoTaskCreateResponse

    // 3. 查询任务状态
    @GET("contents/generations/tasks/{taskId}")
    suspend fun getVideoTaskStatus(
        @Header("Authorization") apiKey: String,
        @Path("taskId") taskId: String
    ): VideoTaskStatusResponse

    // 向量化接口
    @POST("embeddings")
    suspend fun createEmbeddings(
        @Header("Authorization") apiKey: String,
        @Body request: EmbeddingRequest
    ): EmbeddingResponse
}