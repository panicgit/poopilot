package com.panicdev.poopilot.di

import com.panicdev.poopilot.BuildConfig
import com.panicdev.poopilot.data.api.KakaoLocalApi
import com.panicdev.poopilot.data.api.NaverSearchApi
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

/**
 * 네트워크 통신 관련 의존성을 제공하는 Hilt 모듈입니다.
 *
 * 이 모듈은 HTTP 클라이언트([OkHttpClient])와 각 외부 API 서비스 인터페이스를
 * 생성하고 관리합니다. 앱에서 사용하는 외부 API는 다음과 같습니다.
 *
 * - 카카오 로컬 API: 장소 검색, 주소 변환 등
 * - 공공데이터 포털 API: 공공 화장실 정보 조회
 * - 네이버 검색 API: 장소 검색 보완
 *
 * [SingletonComponent]에 설치되므로 앱 전체에서 동일한 HTTP 클라이언트를 재사용합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * 앱 전체에서 공유할 OkHttp HTTP 클라이언트를 제공합니다.
     *
     * 다음과 같은 설정이 적용됩니다.
     * - 연결 타임아웃: 5초
     * - 읽기 타임아웃: 10초
     * - 쓰기 타임아웃: 10초
     * - 로깅: 디버그 빌드에서는 요청/응답 본문 전체를 출력하고,
     *   릴리즈 빌드에서는 로그를 남기지 않아 민감한 정보를 보호합니다.
     *
     * @return 설정이 적용된 [OkHttpClient] 인스턴스
     */
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

    /**
     * 카카오 로컬 API 서비스 인터페이스를 제공합니다.
     *
     * 카카오 로컬 API는 키워드로 장소를 검색하거나, 좌표를 주소로 변환하는 등
     * 위치 기반 서비스에 필요한 기능을 제공합니다.
     *
     * 기본 URL: https://dapi.kakao.com/
     *
     * @param client 네트워크 요청에 사용할 [OkHttpClient]
     * @return Retrofit으로 생성된 [KakaoLocalApi] 서비스 인스턴스
     */
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

    /**
     * 공공 화장실 정보 API 서비스 인터페이스를 제공합니다.
     *
     * 공공데이터 포털(data.go.kr)에서 제공하는 전국 공중화장실 데이터를
     * 조회할 때 사용합니다.
     *
     * 기본 URL: https://api.odcloud.kr/api/
     *
     * @param client 네트워크 요청에 사용할 [OkHttpClient]
     * @return Retrofit으로 생성된 [PublicRestroomApi] 서비스 인스턴스
     */
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

    /**
     * 네이버 검색 API 서비스 인터페이스를 제공합니다.
     *
     * 네이버 오픈 API를 통해 장소·지역 정보를 검색할 때 사용합니다.
     * 카카오 API와 함께 검색 결과의 정확도를 높이는 데 활용됩니다.
     *
     * 기본 URL: https://openapi.naver.com/
     *
     * @param client 네트워크 요청에 사용할 [OkHttpClient]
     * @return Retrofit으로 생성된 [NaverSearchApi] 서비스 인스턴스
     */
    @Provides
    @Singleton
    fun provideNaverSearchApi(client: OkHttpClient): NaverSearchApi {
        return Retrofit.Builder()
            .baseUrl("https://openapi.naver.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NaverSearchApi::class.java)
    }
}
