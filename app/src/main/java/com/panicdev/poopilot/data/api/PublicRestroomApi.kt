package com.panicdev.poopilot.data.api

import com.panicdev.poopilot.data.model.PublicRestroomResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 공공데이터포털의 공중화장실 정보 API와 통신하는 인터페이스입니다.
 *
 * 행정안전부에서 제공하는 공중화장실 위치 공공 API를 활용해
 * 전국의 공중화장실 정보를 조회할 수 있습니다.
 * Retrofit 라이브러리가 이 인터페이스를 보고 실제 네트워크 요청 코드를 자동으로 만들어 줍니다.
 *
 * 공식 문서: https://www.data.go.kr (공공데이터포털 - 공중화장실 정보)
 */
interface PublicRestroomApi {

    /**
     * 공중화장실 목록을 조회합니다.
     *
     * 공공데이터포털 API를 호출해 주소 조건에 맞는 공중화장실 목록을 페이지 단위로 가져옵니다.
     *
     * @param serviceKey 공공데이터포털에서 발급받은 서비스 인증키
     * @param pageNo 가져올 페이지 번호 (1부터 시작, 기본값 1)
     * @param numOfRows 한 페이지에 가져올 항목 수 (기본값 10개)
     * @param type 응답 데이터 형식 ("json" 또는 "xml", 기본값 "json")
     * @param address 검색할 주소 (지번 주소 기준, 빈 문자열이면 전체 조회)
     * @return 조회된 공중화장실 목록과 응답 정보를 담은 [PublicRestroomResponse]
     */
    @GET("openapi/tn_pubr_public_toilet_api")
    suspend fun searchPublicRestrooms(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 10,
        @Query("type") type: String = "json",
        @Query("lnmadr") address: String = ""
    ): PublicRestroomResponse
}
