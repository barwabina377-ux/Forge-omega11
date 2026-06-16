package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_history ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>

    @Insert
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_history WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Int)

    @Query("UPDATE chat_history SET title = :newTitle WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: String, newTitle: String)

    @Query("DELETE FROM chat_history")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    @Transaction
    suspend fun clearHistory() {
        deleteAllMessages()
        deleteAllSessions()
    }

    @Transaction
    suspend fun deleteSessionWithMessages(sessionId: String) {
        deleteMessagesForSession(sessionId)
        deleteSession(sessionId)
    }
}
