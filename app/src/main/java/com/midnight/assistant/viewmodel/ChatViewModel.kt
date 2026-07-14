package com.midnight.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.midnight.assistant.data.AssistantSettings
import com.midnight.assistant.data.ChatMessage
import com.midnight.assistant.data.GatewayModel
import com.midnight.assistant.data.GatewayResult
import com.midnight.assistant.data.KiloGatewayClient
import com.midnight.assistant.data.Role
import com.midnight.assistant.data.SettingsStore
import com.midnight.assistant.speech.SpeechRecognizerManager
import com.midnight.assistant.speech.TextToSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class OrbState { IDLE, LISTENING, THINKING, SPEAKING, ERROR }

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val orbState: OrbState = OrbState.IDLE,
    val liveTranscript: String = "",
    val statusText: String = "Tap the orb and start talking",
    val micLevel: Float = 0f,
    val errorText: String? = null
)

data class SettingsUiState(
    val settings: AssistantSettings = AssistantSettings(),
    val availableModels: List<GatewayModel> = emptyList(),
    val isLoadingModels: Boolean = false,
    val modelLoadError: String? = null,
    val testStatus: String? = null,
    val isTesting: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)
    private val gatewayClient = KiloGatewayClient()
    private val speechRecognizer = SpeechRecognizerManager(application)
    private val textToSpeech = TextToSpeechManager(application)

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val _settingsState = MutableStateFlow(SettingsUiState())
    val settingsState: StateFlow<SettingsUiState> = _settingsState.asStateFlow()

    private var currentSettings = AssistantSettings()

    init {
        viewModelScope.launch {
            settingsStore.settingsFlow.collect { settings ->
                currentSettings = settings
                _settingsState.value = _settingsState.value.copy(settings = settings)
            }
        }

        speechRecognizer.onListeningStarted = {
            _chatState.value = _chatState.value.copy(
                orbState = OrbState.LISTENING,
                statusText = "Listening…",
                liveTranscript = "",
                errorText = null
            )
        }
        speechRecognizer.onRmsChanged = { level ->
            _chatState.value = _chatState.value.copy(micLevel = level.coerceIn(0f, 10f) / 10f)
        }
        speechRecognizer.onPartialResult = { partial ->
            _chatState.value = _chatState.value.copy(liveTranscript = partial)
        }
        speechRecognizer.onFinalResult = { finalText ->
            _chatState.value = _chatState.value.copy(liveTranscript = "")
            sendUserMessage(finalText)
        }
        speechRecognizer.onListeningStopped = {
            if (_chatState.value.orbState == OrbState.LISTENING) {
                _chatState.value = _chatState.value.copy(orbState = OrbState.IDLE, statusText = "Tap the orb and start talking")
            }
        }
        speechRecognizer.onError = { message ->
            _chatState.value = _chatState.value.copy(
                orbState = OrbState.ERROR,
                statusText = message,
                errorText = message
            )
        }

        textToSpeech.onStart = {
            _chatState.value = _chatState.value.copy(orbState = OrbState.SPEAKING, statusText = "Speaking…")
        }
        textToSpeech.onDone = {
            _chatState.value = _chatState.value.copy(orbState = OrbState.IDLE, statusText = "Tap the orb and start talking")
        }
    }

    fun isSpeechAvailable(): Boolean = speechRecognizer.isAvailable()

    fun startListening() {
        if (currentSettings.apiKey.isBlank()) {
            _chatState.value = _chatState.value.copy(
                orbState = OrbState.ERROR,
                statusText = "Add your Kilo Gateway API key in Settings first.",
                errorText = "Missing API key"
            )
            return
        }
        textToSpeech.stop()
        speechRecognizer.startListening()
    }

    fun stopListening() {
        speechRecognizer.stopListening()
        _chatState.value = _chatState.value.copy(orbState = OrbState.IDLE, statusText = "Tap the orb and start talking")
    }

    fun sendTypedMessage(text: String) {
        if (text.isBlank()) return
        sendUserMessage(text)
    }

    private fun sendUserMessage(text: String) {
        val userMessage = ChatMessage(role = Role.USER, text = text)
        val updatedHistory = _chatState.value.messages + userMessage
        _chatState.value = _chatState.value.copy(
            messages = updatedHistory,
            orbState = OrbState.THINKING,
            statusText = "Thinking…",
            errorText = null
        )

        viewModelScope.launch {
            val result = gatewayClient.sendChat(
                baseUrl = currentSettings.baseUrl,
                apiKey = currentSettings.apiKey,
                model = currentSettings.modelId,
                systemPrompt = currentSettings.systemPrompt,
                history = updatedHistory
            )

            when (result) {
                is GatewayResult.Success -> {
                    val assistantMessage = ChatMessage(role = Role.ASSISTANT, text = result.value)
                    _chatState.value = _chatState.value.copy(
                        messages = _chatState.value.messages + assistantMessage,
                        orbState = if (currentSettings.autoSpeak) OrbState.SPEAKING else OrbState.IDLE,
                        statusText = if (currentSettings.autoSpeak) "Speaking…" else "Tap the orb and start talking"
                    )
                    if (currentSettings.autoSpeak) {
                        textToSpeech.speak(result.value)
                    }
                }
                is GatewayResult.Failure -> {
                    _chatState.value = _chatState.value.copy(
                        orbState = OrbState.ERROR,
                        statusText = result.message,
                        errorText = result.message
                    )
                }
            }
        }
    }

    fun clearConversation() {
        _chatState.value = _chatState.value.copy(messages = emptyList(), errorText = null)
    }

    fun dismissError() {
        _chatState.value = _chatState.value.copy(
            orbState = OrbState.IDLE,
            errorText = null,
            statusText = "Tap the orb and start talking"
        )
    }

    // ---- Settings screen actions ----

    fun saveApiKey(key: String) = viewModelScope.launch { settingsStore.saveApiKey(key) }

    fun saveBaseUrl(url: String) = viewModelScope.launch { settingsStore.saveBaseUrl(url) }

    fun saveModel(id: String, displayName: String) =
        viewModelScope.launch { settingsStore.saveModel(id, displayName) }

    fun saveAutoSpeak(enabled: Boolean) = viewModelScope.launch { settingsStore.saveAutoSpeak(enabled) }

    fun saveSystemPrompt(prompt: String) = viewModelScope.launch { settingsStore.saveSystemPrompt(prompt) }

    fun fetchModels(baseUrlOverride: String? = null, apiKeyOverride: String? = null) {
        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(isLoadingModels = true, modelLoadError = null)
            val latest = settingsStore.settingsFlow.first()
            val baseUrl = baseUrlOverride ?: latest.baseUrl
            val apiKey = apiKeyOverride ?: latest.apiKey

            when (val result = gatewayClient.listModels(baseUrl, apiKey)) {
                is GatewayResult.Success -> {
                    _settingsState.value = _settingsState.value.copy(
                        isLoadingModels = false,
                        availableModels = result.value,
                        modelLoadError = if (result.value.isEmpty()) "No models returned." else null
                    )
                }
                is GatewayResult.Failure -> {
                    _settingsState.value = _settingsState.value.copy(
                        isLoadingModels = false,
                        modelLoadError = result.message
                    )
                }
            }
        }
    }

    fun testConnection(baseUrlOverride: String, apiKeyOverride: String, modelOverride: String) {
        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(isTesting = true, testStatus = null)
            val result = gatewayClient.sendChat(
                baseUrl = baseUrlOverride,
                apiKey = apiKeyOverride,
                model = modelOverride,
                systemPrompt = "Respond with a short one-sentence greeting only.",
                history = listOf(ChatMessage(role = Role.USER, text = "Say hello in one short sentence."))
            )
            _settingsState.value = when (result) {
                is GatewayResult.Success -> _settingsState.value.copy(
                    isTesting = false,
                    testStatus = "✓ Connected — ${result.value.take(80)}"
                )
                is GatewayResult.Failure -> _settingsState.value.copy(
                    isTesting = false,
                    testStatus = "✗ ${result.message}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
        textToSpeech.shutdown()
    }
}
