package com.panicdev.poopilot.data.repository

import com.panicdev.poopilot.data.db.FavoriteRestroom
import com.panicdev.poopilot.data.db.FavoriteRestroomDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 즐겨찾기 및 방문 기록 화장실 데이터를 관리하는 Repository입니다.
 *
 * 사용자가 방문한 화장실을 기록하고, 즐겨찾기로 저장하거나 삭제하는 기능을 제공합니다.
 * 내부적으로 Room 데이터베이스의 [FavoriteRestroomDao]를 사용하여 데이터를 저장/조회합니다.
 */
@Singleton
class FavoriteRepository @Inject constructor(
    /** 화장실 즐겨찾기 데이터베이스 접근 객체 (DAO) */
    private val dao: FavoriteRestroomDao
) {
    /**
     * 즐겨찾기로 등록된 화장실 목록을 실시간으로 관찰할 수 있는 Flow를 반환합니다.
     * 데이터가 변경되면 자동으로 최신 목록이 전달됩니다.
     */
    fun getFavorites(): Flow<List<FavoriteRestroom>> = dao.getFavorites()

    /**
     * 최근 방문한 화장실 목록을 실시간으로 관찰할 수 있는 Flow를 반환합니다.
     * 데이터가 변경되면 자동으로 최신 목록이 전달됩니다.
     */
    fun getRecentVisits(): Flow<List<FavoriteRestroom>> = dao.getRecentVisits()

    /**
     * 화장실 방문 기록을 저장합니다.
     *
     * 이미 같은 장소에 방문한 기록이 있으면 방문 횟수를 1 증가시키고 마지막 방문 시간을 갱신합니다.
     * 처음 방문하는 장소라면 새로운 기록을 생성합니다.
     *
     * @param placeName 장소 이름
     * @param addressName 지번 주소
     * @param roadAddressName 도로명 주소
     * @param latitude 위도
     * @param longitude 경도
     * @param categoryName 카테고리 이름 (예: 공중화장실)
     * @param phone 전화번호
     */
    suspend fun recordVisit(
        placeName: String,
        addressName: String,
        roadAddressName: String,
        latitude: Double,
        longitude: Double,
        categoryName: String,
        phone: String
    ) {
        // 같은 장소(이름 + 위/경도)로 기존 방문 기록이 있는지 확인
        val existing = dao.findByLocation(placeName, latitude, longitude)
        if (existing != null) {
            // 이미 방문한 적 있는 장소: 방문 횟수 증가 + 마지막 방문 시간 갱신
            dao.update(existing.copy(
                visitCount = existing.visitCount + 1,
                lastVisitedAt = System.currentTimeMillis()
            ))
        } else {
            // 처음 방문하는 장소: 새 기록 생성 (방문 횟수 1로 시작)
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

    /**
     * 특정 화장실의 즐겨찾기 상태를 변경합니다.
     *
     * @param id 즐겨찾기 항목의 고유 ID
     * @param isFavorite true이면 즐겨찾기 등록, false이면 즐겨찾기 해제
     */
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        dao.setFavorite(id, isFavorite)
    }

    /**
     * 특정 화장실의 방문 기록 및 즐겨찾기 항목을 영구적으로 삭제합니다.
     *
     * @param id 삭제할 항목의 고유 ID
     */
    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}
