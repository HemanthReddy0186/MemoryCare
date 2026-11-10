package com.memorycare.app.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.memorycare.app.model.SoundscapeProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Pure offline acoustic waveform synthesizer for therapeutic memory soundscapes.
 * Uses Android's native AudioTrack with PCM 16-bit DSP synthesis.
 * Zero external audio assets, downloads, or network permissions required.
 */
class SoundscapeSynthesizer {

    private val sampleRate = 44100
    private var activeTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays the specified soundscape asynchronously.
     */
    fun playSoundscape(profile: SoundscapeProfile, onCompleted: (() -> Unit)? = null) {
        stop()
        playbackJob = scope.launch {
            try {
                val pcmSamples = when (profile) {
                    SoundscapeProfile.TEMPLE_BELL -> generateTempleBellPCM()
                    SoundscapeProfile.MONSOON_RAIN -> generateMonsoonRainPCM()
                    SoundscapeProfile.PRESSURE_COOKER -> generatePressureCookerPCM()
                    SoundscapeProfile.STEAM_TRAIN -> generateSteamTrainPCM()
                    SoundscapeProfile.MORNING_SPARROWS -> generateMorningSparrowsPCM()
                }

                if (!isActive) return@launch
                playPcmBuffer(pcmSamples)

                withContext(Dispatchers.Main) {
                    onCompleted?.invoke()
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Plays a crisp, satisfying mechanical clock tick click (e.g. when rotating the clock hand).
     */
    fun playClockTick() {
        scope.launch {
            try {
                val tickSamples = generateClockTickPCM()
                playPcmBuffer(tickSamples)
            } catch (_: Exception) {}
        }
    }

    private suspend fun playPcmBuffer(buffer: ShortArray) = withContext(Dispatchers.IO) {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(buffer.size * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        activeTrack = track

        try {
            track.play()
            track.write(buffer, 0, buffer.size)
            // Wait slightly for audio buffer to flush to speaker
            val durationMs = (buffer.size.toLong() * 1000L) / sampleRate
            kotlinx.coroutines.delay(durationMs)
        } catch (_: Exception) {
        } finally {
            try {
                track.stop()
                track.release()
            } catch (_: Exception) {}
            if (activeTrack == track) activeTrack = null
        }
    }

    /**
     * Stops any currently playing synthesized audio.
     */
    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            activeTrack?.stop()
            activeTrack?.release()
        } catch (_: Exception) {}
        activeTrack = null
    }

    // =========================================================================
    // DSP Acoustic Synthesis Algorithms
    // =========================================================================

    /**
     * Sacred Temple Brass Bell:
     * Resonant multi-harmonic sine waves (528 Hz fundamental + 1056 Hz + 1584 Hz)
     * with slow exponential envelope decay and gentle brass shimmer.
     */
    private fun generateTempleBellPCM(): ShortArray {
        val durationSeconds = 3.2
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val buffer = ShortArray(totalSamples)

        val f1 = 528.0 // Auspicious solfeggio frequency
        val f2 = 1056.0
        val f3 = 1584.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            // Exponential decay envelope
            val envelope = exp(-1.8 * t)
            val shimmer = 1.0 + 0.08 * sin(2.0 * PI * 6.0 * t)

            val s1 = sin(2.0 * PI * f1 * t) * 0.65
            val s2 = sin(2.0 * PI * f2 * t) * 0.25
            val s3 = sin(2.0 * PI * f3 * t) * 0.10

            val sample = (s1 + s2 + s3) * envelope * shimmer
            buffer[i] = (sample * 28000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /**
     * Courtyard Monsoon Rain:
     * Filtered Pink/Brownian noise with rhythmic droplet impulse surges.
     */
    private fun generateMonsoonRainPCM(): ShortArray {
        val durationSeconds = 3.2
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val buffer = ShortArray(totalSamples)
        val random = Random(42)

        var lastSample = 0.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val white = (random.nextDouble() * 2.0 - 1.0)
            // Low-pass brownian filter
            lastSample = (lastSample + (0.025 * white)) / 1.025

            // Droplet impulse modulation
            val swell = 0.7 + 0.3 * sin(2.0 * PI * 0.8 * t)
            val droplet = if (random.nextDouble() < 0.003) 0.6 else 0.0

            val sample = (lastSample * 3.5 + droplet) * swell
            buffer[i] = (sample * 16000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /**
     * Kitchen Pressure Cooker:
     * Rising steam hiss that builds up into the signature high-pressure steam whistle burst.
     */
    private fun generatePressureCookerPCM(): ShortArray {
        val durationSeconds = 2.8
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val buffer = ShortArray(totalSamples)
        val random = Random(101)

        val whistleStart = (sampleRate * 0.8).toInt()
        val whistleFreq = 2250.0

        var noiseState = 0.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val white = random.nextDouble() * 2.0 - 1.0
            noiseState = (noiseState * 0.85) + (white * 0.15)

            val sample = if (i < whistleStart) {
                // Rising pre-steam hiss
                val prog = i.toDouble() / whistleStart
                noiseState * (0.2 + 0.4 * prog)
            } else {
                // Full high-pressure steam whistle burst (Chhhhhhh-Whistle!)
                val whistleProgress = (i - whistleStart).toDouble() / (totalSamples - whistleStart)
                val decay = 1.0 - (whistleProgress * 0.4)
                val whistleSine = sin(2.0 * PI * (whistleFreq + 80.0 * sin(2.0 * PI * 18.0 * t)) * t) * 0.55
                (noiseState * 0.45 + whistleSine) * decay
            }
            buffer[i] = (sample * 24000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /**
     * Vintage Steam Train:
     * Double-chime brass train whistle (440 Hz + 554 Hz) with natural acoustic swell and rhythmic chug beats.
     */
    private fun generateSteamTrainPCM(): ShortArray {
        val durationSeconds = 3.0
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val buffer = ShortArray(totalSamples)
        val random = Random(7)

        val f1 = 440.0 // A4
        val f2 = 554.37 // C#5 major third chord

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            // Swell envelope: smooth attack, sustained, smooth release
            val env = when {
                t < 0.4 -> t / 0.4
                t > 2.2 -> (3.0 - t) / 0.8
                else -> 1.0
            }

            // Dual tone train whistle
            val whistle = (sin(2.0 * PI * f1 * t) * 0.5 + sin(2.0 * PI * f2 * t) * 0.5)

            // Rhythmic chug background (2 beats per second)
            val chugPhase = (t * 2.5) % 1.0
            val chugNoise = if (chugPhase < 0.25) (random.nextDouble() * 2.0 - 1.0) * 0.25 else 0.0

            val sample = (whistle * 0.75 + chugNoise) * env
            buffer[i] = (sample * 25000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /**
     * Morning Courtyard Sparrows:
     * Frequency-modulated melodic bird chirps with natural pauses.
     */
    private fun generateMorningSparrowsPCM(): ShortArray {
        val durationSeconds = 3.0
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate

            // 3 chirp bursts across the 3 seconds: [0.3s - 0.7s], [1.2s - 1.6s], [2.1s - 2.5s]
            val chirp1 = renderSingleChirp(t - 0.3)
            val chirp2 = renderSingleChirp(t - 1.2)
            val chirp3 = renderSingleChirp(t - 2.1)

            val sample = chirp1 + chirp2 + chirp3
            buffer[i] = (sample * 26000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun renderSingleChirp(localT: Double): Double {
        if (localT < 0.0 || localT > 0.35) return 0.0
        // Rapid pitch drop/rise curve (2800 Hz -> 4200 Hz -> 3200 Hz)
        val norm = localT / 0.35
        val freq = 2900.0 + 1300.0 * sin(PI * norm)
        val env = sin(PI * norm)
        return sin(2.0 * PI * freq * localT) * env * 0.8
    }

    /**
     * Crisp mechanical clock tick for tactile clock-face dragging.
     */
    private fun generateClockTickPCM(): ShortArray {
        val durationSeconds = 0.035
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val buffer = ShortArray(totalSamples)
        val random = Random()

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-120.0 * t)
            val click = sin(2.0 * PI * 1850.0 * t) * 0.6 + (random.nextDouble() * 2.0 - 1.0) * 0.4
            buffer[i] = (click * env * 22000).toInt().toShort()
        }
        return buffer
    }
}
