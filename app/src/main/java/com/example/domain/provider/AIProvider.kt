package com.example.domain.provider

import com.example.data.local.ChatMessage
import kotlinx.coroutines.flow.Flow

interface AIProvider {
    val name: String
    suspend fun generateResponseStream(
        apiKey: String,
        systemPrompt: String,
        history: List<ChatMessage>,
        currentMessage: String,
        onStatusUpdate: (String) -> Unit = {}
    ): Flow<String>
}
