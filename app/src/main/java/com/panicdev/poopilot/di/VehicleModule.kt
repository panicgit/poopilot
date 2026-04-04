package com.panicdev.poopilot.di

import android.content.Context
import ai.pleos.playground.vehicle.Vehicle
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VehicleModule {

    @Provides
    @Singleton
    fun provideVehicle(
        @ApplicationContext context: Context
    ): Vehicle = Vehicle(context)
}
