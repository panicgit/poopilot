package com.panicdev.poopilot.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사용자가 저장하거나 방문한 화장실 정보를 로컬 데이터베이스에 보관하는 엔티티 클래스입니다.
 *
 * Room 라이브러리가 이 클래스를 보고 "favorite_restrooms"라는 이름의 테이블을 자동으로 만들어 줍니다.
 * 앱을 오프라인 상태에서도 즐겨찾기 목록이나 최근 방문 기록을 조회할 수 있도록
 * 기기 내부 SQLite 데이터베이스에 저장됩니다.
 *
 * @property id 각 행을 구분하는 고유 식별자 (자동 증가, 직접 지정 불필요)
 * @property placeName 화장실(장소) 이름
 * @property addressName 지번 주소
 * @property roadAddressName 도로명 주소
 * @property latitude 위도 좌표 (WGS84 기준)
 * @property longitude 경도 좌표 (WGS84 기준)
 * @property categoryName 장소 카테고리명
 * @property phone 전화번호
 * @property visitCount 이 화장실을 방문한 횟수 (기본값 0)
 * @property lastVisitedAt 마지막으로 방문한 시각 (Unix 타임스탬프, 밀리초 단위, 기본값은 현재 시각)
 * @property isFavorite 즐겨찾기 등록 여부 (true면 즐겨찾기에 표시됨, 기본값 false)
 * @property userMemo 사용자가 직접 남긴 메모 (기본값 빈 문자열)
 */
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
