package com.panicdev.poopilot.data.api

import com.panicdev.poopilot.data.model.NaverLocalSearchResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * 네이버 검색 API와 통신하는 인터페이스입니다.
 *
 * 네이버 오픈 API를 이용해 지역 장소를 검색할 수 있습니다.
 * Retrofit 라이브러리가 이 인터페이스를 보고 실제 네트워크 요청 코드를 자동으로 만들어 줍니다.
 *
 * 공식 문서: https://developers.naver.com/docs/serviceapi/search/local/local.md
 */
interface NaverSearchApi {

    /**
     * 키워드로 지역 장소를 검색합니다.
     *
     * 네이버 지역 검색 API를 호출해 검색어와 관련된 장소 목록을 가져옵니다.
     *
     * @param clientId 네이버 개발자 센터에서 발급받은 클라이언트 ID
     * @param clientSecret 네이버 개발자 센터에서 발급받은 클라이언트 시크릿
     * @param query 검색할 키워드 (예: "화장실", "공중화장실")
     * @param display 한 번에 가져올 검색 결과 개수 (기본값 5개)
     * @param sort 정렬 방식 ("random"은 관련도순, "comment"는 블로그 리뷰 순)
     * @return 검색된 지역 장소 목록을 담은 [NaverLocalSearchResponse]
     */
    @GET("v1/search/local.json")
    suspend fun searchLocal(
        @Header("X-Naver-Client-Id") clientId: String,
        @Header("X-Naver-Client-Secret") clientSecret: String,
        @Query("query") query: String,
        @Query("display") display: Int = 5,
        @Query("sort") sort: String = "random"
    ): NaverLocalSearchResponse
}
