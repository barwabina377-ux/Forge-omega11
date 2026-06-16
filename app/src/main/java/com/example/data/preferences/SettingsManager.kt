package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val ACTIVE_PROVIDER = stringPreferencesKey("active_provider")
    }

    val groqApiKeyFlow: Flow<String?> = context.dataStore.data.map { it[GROQ_API_KEY] }
    val geminiApiKeyFlow: Flow<String?> = context.dataStore.data.map { it[GEMINI_API_KEY] }
    val activeProviderFlow: Flow<String> = context.dataStore.data.map { it[ACTIVE_PROVIDER] ?: "Groq" }

    suspend fun saveGroqApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[GROQ_API_KEY] = apiKey
        }
    }

    suspend fun saveGeminiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[GEMINI_API_KEY] = apiKey
        }
    }

    suspend fun saveActiveProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_PROVIDER] = provider
        }
    }
}
