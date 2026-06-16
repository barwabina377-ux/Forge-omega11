package com.example.domain.provider

import android.util.Log
import com.example.data.local.ChatMessage
import com.example.data.remote.GroqChatRequest
import com.example.data.remote.GroqClient
import com.example.data.remote.GroqMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GroqProvider : AIProvider {
    override val name: String = "Groq"

    override suspend fun generateResponseStream(
        apiKey: String,
        systemPrompt: String,
        history: List<ChatMessage>,
        currentMessage: String,
        onStatusUpdate: (String) -> Unit
    ): Flow<String> = flow {
        val conversation = mutableListOf<GroqMessage>()
        conversation.add(GroqMessage("system", systemPrompt))
        
        // Include last 20 messages as context
        history.takeLast(20).forEach { msg ->
            if (msg.role != "system") {
                conversation.add(GroqMessage(msg.role, msg.content))
            }
        }
        conversation.add(GroqMessage("user", currentMessage))

        val request = GroqChatRequest(messages = conversation)
        Log.d("FORGE_NETWORK", "Groq request model: ${request.model}, messages count: ${request.messages.size}")
        
        onStatusUpdate("Request Sent")
        val response = GroqClient.apiService.createChatCompletion("Bearer $apiKey", request)
        onStatusUpdate("Response Received")
        
        Log.d("FORGE_NETWORK", "Groq response choices count: ${response.choices.size}")
        
        val fullText = response.choices.firstOrNull()?.message?.content ?: "No response from Groq."
        onStatusUpdate("Streaming Response")
        val chunkRegex = "(?<=\\s)|(?=\\n)".toRegex()
        val chunks = fullText.split(chunkRegex)
        for(chunk in chunks) {
            emit(chunk)
            delay(15) // Simulate streaming delay
        }
    }
}
