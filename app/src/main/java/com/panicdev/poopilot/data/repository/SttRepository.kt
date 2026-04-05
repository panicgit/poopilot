package com.panicdev.poopilot.data.repository

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class SttEvent {
    data class TextRecognized(val text: String, val completed: Boolean) : SttEvent()
    data class KeywordDetected(val keyword: String) : SttEvent()
    object RecognitionStarted : SttEvent()
    object RecognitionEnded : SttEvent()
    data class Error(val message: String) : SttEvent()
    object Ready : SttEvent()
}

@Singleton
class SttRepository @Inject constructor(
    private val speechToText: SpeechToText
) {
    @Volatile
    private var isInitialized = false

    @Volatile
    private var isListening = false

    private val _sttEvents = MutableSharedFlow<SttEvent>(extraBufferCapacity = 10)
    val sttEvents: SharedFlow<SttEvent> = _sttEvents

    private val keywords = listOf("급똥모드", "화장실", "급해")

    private val resultListener = object : ResultListener {
        override fun onUpdated(stt: String, completed: Boolean) {
            Log.d(TAG, "STT onUpdated: text='$stt', completed=$completed")
            _sttEvents.tryEmit(SttEvent.TextRecognized(stt, completed))
            if (completed) {
                checkKeywords(stt)
            }
        }

        override fun onStartedRecognition() {
            Log.d(TAG, "STT recognition started")
            _sttEvents.tryEmit(SttEvent.RecognitionStarted)
        }

        override fun onEndedRecognition() {
            Log.d(TAG, "STT recognition ended")
            isListening = false
            _sttEvents.tryEmit(SttEvent.RecognitionEnded)
        }

        override fun onError() {
            Log.e(TAG, "STT recognition error")
            isListening = false
            _sttEvents.tryEmit(SttEvent.Error("음성 인식 오류"))
        }

        override fun onReady() {
            Log.d(TAG, "STT ready")
            _sttEvents.tryEmit(SttEvent.Ready)
        }

        override fun onUpdatedEpdData(on: Long, off: Long) {
            // EPD data for voice activity detection
        }

        override fun onUpdatedRms(rms: Float) {
            // RMS audio level updates
        }
    }

    @Synchronized
    fun initialize() {
        if (isInitialized) return
        try {
            speechToText.initialize()
            speechToText.addListener(resultListener)
            isInitialized = true
            Log.d(TAG, "STT initialized")
        } catch (e: Exception) {
            Log.e(TAG, "STT initialization failed", e)
            _sttEvents.tryEmit(SttEvent.Error("음성 인식 초기화 실패"))
        }
    }

    @Synchronized
    fun startListening() {
        if (!isInitialized) initialize()
        if (isListening) return
        try {
            isListening = true
            speechToText.request()
            Log.d(TAG, "STT listening started")
        } catch (e: Exception) {
            isListening = false
            Log.e(TAG, "STT start listening failed", e)
            _sttEvents.tryEmit(SttEvent.Error("음성 인식 시작 실패"))
        }
    }

    @Synchronized
    fun stopListening() {
        if (!isListening) return
        try {
            speechToText.stop()
            isListening = false
            Log.d(TAG, "STT listening stopped")
        } catch (e: Exception) {
            Log.e(TAG, "STT stop listening failed", e)
        }
    }

    @Synchronized
    fun release() {
        try {
            stopListening()
            speechToText.removeListener(resultListener)
            speechToText.release()
            isInitialized = false
            Log.d(TAG, "STT released")
        } catch (e: Exception) {
            Log.e(TAG, "STT release failed", e)
        }
    }

    fun isCurrentlyListening(): Boolean = isListening

    private fun checkKeywords(text: String) {
        val lowerText = text.lowercase().trim()
        for (keyword in keywords) {
            if (lowerText.contains(keyword)) {
                Log.d(TAG, "Keyword detected: $keyword in '$text'")
                _sttEvents.tryEmit(SttEvent.KeywordDetected(keyword))
                return
            }
        }
    }

    companion object {
        private const val TAG = "SttRepository"
    }
}
