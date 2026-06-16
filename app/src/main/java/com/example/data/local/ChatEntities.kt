package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "chat_history")
data class ChatSession(
    @PrimaryKey val id: String,
    val title: String,
    val timestamp: Long,
    val mode: String
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: String,
    val role: String, // "user", "assistant"
    val content: String,
    val timestamp: Long
)
