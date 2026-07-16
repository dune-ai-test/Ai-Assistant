package com.midnight.assistant.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Drives a ChatGPT-Voice-Mode-style conversation: each call to [startListening] captures
 * one user turn — Android's own pause/silence detection decides when the turn ends — and
 * the caller (ChatViewModel) decides whether to call it again afterwards. That external
 * loop is what makes the conversation feel continuous instead of "press mic every sentence".
 *
 * Every callback and recognizer call is wrapped defensively and forced onto the main
 * thread, and no custom silence-length intent extras are set — several OEM recognition
 * services mishandle those and simply never call back at all, leaving a session stuck at
 * "Listening…" forever. Only extras every recognizer is guaranteed to honor are used here.
 */
class ContinuousSpeechRecognizer(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var isSessionActive = false
    private var hasDeliveredResult = false

    var onListeningStarted: () -> Unit = {}
    var onRmsChanged: (Float) -> Unit = {}
    var onPartialResult: (String) -> Unit = {}

    /** A real, non-empty transcript for this turn. */
    var onFinalResult: (String) -> Unit = {}

    /** Turn ended with nothing said (silence / no match) — normal and expected while in a
     *  continuous conversation; the caller typically just listens again. */
    var onNoSpeechDetected: () -> Unit = {}

    /** Something actually went wrong (permission, network, audio hardware, etc) — the
     *  caller should stop the conversation loop and surface this. */
    var onFatalError: (String) -> Unit = {}

    fun isAvailable(): Boolean = try {
        SpeechRecognizer.isRecognitionAvailable(context)
    } catch (t: Throwable) {
        false
    }

    private fun hasMicPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    /** Most on-device recognizers (esp. the default Google one when no offline language
     *  pack is installed) need network access — without it a session can sit at
     *  "Listening…" forever instead of erroring. Surface that up front instead of hanging. */
    private fun hasNetworkConnection(): Boolean = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork
        val capabilities = network?.let { cm.getNetworkCapabilities(it) }
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } catch (t: Throwable) {
        true
    }

    fun startListening() {
        if (!hasMicPermission()) {
            onFatalErrorSafe("Microphone permission is required.")
            return
        }
        if (!isAvailable()) {
            onFatalErrorSafe("Speech recognition isn't available on this device.")
            return
        }
        if (!hasNetworkConnection()) {
            onFatalErrorSafe("No internet connection — voice recognition needs network access on this device.")
            return
        }
        if (isSessionActive) return

        runOnMain {
            try {
                stopInternal()
                hasDeliveredResult = false

                val newRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                recognizer = newRecognizer
                newRecognizer.setRecognitionListener(buildListener())

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                isSessionActive = true
                newRecognizer.startListening(intent)
            } catch (security: SecurityException) {
                isSessionActive = false
                onFatalErrorSafe("Microphone permission is required.")
            } catch (t: Throwable) {
                isSessionActive = false
                onFatalErrorSafe(t.message ?: "Couldn't start the microphone.")
            }
        }
    }

    fun stopListening() {
        runOnMain { stopInternal() }
    }

    private fun stopInternal() {
        val current = recognizer
        recognizer = null
        isSessionActive = false
        if (current == null) return
        try {
            current.setRecognitionListener(null)
            current.cancel()
        } catch (t: Throwable) {
            // Ignore — recognizer may already be in a torn-down state.
        }
        try {
            current.destroy()
        } catch (t: Throwable) {
            // Ignore — nothing useful to do if destroy() itself throws.
        }
    }

    fun destroy() = stopListening()

    private fun buildListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            safe { onListeningStarted() }
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            safe { this@ContinuousSpeechRecognizer.onRmsChanged(rmsdB) }
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            isSessionActive = false
        }

        override fun onError(error: Int) {
            isSessionActive = false
            safe {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        onNoSpeechDetected()
                    else -> this@ContinuousSpeechRecognizer.onFatalError(mapError(error))
                }
            }
        }

        override fun onResults(results: Bundle?) {
            isSessionActive = false
            safe {
                if (hasDeliveredResult) return@safe
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) {
                    hasDeliveredResult = true
                    onFinalResult(text)
                } else {
                    onNoSpeechDetected()
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            safe {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) onPartialResult(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    /** Runs [block], and if it throws, routes the failure through [onFatalError] instead of
     *  letting it escape into the SpeechRecognizer binder callback and crash the app. */
    private inline fun safe(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            onFatalErrorSafe(t.message ?: "Something went wrong with voice input.")
        }
    }

    private fun onFatalErrorSafe(message: String) {
        try {
            onFatalError(message)
        } catch (t: Throwable) {
            // The error callback itself is misbehaving — nothing more we can safely do.
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun mapError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
        SpeechRecognizer.ERROR_CLIENT -> "Recognition was interrupted."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK -> "Network error during recognition."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout during recognition."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy."
        SpeechRecognizer.ERROR_SERVER -> "Server error during recognition."
        else -> "Unknown speech recognition error."
    }
}
