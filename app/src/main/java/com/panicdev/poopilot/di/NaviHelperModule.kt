package com.panicdev.poopilot.di

import android.content.Context
import ai.pleos.playground.navi.helper.NaviHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NaviHelperModule {

    @Provides
    @Singleton
    fun provideNaviHelper(
        @ApplicationContext context: Context
    ): NaviHelper = NaviHelper(context)
}
