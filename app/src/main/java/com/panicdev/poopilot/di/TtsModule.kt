package com.panicdev.poopilot.di

import android.content.Context
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * TTS(Text-To-Speech, 음성 합성) 관련 의존성을 제공하는 Hilt 모듈입니다.
 *
 * TTS는 텍스트를 사람의 목소리처럼 읽어주는 기능입니다.
 * 이 모듈은 앱이 사용자에게 음성으로 안내나 응답을 전달할 때 사용하는
 * [TextToSpeech] 인스턴스를 앱 전체에 공급합니다.
 *
 * [SingletonComponent]에 설치되므로 앱이 살아있는 동안 단 하나의 인스턴스만 유지됩니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object TtsModule {

    /**
     * 음성 합성(TTS) 인스턴스를 제공합니다.
     *
     * [Mode.HYBRID] 모드로 초기화되며, 이는 온디바이스(기기 내) 합성과
     * 클라우드 합성을 상황에 따라 혼합하여 사용하는 방식입니다.
     * 네트워크 환경에 따라 더 자연스러운 음성을 선택해 출력합니다.
     *
     * @param context [TextToSpeech] 초기화에 필요한 앱 Context
     * @return 앱 전역에서 공유할 [TextToSpeech] 인스턴스
     */
    @Provides
    @Singleton
    fun provideTextToSpeech(
        @ApplicationContext context: Context
    ): TextToSpeech = TextToSpeech(context, Mode.HYBRID)
}
