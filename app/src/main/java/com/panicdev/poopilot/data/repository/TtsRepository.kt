package com.panicdev.poopilot.data.repository

import ai.pleos.playground.tts.TextToSpeech
import android.media.AudioAttributes
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsRepository @Inject constructor(
    private val textToSpeech: TextToSpeech
) {
    @Volatile
    private var isInitialized = false

    @Synchronized
    fun initialize() {
        if (isInitialized) return
        try {
            textToSpeech.initialize()
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .build()
            textToSpeech.setAudioAttributes(audioAttributes)
            isInitialized = true
            Log.d(TAG, "TTS initialized")
        } catch (e: Exception) {
            Log.e(TAG, "TTS initialization failed", e)
        }
    }

    fun speak(text: String) {
        if (!isInitialized) {
            initialize()
        }
        try {
            textToSpeech.speak(text)
            Log.d(TAG, "TTS speak: $text")
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak failed", e)
        }
    }

    fun stop() {
        try {
            textToSpeech.stop()
        } catch (e: Exception) {
            Log.e(TAG, "TTS stop failed", e)
        }
    }

    fun release() {
        try {
            textToSpeech.release()
            isInitialized = false
            Log.d(TAG, "TTS released")
        } catch (e: Exception) {
            Log.e(TAG, "TTS release failed", e)
        }
    }

    companion object {
        private const val TAG = "TtsRepository"
    }
}
