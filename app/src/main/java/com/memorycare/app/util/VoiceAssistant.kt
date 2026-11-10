package com.memorycare.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Text-to-Speech Voice Assistant manager calibrated for clear, elderly-accessible speech.
 */
class VoiceAssistant(context: Context) {
    private var tts: TextToSpeech? = null
    var isReady = mutableStateOf(false)
        private set

    val isSpeaking = mutableStateOf(false)
    val currentlySpeakingText = mutableStateOf<String?>(null)

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val result = engine.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.setLanguage(Locale.ENGLISH)
                    }
                    // 0.88x speech rate for clear, elder-friendly pacing
                    engine.setSpeechRate(0.88f)
                    engine.setPitch(1.0f)
                    isReady.value = true
                }
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking.value = false
                currentlySpeakingText.value = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking.value = false
                currentlySpeakingText.value = null
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isSpeaking.value = false
                currentlySpeakingText.value = null
            }
        })
    }

    private var currentLocale: Locale = Locale.ENGLISH

    /**
     * Dynamically sets language locale for TTS synthesis (e.g. Hindi, Telugu, Tamil, Bengali).
     */
    fun setLanguage(locale: Locale): Boolean {
        currentLocale = locale
        val engine = tts ?: return false
        val result = engine.setLanguage(locale)
        return if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.ENGLISH)
            false
        } else {
            true
        }
    }

    fun getCurrentLocale(): Locale = currentLocale

    /**
     * Speaks the provided text out loud using Android TTS engine.
     */
    fun speak(text: String, utteranceId: String = "utterance_${System.currentTimeMillis()}") {
        if (text.isBlank()) return
        currentlySpeakingText.value = text
        isSpeaking.value = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Stops current speech playback.
     */
    fun stop() {
        tts?.stop()
        isSpeaking.value = false
        currentlySpeakingText.value = null
    }

    /**
     * Releases TTS resources.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isSpeaking.value = false
        currentlySpeakingText.value = null
    }
}

/**
 * Rememberable, lifecycle-aware Text-to-Speech Voice Assistant composable helper.
 */
@Composable
fun rememberVoiceAssistant(): VoiceAssistant {
    val context = LocalContext.current
    val voiceAssistant = remember { VoiceAssistant(context) }

    DisposableEffect(voiceAssistant) {
        onDispose {
            voiceAssistant.shutdown()
        }
    }

    return voiceAssistant
}
