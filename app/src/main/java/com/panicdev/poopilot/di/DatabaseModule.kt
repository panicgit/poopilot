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

/**
 * 데이터베이스 관련 의존성을 제공하는 Hilt 모듈입니다.
 *
 * 이 모듈은 앱 전체에서 사용할 Room 데이터베이스와 DAO(Data Access Object)를
 * 싱글톤으로 생성하고 관리합니다.
 *
 * [SingletonComponent]에 설치되므로 앱이 살아있는 동안 단 하나의 인스턴스만 유지됩니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 앱의 Room 데이터베이스 인스턴스를 제공합니다.
     *
     * - 데이터베이스 파일명: "poopilot_db"
     * - [fallbackToDestructiveMigration]: 스키마 버전이 바뀌었을 때 기존 데이터를 삭제하고
     *   새로 생성합니다. (마이그레이션 전략이 없을 경우 앱 충돌 방지)
     *
     * @param context 데이터베이스를 생성할 때 필요한 앱 Context
     * @return 앱 전역에서 공유할 [PoopilotDatabase] 인스턴스
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PoopilotDatabase = Room.databaseBuilder(
        context,
        PoopilotDatabase::class.java,
        "poopilot_db"
    ).fallbackToDestructiveMigration()
    .build()

    /**
     * 즐겨찾기 화장실 데이터에 접근하는 DAO를 제공합니다.
     *
     * DAO는 데이터베이스의 특정 테이블에 대한 CRUD(생성·조회·수정·삭제) 작업을
     * 담당하는 인터페이스입니다. 데이터베이스 인스턴스에서 꺼내어 제공합니다.
     *
     * @param database [provideDatabase]에서 생성된 [PoopilotDatabase] 인스턴스
     * @return 즐겨찾기 화장실 테이블 접근용 [FavoriteRestroomDao]
     */
    @Provides
    @Singleton
    fun provideFavoriteRestroomDao(
        database: PoopilotDatabase
    ): FavoriteRestroomDao = database.favoriteRestroomDao()
}
