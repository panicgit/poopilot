package com.panicdev.poopilot.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoriteRestroom::class], version = 1, exportSchema = false)
abstract class PoopilotDatabase : RoomDatabase() {
    abstract fun favoriteRestroomDao(): FavoriteRestroomDao
}
