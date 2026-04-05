package com.panicdev.poopilot.data.model

import com.google.gson.annotations.SerializedName

/**
 * 카카오 키워드 검색 API의 전체 응답을 담는 최상위 데이터 클래스입니다.
 *
 * API를 호출하면 이 형태로 결과가 돌아옵니다.
 *
 * @property documents 실제 검색된 장소 목록 ([KakaoPlace] 리스트)
 * @property meta 검색 결과에 대한 부가 정보 (총 개수, 마지막 페이지 여부 등)
 */
data class KakaoSearchResponse(
    val documents: List<KakaoPlace>,
    val meta: KakaoMeta
)

/**
 * 카카오 검색 결과에서 개별 장소 하나의 정보를 담는 데이터 클래스입니다.
 *
 * API 응답의 JSON 필드명과 Kotlin 프로퍼티명이 다른 경우
 * @SerializedName 어노테이션으로 매핑해 줍니다.
 *
 * @property placeName 장소 이름 (예: "서울역 공중화장실")
 * @property addressName 지번 주소 (예: "서울 중구 봉래동2가 122")
 * @property roadAddressName 도로명 주소 (예: "서울 중구 통일로 1")
 * @property x 장소의 경도 (longitude) 좌표값 (문자열 형식)
 * @property y 장소의 위도 (latitude) 좌표값 (문자열 형식)
 * @property distance 현재 위치로부터의 거리 (단위: 미터)
 * @property categoryName 카카오 카테고리 분류명 (예: "공공기관 > 화장실")
 * @property phone 장소 전화번호
 */
data class KakaoPlace(
    @SerializedName("place_name") val placeName: String,
    @SerializedName("address_name") val addressName: String,
    @SerializedName("road_address_name") val roadAddressName: String,
    val x: String,
    val y: String,
    val distance: String,
    @SerializedName("category_name") val categoryName: String,
    val phone: String
)

/**
 * 카카오 검색 결과의 메타 정보를 담는 데이터 클래스입니다.
 *
 * 검색 결과가 몇 개인지, 더 가져올 데이터가 있는지 등을 알 수 있습니다.
 *
 * @property totalCount 조건에 맞는 전체 검색 결과 수
 * @property isEnd 현재 페이지가 마지막 페이지인지 여부 (true면 더 이상 결과 없음)
 */
data class KakaoMeta(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("is_end") val isEnd: Boolean
)
