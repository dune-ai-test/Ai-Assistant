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
 * Thin, defensive wrapper around [SpeechRecognizer].
 *
 * SpeechRecognizer callbacks arrive from the system's speech service over a binder — any
 * uncaught exception thrown inside one of those callbacks propagates straight through and
 * crashes the whole app, not just this class. Every callback body below is therefore
 * wrapped in try/catch, every recognizer call is guarded, and everything is forced onto
 * the main thread (SpeechRecognizer is not thread-safe and must be driven from a Looper
 * thread).
 *
 * Note: this deliberately does NOT set EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS or
 * similar timing extras. Several OEM recognition services mishandle those and simply never
 * call back at all (the session looks permanently "Listening…" with no result and no
 * error) instead of rejecting them cleanly. Stick to the extras every recognizer is
 * guaranteed to honor, and let ChatViewModel's stop-to-send + safety timeout handle
 * finalization instead.
 */
class SpeechRecognizerManager(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var isSessionActive = false
    private var hasDeliveredResult = false

    var onPartialResult: (String) -> Unit = {}
    var onFinalResult: (String) -> Unit = {}
    var onRmsChanged: (Float) -> Unit = {}
    var onListeningStarted: () -> Unit = {}
    var onListeningStopped: () -> Unit = {}
    var onError: (String) -> Unit = {}

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
        // If we can't tell, don't block the user over it — let the recognizer itself decide.
        true
    }

    fun startListening() {
        if (!hasMicPermission()) {
            safeError("Microphone permission is required.")
            return
        }
        if (!isAvailable()) {
            safeError("Speech recognition isn't available on this device.")
            return
        }
        if (!hasNetworkConnection()) {
            safeError("No internet connection — voice recognition needs network access on this device.")
            return
        }
        if (isSessionActive) {
            // Already listening — ignore the extra tap instead of racing a second session.
            return
        }

        runOnMain {
            try {
                stopListeningInternal()
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
                safeError("Microphone permission is required.")
            } catch (t: Throwable) {
                isSessionActive = false
                safeError(t.message ?: "Couldn't start the microphone. Try again.")
            }
        }
    }

    fun stopListening() {
        runOnMain { stopListeningInternal() }
    }

    private fun stopListeningInternal() {
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

    fun destroy() {
        stopListening()
    }

    private fun buildListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            safeCallback { onListeningStarted() }
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            safeCallback { onRmsChanged(rmsdB) }
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            isSessionActive = false
            safeCallback { onListeningStopped() }
        }

        override fun onError(error: Int) {
            isSessionActive = false
            safeCallback {
                onListeningStopped()
                // ERROR_NO_MATCH / ERROR_SPEECH_TIMEOUT are routine ("didn't catch that"),
                // not failures worth alarming about, but still surfaced so nothing looks stuck.
                this@SpeechRecognizerManager.onError(mapError(error))
            }
        }

        override fun onResults(results: Bundle?) {
            isSessionActive = false
            safeCallback {
                onListeningStopped()
                if (hasDeliveredResult) return@safeCallback
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) {
                    hasDeliveredResult = true
                    onFinalResult(text)
                } else {
                    this@SpeechRecognizerManager.onError("Didn't catch that — try again.")
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            safeCallback {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) onPartialResult(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    /** Runs [block], and if it throws, routes the failure through [onError] instead of
     *  letting it escape into the SpeechRecognizer binder callback and crash the app. */
    private inline fun safeCallback(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            safeError(t.message ?: "Something went wrong with voice input.")
        }
    }

    private fun safeError(message: String) {
        try {
            onError(message)
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
        SpeechRecognizer.ERROR_CLIENT -> "Recognition was interrupted — try again."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK -> "Network error during recognition."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout during recognition."
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — try again."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy, try again in a moment."
        SpeechRecognizer.ERROR_SERVER -> "Server error during recognition."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
        else -> "Unknown speech recognition error."
    }
}
