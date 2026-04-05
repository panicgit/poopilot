package com.panicdev.poopilot.data.repository

import com.panicdev.poopilot.data.db.FavoriteRestroom
import com.panicdev.poopilot.data.db.FavoriteRestroomDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val dao: FavoriteRestroomDao
) {
    fun getFavorites(): Flow<List<FavoriteRestroom>> = dao.getFavorites()

    fun getRecentVisits(): Flow<List<FavoriteRestroom>> = dao.getRecentVisits()

    suspend fun recordVisit(
        placeName: String,
        addressName: String,
        roadAddressName: String,
        latitude: Double,
        longitude: Double,
        categoryName: String,
        phone: String
    ) {
        val existing = dao.findByLocation(placeName, latitude, longitude)
        if (existing != null) {
            dao.update(existing.copy(
                visitCount = existing.visitCount + 1,
                lastVisitedAt = System.currentTimeMillis()
            ))
        } else {
            dao.insert(FavoriteRestroom(
                placeName = placeName,
                addressName = addressName,
                roadAddressName = roadAddressName,
                latitude = latitude,
                longitude = longitude,
                categoryName = categoryName,
                phone = phone,
                visitCount = 1,
                lastVisitedAt = System.currentTimeMillis()
            ))
        }
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        dao.setFavorite(id, isFavorite)
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}
