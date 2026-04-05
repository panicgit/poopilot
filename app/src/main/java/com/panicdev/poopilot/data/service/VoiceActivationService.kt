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

/**
 * 음성 명령으로 급똥모드를 활성화하는 서비스 클래스입니다.
 *
 * 백그라운드에서 마이크를 통해 사용자의 음성을 지속적으로 감지하며,
 * "급똥모드", "화장실", "급해" 등의 키워드가 인식되면 [activationEvents] Flow를 통해
 * 상위 컴포넌트에 알립니다.
 *
 * 오류 발생 시 최대 [MAX_RETRIES]회까지 지수 백오프 방식으로 재시도하며,
 * 횟수를 초과하면 자동으로 서비스를 중지합니다.
 */
@Singleton
class VoiceActivationService @Inject constructor(
    private val sttRepository: SttRepository,
    private val settingsRepository: SettingsRepository
) {
    /** 현재 음성 감지 서비스가 활성 상태인지 여부 */
    @Volatile
    private var isActive = false

    /** STT 이벤트를 모니터링하는 코루틴 Job */
    private var monitorJob: Job? = null

    /** 연속 오류 발생 횟수 (재시도 제한에 사용됩니다) */
    private var errorRetryCount = 0

    /** 내부적으로 활성화 이벤트를 발행하는 MutableSharedFlow */
    private val _activationEvents = MutableSharedFlow<String>(extraBufferCapacity = 5)

    /** 외부에서 구독할 수 있는 음성 활성화 이벤트 스트림. 감지된 키워드 문자열을 전달합니다. */
    val activationEvents: SharedFlow<String> = _activationEvents

    /**
     * 음성 감지 서비스를 시작합니다.
     *
     * 이미 실행 중이거나 설정에서 음성 명령이 비활성화된 경우에는 시작하지 않습니다.
     * STT 이벤트를 수집하는 코루틴을 [scope] 내에서 실행하고, 즉시 음성 인식을 시작합니다.
     *
     * @param scope 코루틴을 실행할 [CoroutineScope]. 일반적으로 ViewModel의 scope를 전달합니다.
     */
    fun start(scope: CoroutineScope) {
        if (isActive) return
        if (!settingsRepository.voiceCommandEnabled) {
            Log.d(TAG, "Voice command disabled in settings")
            return
        }

        isActive = true
        errorRetryCount = 0
        sttRepository.initialize()

        monitorJob = scope.launch {
            sttRepository.sttEvents.collect { event ->
                when (event) {
                    is SttEvent.KeywordDetected -> {
                        // 키워드 감지 시 활성화 이벤트를 발행하고 음성 인식을 일시 중지합니다
                        Log.d(TAG, "Activation keyword: ${event.keyword}")
                        errorRetryCount = 0
                        _activationEvents.tryEmit(event.keyword)
                        sttRepository.stopListening()
                    }
                    is SttEvent.RecognitionStarted -> {
                        // 인식이 정상 시작되면 오류 카운트를 초기화합니다
                        errorRetryCount = 0
                    }
                    is SttEvent.RecognitionEnded -> {
                        // 인식이 끝나면 잠시 후 다시 시작하여 지속적으로 감지합니다
                        if (isActive && settingsRepository.voiceCommandEnabled) {
                            delay(500L)
                            sttRepository.startListening()
                        }
                    }
                    is SttEvent.Error -> {
                        // 오류 발생 시 지수 백오프로 재시도하고, 한도 초과 시 서비스를 중지합니다
                        if (isActive && errorRetryCount < MAX_RETRIES) {
                            errorRetryCount++
                            val backoffMs = 2_000L * errorRetryCount
                            Log.w(TAG, "STT error, retry $errorRetryCount/$MAX_RETRIES in ${backoffMs}ms")
                            delay(backoffMs)
                            sttRepository.startListening()
                        } else if (errorRetryCount >= MAX_RETRIES) {
                            Log.e(TAG, "Max retries ($MAX_RETRIES) reached, stopping voice activation")
                            stop()
                        }
                    }
                    else -> {}
                }
            }
        }

        sttRepository.startListening()
        Log.d(TAG, "Voice activation service started")
    }

    /**
     * 음성 감지 서비스를 중지합니다.
     *
     * 모니터링 코루틴을 취소하고 STT 음성 인식을 중지합니다.
     * 오류 카운트도 초기화됩니다.
     */
    fun stop() {
        isActive = false
        monitorJob?.cancel()
        monitorJob = null
        sttRepository.stopListening()
        errorRetryCount = 0
        Log.d(TAG, "Voice activation service stopped")
    }

    /** 현재 음성 감지 서비스가 실행 중인지 여부를 반환합니다. */
    fun isRunning(): Boolean = isActive

    companion object {
        /** 로그 태그 */
        private const val TAG = "VoiceActivation"

        /** STT 오류 발생 시 최대 재시도 횟수 */
        private const val MAX_RETRIES = 5
    }
}
