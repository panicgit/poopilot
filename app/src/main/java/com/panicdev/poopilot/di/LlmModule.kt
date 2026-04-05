package com.panicdev.poopilot.di

import android.content.Context
import ai.pleos.playground.llm.LLM
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * LLM(대규모 언어 모델) 관련 의존성을 제공하는 Hilt 모듈입니다.
 *
 * LLM은 자연어를 이해하고 생성하는 AI 모델로, 사용자의 음성이나 텍스트 명령을
 * 해석하거나 자연스러운 응답을 만드는 데 사용됩니다.
 *
 * [SingletonComponent]에 설치되므로 앱 전체에서 단 하나의 인스턴스를 공유합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object LlmModule {

    /**
     * LLM(대규모 언어 모델) 인스턴스를 제공합니다.
     *
     * 앱이 실행되는 동안 단 한 번만 생성되며, 이후 필요한 곳에 자동으로 주입됩니다.
     *
     * @param context LLM 초기화에 필요한 앱 Context
     * @return 앱 전역에서 공유할 [LLM] 인스턴스
     */
    @Provides
    @Singleton
    fun provideLlm(
        @ApplicationContext context: Context
    ): LLM = LLM(context)
}
