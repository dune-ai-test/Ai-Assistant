package com.midnight.assistant.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Best-effort "is the user talking right now" detector, used ONLY to notice a barge-in
 * attempt while the assistant is speaking — it never runs at the same time as the real
 * [ContinuousSpeechRecognizer] session. This is a cheap amplitude trigger, not real speech
 * recognition: "something loud enough to be a voice just happened, stop talking and start
 * really listening."
 *
 * This is inherently approximate. Without dedicated hardware/OS-level echo cancellation
 * between the speaker and mic, the assistant's own voice can occasionally trigger it. An
 * [AcousticEchoCanceler] is attached when the device supports one, and a sustained-energy
 * requirement (several consecutive loud frames, not just one) cuts down on false triggers
 * — but it won't be perfect on every device. That's exactly why manually tapping the orb
 * to interrupt always works too, regardless of whether this fires.
 */
class MicActivityMonitor(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var job: Job? = null

    private val sampleRate = 16_000
    private val frameSize = (sampleRate * 0.02).toInt() // ~20ms frames

    fun start(scope: CoroutineScope, onVoiceDetected: () -> Unit) {
        stop()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        job = scope.launch(Dispatchers.IO) {
            var record: AudioRecord? = null
            try {
                val minBuffer = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuffer <= 0) return@launch

                record = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuffer * 2
                )
                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    record.release()
                    return@launch
                }
                audioRecord = record

                if (AcousticEchoCanceler.isAvailable()) {
                    echoCanceler = AcousticEchoCanceler.create(record.audioSessionId)?.apply {
                        enabled = true
                    }
                }

                val buffer = ShortArray(frameSize.coerceAtLeast(160))
                var consecutiveLoudFrames = 0
                val loudThreshold = 1800.0 // empirical RMS threshold on a 16-bit PCM scale
                val framesNeeded = 4 // ~80ms of sustained sound before treating it as real speech

                record.startRecording()
                while (isActive && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read <= 0) continue

                    var sumSquares = 0.0
                    for (i in 0 until read) {
                        val sample = buffer[i].toDouble()
                        sumSquares += sample * sample
                    }
                    val rms = sqrt(sumSquares / read)

                    consecutiveLoudFrames = if (rms > loudThreshold) consecutiveLoudFrames + 1 else 0

                    if (consecutiveLoudFrames >= framesNeeded) {
                        onVoiceDetected()
                        break
                    }
                }
            } catch (t: Throwable) {
                // Best-effort feature — any failure here just means no auto barge-in this turn.
            } finally {
                try {
                    record?.stop()
                } catch (t: Throwable) {
                    // Ignore.
                }
                try {
                    record?.release()
                } catch (t: Throwable) {
                    // Ignore.
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            echoCanceler?.release()
        } catch (t: Throwable) {
            // Ignore.
        }
        echoCanceler = null
        try {
            audioRecord?.stop()
        } catch (t: Throwable) {
            // Ignore.
        }
        try {
            audioRecord?.release()
        } catch (t: Throwable) {
            // Ignore.
        }
        audioRecord = null
    }
}
