package com.panicdev.poopilot.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_restrooms")
data class FavoriteRestroom(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val placeName: String,
    val addressName: String,
    val roadAddressName: String,
    val latitude: Double,
    val longitude: Double,
    val categoryName: String,
    val phone: String,
    val visitCount: Int = 0,
    val lastVisitedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val userMemo: String = ""
)
