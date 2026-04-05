package com.panicdev.poopilot.di

import com.panicdev.poopilot.BuildConfig
import com.panicdev.poopilot.data.api.KakaoLocalApi
import com.panicdev.poopilot.data.api.PublicRestroomApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
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

    @Provides
    @Singleton
    fun providePublicRestroomApi(client: OkHttpClient): PublicRestroomApi {
        return Retrofit.Builder()
            .baseUrl("https://api.odcloud.kr/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PublicRestroomApi::class.java)
    }
}
