package com.panicdev.poopilot.data.repository

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.panicdev.poopilot.BuildConfig
import com.panicdev.poopilot.data.api.PublicRestroomApi
import com.panicdev.poopilot.data.model.KakaoPlace
import com.panicdev.poopilot.data.model.PublicRestroom
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 공공 데이터 포털 API를 통해 공중화장실 정보를 조회하는 저장소(Repository)입니다.
 *
 * 카카오 로컬 API와 달리, 정부에서 제공하는 공공 화장실 데이터를 가져옵니다.
 * 가져온 데이터를 앱 내부에서 공통으로 사용하는 [KakaoPlace] 형식으로 변환하고,
 * 현재 위치를 기준으로 지정된 반경 내에 있는 화장실만 필터링하여 반환합니다.
 */
@Singleton
class PublicRestroomRepository @Inject constructor(
    private val publicRestroomApi: PublicRestroomApi,
    @ApplicationContext private val context: Context
) {
    /**
     * 주어진 위도/경도를 기준으로 반경 내의 공중화장실 목록을 검색합니다.
     *
     * API 키가 설정되지 않은 경우 빈 목록을 반환합니다.
     * 거리 계산은 Haversine 공식을 사용하며, 결과는 가까운 순서로 정렬됩니다.
     *
     * @param latitude 현재 위치의 위도
     * @param longitude 현재 위치의 경도
     * @param radius 검색 반경 (단위: 미터, 기본값 1000m)
     * @return 반경 내 공중화장실 목록 또는 실패 시 에러를 담은 [Result]
     */
    suspend fun searchNearbyPublicRestrooms(
        latitude: Double,
        longitude: Double,
        radius: Int = 1000
    ): Result<List<KakaoPlace>> {
        if (PUBLIC_API_KEY.isBlank()) {
            Log.w(TAG, "PUBLIC_API_KEY not configured, skipping public restroom search")
            return Result.success(emptyList())
        }
        return try {
            // 좌표를 주소로 변환하여 해당 시/구 지역만 필터링 (전국 랜덤 100건 방지)
            val addressKeyword = getDistrictName(latitude, longitude)
            Log.d(TAG, "Public API search with address filter: '$addressKeyword'")
            val response = publicRestroomApi.searchPublicRestrooms(
                serviceKey = PUBLIC_API_KEY,
                roadAddr = addressKeyword
            )
            val items = response.response?.body?.items?.item ?: emptyList()
            Log.d(TAG, "Public API returned ${items.size} items, filtering by ${radius}m radius")
            val nearby = items
                .filter { it.latitude != null && it.longitude != null }
                .mapNotNull { restroom ->
                    val lat = restroom.latitude?.toDoubleOrNull() ?: return@mapNotNull null
                    val lng = restroom.longitude?.toDoubleOrNull() ?: return@mapNotNull null
                    val dist = calculateDistance(latitude, longitude, lat, lng)
                    if (dist <= radius) {
                        toKakaoPlace(restroom, dist)
                    } else null
                }
                .sortedBy { it.distance.toIntOrNull() ?: Int.MAX_VALUE }
            Result.success(nearby)
        } catch (e: Exception) {
            Log.e(TAG, "Public restroom search failed", e)
            Result.failure(e)
        }
    }

    /**
     * 공공 데이터의 [PublicRestroom] 객체를 앱 공통 모델인 [KakaoPlace]로 변환합니다.
     *
     * 이름, 주소, 좌표, 거리, 화장실 유형 등의 정보를 매핑하며,
     * 값이 없는 필드는 기본값으로 채웁니다.
     *
     * @param restroom 변환할 공중화장실 데이터
     * @param distance 현재 위치로부터의 거리 (단위: 미터)
     * @return [KakaoPlace] 형식으로 변환된 화장실 정보
     */
    private fun toKakaoPlace(restroom: PublicRestroom, distance: Int): KakaoPlace {
        return KakaoPlace(
            placeName = restroom.name ?: "공중화장실",
            addressName = restroom.address ?: "",
            roadAddressName = restroom.roadAddress ?: "",
            x = restroom.longitude ?: "0",
            y = restroom.latitude ?: "0",
            distance = distance.toString(),
            categoryName = "${restroom.category ?: "공중화장실"} > ${restroom.toiletType ?: "일반"}",
            phone = restroom.phone ?: ""
        )
    }

    /**
     * 두 좌표 사이의 거리를 Haversine 공식으로 계산합니다.
     *
     * 지구를 구체로 가정하여 위도/경도 차이를 실제 거리(미터)로 변환합니다.
     *
     * @param lat1 출발 지점의 위도
     * @param lon1 출발 지점의 경도
     * @param lat2 도착 지점의 위도
     * @param lon2 도착 지점의 경도
     * @return 두 지점 사이의 거리 (단위: 미터, 정수 반환)
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Int {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toInt()
    }

    /**
     * 좌표를 시/구 단위 주소로 변환합니다.
     * 예: (37.4127, 127.0947) → "성남시 분당구"
     * 변환 실패 시 빈 문자열을 반환합니다 (전체 검색으로 폴백).
     */
    @Suppress("DEPRECATION")
    private fun getDistrictName(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.KOREA)
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                // 시/구 단위로 필터링 (예: "성남시 분당구", "서울특별시 강남구")
                val locality = addr.locality ?: addr.subAdminArea ?: ""
                val subLocality = addr.subLocality ?: ""
                val result = "$locality $subLocality".trim()
                Log.d(TAG, "Reverse geocoded: $result (full: ${addr.getAddressLine(0)})")
                result
            } else ""
        } catch (e: Exception) {
            Log.w(TAG, "Reverse geocoding failed, searching without address filter", e)
            ""
        }
    }

    companion object {
        private const val TAG = "PublicRestroomRepo"
        private val PUBLIC_API_KEY = BuildConfig.PUBLIC_DATA_API_KEY
    }
}
