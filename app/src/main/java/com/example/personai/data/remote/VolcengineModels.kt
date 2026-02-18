package com.example.personai.data.remote

import com.google.api.Usage
import com.google.gson.annotations.SerializedName

// --- 生图相关 ---
data class ImageGenerationRequest(
    val model: String,
    val prompt: String,
    @SerializedName("response_format") val responseFormat: String = "url",
    val size: String = "1024x1024" // 分辨率
)

data class ImageGenerationResponse(
    val created: Long,
    val data: List<ImageData>
)

data class ImageData(
    val url: String?
)

// --- 生视频相关  ---
data class VideoTaskRequest(
    val model: String,
    val content: List<VideoContentItem>
)

data class VideoContentItem(
    val type: String = "text",
    val text: String
)

data class VideoTaskCreateResponse(
    val id: String,
    val status: String?
)

data class VideoTaskStatusResponse(
    val id: String,
    val status: String,
    val content: VideoOutput?
)


data class VideoOutput(
    @SerializedName("video_url") val videoUrl: String?,
    @SerializedName("code") val code: Int?,
    @SerializedName("message") val message: String?
)
data class ChatCompletionResponse(
    val id: String,
    val model: String,
    val created: Long,
    val choices: List<CompletionChoice>,
    val usage: Usage
)

data class CompletionChoice(
    val index: Int,
    val message: CompletionMessage,
    @SerializedName("finish_reason") val finishReason: String
)

data class CompletionMessage(
    val role: String,
    val content: String
)

// 向量化
data class EmbeddingRequest(
    val model: String,
    val input: List<String>,
    val encoding_format: String = "float"
)

data class EmbeddingResponse(
    val data: List<EmbeddingData>,
    val usage: Usage?
)

data class EmbeddingData(
    val index: Int,
    val embedding: List<Float>
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)
