package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.ChatSession
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: com.example.ui.viewmodels.ChatViewModel,
    onNavigateBack: () -> Unit,
    onSessionSelected: (String) -> Unit
) {
    val sessions by viewModel.allSessions.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredSessions = sessions.filter { 
        it.title.contains(searchQuery, ignoreCase = true) || it.mode.contains(searchQuery, ignoreCase = true) 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ARCHIVE", color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete All", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search logs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            )
            
            if (filteredSessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("NO LOGS FOUND", color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredSessions) { session ->
                            var showRename by remember { mutableStateOf(false) }
                            if (showRename) {
                                var newTitle by remember { mutableStateOf(session.title) }
                                AlertDialog(
                                    onDismissRequest = { showRename = false },
                                    title = { Text("Rename Chat") },
                                    text = { 
                                        OutlinedTextField(
                                            value = newTitle,
                                            onValueChange = { newTitle = it },
                                            label = { Text("Title") }
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { 
                                            viewModel.renameSession(session.id, newTitle)
                                            showRename = false 
                                        }) { Text("OK") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showRename = false }) { Text("Cancel") }
                                    }
                                )
                            }
                            
                            SessionItem(
                                session = session,
                                onClick = { onSessionSelected(session.id) },
                                onDelete = { viewModel.deleteSession(session.id) },
                                onRename = { showRename = true }
                            )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionItem(session: ChatSession, onClick: () -> Unit, onDelete: () -> Unit, onRename: () -> Unit) {
    val dateString = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(session.timestamp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${session.mode} • $dateString", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
