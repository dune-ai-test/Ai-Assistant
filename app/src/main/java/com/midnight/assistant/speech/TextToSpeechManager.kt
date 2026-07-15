package com.midnight.assistant.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class TextToSpeechManager(context: Context) {

    private var isReady = false
    private var initFailed = false
    private var pendingText: String? = null

    var onStart: () -> Unit = {}
    var onDone: () -> Unit = {}

    private var tts: TextToSpeech? = null

    init {
        tts = try {
            TextToSpeech(context.applicationContext) { status ->
                isReady = status == TextToSpeech.SUCCESS
                initFailed = !isReady
                if (isReady) {
                    try {
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                safeCallback(onStart)
                            }

                            override fun onDone(utteranceId: String?) {
                                safeCallback(onDone)
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                safeCallback(onDone)
                            }
                        })
                    } catch (t: Throwable) {
                        // Progress listener is a nice-to-have for orb state; skip on failure.
                    }
                    pendingText?.let { speak(it) }
                    pendingText = null
                } else {
                    // No TTS engine available on this device — fail silently rather than crash;
                    // callers still get their onDone/onStart lifecycle via speak()'s own guard.
                    pendingText = null
                }
            }
        } catch (t: Throwable) {
            initFailed = true
            null
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        val engine = tts
        if (engine == null || initFailed) {
            // No usable TTS engine — surface as "done speaking" immediately so the UI
            // doesn't get stuck showing a "Speaking…" state that never resolves.
            safeCallback(onDone)
            return
        }
        if (!isReady) {
            pendingText = text
            return
        }
        try {
            engine.language = Locale.getDefault()
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        } catch (t: Throwable) {
            safeCallback(onDone)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (t: Throwable) {
            // Ignore — nothing meaningful to recover here.
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (t: Throwable) {
            // Ignore — the manager is going away regardless.
        }
    }

    private inline fun safeCallback(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            // A misbehaving UI callback shouldn't be able to crash the TTS engine thread.
        }
    }
}
