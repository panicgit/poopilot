package com.panicdev.poopilot.presentation.search

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panicdev.poopilot.data.model.KakaoPlace
import com.panicdev.poopilot.data.repository.LlmRepository
import com.panicdev.poopilot.data.repository.NaverSearchRepository
import com.panicdev.poopilot.data.repository.PublicRestroomRepository
import com.panicdev.poopilot.data.repository.RestroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 화장실 검색 화면(SearchFragment)의 비즈니스 로직을 담당하는 ViewModel입니다.
 *
 * 카카오 장소 API, 공공 데이터 API, 네이버 검색 API를 동시에 호출하여
 * 주변 화장실을 검색하고, 중복을 제거하여 거리순으로 정렬합니다.
 * 결과가 2개 이상이면 AI(LLM)를 통해 가장 적합한 화장실을 추천받습니다.
 *
 * @param restroomRepository 카카오 API로 주변 화장실을 검색하는 저장소
 * @param llmRepository AI 추천을 요청하는 저장소
 * @param publicRestroomRepository 공공 데이터 API로 공중화장실을 검색하는 저장소
 * @param naverSearchRepository 네이버 검색 API로 주변 화장실을 검색하는 저장소
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val restroomRepository: RestroomRepository,
    private val llmRepository: LlmRepository,
    private val publicRestroomRepository: PublicRestroomRepository,
    private val naverSearchRepository: NaverSearchRepository
) : ViewModel() {

    /** 검색된 화장실 목록입니다. 여러 API의 결과가 중복 제거 후 거리순으로 정렬됩니다. */
    private val _searchResults = MutableLiveData<List<KakaoPlace>>()
    val searchResults: LiveData<List<KakaoPlace>> = _searchResults

    /** 검색이 진행 중이면 true, 완료되면 false입니다. */
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /** 사용자에게 보여줄 오류 또는 안내 메시지입니다. 없으면 null입니다. */
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /** 사용자가 선택한 화장실 장소입니다. 선택 후 길 안내로 이동하면 null로 초기화해야 합니다. */
    private val _selectedPlace = MutableLiveData<KakaoPlace?>()
    val selectedPlace: LiveData<KakaoPlace?> = _selectedPlace

    /** AI가 추천하는 화장실 이름입니다. 추천이 없으면 null입니다. */
    private val _llmRecommendation = MutableLiveData<String?>()
    val llmRecommendation: LiveData<String?> = _llmRecommendation

    /**
     * 주어진 좌표를 기준으로 주변 화장실을 검색합니다.
     * 카카오, 공공 데이터, 네이버 API를 동시에 호출하고 결과를 합쳐서 표시합니다.
     * 모든 API가 실패하면 오류를 표시하고, 일부만 실패하면 성공한 결과만 보여줍니다.
     *
     * @param latitude 현재 위치의 위도
     * @param longitude 현재 위치의 경도
     * @param radius 검색 반경(미터 단위). 기본값은 1000m(1km)입니다.
     */
    fun searchRestrooms(latitude: Double, longitude: Double, radius: Int = 1000) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // 반경을 점진적으로 넓혀가며 검색 (1km → 2km → 3km)
                val searchRadii = listOf(radius, radius * 2, radius * 3).distinct()
                var merged = emptyList<KakaoPlace>()

                for (currentRadius in searchRadii) {
                    val kakaoResult = restroomRepository.searchNearbyRestrooms(latitude, longitude, currentRadius)
                    Log.d("ATP_API", "apiResponse: endpoint=KakaoLocal/searchByKeyword, status=${if (kakaoResult.isSuccess) "SUCCESS" else "FAIL"}, bodyLength=${kakaoResult.getOrNull()?.size ?: 0}")
                    val publicResult = publicRestroomRepository.searchNearbyPublicRestrooms(latitude, longitude, currentRadius)
                    Log.d("ATP_API", "apiResponse: endpoint=PublicRestroom/searchPublicRestrooms, status=${if (publicResult.isSuccess) "SUCCESS" else "FAIL"}, bodyLength=${publicResult.getOrNull()?.size ?: 0}")
                    val naverResult = naverSearchRepository.searchNearbyRestrooms(latitude, longitude)
                    Log.d("ATP_API", "apiResponse: endpoint=NaverSearch/searchLocal, status=${if (naverResult.isSuccess) "SUCCESS" else "FAIL"}, bodyLength=${naverResult.getOrNull()?.size ?: 0}")

                    if (kakaoResult.isFailure && publicResult.isFailure && naverResult.isFailure) {
                        _isLoading.value = false
                        _errorMessage.value = "네트워크 오류로 검색에 실패했습니다. 연결 상태를 확인해주세요."
                        _searchResults.value = emptyList()
                        return@launch
                    }

                    val kakaoPlaces = kakaoResult.getOrDefault(emptyList())
                    val publicPlaces = publicResult.getOrDefault(emptyList())
                    val naverPlaces = naverResult.getOrDefault(emptyList())
                    merged = mergeResults(kakaoPlaces, publicPlaces, naverPlaces)

                    if (merged.isNotEmpty()) {
                        if (currentRadius > radius) {
                            _errorMessage.value = "반경 ${currentRadius}m로 넓혀서 검색했습니다."
                        }
                        if (kakaoResult.isFailure || publicResult.isFailure || naverResult.isFailure) {
                            _errorMessage.value = "일부 검색 결과만 표시됩니다."
                        }
                        break
                    }
                }

                _isLoading.value = false

                if (merged.isEmpty()) {
                    _errorMessage.value = "반경 ${searchRadii.last()}m 내 화장실을 찾을 수 없습니다."
                    _searchResults.value = emptyList()
                } else {
                    _searchResults.value = merged
                    if (merged.size > 1) {
                        filterWithLlm(merged)
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "검색 중 오류가 발생했습니다: ${e.localizedMessage}"
                _searchResults.value = emptyList()
            }
        }
    }

    /**
     * 세 API의 검색 결과를 중복 없이 합치고 거리순으로 정렬합니다.
     * 카카오 결과를 기준으로 공공 데이터, 네이버 결과에서 중복 이름을 제거합니다.
     *
     * @param kakaoPlaces 카카오 API 결과 목록
     * @param publicPlaces 공공 데이터 API 결과 목록
     * @param naverPlaces 네이버 검색 API 결과 목록
     * @return 중복 제거 후 거리순으로 정렬된 화장실 목록
     */
    private fun mergeResults(
        kakaoPlaces: List<KakaoPlace>,
        publicPlaces: List<KakaoPlace>,
        naverPlaces: List<KakaoPlace>
    ): List<KakaoPlace> {
        val seenNames = kakaoPlaces.map { it.placeName }.toMutableSet()
        val uniquePublic = publicPlaces.filter { seenNames.add(it.placeName) }
        val uniqueNaver = naverPlaces.filter { seenNames.add(it.placeName) }
        return (kakaoPlaces + uniquePublic + uniqueNaver)
            .sortedWith(compareBy(nullsLast()) { it.distance.toIntOrNull() })
    }

    /**
     * AI(LLM)에게 검색된 화장실 목록 중 가장 적합한 곳을 추천받습니다.
     * 거리, 공용화장실 여부, 24시간 이용 가능 여부를 기준으로 판단을 요청합니다.
     * 추천 결과는 [_llmRecommendation]에 저장됩니다.
     *
     * @param places 추천 후보가 될 화장실 목록
     */
    private fun filterWithLlm(places: List<KakaoPlace>) {
        viewModelScope.launch {
            val placeList = places.mapIndexed { index, place ->
                "${index + 1}. ${place.placeName} (${place.distance}m, ${place.categoryName})"
            }.joinToString("\n")

            val prompt = """다음 화장실 검색 결과에서 가장 적합한 1곳을 추천해주세요.
기준: 거리가 가까운 것, 공용화장실 우선, 24시간 이용 가능한 곳 우선.
번호만 답해주세요.

$placeList"""

            val result = llmRepository.generateContent(prompt)
            Log.d("ATP_API", "apiResponse: endpoint=LLM/generateContent, status=${if (result.isSuccess) "SUCCESS" else "FAIL"}, bodyLength=${result.getOrNull()?.length ?: 0}")
            result.onSuccess { response ->
                // 응답에서 숫자만 추출하여 추천 화장실의 인덱스로 사용합니다
                val recommendedIndex = response.trim().filter { it.isDigit() }.toIntOrNull()
                if (recommendedIndex != null && recommendedIndex in 1..places.size) {
                    val recommended = places[recommendedIndex - 1]
                    _llmRecommendation.value = recommended.placeName
                }
            }
        }
    }

    /**
     * 사용자가 목록에서 화장실을 선택했을 때 호출됩니다.
     * 선택된 장소를 [_selectedPlace]에 저장하여 화면 이동 이벤트를 발행합니다.
     *
     * @param place 사용자가 선택한 화장실 장소
     */
    fun selectPlace(place: KakaoPlace) {
        _selectedPlace.value = place
    }

    /** 장소 선택 이벤트를 소비(초기화)합니다. Fragment에서 길 안내 화면으로 이동한 후 반드시 호출해야 합니다. */
    fun onPlaceSelectionConsumed() {
        _selectedPlace.value = null
    }
}
