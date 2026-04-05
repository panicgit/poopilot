package com.panicdev.poopilot.data.model

import com.google.gson.annotations.SerializedName

/**
 * 공공데이터포털 공중화장실 API의 최상위 응답 래퍼 클래스입니다.
 *
 * 공공 API는 응답을 "response" 키로 한 번 감싸서 반환하는 구조입니다.
 *
 * @property response 실제 응답 내용 (헤더 + 바디). 네트워크 오류 시 null일 수 있습니다.
 */
data class PublicRestroomResponse(
    val response: PublicRestroomBody?
)

/**
 * 공중화장실 API 응답의 본문을 담는 클래스입니다.
 *
 * 공공 API의 표준 응답 구조에 따라 헤더(상태 정보)와 바디(실제 데이터)로 구성됩니다.
 *
 * @property header 응답 상태 정보 (성공/실패 코드 및 메시지)
 * @property body 실제 화장실 데이터 목록 및 전체 개수
 */
data class PublicRestroomBody(
    val header: PublicRestroomHeader?,
    val body: PublicRestroomItems?
)

/**
 * 공중화장실 API 응답의 헤더 정보를 담는 클래스입니다.
 *
 * API 호출 성공 여부를 확인할 때 사용합니다.
 * resultCode가 "00"이면 정상, 그 외 코드는 오류를 의미합니다.
 *
 * @property resultCode 응답 결과 코드 (예: "00" = 정상, "99" = 오류)
 * @property resultMsg 응답 결과 메시지 (예: "NORMAL SERVICE", "SERVICE ERROR")
 */
data class PublicRestroomHeader(
    val resultCode: String?,
    val resultMsg: String?
)

/**
 * 공중화장실 목록과 전체 개수를 담는 클래스입니다.
 *
 * @property items 조회된 공중화장실 상세 정보 목록 ([PublicRestroom] 리스트). 결과 없을 시 null.
 * @property totalCount 조건에 해당하는 전체 화장실 수 (페이지 처리에 활용)
 */
data class PublicRestroomItems(
    val items: List<PublicRestroom>?,
    val totalCount: Int?
)

/**
 * 공중화장실 하나의 상세 정보를 담는 데이터 클래스입니다.
 *
 * 공공데이터 API의 JSON 필드명은 영문 약어로 되어 있어
 * @SerializedName 어노테이션으로 읽기 쉬운 Kotlin 프로퍼티명으로 매핑해 줍니다.
 * 공공 데이터 특성상 일부 항목이 비어있을 수 있으므로 모든 필드는 null 허용입니다.
 *
 * @property name 화장실 이름 (JSON: toiletNm, 예: "서울역 공중화장실")
 * @property roadAddress 도로명 주소 (JSON: rdnmadr)
 * @property address 지번 주소 (JSON: lnmadr)
 * @property latitude 위도 좌표 (JSON: latitude, WGS84 기준)
 * @property longitude 경도 좌표 (JSON: longitude, WGS84 기준)
 * @property openTime 개방 시간 (JSON: openTime, 예: "24시간")
 * @property phone 관리 기관 전화번호 (JSON: phoneNumber)
 * @property institution 관리 기관명 (JSON: institutionNm, 예: "서울시설공단")
 * @property toiletType 화장실 유형 (JSON: toiletType, 예: "공중화장실", "이동화장실")
 */
data class PublicRestroom(
    @SerializedName("toiletNm") val name: String?,
    @SerializedName("rdnmadr") val roadAddress: String?,
    @SerializedName("lnmadr") val address: String?,
    @SerializedName("latitude") val latitude: String?,
    @SerializedName("longitude") val longitude: String?,
    @SerializedName("openTime") val openTime: String?,
    @SerializedName("phoneNumber") val phone: String?,
    @SerializedName("institutionNm") val institution: String?,
    @SerializedName("toiletType") val toiletType: String?
)
