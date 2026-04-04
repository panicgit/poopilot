package com.panicdev.poopilot.data.api

import com.panicdev.poopilot.data.model.KakaoSearchResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoLocalApi {

    @GET("v2/local/search/keyword.json")
    suspend fun searchByKeyword(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("x") longitude: String,
        @Query("y") latitude: String,
        @Query("radius") radius: Int = 1000,
        @Query("sort") sort: String = "distance"
    ): KakaoSearchResponse
}
