package com.panicdev.poopilot.data.repository

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.EventListener
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsRepository @Inject constructor(
    private val textToSpeech: TextToSpeech,
    @ApplicationContext private val context: Context
) {
    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private var audioFocusRequest: AudioFocusRequest? = null

    private val ttsEventListener = object : EventListener {
        override fun onDone() {
            abandonAudioFocus()
        }
        override fun onError(message: String) {
            Log.e(TAG, "TTS error: $message")
            abandonAudioFocus()
        }
        override fun onReady() {}
        override fun onStart() {}
        override fun onStop() {
            abandonAudioFocus()
        }
        override fun onUpdatedRms(rms: Double) {}
    }
    @Volatile
    private var isInitialized = false

    @Synchronized
    fun initialize() {
        if (isInitialized) return
        try {
            textToSpeech.initialize()
            textToSpeech.addEventListener(ttsEventListener)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .build()
            textToSpeech.setAudioAttributes(audioAttributes)
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setWillPauseWhenDucked(false)
                .build()
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
            requestAudioFocus()
            textToSpeech.speak(text)
            Log.d(TAG, "TTS speak: $text")
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak failed", e)
            abandonAudioFocus()
        }
    }

    fun stop() {
        try {
            textToSpeech.stop()
            abandonAudioFocus()
        } catch (e: Exception) {
            Log.e(TAG, "TTS stop failed", e)
        }
    }

    private fun requestAudioFocus() {
        audioFocusRequest?.let { request ->
            audioManager.requestAudioFocus(request)
        }
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { request ->
            audioManager.abandonAudioFocusRequest(request)
        }
    }

    fun release() {
        try {
            abandonAudioFocus()
            textToSpeech.removeEventListener(ttsEventListener)
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
