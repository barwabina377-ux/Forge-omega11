package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.domain.provider.ProviderManager
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val chatDao: ChatDao,
    private val providerManager: ProviderManager
) {
    fun getAllSessions(): Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> = chatDao.getMessagesForSession(sessionId)

    suspend fun insertSession(session: ChatSession) = chatDao.insertSession(session)

    suspend fun insertMessage(message: ChatMessage) = chatDao.insertMessage(message)

    suspend fun deleteMessage(messageId: Int) = chatDao.deleteMessage(messageId)

    suspend fun deleteSession(sessionId: String) = chatDao.deleteSessionWithMessages(sessionId)

    suspend fun clearHistory() = chatDao.clearHistory()

    suspend fun renameSession(sessionId: String, newTitle: String) = chatDao.updateSessionTitle(sessionId, newTitle)

    suspend fun createChatCompletionStream(
        activeProviderName: String,
        groqApiKey: String?,
        geminiApiKey: String?,
        systemPrompt: String,
        history: List<ChatMessage>,
        currentMessage: String,
        onStatusUpdate: (String) -> Unit = {}
    ): Flow<String> {
        return providerManager.generateResponseStream(activeProviderName, groqApiKey, geminiApiKey, systemPrompt, history, currentMessage, onStatusUpdate)
    }
}
