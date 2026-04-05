package com.panicdev.poopilot.data.repository

import android.util.Log
import com.panicdev.poopilot.BuildConfig
import com.panicdev.poopilot.data.api.NaverSearchApi
import com.panicdev.poopilot.data.model.KakaoPlace
import com.panicdev.poopilot.data.model.NaverLocalItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NaverSearchRepository @Inject constructor(
    private val naverSearchApi: NaverSearchApi
) {
    suspend fun searchNearbyRestrooms(
        latitude: Double,
        longitude: Double
    ): Result<List<KakaoPlace>> {
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
            val places = response.items.map { toKakaoPlace(it) }
            Result.success(places)
        } catch (e: Exception) {
            Log.e(TAG, "Naver local search failed", e)
            Result.failure(e)
        }
    }

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
        private val NAVER_CLIENT_ID = BuildConfig.NAVER_CLIENT_ID
        private val NAVER_CLIENT_SECRET = BuildConfig.NAVER_CLIENT_SECRET
    }
}
