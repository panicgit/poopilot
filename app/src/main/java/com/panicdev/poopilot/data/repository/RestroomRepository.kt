package com.panicdev.poopilot.data.repository

import com.panicdev.poopilot.BuildConfig
import com.panicdev.poopilot.data.api.KakaoLocalApi
import com.panicdev.poopilot.data.model.KakaoPlace
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 카카오 로컬 API를 통해 주변 화장실 정보를 검색하는 저장소(Repository)입니다.
 *
 * "화장실" 키워드로 카카오 장소 검색을 수행하며,
 * 현재 위치 기준으로 가까운 순서대로 결과를 반환합니다.
 * API 인증 키는 BuildConfig에서 자동으로 주입됩니다.
 */
@Singleton
class RestroomRepository @Inject constructor(
    private val kakaoLocalApi: KakaoLocalApi
) {
    /**
     * 주어진 위도/경도를 기준으로 반경 내의 화장실 목록을 카카오 API로 검색합니다.
     *
     * 결과는 거리순(distance)으로 정렬되어 반환됩니다.
     *
     * @param latitude 현재 위치의 위도
     * @param longitude 현재 위치의 경도
     * @param radius 검색 반경 (단위: 미터, 기본값 1000m)
     * @return 검색된 화장실 목록 또는 실패 시 에러를 담은 [Result]
     */
    suspend fun searchNearbyRestrooms(
        latitude: Double,
        longitude: Double,
        radius: Int = 1000
    ): Result<List<KakaoPlace>> {
        return try {
            val response = kakaoLocalApi.searchByKeyword(
                apiKey = "KakaoAK ${getApiKey()}",
                query = "화장실",
                longitude = longitude.toString(),
                latitude = latitude.toString(),
                radius = radius,
                sort = "distance"
            )
            Result.success(response.documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** BuildConfig에서 카카오 API 키를 가져옵니다. */
    private fun getApiKey(): String = BuildConfig.KAKAO_API_KEY
}
