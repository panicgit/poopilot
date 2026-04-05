package com.panicdev.poopilot.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteRestroomDao {

    @Query("SELECT * FROM favorite_restrooms WHERE isFavorite = 1 ORDER BY visitCount DESC")
    fun getFavorites(): Flow<List<FavoriteRestroom>>

    @Query("SELECT * FROM favorite_restrooms ORDER BY lastVisitedAt DESC LIMIT 5")
    fun getRecentVisits(): Flow<List<FavoriteRestroom>>

    @Query("SELECT * FROM favorite_restrooms WHERE placeName = :name AND latitude = :lat AND longitude = :lng LIMIT 1")
    suspend fun findByLocation(name: String, lat: Double, lng: Double): FavoriteRestroom?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(restroom: FavoriteRestroom): Long

    @Update
    suspend fun update(restroom: FavoriteRestroom)

    @Query("DELETE FROM favorite_restrooms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE favorite_restrooms SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
}
