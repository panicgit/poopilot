package com.panicdev.poopilot.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Poopilot 앱의 로컬 데이터베이스 클래스입니다.
 *
 * Room 라이브러리를 사용해 기기 내부 SQLite 데이터베이스를 관리합니다.
 * 앱에서 데이터베이스 인스턴스는 하나만 존재해야 하므로,
 * 실제로는 의존성 주입(DI) 모듈에서 싱글톤으로 생성해 사용합니다.
 *
 * - entities: 이 데이터베이스가 관리하는 테이블 목록 (현재 [FavoriteRestroom] 테이블 1개)
 * - version: 데이터베이스 스키마 버전 (테이블 구조가 바뀌면 올려야 합니다)
 * - exportSchema: 스키마 파일을 assets 폴더에 내보낼지 여부 (false = 내보내지 않음)
 */
@Database(entities = [FavoriteRestroom::class], version = 1, exportSchema = false)
abstract class PoopilotDatabase : RoomDatabase() {

    /**
     * 즐겨찾기 화장실 테이블에 접근하는 DAO 인스턴스를 반환합니다.
     *
     * Room이 자동으로 구현체를 생성해 주므로 직접 구현할 필요가 없습니다.
     */
    abstract fun favoriteRestroomDao(): FavoriteRestroomDao
}
