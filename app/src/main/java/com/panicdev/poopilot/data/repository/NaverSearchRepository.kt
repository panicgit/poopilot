package com.panicdev.poopilot.data.repository

import android.util.Log
import com.panicdev.poopilot.BuildConfig
import com.panicdev.poopilot.data.api.NaverSearchApi
import com.panicdev.poopilot.data.model.KakaoPlace
import com.panicdev.poopilot.data.model.NaverLocalItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 네이버 지역 검색 API를 이용하여 주변 화장실을 검색하는 Repository입니다.
 *
 * 네이버 검색 API 키(Client ID, Secret)가 설정된 경우에만 실제 검색을 수행하며,
 * API 키가 없으면 빈 목록을 반환합니다.
 * 검색 결과는 앱 내부에서 공통으로 사용하는 [KakaoPlace] 형식으로 변환됩니다.
 */
@Singleton
class NaverSearchRepository @Inject constructor(
    /** 네이버 지역 검색 API 통신 인터페이스 */
    private val naverSearchApi: NaverSearchApi
) {
    /**
     * 주어진 위치(위도/경도) 근처의 화장실을 네이버 지역 검색 API로 검색합니다.
     *
     * API 키가 설정되지 않은 경우 서버 호출 없이 빈 목록을 성공으로 반환합니다.
     *
     * @param latitude 검색 기준 위도
     * @param longitude 검색 기준 경도
     * @return 성공 시 주변 화장실 목록([KakaoPlace] 형식), 실패 시 예외를 담은 [Result]
     */
    suspend fun searchNearbyRestrooms(
        latitude: Double,
        longitude: Double
    ): Result<List<KakaoPlace>> {
        // API 키가 설정되지 않은 경우 네이버 검색을 건너뜁니다
        if (NAVER_CLIENT_ID.isBlank() || NAVER_CLIENT_SECRET.isBlank()) {
            Log.w(TAG, "NAVER_CLIENT_ID or NAVER_CLIENT_SECRET not configured, skipping naver search")
            return Result.success(emptyList())
        }
        return try {
            val response = naverSearchApi.searchLocal(
                clientId = NAVER_CLIENT_ID,
                clientSecret = NAVER_CLIENT_SECRET,
                query = "화장실",
                display = 5,
                sort = "random"
            )
            // 네이버 검색 결과를 앱 공통 모델(KakaoPlace)로 변환
            val places = response.items.map { toKakaoPlace(it) }
            Result.success(places)
        } catch (e: Exception) {
            Log.e(TAG, "Naver local search failed", e)
            Result.failure(e)
        }
    }

    /**
     * 네이버 검색 결과 항목([NaverLocalItem])을 앱 공통 장소 모델([KakaoPlace])로 변환합니다.
     *
     * 네이버 API는 장소 이름에 HTML 태그(<b>, </b>)를 포함하므로, 이를 제거하여 순수 텍스트만 남깁니다.
     * 네이버 API는 좌표를 제공하지 않으므로 x, y 값은 "0"으로 채웁니다.
     *
     * @param item 네이버 지역 검색 결과 항목
     * @return 변환된 [KakaoPlace] 객체
     */
    private fun toKakaoPlace(item: NaverLocalItem): KakaoPlace {
        return KakaoPlace(
            placeName = item.title.replace("<b>", "").replace("</b>", ""),
            addressName = item.address,
            roadAddressName = item.roadAddress,
            x = "0",
            y = "0",
            distance = "",
            categoryName = item.category,
            phone = item.telephone
        )
    }

    companion object {
        private const val TAG = "NaverSearchRepo"
        /** BuildConfig에서 읽어온 네이버 API 클라이언트 ID */
        private val NAVER_CLIENT_ID = BuildConfig.NAVER_CLIENT_ID
        /** BuildConfig에서 읽어온 네이버 API 클라이언트 시크릿 */
        private val NAVER_CLIENT_SECRET = BuildConfig.NAVER_CLIENT_SECRET
    }
}
