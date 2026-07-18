package com.midnight.assistant.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.midnight.assistant.data.AssistantSettings
import com.midnight.assistant.data.ChatHistoryStore
import com.midnight.assistant.data.ChatMessage
import com.midnight.assistant.data.ChatSessionMeta
import com.midnight.assistant.data.GatewayModel
import com.midnight.assistant.data.GatewayResult
import com.midnight.assistant.data.KiloGatewayClient
import com.midnight.assistant.data.Role
import com.midnight.assistant.data.SettingsStore
import com.midnight.assistant.speech.ContinuousSpeechRecognizer
import com.midnight.assistant.speech.MicActivityMonitor
import com.midnight.assistant.speech.TextToSpeechManager
import com.midnight.assistant.util.markdownToPlainText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OrbState { IDLE, LISTENING, THINKING, SPEAKING, CONFIRMING, ERROR }

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val orbState: OrbState = OrbState.IDLE,
    val statusText: String = "Tap the orb to start Voice Mode",
    val liveTranscript: String = "",
    val micLevel: Float = 0f,
    val isVoiceModeActive: Boolean = false,
    /** What the recognizer heard, shown for review before it's actually sent — see
     *  [ChatViewModel.beginPendingSend]. Empty unless orbState == CONFIRMING. */
    val pendingTranscript: String = "",
    val pendingSecondsLeft: Int = 0,
    val errorText: String? = null
)

data class SettingsUiState(
    val settings: AssistantSettings = AssistantSettings(),
    val availableModels: List<GatewayModel> = emptyList(),
    val isLoadingModels: Boolean = false,
    val modelLoadError: String? = null,
    val testStatus: String? = null,
    val isTesting: Boolean = false,
    val backupStatus: String? = null,
    val isBackupBusy: Boolean = false
)

data class HistoryUiState(
    val sessions: List<ChatSessionMeta> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * Powers a ChatGPT-Voice-Mode-style continuous conversation:
 *   tap orb -> listen -> (silence detected) -> send -> reply -> speak -> listen again -> …
 * looping automatically with no per-utterance button presses, until the user ends Voice
 * Mode or taps the orb while the assistant is talking to interrupt it (barge-in).
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)
    private val gatewayClient = KiloGatewayClient()
    private val speechRecognizer = ContinuousSpeechRecognizer(application)
    private val micActivityMonitor = MicActivityMonitor(application)
    private val textToSpeech = TextToSpeechManager(application)
    private val historyStore = ChatHistoryStore(application)

    private var pendingSendJob: Job? = null

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val _settingsState = MutableStateFlow(SettingsUiState())
    val settingsState: StateFlow<SettingsUiState> = _settingsState.asStateFlow()

    private val _historyState = MutableStateFlow(HistoryUiState())
    val historyState: StateFlow<HistoryUiState> = _historyState.asStateFlow()

    private var currentSettings = AssistantSettings()

    /** The conversation currently open on the Chat screen. Every voice/typed exchange
     *  keeps appending here until [startNewConversation] or [openSession] is called. */
    private var currentSessionId: String? = null

    init {
        viewModelScope.launch {
            settingsStore.settingsFlow.collect { settings ->
                currentSettings = settings
                _settingsState.value = _settingsState.value.copy(settings = settings)
            }
        }

        // Show whatever models were fetched last time immediately — no network round trip
        // needed just to open Settings. A fresh "Fetch" click is the only thing that updates it.
        viewModelScope.launch {
            val cached = settingsStore.loadCachedModels()
            if (cached.isNotEmpty()) {
                _settingsState.value = _settingsState.value.copy(availableModels = cached)
            }
        }

        viewModelScope.launch {
            val sessionId = historyStore.getOrCreateCurrentSessionId()
            currentSessionId = sessionId
            val savedMessages = historyStore.loadMessages(sessionId)
            if (savedMessages.isNotEmpty()) {
                _chatState.value = _chatState.value.copy(messages = savedMessages)
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
            _chatState.value = _chatState.value.copy(micLevel = (level.coerceIn(0f, 10f) / 10f))
        }
        speechRecognizer.onPartialResult = { partial ->
            _chatState.value = _chatState.value.copy(liveTranscript = partial)
        }
        speechRecognizer.onFinalResult = { text ->
            if (currentSettings.confirmBeforeSendEnabled) {
                beginPendingSend(text)
            } else {
                _chatState.value = _chatState.value.copy(liveTranscript = "")
                sendUserMessage(text)
            }
        }
        speechRecognizer.onNoSpeechDetected = {
            _chatState.value = _chatState.value.copy(liveTranscript = "")
            if (_chatState.value.isVoiceModeActive) {
                // Normal pause in a continuous conversation — just keep listening.
                speechRecognizer.startListening()
            } else {
                _chatState.value = _chatState.value.copy(
                    orbState = OrbState.IDLE,
                    statusText = "Tap the orb to start Voice Mode"
                )
            }
        }
        speechRecognizer.onFatalError = { message ->
            micActivityMonitor.stop()
            _chatState.value = _chatState.value.copy(
                isVoiceModeActive = false,
                orbState = OrbState.ERROR,
                statusText = message,
                errorText = message,
                liveTranscript = ""
            )
        }

        textToSpeech.onStart = {
            _chatState.value = _chatState.value.copy(orbState = OrbState.SPEAKING, statusText = "Speaking…")
            if (_chatState.value.isVoiceModeActive && currentSettings.allowVoiceInterrupt) {
                micActivityMonitor.start(viewModelScope) {
                    interruptSpeaking()
                }
            }
        }
        textToSpeech.onDone = {
            micActivityMonitor.stop()
            if (_chatState.value.isVoiceModeActive) {
                _chatState.value = _chatState.value.copy(statusText = "Listening…")
                speechRecognizer.startListening()
            } else {
                _chatState.value = _chatState.value.copy(
                    orbState = OrbState.IDLE,
                    statusText = "Tap the orb to start Voice Mode"
                )
            }
        }
    }

    // ---- Voice Mode ----

    /** Single entry point the orb calls when tapped. Behavior depends on current state:
     *  off -> start; speaking -> interrupt (barge-in); confirming -> cancel the pending
     *  send; otherwise -> end Voice Mode. */
    fun onOrbTapped() {
        val current = _chatState.value
        when {
            !current.isVoiceModeActive -> startVoiceMode()
            current.orbState == OrbState.SPEAKING -> interruptSpeaking()
            current.orbState == OrbState.CONFIRMING -> cancelPendingSend()
            else -> stopVoiceMode()
        }
    }

    /** A turn was heard — instead of sending immediately, show it for a few seconds (see
     *  [AssistantSettings.confirmBeforeSendSeconds]) so a misheard sentence can be caught
     *  and cancelled before it actually goes out. Skipped entirely if the user has turned
     *  "Review before sending" off in Settings. */
    private fun beginPendingSend(text: String) {
        pendingSendJob?.cancel()
        val totalSeconds = currentSettings.confirmBeforeSendSeconds.coerceAtLeast(1)
        _chatState.value = _chatState.value.copy(
            orbState = OrbState.CONFIRMING,
            liveTranscript = "",
            pendingTranscript = text,
            pendingSecondsLeft = totalSeconds,
            statusText = "Did I get that right?",
            errorText = null
        )
        pendingSendJob = viewModelScope.launch {
            for (secondsLeft in totalSeconds downTo 1) {
                _chatState.value = _chatState.value.copy(pendingSecondsLeft = secondsLeft)
                delay(1000)
            }
            val toSend = _chatState.value.pendingTranscript
            _chatState.value = _chatState.value.copy(pendingTranscript = "", pendingSecondsLeft = 0)
            if (toSend.isNotBlank()) {
                sendUserMessage(toSend)
            }
        }
    }

    /** Cancels a pending voice turn before it's sent. Voice Mode (if active) resumes
     *  listening right away so the user can just try saying it again. */
    fun cancelPendingSend() {
        pendingSendJob?.cancel()
        pendingSendJob = null
        val stillInVoiceMode = _chatState.value.isVoiceModeActive
        _chatState.value = _chatState.value.copy(
            pendingTranscript = "",
            pendingSecondsLeft = 0,
            orbState = if (stillInVoiceMode) OrbState.LISTENING else OrbState.IDLE,
            statusText = if (stillInVoiceMode) "Listening…" else "Tap the orb to start Voice Mode"
        )
        if (stillInVoiceMode) {
            speechRecognizer.startListening()
        }
    }

    /** Skips the rest of the review window and sends the pending transcript right now. */
    fun sendPendingNow() {
        pendingSendJob?.cancel()
        pendingSendJob = null
        val toSend = _chatState.value.pendingTranscript
        _chatState.value = _chatState.value.copy(pendingTranscript = "", pendingSecondsLeft = 0)
        if (toSend.isNotBlank()) {
            sendUserMessage(toSend)
        }
    }

    fun startVoiceMode() {
        if (currentSettings.apiKey.isBlank()) {
            _chatState.value = _chatState.value.copy(
                orbState = OrbState.ERROR,
                statusText = "Add your Kilo Gateway API key in Settings first.",
                errorText = "Missing API key"
            )
            return
        }
        pendingSendJob?.cancel()
        pendingSendJob = null
        textToSpeech.stop()
        micActivityMonitor.stop()
        _chatState.value = _chatState.value.copy(
            isVoiceModeActive = true,
            pendingTranscript = "",
            pendingSecondsLeft = 0,
            errorText = null
        )
        speechRecognizer.startListening()
    }

    fun stopVoiceMode() {
        pendingSendJob?.cancel()
        pendingSendJob = null
        micActivityMonitor.stop()
        speechRecognizer.stopListening()
        textToSpeech.stop()
        _chatState.value = _chatState.value.copy(
            isVoiceModeActive = false,
            orbState = OrbState.IDLE,
            statusText = "Tap the orb to start Voice Mode",
            liveTranscript = "",
            pendingTranscript = "",
            pendingSecondsLeft = 0
        )
    }

    /** Called when the user starts talking while the assistant is mid-reply (either
     *  automatically via [MicActivityMonitor], or manually by tapping the orb). */
    fun interruptSpeaking() {
        val current = _chatState.value
        if (!current.isVoiceModeActive || current.orbState != OrbState.SPEAKING) return
        micActivityMonitor.stop()
        textToSpeech.stop()
        _chatState.value = current.copy(orbState = OrbState.LISTENING, statusText = "Listening…")
        speechRecognizer.startListening()
    }

    fun sendTypedMessage(text: String) {
        if (text.isBlank()) return
        sendUserMessage(text.trim())
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
        persistCurrentSession(updatedHistory)

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
                    val replyText = result.value.text
                    result.value.totalTokens?.let { settingsStore.addTokensUsed(it) }

                    val assistantMessage = ChatMessage(role = Role.ASSISTANT, text = replyText)
                    val finalHistory = _chatState.value.messages + assistantMessage
                    // Always speak while Voice Mode is active (that's the whole point of it);
                    // otherwise respect the general "speak replies aloud" setting.
                    val shouldSpeak = currentSettings.autoSpeak || _chatState.value.isVoiceModeActive
                    _chatState.value = _chatState.value.copy(
                        messages = finalHistory,
                        orbState = if (shouldSpeak) OrbState.SPEAKING else OrbState.IDLE,
                        statusText = if (shouldSpeak) "Speaking…" else "Tap the orb to start Voice Mode"
                    )
                    persistCurrentSession(finalHistory)
                    if (shouldSpeak) {
                        textToSpeech.speak(markdownToPlainText(replyText))
                    }
                }
                is GatewayResult.Failure -> {
                    // A failing turn ends Voice Mode rather than silently retrying forever
                    // on a persistent error (e.g. a bad API key would otherwise loop non-stop).
                    speechRecognizer.stopListening()
                    micActivityMonitor.stop()
                    _chatState.value = _chatState.value.copy(
                        isVoiceModeActive = false,
                        orbState = OrbState.ERROR,
                        statusText = result.message,
                        errorText = result.message
                    )
                }
            }
        }
    }

    private fun persistCurrentSession(messages: List<ChatMessage>) {
        val sessionId = currentSessionId ?: return
        viewModelScope.launch { historyStore.saveMessages(sessionId, messages) }
    }

    /** Explicit "new conversation" action — the ONLY way a fresh, empty thread starts.
     *  Everything else (voice or typed) keeps appending to the current session. */
    fun startNewConversation() {
        stopVoiceMode()
        viewModelScope.launch {
            val newId = historyStore.startNewSession()
            currentSessionId = newId
            _chatState.value = ChatUiState()
        }
    }

    fun openSession(sessionId: String) {
        stopVoiceMode()
        viewModelScope.launch {
            historyStore.setCurrentSession(sessionId)
            currentSessionId = sessionId
            val messages = historyStore.loadMessages(sessionId)
            _chatState.value = ChatUiState(messages = messages)
        }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            _historyState.value = _historyState.value.copy(isLoading = true)
            val sessions = historyStore.listSessions()
            _historyState.value = HistoryUiState(sessions = sessions, isLoading = false)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            historyStore.deleteSession(sessionId)
            refreshHistory()
        }
    }

    fun dismissError() {
        _chatState.value = _chatState.value.copy(
            orbState = OrbState.IDLE,
            errorText = null,
            statusText = "Tap the orb to start Voice Mode"
        )
    }

    // ---- Settings screen actions ----

    fun saveApiKey(key: String) = viewModelScope.launch { settingsStore.saveApiKey(key) }

    fun saveBaseUrl(url: String) = viewModelScope.launch { settingsStore.saveBaseUrl(url) }

    fun saveModel(id: String, displayName: String) =
        viewModelScope.launch { settingsStore.saveModel(id, displayName) }

    fun saveAutoSpeak(enabled: Boolean) = viewModelScope.launch { settingsStore.saveAutoSpeak(enabled) }

    fun saveAllowVoiceInterrupt(enabled: Boolean) =
        viewModelScope.launch { settingsStore.saveAllowVoiceInterrupt(enabled) }

    fun saveConfirmBeforeSendEnabled(enabled: Boolean) =
        viewModelScope.launch { settingsStore.saveConfirmBeforeSendEnabled(enabled) }

    fun saveConfirmBeforeSendSeconds(seconds: Int) =
        viewModelScope.launch { settingsStore.saveConfirmBeforeSendSeconds(seconds) }

    fun saveShowTypingBar(enabled: Boolean) =
        viewModelScope.launch { settingsStore.saveShowTypingBar(enabled) }

    fun saveSystemPrompt(prompt: String) = viewModelScope.launch { settingsStore.saveSystemPrompt(prompt) }

    fun resetTokensUsed() = viewModelScope.launch { settingsStore.resetTokensUsed() }

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
                    if (result.value.isNotEmpty()) {
                        settingsStore.saveCachedModels(result.value)
                    }
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
                    testStatus = "✓ Connected — ${result.value.text.take(80)}"
                )
                is GatewayResult.Failure -> _settingsState.value.copy(
                    isTesting = false,
                    testStatus = "✗ ${result.message}"
                )
            }
        }
    }

    // ---- Backup & restore (all conversations) ----

    fun exportConversations(destination: Uri) {
        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(isBackupBusy = true, backupStatus = "Exporting…")
            try {
                val json = historyStore.exportAllToJson()
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val stream = resolver.openOutputStream(destination)
                        ?: throw IllegalStateException("Couldn't open the selected file for writing.")
                    stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
                _settingsState.value = _settingsState.value.copy(
                    isBackupBusy = false,
                    backupStatus = "✓ Exported successfully"
                )
            } catch (t: Throwable) {
                _settingsState.value = _settingsState.value.copy(
                    isBackupBusy = false,
                    backupStatus = "✗ Export failed: ${t.message ?: "unknown error"}"
                )
            }
        }
    }

    fun importConversations(source: Uri) {
        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(isBackupBusy = true, backupStatus = "Importing…")
            try {
                val text = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val stream = resolver.openInputStream(source)
                        ?: throw IllegalStateException("Couldn't open the selected file for reading.")
                    stream.bufferedReader().use { it.readText() }
                }
                val count = historyStore.importAllFromJson(text)
                _settingsState.value = _settingsState.value.copy(
                    isBackupBusy = false,
                    backupStatus = if (count > 0) {
                        "✓ Imported $count conversation${if (count == 1) "" else "s"}"
                    } else {
                        "No conversations found in that file."
                    }
                )
                refreshHistory()
            } catch (t: Throwable) {
                _settingsState.value = _settingsState.value.copy(
                    isBackupBusy = false,
                    backupStatus = "✗ Import failed: ${t.message ?: "invalid file"}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
        micActivityMonitor.stop()
        textToSpeech.shutdown()
    }
}
