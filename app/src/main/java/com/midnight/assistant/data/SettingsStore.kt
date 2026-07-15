package com.midnight.assistant.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "midnight_assistant_settings")

/** Default Kilo Gateway values (see https://kilo.ai/gateway and the Kilo API docs). */
object KiloDefaults {
    const val BASE_URL = "https://api.kilo.ai/api/gateway"
    const val MODEL = "anthropic/claude-sonnet-4.5"
}

data class AssistantSettings(
    val apiKey: String = "",
    val baseUrl: String = KiloDefaults.BASE_URL,
    val modelId: String = KiloDefaults.MODEL,
    val modelDisplayName: String = KiloDefaults.MODEL,
    val autoSpeak: Boolean = true,
    val systemPrompt: String = "You are a warm, concise voice assistant. Keep spoken replies short and natural."
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL_ID = stringPreferencesKey("model_id")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val AUTO_SPEAK = stringPreferencesKey("auto_speak")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val CACHED_MODELS = stringPreferencesKey("cached_models_json")
    }

    val settingsFlow: Flow<AssistantSettings> = context.dataStore.data.map { prefs ->
        AssistantSettings(
            apiKey = prefs[Keys.API_KEY].orEmpty(),
            baseUrl = prefs[Keys.BASE_URL] ?: KiloDefaults.BASE_URL,
            modelId = prefs[Keys.MODEL_ID] ?: KiloDefaults.MODEL,
            modelDisplayName = prefs[Keys.MODEL_NAME] ?: (prefs[Keys.MODEL_ID] ?: KiloDefaults.MODEL),
            autoSpeak = (prefs[Keys.AUTO_SPEAK] ?: "true").toBoolean(),
            systemPrompt = prefs[Keys.SYSTEM_PROMPT]
                ?: "You are a warm, concise voice assistant. Keep spoken replies short and natural."
        )
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { it[Keys.API_KEY] = key.trim() }
    }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { it[Keys.BASE_URL] = url.trim().trimEnd('/') }
    }

    suspend fun saveModel(id: String, displayName: String) {
        context.dataStore.edit {
            it[Keys.MODEL_ID] = id
            it[Keys.MODEL_NAME] = displayName
        }
    }

    suspend fun saveAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SPEAK] = enabled.toString() }
    }

    suspend fun saveSystemPrompt(prompt: String) {
        context.dataStore.edit { it[Keys.SYSTEM_PROMPT] = prompt }
    }

    /** Persists the last-fetched model list so Settings shows it immediately on next open,
     *  without re-hitting the network — only an explicit "Fetch" click replaces it. */
    suspend fun saveCachedModels(models: List<GatewayModel>) {
        val array = JSONArray()
        models.forEach { model ->
            array.put(
                JSONObject().apply {
                    put("id", model.id)
                    put("name", model.displayName)
                    if (model.contextWindow != null) put("contextWindow", model.contextWindow)
                }
            )
        }
        context.dataStore.edit { it[Keys.CACHED_MODELS] = array.toString() }
    }

    suspend fun loadCachedModels(): List<GatewayModel> {
        val raw = context.dataStore.data.first()[Keys.CACHED_MODELS] ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                GatewayModel(
                    id = id,
                    displayName = obj.optString("name", id),
                    contextWindow = obj.optInt("contextWindow", -1).takeIf { it > 0 }
                )
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }
}
