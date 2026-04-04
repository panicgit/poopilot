package com.panicdev.poopilot.presentation.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panicdev.poopilot.data.model.KakaoPlace
import com.panicdev.poopilot.data.repository.RestroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val restroomRepository: RestroomRepository
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<KakaoPlace>>()
    val searchResults: LiveData<List<KakaoPlace>> = _searchResults

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _selectedPlace = MutableLiveData<KakaoPlace?>()
    val selectedPlace: LiveData<KakaoPlace?> = _selectedPlace

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
            }.onFailure { error ->
                _errorMessage.value = "검색 실패: ${error.message}"
                _searchResults.value = emptyList()
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
