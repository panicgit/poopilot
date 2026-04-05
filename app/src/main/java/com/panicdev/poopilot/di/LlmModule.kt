package com.panicdev.poopilot.di

import android.content.Context
import ai.pleos.playground.llm.LLM
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LlmModule {

    @Provides
    @Singleton
    fun provideLlm(
        @ApplicationContext context: Context
    ): LLM = LLM(context)
}
