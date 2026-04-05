package com.panicdev.poopilot.presentation.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panicdev.poopilot.data.model.KakaoPlace
import com.panicdev.poopilot.data.repository.LlmRepository
import com.panicdev.poopilot.data.repository.RestroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val restroomRepository: RestroomRepository,
    private val llmRepository: LlmRepository
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<KakaoPlace>>()
    val searchResults: LiveData<List<KakaoPlace>> = _searchResults

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _selectedPlace = MutableLiveData<KakaoPlace?>()
    val selectedPlace: LiveData<KakaoPlace?> = _selectedPlace

    private val _llmRecommendation = MutableLiveData<String?>()
    val llmRecommendation: LiveData<String?> = _llmRecommendation

    fun searchRestrooms(latitude: Double, longitude: Double, radius: Int = 1000) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = restroomRepository.searchNearbyRestrooms(latitude, longitude, radius)
            _isLoading.value = false

            result.onSuccess { places ->
                if (places.isEmpty()) {
                    _errorMessage.value = "주변에 화장실을 찾을 수 없습니다"
                }
                _searchResults.value = places
                if (places.size > 1) {
                    filterWithLlm(places)
                }
            }.onFailure { error ->
                _errorMessage.value = "검색 실패: ${error.message}"
                _searchResults.value = emptyList()
            }
        }
    }

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
            result.onSuccess { response ->
                val recommendedIndex = response.trim().filter { it.isDigit() }.toIntOrNull()
                if (recommendedIndex != null && recommendedIndex in 1..places.size) {
                    val recommended = places[recommendedIndex - 1]
                    _llmRecommendation.value = recommended.placeName
                }
            }
        }
    }

    fun selectPlace(place: KakaoPlace) {
        _selectedPlace.value = place
    }

    fun onPlaceSelectionConsumed() {
        _selectedPlace.value = null
    }
}
