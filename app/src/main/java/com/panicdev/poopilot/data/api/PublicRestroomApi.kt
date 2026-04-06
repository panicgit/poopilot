package com.panicdev.poopilot.data.api

import com.panicdev.poopilot.data.model.PublicRestroomResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 공공데이터포털의 행정안전부 공중화장실 정보 API와 통신하는 인터페이스입니다.
 *
 * Base URL: https://apis.data.go.kr/1741000/public_restroom_info/
 * 오퍼레이션: /info (공중화장실정보 데이터 이력조회)
 */
interface PublicRestroomApi {

    /**
     * 공중화장실 목록을 조회합니다.
     *
     * @param serviceKey 공공데이터포털에서 발급받은 서비스 인증키 (인코딩 키 사용)
     * @param pageNo 페이지 번호 (1부터 시작)
     * @param numOfRows 한 페이지 결과 수 (최대 100)
     * @param returnType 응답 데이터 형식 ("json" 또는 "xml")
     * @param baseDate 데이터 기준일자 (YYYYMMDD 형식)
     * @param atmyCode 개방자치단체코드 (예: "3000000")
     * @param roadAddr 소재지도로명주소 검색 (LIKE 조건, 선택)
     */
    @GET("info")
    suspend fun searchPublicRestrooms(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("returnType") returnType: String = "json",
        @Query("cond[BASE_DATE::EQ]") baseDate: String,
        @Query("cond[OPN_ATMY_GRP_CD::EQ]") atmyCode: String = "",
        @Query("cond[LCTN_ROAD_NM_ADDR::LIKE]") roadAddr: String = ""
    ): PublicRestroomResponse
}
