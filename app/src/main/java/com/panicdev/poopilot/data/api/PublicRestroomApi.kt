package com.panicdev.poopilot.data.api

import com.panicdev.poopilot.data.model.PublicRestroomResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PublicRestroomApi {

    @GET("openapi/tn_pubr_public_toilet_api")
    suspend fun searchPublicRestrooms(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 10,
        @Query("type") type: String = "json",
        @Query("lnmadr") address: String = ""
    ): PublicRestroomResponse
}
