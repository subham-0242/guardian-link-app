package com.example.util

import android.content.Context
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

    fun speak(text: String, languageName: String = "English", onComplete: (() -> Unit)? = null) {
        if (!isInitialized || tts == null) return

        val locale = getLocaleForLanguage(languageName)
        tts?.language = locale
        val utteranceId = "emergency_tts_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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
