package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val groqApiKeyFlow: Flow<String?>,
    private val geminiApiKeyFlow: Flow<String?>,
    private val activeProviderFlow: Flow<String>
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(repository, groqApiKeyFlow, geminiApiKeyFlow, activeProviderFlow) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
