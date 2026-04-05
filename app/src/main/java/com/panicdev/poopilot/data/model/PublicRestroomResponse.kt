package com.panicdev.poopilot.data.model

import com.google.gson.annotations.SerializedName

data class PublicRestroomResponse(
    val response: PublicRestroomBody?
)

data class PublicRestroomBody(
    val header: PublicRestroomHeader?,
    val body: PublicRestroomItems?
)

data class PublicRestroomHeader(
    val resultCode: String?,
    val resultMsg: String?
)

data class PublicRestroomItems(
    val items: List<PublicRestroom>?,
    val totalCount: Int?
)

data class PublicRestroom(
    @SerializedName("toiletNm") val name: String?,
    @SerializedName("rdnmadr") val roadAddress: String?,
    @SerializedName("lnmadr") val address: String?,
    @SerializedName("latitude") val latitude: String?,
    @SerializedName("longitude") val longitude: String?,
    @SerializedName("openTime") val openTime: String?,
    @SerializedName("phoneNumber") val phone: String?,
    @SerializedName("institutionNm") val institution: String?,
    @SerializedName("toiletType") val toiletType: String?
)
