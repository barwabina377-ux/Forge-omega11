package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.SettingsManager
import com.example.data.remote.GroqClient
import kotlinx.coroutines.launch

import com.example.data.repository.ChatRepository
import com.example.data.local.AppDatabase
import com.example.domain.provider.ProviderManager

class SettingsViewModelFactory(private val settingsManager: SettingsManager, private val chatRepository: ChatRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(settingsManager, chatRepository) as T
    }
}

class SettingsViewModel(private val settingsManager: SettingsManager, private val chatRepository: ChatRepository) : androidx.lifecycle.ViewModel() {
    val groqApiKey = settingsManager.groqApiKeyFlow
    val geminiApiKey = settingsManager.geminiApiKeyFlow
    val activeProvider = settingsManager.activeProviderFlow

    fun saveSettings(groqKey: String, geminiKey: String, provider: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            settingsManager.saveGroqApiKey(groqKey)
            settingsManager.saveGeminiApiKey(geminiKey)
            settingsManager.saveActiveProvider(provider)
            onComplete()
        }
    }

    fun clearHistory(onComplete: () -> Unit) {
        viewModelScope.launch {
            chatRepository.clearHistory()
            onComplete()
        }
    }

    fun testConnection(provider: String, groqKey: String, geminiKey: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val providerManager = ProviderManager()
                val sb = java.lang.StringBuilder()
                providerManager.generateResponseStream(
                    activeProviderName = provider,
                    groqApiKey = groqKey,
                    geminiApiKey = geminiKey,
                    systemPrompt = "You are a test bot. Say hello.",
                    history = emptyList(),
                    currentMessage = "Hello"
                ).collect { chunk ->
                    sb.append(chunk)
                }
                
                val response = sb.toString()
                if (response.isNotEmpty()) {
                    onResult("Connection Successful! (${response.take(20)}...)")
                } else {
                    onResult("Error: Empty response")
                }
            } catch (e: Exception) {
                onResult("Error: ${e.localizedMessage}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val db = remember { AppDatabase.getDatabase(context) }
    val chatRepository = remember { ChatRepository(db.chatDao(), ProviderManager()) }
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(settingsManager, chatRepository))
    
    val currentGroqKey by viewModel.groqApiKey.collectAsState(initial = "")
    val currentGeminiKey by viewModel.geminiApiKey.collectAsState(initial = "")
    val currentProvider by viewModel.activeProvider.collectAsState(initial = "Groq")
    
    var groqInput by remember { mutableStateOf("") }
    var geminiInput by remember { mutableStateOf("") }
    var providerInput by remember(currentProvider) { mutableStateOf(currentProvider) }
    
    var testStatus by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentGroqKey) {
        if (!currentGroqKey.isNullOrEmpty() && groqInput.isEmpty()) {
            groqInput = currentGroqKey ?: ""
        }
    }
    LaunchedEffect(currentGeminiKey) {
        if (!currentGeminiKey.isNullOrEmpty() && geminiInput.isEmpty()) {
            geminiInput = currentGeminiKey ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SETTINGS", color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = groqInput,
                onValueChange = { groqInput = it },
                label = { Text("Groq API Key") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ),
                singleLine = true
            )
            
            OutlinedTextField(
                value = geminiInput,
                onValueChange = { geminiInput = it },
                label = { Text("Gemini API Key") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ),
                singleLine = true
            )
            
            var expandedProvider by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = providerInput,
                    onValueChange = { },
                    label = { Text("Active Provider") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Box(modifier = Modifier.matchParentSize().clickable { expandedProvider = true })
                DropdownMenu(expanded = expandedProvider, onDismissRequest = { expandedProvider = false }) {
                    DropdownMenuItem(text = { Text("Groq") }, onClick = { providerInput = "Groq"; expandedProvider = false })
                    DropdownMenuItem(text = { Text("Gemini") }, onClick = { providerInput = "Gemini"; expandedProvider = false })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        viewModel.saveSettings(groqInput, geminiInput, providerInput) {
                            testStatus = "Saved locally."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("SAVE", color = MaterialTheme.colorScheme.primary)
                }
                
                Button(
                    onClick = {
                        testStatus = "Testing connection..."
                        viewModel.testConnection(providerInput, groqInput, geminiInput) { result ->
                            testStatus = result
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("TEST CONNECTION", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            if (testStatus.isNotEmpty()) {
                Text(
                    text = testStatus,
                    color = if (testStatus.contains("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = { 
                    viewModel.clearHistory() {
                        testStatus = "History Cleared!"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("CLEAR HISTORY")
            }
        }
    }
}
