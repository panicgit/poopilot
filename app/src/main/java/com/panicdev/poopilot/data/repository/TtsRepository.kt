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

/**
 * TTS(Text-To-Speech, 텍스트 음성 변환) 기능을 관리하는 저장소(Repository)입니다.
 *
 * 텍스트를 음성으로 읽어주는 기능을 제공하며, 음성 안내 시 다른 앱의 소리를 방해하지 않도록
 * Android 오디오 포커스(Audio Focus)를 적절하게 요청하고 해제합니다.
 * 내비게이션 안내 스타일(USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)로 오디오를 설정하여
 * 음악 등 다른 소리와 함께 재생될 수 있습니다.
 */
@Singleton
class TtsRepository @Inject constructor(
    private val textToSpeech: TextToSpeech,
    @ApplicationContext private val context: Context
) {
    /** 시스템 오디오 관리 서비스. 오디오 포커스 요청/해제에 사용됩니다. */
    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    /** 현재 오디오 포커스 요청 객체. null이면 포커스를 보유하지 않은 상태입니다. */
    private var audioFocusRequest: AudioFocusRequest? = null

    /**
     * TTS 엔진의 재생 상태 변화를 수신하는 리스너입니다.
     * 재생 완료, 오류, 중지 시 오디오 포커스를 자동으로 해제합니다.
     */
    private val ttsEventListener = object : EventListener {
        /** TTS 재생이 완료되면 오디오 포커스를 반환합니다. */
        override fun onDone() {
            abandonAudioFocus()
        }
        /** TTS 재생 중 오류가 발생하면 로그를 남기고 오디오 포커스를 반환합니다. */
        override fun onError(message: String) {
            Log.e(TAG, "TTS error: $message")
            abandonAudioFocus()
        }
        /** TTS 엔진 준비 완료 시 호출됩니다. */
        override fun onReady() {}
        /** TTS 재생이 시작될 때 호출됩니다. */
        override fun onStart() {}
        /** TTS 재생이 중지되면 오디오 포커스를 반환합니다. */
        override fun onStop() {
            abandonAudioFocus()
        }
        /** TTS 재생 음량(RMS) 값이 업데이트될 때 호출됩니다. */
        override fun onUpdatedRms(rms: Double) {}
    }

    /** TTS 엔진 초기화 완료 여부 */
    @Volatile
    private var isInitialized = false

    /**
     * TTS 엔진을 초기화하고 오디오 속성 및 포커스 요청을 설정합니다.
     * 이미 초기화된 경우 중복 실행을 방지합니다.
     */
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

    /**
     * 주어진 텍스트를 음성으로 읽어줍니다.
     * 초기화가 되지 않은 경우 자동으로 초기화를 먼저 수행합니다.
     * 재생 전 오디오 포커스를 요청하여 다른 앱의 소리와 균형을 맞춥니다.
     *
     * @param text 읽어줄 텍스트 내용
     */
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

    /**
     * 현재 재생 중인 TTS 음성을 중지합니다.
     * 재생 중지 후 오디오 포커스를 반환합니다.
     */
    fun stop() {
        try {
            textToSpeech.stop()
            abandonAudioFocus()
        } catch (e: Exception) {
            Log.e(TAG, "TTS stop failed", e)
        }
    }

    /**
     * 시스템에 오디오 포커스를 요청합니다.
     * 포커스를 얻으면 다른 앱의 음량이 일시적으로 줄어들 수 있습니다(ducking).
     */
    private fun requestAudioFocus() {
        audioFocusRequest?.let { request ->
            audioManager.requestAudioFocus(request)
        }
    }

    /**
     * 보유 중인 오디오 포커스를 시스템에 반환합니다.
     * TTS 재생이 끝나거나 중단될 때 호출됩니다.
     */
    private fun abandonAudioFocus() {
        audioFocusRequest?.let { request ->
            audioManager.abandonAudioFocusRequest(request)
        }
    }

    /**
     * TTS 엔진을 완전히 해제하고 리소스를 정리합니다.
     * 앱 종료 또는 기능 비활성화 시 호출해야 합니다.
     */
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
        /** 로그 태그 */
        private const val TAG = "TtsRepository"
    }
}
