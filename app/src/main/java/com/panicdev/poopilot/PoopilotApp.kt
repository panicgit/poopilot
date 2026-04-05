package com.panicdev.poopilot

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Poopilot 앱의 Application 클래스입니다.
 *
 * Android 앱이 실행될 때 가장 먼저 생성되는 클래스로, 앱 전체의 생명주기 동안 유지됩니다.
 * 앱 수준의 전역 초기화가 필요한 경우 이 클래스에서 처리합니다.
 *
 * [@HiltAndroidApp]은 Hilt 의존성 주입 프레임워크를 앱 전체에서 사용할 수 있도록
 * 초기화하는 어노테이션입니다. 이 어노테이션이 있어야 앱 내 모든 컴포넌트에서
 * @Inject, @AndroidEntryPoint 등 Hilt 기능을 사용할 수 있습니다.
 */
@HiltAndroidApp
class PoopilotApp : Application()
