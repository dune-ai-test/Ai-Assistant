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

const val DEFAULT_CONFIRM_SEND_SECONDS = 4

data class AssistantSettings(
    val apiKey: String = "",
    val baseUrl: String = KiloDefaults.BASE_URL,
    val modelId: String = KiloDefaults.MODEL,
    val modelDisplayName: String = KiloDefaults.MODEL,
    val autoSpeak: Boolean = true,
    val allowVoiceInterrupt: Boolean = true,
    /** "Review before sending" — show a heard transcript for a few seconds with a
     *  Cancel/Send now option before it actually goes out. */
    val confirmBeforeSendEnabled: Boolean = true,
    val confirmBeforeSendSeconds: Int = DEFAULT_CONFIRM_SEND_SECONDS,
    /** The typed-message fallback row on the Chat screen — off by default; Voice Mode is
     *  the primary interaction. */
    val showTypingBar: Boolean = false,
    val totalTokensUsed: Long = 0L,
    /** Empty = engine's own language-matched default voice. */
    val ttsVoiceName: String = "",
    val systemPrompt: String = "You are a warm, concise voice assistant. Keep spoken replies short and natural."
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL_ID = stringPreferencesKey("model_id")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val AUTO_SPEAK = stringPreferencesKey("auto_speak")
        val ALLOW_VOICE_INTERRUPT = stringPreferencesKey("allow_voice_interrupt")
        val CONFIRM_BEFORE_SEND_ENABLED = stringPreferencesKey("confirm_before_send_enabled")
        val CONFIRM_BEFORE_SEND_SECONDS = stringPreferencesKey("confirm_before_send_seconds")
        val SHOW_TYPING_BAR = stringPreferencesKey("show_typing_bar")
        val TOTAL_TOKENS_USED = stringPreferencesKey("total_tokens_used")
        val TTS_VOICE_NAME = stringPreferencesKey("tts_voice_name")
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
            allowVoiceInterrupt = (prefs[Keys.ALLOW_VOICE_INTERRUPT] ?: "true").toBoolean(),
            confirmBeforeSendEnabled = (prefs[Keys.CONFIRM_BEFORE_SEND_ENABLED] ?: "true").toBoolean(),
            confirmBeforeSendSeconds = prefs[Keys.CONFIRM_BEFORE_SEND_SECONDS]?.toIntOrNull()
                ?: DEFAULT_CONFIRM_SEND_SECONDS,
            showTypingBar = (prefs[Keys.SHOW_TYPING_BAR] ?: "false").toBoolean(),
            totalTokensUsed = prefs[Keys.TOTAL_TOKENS_USED]?.toLongOrNull() ?: 0L,
            ttsVoiceName = prefs[Keys.TTS_VOICE_NAME].orEmpty(),
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

    suspend fun saveAllowVoiceInterrupt(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALLOW_VOICE_INTERRUPT] = enabled.toString() }
    }

    suspend fun saveConfirmBeforeSendEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CONFIRM_BEFORE_SEND_ENABLED] = enabled.toString() }
    }

    suspend fun saveConfirmBeforeSendSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.CONFIRM_BEFORE_SEND_SECONDS] = seconds.toString() }
    }

    suspend fun saveShowTypingBar(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TYPING_BAR] = enabled.toString() }
    }

    suspend fun saveTtsVoiceName(name: String) {
        context.dataStore.edit { it[Keys.TTS_VOICE_NAME] = name }
    }

    suspend fun saveSystemPrompt(prompt: String) {
        context.dataStore.edit { it[Keys.SYSTEM_PROMPT] = prompt }
    }

    suspend fun addTokensUsed(count: Int) {
        if (count <= 0) return
        context.dataStore.edit {
            val current = it[Keys.TOTAL_TOKENS_USED]?.toLongOrNull() ?: 0L
            it[Keys.TOTAL_TOKENS_USED] = (current + count).toString()
        }
    }

    suspend fun resetTokensUsed() {
        context.dataStore.edit { it[Keys.TOTAL_TOKENS_USED] = "0" }
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
