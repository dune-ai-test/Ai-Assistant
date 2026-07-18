package com.midnight.assistant.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class TextToSpeechManager(context: Context) {

    /** A selectable device TTS voice — Android's own [android.speech.tts.Voice] trimmed
     *  down to what the UI actually needs. */
    data class VoiceOption(
        val name: String,
        val localeLabel: String,
        val quality: Int,
        val isNetworkRequired: Boolean
    )

    private var isReady = false
    private var initFailed = false
    private var pendingText: String? = null
    private var selectedVoiceName: String? = null

    var onStart: () -> Unit = {}
    var onDone: () -> Unit = {}

    /** Fired once the engine finishes initializing — [voices] is meaningless before this. */
    var onReady: () -> Unit = {}

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
                    applySelectedVoice()
                    pendingText?.let { speak(it) }
                    pendingText = null
                    safeCallback(onReady)
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

    /** All voices the device's TTS engine currently offers, sorted by locale then quality.
     *  Empty until the engine finishes initializing — see [onReady]. */
    fun voices(): List<VoiceOption> {
        val engine = tts ?: return emptyList()
        if (!isReady) return emptyList()
        return try {
            engine.voices.orEmpty()
                .filter { !it.name.isNullOrBlank() }
                .map {
                    VoiceOption(
                        name = it.name,
                        localeLabel = it.locale?.displayName?.takeIf { label -> label.isNotBlank() }
                            ?: it.locale?.toString().orEmpty(),
                        quality = it.quality,
                        isNetworkRequired = it.isNetworkConnectionRequired
                    )
                }
                .sortedWith(compareBy({ v -> v.localeLabel }, { v -> -v.quality }))
        } catch (t: Throwable) {
            emptyList()
        }
    }

    /** Selects a voice by its [VoiceOption.name]; pass null/blank to go back to the
     *  engine's own language-matched default. */
    fun setVoice(name: String?) {
        selectedVoiceName = name?.takeIf { it.isNotBlank() }
        if (isReady) applySelectedVoice()
    }

    private fun applySelectedVoice() {
        val name = selectedVoiceName ?: return
        try {
            val match = tts?.voices?.firstOrNull { it.name == name } ?: return
            tts?.voice = match
        } catch (t: Throwable) {
            // Ignore — falls back to whatever the engine already has selected.
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
            if (selectedVoiceName == null) {
                engine.language = Locale.getDefault()
            } else {
                // Re-apply on every utterance: setting .language elsewhere can reset the
                // engine back to its own default voice for that language.
                applySelectedVoice()
            }
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
