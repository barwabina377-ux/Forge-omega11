package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GroqChatRequest(
    val messages: List<GroqMessage>,
    val model: String = "llama3-70b-8192", // We can use llama3-8b-8192 or 70b
    val temperature: Double = 0.7,
    @Json(name = "max_tokens") val maxTokens: Int = 2048,
    @Json(name = "top_p") val topP: Double = 1.0,
    val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GroqMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class GroqChatResponse(
    val id: String,
    val choices: List<GroqChoice>
)

@JsonClass(generateAdapter = true)
data class GroqChoice(
    val index: Int,
    val message: GroqMessage,
    @Json(name = "finish_reason") val finishReason: String?
)
