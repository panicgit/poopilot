package com.panicdev.poopilot.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 즐겨찾기 화장실 데이터베이스 테이블에 접근하는 DAO(Data Access Object) 인터페이스입니다.
 *
 * DAO는 데이터베이스와 앱 코드 사이의 창구 역할을 합니다.
 * Room 라이브러리가 이 인터페이스를 보고 실제 SQL 쿼리를 실행하는 코드를 자동으로 생성해 줍니다.
 *
 * Flow를 반환하는 함수는 데이터가 변경될 때마다 자동으로 최신 값을 다시 전달해 주므로
 * UI가 항상 최신 상태를 유지할 수 있습니다.
 */
@Dao
interface FavoriteRestroomDao {

    /**
     * 즐겨찾기로 등록된 화장실 목록을 방문 횟수 기준 내림차순으로 가져옵니다.
     *
     * Flow를 반환하므로 즐겨찾기 목록이 바뀌면 화면이 자동으로 갱신됩니다.
     */
    @Query("SELECT * FROM favorite_restrooms WHERE isFavorite = 1 ORDER BY visitCount DESC")
    fun getFavorites(): Flow<List<FavoriteRestroom>>

    /**
     * 최근 방문한 화장실 5곳을 방문 시각 기준 최신순으로 가져옵니다.
     *
     * Flow를 반환하므로 방문 기록이 추가되거나 변경되면 화면이 자동으로 갱신됩니다.
     */
    @Query("SELECT * FROM favorite_restrooms ORDER BY lastVisitedAt DESC LIMIT 5")
    fun getRecentVisits(): Flow<List<FavoriteRestroom>>

    /**
     * 장소 이름과 위경도 좌표로 데이터베이스에 이미 저장된 화장실을 찾습니다.
     *
     * 같은 화장실이 중복 저장되는 것을 방지하거나, 기존 기록을 불러올 때 사용합니다.
     *
     * @param name 화장실(장소) 이름
     * @param lat 위도 좌표
     * @param lng 경도 좌표
     * @return 조건에 맞는 화장실 정보, 없으면 null 반환
     */
    @Query("SELECT * FROM favorite_restrooms WHERE placeName = :name AND latitude = :lat AND longitude = :lng LIMIT 1")
    suspend fun findByLocation(name: String, lat: Double, lng: Double): FavoriteRestroom?

    /**
     * 새로운 화장실 정보를 데이터베이스에 저장합니다.
     *
     * 동일한 데이터가 이미 존재하면 오류(ABORT)를 발생시킵니다.
     * 저장 전에 [findByLocation]으로 중복 여부를 먼저 확인하는 것을 권장합니다.
     *
     * @param restroom 저장할 화장실 정보
     * @return 새로 삽입된 행의 id 값
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(restroom: FavoriteRestroom): Long

    /**
     * 기존에 저장된 화장실 정보를 수정합니다.
     *
     * [FavoriteRestroom]의 id를 기준으로 해당 행을 찾아 모든 필드를 업데이트합니다.
     *
     * @param restroom 수정할 화장실 정보 (id 값이 반드시 포함되어야 합니다)
     */
    @Update
    suspend fun update(restroom: FavoriteRestroom)

    /**
     * 지정한 id에 해당하는 화장실 기록을 데이터베이스에서 삭제합니다.
     *
     * @param id 삭제할 화장실 기록의 고유 식별자
     */
    @Query("DELETE FROM favorite_restrooms WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 특정 화장실의 즐겨찾기 등록 상태를 변경합니다.
     *
     * 즐겨찾기 추가/해제 버튼을 누를 때 호출합니다.
     *
     * @param id 상태를 변경할 화장실 기록의 고유 식별자
     * @param isFavorite true면 즐겨찾기 등록, false면 즐겨찾기 해제
     */
    @Query("UPDATE favorite_restrooms SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
}
