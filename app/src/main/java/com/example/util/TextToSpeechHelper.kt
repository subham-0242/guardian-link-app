package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
        } else {
            Log.e("TextToSpeechHelper", "TTS Initialization failed with status: $status")
        }
    }

    fun playPaChime(onFinish: () -> Unit = {}) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    toneGen.release()
                } catch (e: Exception) {
                    // Ignore
                }
                onFinish()
            }, 400)
        } catch (e: Exception) {
            Log.e("TextToSpeechHelper", "Tone generation failed", e)
            onFinish()
        }
    }

    fun speak(text: String, languageName: String = "English", onComplete: (() -> Unit)? = null) {
        if (!isInitialized || tts == null) return

        val locale = getLocaleForLanguage(languageName)
        tts?.language = locale
        val utteranceId = "emergency_tts_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun playPaChimeAndSpeak(text: String, languageName: String = "English", onComplete: (() -> Unit)? = null) {
        playPaChime {
            speak(text, languageName, onComplete)
        }
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        fun getLocaleForLanguage(lang: String): Locale {
            return when (lang.lowercase().trim()) {
                "spanish", "es" -> Locale("es", "ES")
                "french", "fr" -> Locale.FRANCE
                "mandarin", "chinese", "zh" -> Locale.CHINA
                "japanese", "ja" -> Locale.JAPAN
                "hindi", "hi" -> Locale("hi", "IN")
                "arabic", "ar" -> Locale("ar")
                "russian", "ru" -> Locale("ru", "RU")
                "german", "de" -> Locale.GERMANY
                "tagalog", "filipino", "tl" -> Locale("tl", "PH")
                "tamil", "ta" -> Locale("ta", "IN")
                else -> Locale.US
            }
        }
    }
}
