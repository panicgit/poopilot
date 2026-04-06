package com.panicdev.poopilot.data.model

import com.google.gson.annotations.SerializedName

/**
 * 공공데이터포털 행정안전부 공중화장실 API의 최상위 응답 래퍼 클래스입니다.
 */
data class PublicRestroomResponse(
    val response: PublicRestroomBody?
)

/**
 * 응답 본문: 헤더(상태 정보)와 바디(실제 데이터)로 구성됩니다.
 */
data class PublicRestroomBody(
    val header: PublicRestroomHeader?,
    val body: PublicRestroomItems?
)

/**
 * 응답 헤더: resultCode "00"이면 정상입니다.
 */
data class PublicRestroomHeader(
    val resultCode: String?,
    val resultMsg: String?
)

/**
 * 응답 바디: 화장실 목록과 페이징 정보를 담고 있습니다.
 * 주의: items 안에 item 배열이 한 단계 더 중첩되어 있습니다.
 */
data class PublicRestroomItems(
    val items: PublicRestroomItemWrapper?,
    val totalCount: Int?,
    val numOfRows: Int?,
    val pageNo: Int?
)

/**
 * items 내부의 item 배열 래퍼입니다.
 * API 응답 구조: response.body.items.item[]
 */
data class PublicRestroomItemWrapper(
    val item: List<PublicRestroom>?
)

/**
 * 공중화장실 하나의 상세 정보입니다.
 *
 * 행정안전부 API 필드명(대문자 약어)을 읽기 쉬운 프로퍼티명으로 매핑합니다.
 * 공공 데이터 특성상 일부 항목이 비어있을 수 있으므로 모든 필드는 null 허용입니다.
 */
data class PublicRestroom(
    /** 화장실 이름 (예: "서울역 공중화장실") */
    @SerializedName("RSTRM_NM") val name: String?,
    /** 도로명 주소 */
    @SerializedName("LCTN_ROAD_NM_ADDR") val roadAddress: String?,
    /** 지번 주소 */
    @SerializedName("LCTN_LOTNO_ADDR") val address: String?,
    /** 위도 (WGS84) */
    @SerializedName("WGS84_LAT") val latitude: String?,
    /** 경도 (WGS84) */
    @SerializedName("WGS84_LOT") val longitude: String?,
    /** 개방 시간 */
    @SerializedName("OPN_HR") val openTime: String?,
    /** 개방 시간 상세 */
    @SerializedName("OPN_HR_DTL") val openTimeDetail: String?,
    /** 관리기관 전화번호 */
    @SerializedName("TELNO") val phone: String?,
    /** 관리기관명 */
    @SerializedName("MNG_INST_NM") val institution: String?,
    /** 화장실 구분 (공공기관-지방자치단체 등) */
    @SerializedName("RSTRM_PSN_SE_NM") val toiletType: String?,
    /** 화장실 유형 (공중화장실, 개방화장실 등) */
    @SerializedName("SE_NM") val category: String?,
    /** 설치 연월 */
    @SerializedName("INSTL_YM") val installDate: String?,
    /** 비상벨 설치 여부 */
    @SerializedName("EMRGNCBLL_INSTL_YN") val emergencyBell: String?,
    /** 입구 CCTV 설치 여부 */
    @SerializedName("RSTRM_ENTRAN_CCTV_INSTL_EN") val cctvInstalled: String?,
    /** 기저귀 교환대 여부 */
    @SerializedName("DIAP_EXCHCON_EN") val diaperExchange: String?
)
