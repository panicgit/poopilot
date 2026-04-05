package com.panicdev.poopilot.data.model

import com.google.gson.annotations.SerializedName

/**
 * 네이버 지역 검색 API의 전체 응답을 담는 최상위 데이터 클래스입니다.
 *
 * API를 호출하면 이 형태로 결과가 돌아옵니다.
 *
 * @property items 실제 검색된 지역 장소 목록 ([NaverLocalItem] 리스트)
 */
data class NaverLocalSearchResponse(
    val items: List<NaverLocalItem>
)

/**
 * 네이버 지역 검색 결과에서 개별 장소 하나의 정보를 담는 데이터 클래스입니다.
 *
 * 네이버 API는 HTML 태그가 포함된 문자열을 반환하는 경우가 있으므로
 * UI에서 표시할 때 태그를 제거해야 할 수 있습니다.
 *
 * @property title 장소 이름 (HTML 태그가 포함될 수 있음, 예: "<b>화장실</b>")
 * @property link 장소의 네이버 상세 페이지 URL
 * @property category 장소 카테고리 (예: "공공기관")
 * @property description 장소에 대한 간단한 설명
 * @property telephone 장소 전화번호
 * @property address 지번 주소
 * @property roadAddress 도로명 주소
 * @property mapx 장소의 경도 (longitude) 좌표값 — 카텍(KATEC) 좌표계 기준
 * @property mapy 장소의 위도 (latitude) 좌표값 — 카텍(KATEC) 좌표계 기준
 */
data class NaverLocalItem(
    val title: String,
    val link: String,
    val category: String,
    val description: String,
    val telephone: String,
    val address: String,
    val roadAddress: String,
    val mapx: String,
    val mapy: String
)
