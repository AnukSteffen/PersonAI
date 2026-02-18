package com.example.personai.data.remote

import com.google.gson.annotations.SerializedName

// 1. 发送给 AI 的请求体
data class MultiChatRequest(
    val model: String = "ep-20260323181337-nr4sg",
    val messages: List<MultimodalMessageDto>,
    val stream: Boolean = true,
    val temperature: Double = 1.3
)

data class ChatRequest(
    val model: String = "ep-20260323181337-nr4sg",
    val messages: List<MessageDto>,
    val stream: Boolean = true,
    val temperature: Double = 1.3
)

data class MultimodalMessageDto(
    val role: String,
    val content: List<ContentPart>
)

data class ContentPart(
    val type: String,
    val text: String? = null,
    @SerializedName("image_url") val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    val url: String
)

// 2. 消息单元
data class MessageDto(
    val role: String,
    val content: String
)

// 3. AI 返回的流式响应片段
data class ChatStreamResponse(
    val id: String,
    val choices: List<StreamChoice>
)

data class StreamChoice(
    val delta: Delta,
    @SerializedName("finish_reason") val finishReason: String?
)

data class Delta(
    val content: String?
)

