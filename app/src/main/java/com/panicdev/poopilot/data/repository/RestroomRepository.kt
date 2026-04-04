package com.panicdev.poopilot.data.repository

import com.panicdev.poopilot.data.api.KakaoLocalApi
import com.panicdev.poopilot.data.model.KakaoPlace
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestroomRepository @Inject constructor(
    private val kakaoLocalApi: KakaoLocalApi
) {
    suspend fun searchNearbyRestrooms(
        latitude: Double,
        longitude: Double,
        radius: Int = 1000
    ): List<KakaoPlace> {
        return try {
            val response = kakaoLocalApi.searchByKeyword(
                apiKey = "KakaoAK ${getApiKey()}",
                query = "화장실",
                longitude = longitude.toString(),
                latitude = latitude.toString(),
                radius = radius,
                sort = "distance"
            )
            response.documents
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getApiKey(): String {
        // TODO: Move to BuildConfig or secure storage
        return ""
    }
}
