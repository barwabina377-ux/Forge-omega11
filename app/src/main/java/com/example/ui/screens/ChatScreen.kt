package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.data.local.ChatMessage
import com.example.domain.PromptEngine
import com.example.ui.viewmodels.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val debugStatus by viewModel.debugStatus.collectAsState()
    val streamingMessage by viewModel.streamingMessage.collectAsState()
    val activeProviderName by viewModel.activeProviderName.collectAsState()
    var inputText by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(messages) {
        val lastMessage = messages.lastOrNull()
        if (lastMessage?.role == "system" && lastMessage.content.startsWith("Error")) {
            snackbarHostState.showSnackbar(
                message = lastMessage.content,
                duration = SnackbarDuration.Long
            )
        }
    }

    val scrollTarget = messages.size + if (isLoading) 1 else 0
    LaunchedEffect(scrollTarget, streamingMessage) {
        if (scrollTarget > 0) {
            listState.animateScrollToItem(scrollTarget - 1)
        }
    }

    val context = LocalContext.current
    
    val txtExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.exportConversation(context, uri, "txt")
        }
    }

    val mdExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            viewModel.exportConversation(context, uri, "md")
        }
    }

    var messageToExport by remember { mutableStateOf<String?>(null) }
    val msgExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null && messageToExport != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(messageToExport!!.toByteArray())
                }
            } catch (e: Exception) {
                Log.e("FORGE_UI", "Failed to export message", e)
            }
        }
        messageToExport = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("FORGE Ω", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                        Text("$activeProviderName Connected", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
                    }
                },

                actions = {
                    IconButton(onClick = { viewModel.startNewSession() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.List, contentDescription = "History", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                    var showExportMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Export menu", tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Export as TXT") },
                                onClick = {
                                    showExportMenu = false
                                    txtExportLauncher.launch("Conversation.txt")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as Markdown") },
                                onClick = {
                                    showExportMenu = false
                                    mdExportLauncher.launch("Conversation.md")
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Column {
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Mode: $currentMode", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable { expanded = true }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        PromptEngine.modes.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode) },
                                onClick = { 
                                    viewModel.setMode(mode)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                ChatInputBar(
                    text = inputText,
                    onTextChange = { inputText = it },
                    onSend = {
                        Log.d("FORGE_UI", "Send button clicked with text length: ${inputText.length}")
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                            focusManager.clearFocus()
                        }
                    },
                    isLoading = isLoading,
                    onStop = { viewModel.stopGeneration() }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("AWAITING INPUT...", color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f), fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                items(messages) { msg ->
                    ChatBubble(
                        message = msg,
                        onCopy = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Copied Text", msg.content))
                        },
                        onShare = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, msg.content)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share response via"))
                        },
                        onRegenerate = { viewModel.regenerateMessage(msg) },
                        onExport = {
                            messageToExport = msg.content
                            msgExportLauncher.launch("Message_${System.currentTimeMillis()}.txt")
                        }
                    )
                }
            }
            if (isLoading) {
                item {
                    val displayContent = if (streamingMessage.isNotEmpty()) streamingMessage else "..."
                    ChatBubble(
                        message = ChatMessage(sessionId = "", role = "assistant", content = displayContent, timestamp = 0), 
                        isTyping = streamingMessage.isEmpty(), 
                        typingText = debugStatus
                    )
                }
            }
        }
    }
}


@Composable
fun ChatBubble(
    message: ChatMessage,
    isTyping: Boolean = false,
    typingText: String = "",
    onCopy: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null
) {
    val isUser = message.role == "user"
    val isSystem = message.role == "system"
    
    val bubbleColor = when {
        isSystem -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
        isUser -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    
    val textColor = when {
        isSystem -> MaterialTheme.colorScheme.error
        isUser -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bubbleColor)
                    .padding(12.dp)
                    .widthIn(max = 300.dp)
            ) {
                Text(
                    text = if (isTyping) "FORGE Ω is generating...\n[$typingText]" else message.content,
                    color = textColor,
                    fontFamily = if (isUser) FontFamily.Default else FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!isUser && !isTyping && !isSystem) {
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (onCopy != null) {
                        Text(
                            text = "COPY", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onCopy() }
                        )
                    }
                    if (onShare != null) {
                        Text(
                            text = "SHARE", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onShare() }
                        )
                    }
                    if (onExport != null) {
                        Text(
                            text = "EXPORT", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onExport() }
                        )
                    }
                    if (onRegenerate != null) {
                        Text(
                            text = "REGENERATE", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onRegenerate() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("> INIT_PROTOCOL", color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f), fontFamily = FontFamily.Monospace) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp),
            maxLines = 4
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (isLoading) {
            IconButton(
                onClick = onStop,
                modifier = Modifier.background(MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Stop", tint = MaterialTheme.colorScheme.onError)
            }
        } else {
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank(),
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
