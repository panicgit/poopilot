package com.panicdev.poopilot.di

import android.content.Context
import ai.pleos.playground.vehicle.Vehicle
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 차량 연동 관련 의존성을 제공하는 Hilt 모듈입니다.
 *
 * [Vehicle]은 차량 내부 시스템(예: 헤드유닛, 차량 상태 정보 등)과
 * 앱을 연결해 주는 인터페이스 역할을 합니다.
 * 이 모듈은 차량 연동 기능이 필요한 곳 어디서든 동일한 인스턴스를 사용할 수 있도록 공급합니다.
 *
 * [SingletonComponent]에 설치되므로 앱이 살아있는 동안 단 하나의 인스턴스만 유지됩니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object VehicleModule {

    /**
     * 차량 연동 인스턴스를 제공합니다.
     *
     * 차량의 상태 정보를 읽거나 차량 시스템과 상호작용할 때 사용됩니다.
     * 앱이 실행되는 동안 단 한 번만 생성되며, 필요한 곳에 자동으로 주입됩니다.
     *
     * @param context [Vehicle] 초기화에 필요한 앱 Context
     * @return 앱 전역에서 공유할 [Vehicle] 인스턴스
     */
    @Provides
    @Singleton
    fun provideVehicle(
        @ApplicationContext context: Context
    ): Vehicle = Vehicle(context)
}
