package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.preferences.SettingsManager
import com.example.data.remote.GroqClient
import com.example.data.repository.ChatRepository
import com.example.domain.provider.ProviderManager
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.ChatViewModel
import com.example.ui.viewmodels.ChatViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = AppDatabase.getDatabase(this)
        val providerManager = ProviderManager()
        val repository = ChatRepository(db.chatDao(), providerManager)
        val settingsManager = SettingsManager(this)
        
        val factory = ChatViewModelFactory(
            repository,
            settingsManager.groqApiKeyFlow,
            settingsManager.geminiApiKeyFlow,
            settingsManager.activeProviderFlow
        )

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ForgeApp(factory)
                }
            }
        }
    }
}

@Composable
fun ForgeApp(factory: ChatViewModelFactory) {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel(factory = factory)

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onNavigateToGraph = {
                navController.navigate("chat") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("chat") {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHistory = { navController.navigate("history") }
            )
        }
        composable("history") {
            HistoryScreen(
                viewModel = chatViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSessionSelected = { sessionId ->
                    chatViewModel.loadSession(sessionId)
                    navController.popBackStack()
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
