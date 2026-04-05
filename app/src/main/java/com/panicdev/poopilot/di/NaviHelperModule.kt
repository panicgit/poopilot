package com.panicdev.poopilot.di

import android.content.Context
import ai.pleos.playground.navi.helper.NaviHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 내비게이션 보조 기능 관련 의존성을 제공하는 Hilt 모듈입니다.
 *
 * [NaviHelper]는 목적지 안내, 경로 탐색 등 내비게이션과 관련된 편의 기능을
 * 앱 내부에서 쉽게 활용할 수 있도록 도와주는 헬퍼 클래스입니다.
 *
 * [SingletonComponent]에 설치되므로 앱 전체에서 단 하나의 인스턴스를 공유합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object NaviHelperModule {

    /**
     * 내비게이션 헬퍼 인스턴스를 제공합니다.
     *
     * 화장실 목적지로의 경로 안내 등 내비게이션 관련 작업을 처리할 때 사용됩니다.
     * 앱이 실행되는 동안 단 한 번만 생성되며, 필요한 곳에 자동으로 주입됩니다.
     *
     * @param context [NaviHelper] 초기화에 필요한 앱 Context
     * @return 앱 전역에서 공유할 [NaviHelper] 인스턴스
     */
    @Provides
    @Singleton
    fun provideNaviHelper(
        @ApplicationContext context: Context
    ): NaviHelper = NaviHelper(context)
}
