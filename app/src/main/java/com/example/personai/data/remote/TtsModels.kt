package com.example.personai.data.remote

import com.google.gson.annotations.SerializedName

data class TtsRequest(
    val app: TtsApp,
    val user: TtsUser,
    val audio: TtsAudio,
    val request: TtsRequestPayload
)
data class TtsV3Response(
    val reqid: String,
    val code: Int,
    val message: String,
    val sequence: Int,
    val data: String?
)

data class TtsResponse(
    val reqid: String,
    val code: Int,
    val message: String,
    val sequence: Int,
    val data: String?
)

data class TtsApp(
    val appid: String,
    val token: String,
    val cluster: String
)

data class TtsUser(
    val uid: String
)

data class TtsAudio(
    @SerializedName("voice_type") val voiceType: String,
    val encoding: String = "mp3",
    @SerializedName("speed_ratio") val speedRatio: Double = 1.0,
    @SerializedName("volume_ratio") val volumeRatio: Double = 1.0,
    @SerializedName("pitch_ratio") val pitchRatio: Double = 1.0
)

data class TtsRequestPayload(
    val reqid: String,
    val text: String,
    val operation: String = "query",
)