package com.example.domain.provider

import android.util.Log
import com.example.data.local.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll

class ProviderManager(
    private val groqProvider: GroqProvider = GroqProvider(),
    private val geminiProvider: GeminiProvider = GeminiProvider()
) {
    suspend fun generateResponseStream(
        activeProviderName: String,
        groqApiKey: String?,
        geminiApiKey: String?,
        systemPrompt: String,
        history: List<ChatMessage>,
        currentMessage: String,
        onStatusUpdate: (String) -> Unit = {}
    ): Flow<String> = flow {
        val primaryProvider = if (activeProviderName == "Gemini") geminiProvider else groqProvider
        val primaryKey = if (activeProviderName == "Gemini") geminiApiKey else groqApiKey
        
        Log.d("FORGE_PROVIDER", "ProviderManager generateResponse called. activeProviderName: $activeProviderName, primaryKey is set: ${!primaryKey.isNullOrEmpty()}")
        try {
            if (primaryKey.isNullOrEmpty()) throw IllegalArgumentException("${primaryProvider.name} API Key is not set.")
            emitAll(primaryProvider.generateResponseStream(primaryKey, systemPrompt, history, currentMessage, onStatusUpdate))
        } catch (e: Exception) {
            Log.e("FORGE_PROVIDER", "Primary provider (${primaryProvider.name}) failed", e)
            if (primaryProvider.name == "Groq") {
                // Fallback to Gemini
                if (!geminiApiKey.isNullOrEmpty()) {
                    try {
                        onStatusUpdate("Fallback to Gemini")
                        emitAll(geminiProvider.generateResponseStream(geminiApiKey, systemPrompt, history, currentMessage, onStatusUpdate))
                    } catch (fallbackEx: Exception) {
                        throw Exception("Groq failed (${e.localizedMessage}) AND Gemini fallback failed (${fallbackEx.localizedMessage})")
                    }
                } else {
                    throw Exception("Groq failed (${e.localizedMessage}) and Gemini fallback is not configured.")
                }
            } else {
                throw e
            }
        }
    }
}
