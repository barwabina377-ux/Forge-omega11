package com.example.domain.provider

import com.example.data.local.ChatMessage
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GeminiProvider : AIProvider {
    override val name: String = "Gemini"

    override suspend fun generateResponseStream(
        apiKey: String,
        systemPrompt: String,
        history: List<ChatMessage>,
        currentMessage: String,
        onStatusUpdate: (String) -> Unit
    ): Flow<String> = flow {
        val contents = mutableListOf<GeminiContent>()
        
        history.takeLast(20).forEach { msg ->
            if (msg.role != "system") {
                val role = if (msg.role == "user") "user" else "model"
                contents.add(GeminiContent(role = role, parts = listOf(GeminiPart(text = msg.content))))
            }
        }
        contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = currentMessage))))
        
        val request = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )
        
        onStatusUpdate("Request Sent")
        val response = GeminiClient.apiService.generateContent(apiKey, request)
        onStatusUpdate("Response Received")
        val fullText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from Gemini."
        
        onStatusUpdate("Streaming Response")
        val chunkRegex = "(?<=\\s)|(?=\\n)".toRegex()
        val chunks = fullText.split(chunkRegex)
        for(chunk in chunks) {
            emit(chunk)
            delay(15) // Simulate streaming delay
        }
    }
}
