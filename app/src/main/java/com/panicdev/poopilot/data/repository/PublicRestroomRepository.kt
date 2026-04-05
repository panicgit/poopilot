package com.panicdev.poopilot.data.repository

import android.util.Log
import com.panicdev.poopilot.data.api.PublicRestroomApi
import com.panicdev.poopilot.data.model.KakaoPlace
import com.panicdev.poopilot.data.model.PublicRestroom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class PublicRestroomRepository @Inject constructor(
    private val publicRestroomApi: PublicRestroomApi
) {
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
            val response = publicRestroomApi.searchPublicRestrooms(
                serviceKey = PUBLIC_API_KEY
            )
            val items = response.response?.body?.items ?: emptyList()
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
            Result.success(emptyList())
        }
    }

    private fun toKakaoPlace(restroom: PublicRestroom, distance: Int): KakaoPlace {
        return KakaoPlace(
            placeName = restroom.name ?: "공중화장실",
            addressName = restroom.address ?: "",
            roadAddressName = restroom.roadAddress ?: "",
            x = restroom.longitude ?: "0",
            y = restroom.latitude ?: "0",
            distance = distance.toString(),
            categoryName = "공중화장실 > ${restroom.toiletType ?: "일반"}",
            phone = restroom.phone ?: ""
        )
    }

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

    companion object {
        private const val TAG = "PublicRestroomRepo"
        private const val PUBLIC_API_KEY = "" // Sprint 5: 공공데이터포털 API 키 필요
    }
}
