package com.example.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.data.repository.ChatRepository
import com.example.domain.PromptEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: ChatRepository,
    private val groqApiKeyFlow: Flow<String?>,
    private val geminiApiKeyFlow: Flow<String?>,
    private val activeProviderFlow: Flow<String>
) : ViewModel() {

    val activeProviderName: StateFlow<String> = activeProviderFlow.stateIn(
        scope = viewModelScope,
        initialValue = "Groq",
        started = SharingStarted.WhileSubscribed(5000)
    )

    val allSessions: Flow<List<ChatSession>> = repository.getAllSessions()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentMode = MutableStateFlow(PromptEngine.MODE_CREATOR_OS)
    val currentMode: StateFlow<String> = _currentMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _debugStatus = MutableStateFlow("")
    val debugStatus: StateFlow<String> = _debugStatus.asStateFlow()

    private val _streamingMessage = MutableStateFlow("")
    val streamingMessage: StateFlow<String> = _streamingMessage.asStateFlow()

    private var currentGroqApiKey: String? = null
    private var currentGeminiApiKey: String? = null
    private var currentActiveProvider: String = "Groq"
    
    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            groqApiKeyFlow.collect { key -> currentGroqApiKey = key }
        }
        viewModelScope.launch {
            geminiApiKeyFlow.collect { key -> currentGeminiApiKey = key }
        }
        viewModelScope.launch {
            activeProviderFlow.collect { provider -> currentActiveProvider = provider }
        }
        viewModelScope.launch {
            _currentSessionId.collect { sessionId ->
                if (sessionId != null) {
                    repository.getMessagesForSession(sessionId).collect { msgs ->
                        _messages.value = msgs
                    }
                } else {
                    _messages.value = emptyList()
                }
            }
        }
    }

    fun setMode(mode: String) {
        _currentMode.value = mode
    }

    fun loadSession(sessionId: String) {
        _currentSessionId.value = sessionId
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
            }
        }
    }

    fun startNewSession() {
        _currentSessionId.value = null
        _messages.value = emptyList()
    }

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        _isLoading.value = false
        if (_streamingMessage.value.isNotEmpty()) {
            val sessionId = _currentSessionId.value
            if (sessionId != null) {
                addMessageLocally("assistant", _streamingMessage.value + " [Stopped]", sessionId)
            }
        }
        _streamingMessage.value = ""
        _debugStatus.value = "Generation Stopped"
    }

    fun sendMessage(content: String) {
        Log.d("FORGE_CHAT", "sendMessage called with content: $content")
        val sessionId = _currentSessionId.value ?: createNewSession(content).also { _currentSessionId.value = it }

        addMessageLocally("user", content, sessionId)

        generationJob = viewModelScope.launch {
            _isLoading.value = true
            _streamingMessage.value = ""
            _debugStatus.value = "Request Started"
            Log.d("FORGE_CHAT", "isLoading set to true, starting API request")
            try {
                val systemPrompt = PromptEngine.getSystemPromptForMode(_currentMode.value)
                Log.d("FORGE_CHAT", "Mode: ${_currentMode.value}, SystemPrompt length: ${systemPrompt.length}")
                
                Log.d("FORGE_CHAT", "Calling repository.createChatCompletionStream")
                _debugStatus.value = "Provider Selected: $currentActiveProvider"
                
                repository.createChatCompletionStream(
                    activeProviderName = currentActiveProvider,
                    groqApiKey = currentGroqApiKey,
                    geminiApiKey = currentGeminiApiKey,
                    systemPrompt = systemPrompt,
                    history = _messages.value,
                    currentMessage = content,
                    onStatusUpdate = { status -> _debugStatus.value = status }
                ).collect { chunk ->
                    _streamingMessage.value += chunk
                }
                
                _debugStatus.value = "Parse Complete"

                addMessageLocally("assistant", _streamingMessage.value, sessionId)
                _streamingMessage.value = ""
                _debugStatus.value = "UI Updated"

            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d("FORGE_CHAT", "Generation Cancelled")
            } catch (e: Exception) {
                Log.e("FORGE_CHAT", "Error in sendMessage: ", e)
                val displayError = mapError(e)
                addMessageLocally("system", displayError, sessionId)
                _debugStatus.value = displayError
                _streamingMessage.value = ""
            } finally {
                _isLoading.value = false
                Log.d("FORGE_CHAT", "isLoading set to false, request completed")
            }
        }
    }
    
    private fun mapError(e: Exception): String {
        val msg = e.localizedMessage?.lowercase() ?: ""
        return when {
            msg.contains("401") || msg.contains("403") -> "Error: Invalid API Key. Please check your settings."
            msg.contains("429") -> "Error: Rate limit exceeded. Please wait a moment."
            msg.contains("timeout") -> "Error: Network timeout. The provider timed out."
            else -> "Error: ${e.localizedMessage}"
        }
    }

    private fun createNewSession(firstUserMsg: String): String {
        val newId = UUID.randomUUID().toString()
        val title = if (firstUserMsg.length > 30) firstUserMsg.take(27) + "..." else firstUserMsg
        viewModelScope.launch {
            repository.insertSession(
                ChatSession(
                    id = newId,
                    title = title,
                    timestamp = System.currentTimeMillis(),
                    mode = _currentMode.value
                )
            )
        }
        return newId
    }

    private fun addMessageLocally(role: String, content: String, sessionId: String? = _currentSessionId.value) {
        val sid = sessionId ?: return
        viewModelScope.launch {
            val msg = ChatMessage(
                sessionId = sid,
                role = role,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(msg)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _currentSessionId.value = null
            _messages.value = emptyList()
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameSession(sessionId, newTitle)
        }
    }

    fun regenerateMessage(aiMessage: ChatMessage) {
        val index = _messages.value.indexOf(aiMessage)
        if (index > 0) {
            val userMsg = _messages.value[index - 1]
            if (userMsg.role == "user") {
                generationJob?.cancel()
                generationJob = viewModelScope.launch {
                    repository.deleteMessage(aiMessage.id)
                    
                    _isLoading.value = true
                    _streamingMessage.value = ""
                    _debugStatus.value = "Regenerating..."
                    try {
                        val systemPrompt = PromptEngine.getSystemPromptForMode(_currentMode.value)
                        
                         repository.createChatCompletionStream(
                            activeProviderName = currentActiveProvider,
                            groqApiKey = currentGroqApiKey,
                            geminiApiKey = currentGeminiApiKey,
                            systemPrompt = systemPrompt,
                            history = _messages.value,
                            currentMessage = userMsg.content,
                            onStatusUpdate = { status -> _debugStatus.value = status }
                        ).collect { chunk ->
                            _streamingMessage.value += chunk
                        }

                        addMessageLocally("assistant", _streamingMessage.value, aiMessage.sessionId)
                        _streamingMessage.value = ""

                    } catch (e: kotlinx.coroutines.CancellationException) {
                        Log.d("FORGE_CHAT", "Regenerate Cancelled")
                    } catch (e: Exception) {
                        Log.e("FORGE_CHAT", "Error in regenerate: ", e)
                        val displayError = mapError(e)
                        addMessageLocally("system", displayError, aiMessage.sessionId)
                        _debugStatus.value = displayError
                        _streamingMessage.value = ""
                    } finally {
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    fun exportConversation(context: android.content.Context, uri: android.net.Uri, format: String) {
        viewModelScope.launch {
            try {
                val sb = java.lang.StringBuilder()
                _messages.value.forEach { msg ->
                    if (msg.role != "system") {
                        if (format == "md") {
                            sb.append("### ${if (msg.role == "user") "User" else "FORGE Ω"}\n\n")
                        } else {
                            sb.append("${if (msg.role == "user") "User" else "FORGE Ω"}:\n")
                        }
                        sb.append(msg.content)
                        sb.append("\n\n")
                    }
                }
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(sb.toString().toByteArray())
                }
                _debugStatus.value = "Exported Successfully"
            } catch (e: Exception) {
                Log.e("FORGE_CHAT", "Export failed", e)
                _debugStatus.value = "Export failed: ${e.message}"
            }
        }
    }
}

