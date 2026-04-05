package com.panicdev.poopilot.di

import android.content.Context
import androidx.room.Room
import com.panicdev.poopilot.data.db.FavoriteRestroomDao
import com.panicdev.poopilot.data.db.PoopilotDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PoopilotDatabase = Room.databaseBuilder(
        context,
        PoopilotDatabase::class.java,
        "poopilot_db"
    ).build()

    @Provides
    @Singleton
    fun provideFavoriteRestroomDao(
        database: PoopilotDatabase
    ): FavoriteRestroomDao = database.favoriteRestroomDao()
}
