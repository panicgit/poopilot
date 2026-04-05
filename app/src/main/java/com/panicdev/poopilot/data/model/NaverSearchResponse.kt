package com.panicdev.poopilot.data.model

import com.google.gson.annotations.SerializedName

data class NaverLocalSearchResponse(
    val items: List<NaverLocalItem>
)

data class NaverLocalItem(
    val title: String,
    val link: String,
    val category: String,
    val description: String,
    val telephone: String,
    val address: String,
    val roadAddress: String,
    val mapx: String,
    val mapy: String
)
