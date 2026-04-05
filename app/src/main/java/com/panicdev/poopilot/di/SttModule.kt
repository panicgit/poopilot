package com.panicdev.poopilot.di

import android.content.Context
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.Mode
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * STT(Speech-To-Text, 음성 인식) 관련 의존성을 제공하는 Hilt 모듈입니다.
 *
 * STT는 사용자의 말소리를 텍스트로 변환하는 기능입니다.
 * 이 모듈은 음성 명령 입력 기능을 위해 [SpeechToText] 인스턴스를 앱 전체에 공급합니다.
 *
 * [SingletonComponent]에 설치되므로 앱이 살아있는 동안 단 하나의 인스턴스만 유지됩니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object SttModule {

    /**
     * 음성 인식(STT) 인스턴스를 제공합니다.
     *
     * [Mode.HYBRID] 모드로 초기화되며, 이는 온디바이스(기기 내) 인식과
     * 클라우드 인식을 상황에 따라 혼합하여 사용하는 방식입니다.
     * 네트워크 환경이 좋지 않을 때도 기본적인 음성 인식이 가능합니다.
     *
     * @param context [SpeechToText] 초기화에 필요한 앱 Context
     * @return 앱 전역에서 공유할 [SpeechToText] 인스턴스
     */
    @Provides
    @Singleton
    fun provideSpeechToText(
        @ApplicationContext context: Context
    ): SpeechToText = SpeechToText(context, Mode.HYBRID)
}
