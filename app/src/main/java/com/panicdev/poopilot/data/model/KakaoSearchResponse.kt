package com.panicdev.poopilot.data.model

import com.google.gson.annotations.SerializedName

data class KakaoSearchResponse(
    val documents: List<KakaoPlace>,
    val meta: KakaoMeta
)

data class KakaoPlace(
    @SerializedName("place_name") val placeName: String,
    @SerializedName("address_name") val addressName: String,
    @SerializedName("road_address_name") val roadAddressName: String,
    val x: String,
    val y: String,
    val distance: String,
    @SerializedName("category_name") val categoryName: String,
    val phone: String
)

data class KakaoMeta(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("is_end") val isEnd: Boolean
)
