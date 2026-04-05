package com.panicdev.poopilot.data.repository

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 음성 인식(STT) 기능에서 발생하는 모든 이벤트를 나타내는 sealed class입니다.
 *
 * 음성 인식 상태 변화나 결과를 Flow로 전달할 때 이 타입을 사용합니다.
 */
sealed class SttEvent {
    /** 음성이 텍스트로 인식되었을 때 발생합니다. [completed]가 true이면 최종 결과입니다. */
    data class TextRecognized(val text: String, val completed: Boolean) : SttEvent()

    /** 인식된 텍스트에서 미리 등록된 키워드가 감지되었을 때 발생합니다. */
    data class KeywordDetected(val keyword: String) : SttEvent()

    /** 음성 인식이 시작되었을 때 발생합니다. */
    object RecognitionStarted : SttEvent()

    /** 음성 인식이 종료되었을 때 발생합니다. */
    object RecognitionEnded : SttEvent()

    /** 음성 인식 중 오류가 발생했을 때 발생합니다. */
    data class Error(val message: String) : SttEvent()

    /** STT 엔진이 준비 완료 상태가 되었을 때 발생합니다. */
    object Ready : SttEvent()
}

/**
 * 음성 인식(STT, Speech-To-Text) 기능을 관리하는 저장소(Repository)입니다.
 *
 * STT 엔진의 초기화, 음성 인식 시작/중지, 리소스 해제를 담당합니다.
 * 음성 인식 결과와 상태 변화는 [sttEvents] Flow를 통해 외부로 전달됩니다.
 * "급똥모드", "화장실", "급해" 등 특정 키워드가 인식되면 [SttEvent.KeywordDetected]를 발행합니다.
 */
@Singleton
class SttRepository @Inject constructor(
    private val speechToText: SpeechToText
) {
    /** STT 엔진 초기화 완료 여부 */
    @Volatile
    private var isInitialized = false

    /** 현재 음성 인식 중인지 여부 */
    @Volatile
    private var isListening = false

    /** 내부적으로 이벤트를 발행하는 MutableSharedFlow */
    private val _sttEvents = MutableSharedFlow<SttEvent>(extraBufferCapacity = 10)

    /** 외부에서 구독할 수 있는 음성 인식 이벤트 스트림 */
    val sttEvents: SharedFlow<SttEvent> = _sttEvents

    /** 감지할 키워드 목록. 이 단어들이 인식되면 급똥모드가 활성화됩니다. */
    private val keywords = listOf("급똥모드", "화장실", "급해")

    /**
     * STT 엔진으로부터 결과를 수신하는 리스너입니다.
     * 인식 결과, 시작/종료, 오류, 준비 완료 등의 콜백을 처리합니다.
     */
    private val resultListener = object : ResultListener {
        /** 음성이 텍스트로 변환될 때마다 호출됩니다. 완료된 경우 키워드 검사도 수행합니다. */
        override fun onUpdated(stt: String, completed: Boolean) {
            Log.d(TAG, "STT onUpdated: text='$stt', completed=$completed")
            _sttEvents.tryEmit(SttEvent.TextRecognized(stt, completed))
            if (completed) {
                checkKeywords(stt)
            }
        }

        /** 음성 인식이 시작될 때 호출됩니다. */
        override fun onStartedRecognition() {
            Log.d(TAG, "STT recognition started")
            _sttEvents.tryEmit(SttEvent.RecognitionStarted)
        }

        /** 음성 인식이 종료될 때 호출됩니다. */
        override fun onEndedRecognition() {
            Log.d(TAG, "STT recognition ended")
            isListening = false
            _sttEvents.tryEmit(SttEvent.RecognitionEnded)
        }

        /** 음성 인식 중 오류가 발생했을 때 호출됩니다. */
        override fun onError() {
            Log.e(TAG, "STT recognition error")
            isListening = false
            _sttEvents.tryEmit(SttEvent.Error("음성 인식 오류"))
        }

        /** STT 엔진이 준비 완료 상태가 되었을 때 호출됩니다. */
        override fun onReady() {
            Log.d(TAG, "STT ready")
            _sttEvents.tryEmit(SttEvent.Ready)
        }

        /** 음성 활동 감지(EPD) 데이터가 업데이트될 때 호출됩니다. */
        override fun onUpdatedEpdData(on: Long, off: Long) {
            // EPD data for voice activity detection
        }
    }

    /**
     * STT 엔진을 초기화하고 결과 리스너를 등록합니다.
     * 이미 초기화된 경우 중복 실행을 방지합니다.
     */
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

    /**
     * 음성 인식을 시작합니다.
     * 초기화가 되지 않은 경우 자동으로 초기화를 먼저 수행합니다.
     * 이미 인식 중인 경우에는 중복 실행을 방지합니다.
     */
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

    /**
     * 음성 인식을 중지합니다.
     * 인식 중이 아닌 경우에는 아무 동작도 하지 않습니다.
     */
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

    /**
     * STT 엔진을 완전히 해제하고 리소스를 정리합니다.
     * 앱 종료 또는 기능 비활성화 시 호출해야 합니다.
     */
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

    /** 현재 음성 인식이 진행 중인지 여부를 반환합니다. */
    fun isCurrentlyListening(): Boolean = isListening

    /**
     * 인식된 텍스트에 등록된 키워드가 포함되어 있는지 확인합니다.
     * 키워드가 발견되면 [SttEvent.KeywordDetected] 이벤트를 발행하고 검사를 중단합니다.
     *
     * @param text 키워드 검사를 수행할 인식된 텍스트
     */
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
        /** 로그 태그 */
        private const val TAG = "SttRepository"
    }
}
