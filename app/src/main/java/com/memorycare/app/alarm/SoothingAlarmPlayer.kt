package com.memorycare.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates and plays a continuous, serene, and melodic 10-second acoustic harp & singing bowl ringtone.
 * Built using Android's AudioTrack on the USAGE_ALARM stream.
 *
 * Features:
 * - Continuous ambient harmonic pad (eliminates awkward pauses or dead silence)
 * - Flowing, peaceful pentatonic harp/celesta arpeggio
 * - Smooth fade-in and graceful fade-out
 * - Direct playback without external audio dependencies
 */
class SoothingAlarmPlayer(private val context: Context) {

    companion object {
        private const val TAG = "SoothingAlarmPlayer"
        private const val SAMPLE_RATE = 44100
        private const val DURATION_SECONDS = 10
    }

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private var originalVolume: Int? = null

    /**
     * Starts playing the soothing continuous melody.
     * @param durationSeconds Duration in seconds of the melody before completing (default 10s).
     * @param onFinished Called when playback completes or is stopped.
     */
    fun playSoothingChime(durationSeconds: Int = DURATION_SECONDS, onFinished: (() -> Unit)? = null) {
        stop()

        playbackJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                ensureAlarmVolumeIsAudible()

                val pcmData = generateContinuousHarmonicMelodyPcm()
                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBufferSize, pcmData.size * 2)

                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

                val format = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcmData, 0, pcmData.size)
                track.play()
                audioTrack = track
                Log.d(TAG, "🎶 Continuous soothing melody playback started for $durationSeconds seconds")

                // Wait specified duration for the continuous chime sequence to complete
                delay(durationSeconds * 1000L)

                stop()
                onFinished?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Error playing synthesized melody: ${e.message}", e)
                stop()
                onFinished?.invoke()
            }
        }
    }

    /**
     * Stops the melody immediately and cleans up resources.
     */
    fun stop() {
        try {
            playbackJob?.cancel()
            playbackJob = null

            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
            audioTrack = null
            Log.d(TAG, "Soothing melody stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack: ${e.message}")
        }
    }

    /**
     * Ensures that the system alarm volume is set to a clearly audible level (at least 75%).
     */
    private fun ensureAlarmVolumeIsAudible() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            if (originalVolume == null) {
                originalVolume = curVol
            }
            val targetVol = (maxVol * 0.75f).toInt().coerceAtLeast(1)
            if (curVol < targetVol) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
                Log.d(TAG, "Boosted alarm volume from $curVol to $targetVol/$maxVol")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not adjust alarm volume: ${e.message}")
        }
    }

    /**
     * Synthesizes 10 seconds of a seamless, flowing, continuous musical soundscape.
     * Layer 1: Sustained warm ambient drone/harmonium (C4 + G4) - guarantees continuous sound without silence.
     * Layer 2: Flowing 16-note acoustic harp / celesta melody in peaceful C Pentatonic.
     */
    private fun generateContinuousHarmonicMelodyPcm(): ShortArray {
        val totalSamples = SAMPLE_RATE * DURATION_SECONDS
        val samples = ShortArray(totalSamples)

        // 16-note flowing harp melody (Time in seconds, Note frequency in Hz)
        data class NoteEvent(val timeSec: Float, val frequency: Double)

        val melody = listOf(
            NoteEvent(0.00f, 523.25), // C5
            NoteEvent(0.55f, 659.25), // E5
            NoteEvent(1.10f, 783.99), // G5
            NoteEvent(1.65f, 987.77), // B5
            NoteEvent(2.20f, 1046.50), // C6
            NoteEvent(2.75f, 783.99), // G5
            NoteEvent(3.30f, 659.25), // E5
            NoteEvent(3.85f, 523.25), // C5
            NoteEvent(4.40f, 587.33), // D5
            NoteEvent(4.95f, 659.25), // E5
            NoteEvent(5.50f, 783.99), // G5
            NoteEvent(6.05f, 880.00), // A5
            NoteEvent(6.60f, 783.99), // G5
            NoteEvent(7.15f, 659.25), // E5
            NoteEvent(7.70f, 587.33), // D5
            NoteEvent(8.25f, 523.25)  // C5 (Resolving fundamental, rings till end)
        )

        val noteDurationSec = 1.9f // Long ring-out so notes overlap seamlessly
        val noteSamplesCount = (noteDurationSec * SAMPLE_RATE).toInt()

        // --- Render Layer 2: Flowing Acoustic Harp / Bell Notes ---
        for (event in melody) {
            val startSample = (event.timeSec * SAMPLE_RATE).toInt()

            for (i in 0 until noteSamplesCount) {
                val sampleIdx = startSample + i
                if (sampleIdx >= totalSamples) break

                val t = i.toDouble() / SAMPLE_RATE
                // 35ms soft attack to prevent clicking, natural exponential acoustic decay
                val attack = if (t < 0.035) (t / 0.035) else 1.0
                val decay = exp(-t * 1.85)
                val envelope = attack * decay

                // Acoustic bell harmonics: Fundamental + 2nd + 3rd harmonic
                val f = event.frequency
                val fundamental = sin(2.0 * PI * f * t)
                val overtone2 = 0.32 * sin(2.0 * PI * (f * 2.0) * t)
                val overtone3 = 0.12 * sin(2.0 * PI * (f * 3.0) * t)
                val noteSample = (fundamental + overtone2 + overtone3) * envelope * 0.42

                val currentVal = samples[sampleIdx].toInt()
                val addedVal = (noteSample * 32767.0).toInt()
                samples[sampleIdx] = (currentVal + addedVal).coerceIn(-32768, 32767).toShort()
            }
        }

        // --- Render Layer 1: Continuous Warm Ambient Pad + Master Fade Envelope ---
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            // Continuous peaceful drone (C4 261.63Hz + G4 392Hz) with subtle slow pulsation
            val padTremolo = 1.0 + 0.12 * sin(2.0 * PI * 0.35 * t)
            val padC4 = sin(2.0 * PI * 261.63 * t)
            val padG4 = sin(2.0 * PI * 392.00 * t)
            val padC5 = 0.5 * sin(2.0 * PI * 523.25 * t)
            val ambientPad = ((padC4 + padG4 + padC5) / 2.5) * 0.18 * padTremolo

            // Master envelope: 0.5s fade-in, smooth 1.2s fade-out at end
            val masterFadeIn = if (t < 0.5) (t / 0.5) else 1.0
            val masterFadeOut = if (t > 8.8) ((10.0 - t) / 1.2).coerceIn(0.0, 1.0) else 1.0
            val masterEnvelope = masterFadeIn * masterFadeOut

            val combined = (samples[i].toDouble() / 32767.0 + ambientPad) * masterEnvelope * 0.88
            samples[i] = (combined.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
        }

        return samples
    }
}
