package com.panicdev.poopilot.data.service

import android.util.Log
import com.panicdev.poopilot.data.repository.SettingsRepository
import com.panicdev.poopilot.data.repository.SttEvent
import com.panicdev.poopilot.data.repository.SttRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceActivationService @Inject constructor(
    private val sttRepository: SttRepository,
    private val settingsRepository: SettingsRepository
) {
    @Volatile
    private var isActive = false
    private var monitorJob: Job? = null

    private val _activationEvents = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val activationEvents: SharedFlow<String> = _activationEvents

    fun start(scope: CoroutineScope) {
        if (isActive) return
        if (!settingsRepository.voiceCommandEnabled) {
            Log.d(TAG, "Voice command disabled in settings")
            return
        }

        isActive = true
        sttRepository.initialize()

        monitorJob = scope.launch {
            sttRepository.sttEvents.collect { event ->
                when (event) {
                    is SttEvent.KeywordDetected -> {
                        Log.d(TAG, "Activation keyword: ${event.keyword}")
                        _activationEvents.tryEmit(event.keyword)
                        // Stop listening after keyword detected
                        sttRepository.stopListening()
                    }
                    is SttEvent.RecognitionEnded -> {
                        // Auto-restart listening for continuous keyword detection
                        if (isActive && settingsRepository.voiceCommandEnabled) {
                            delay(500L)
                            sttRepository.startListening()
                        }
                    }
                    is SttEvent.Error -> {
                        // Retry after error with backoff
                        if (isActive) {
                            delay(2_000L)
                            sttRepository.startListening()
                        }
                    }
                    else -> {}
                }
            }
        }

        sttRepository.startListening()
        Log.d(TAG, "Voice activation service started")
    }

    fun stop() {
        isActive = false
        monitorJob?.cancel()
        monitorJob = null
        sttRepository.stopListening()
        Log.d(TAG, "Voice activation service stopped")
    }

    fun isRunning(): Boolean = isActive

    companion object {
        private const val TAG = "VoiceActivation"
    }
}
