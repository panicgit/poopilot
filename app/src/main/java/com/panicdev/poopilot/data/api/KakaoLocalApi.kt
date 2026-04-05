package com.panicdev.poopilot.data.api

import com.panicdev.poopilot.data.model.KakaoSearchResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * 카카오 로컬 API와 통신하는 인터페이스입니다.
 *
 * 카카오 REST API를 이용해 특정 키워드로 주변 장소를 검색할 수 있습니다.
 * Retrofit 라이브러리가 이 인터페이스를 보고 실제 네트워크 요청 코드를 자동으로 만들어 줍니다.
 *
 * 공식 문서: https://developers.kakao.com/docs/latest/ko/local/dev-guide
 */
interface KakaoLocalApi {

    /**
     * 키워드로 주변 장소를 검색합니다.
     *
     * 예를 들어 "화장실"을 검색어로 넣으면 현재 위치 주변의 화장실 목록을 반환합니다.
     *
     * @param apiKey 카카오 REST API 인증 키 (예: "KakaoAK xxxxxxxx")
     * @param query 검색할 키워드 (예: "화장실", "공중화장실")
     * @param longitude 검색 기준 경도 (현재 위치의 X 좌표)
     * @param latitude 검색 기준 위도 (현재 위치의 Y 좌표)
     * @param radius 검색 반경 (단위: 미터, 기본값 1000m = 1km)
     * @param sort 정렬 기준 ("distance"는 거리순 정렬, 기본값)
     * @return 검색된 장소 목록과 메타 정보를 담은 [KakaoSearchResponse]
     */
    @GET("v2/local/search/keyword.json")
    suspend fun searchByKeyword(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("x") longitude: String,
        @Query("y") latitude: String,
        @Query("radius") radius: Int = 1000,
        @Query("sort") sort: String = "distance"
    ): KakaoSearchResponse
}
