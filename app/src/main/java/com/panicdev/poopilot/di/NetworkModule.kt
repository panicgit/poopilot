package com.panicdev.poopilot.di

import com.panicdev.poopilot.BuildConfig
import com.panicdev.poopilot.data.api.KakaoLocalApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideKakaoLocalApi(client: OkHttpClient): KakaoLocalApi {
        return Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KakaoLocalApi::class.java)
    }
}
