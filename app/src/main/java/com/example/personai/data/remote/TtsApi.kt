package com.example.personai.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface TtsApi {
    @POST("api/v1/tts")
    suspend fun generateAudio(
        @Header("Authorization") authorization: String,
        @Body request: TtsRequest
    ): Response<TtsResponse>
}