package com.panicdev.poopilot.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panicdev.poopilot.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppState {
    STANDBY,
    SEARCHING,
    NAVIGATING
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _appState = MutableLiveData(AppState.STANDBY)
    val appState: LiveData<AppState> = _appState

    private val _currentLatitude = MutableLiveData(0.0)
    val currentLatitude: LiveData<Double> = _currentLatitude

    private val _currentLongitude = MutableLiveData(0.0)
    val currentLongitude: LiveData<Double> = _currentLongitude

    private val _isLocationReady = MutableLiveData(false)
    val isLocationReady: LiveData<Boolean> = _isLocationReady

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun initialize() {
        locationRepository.initialize()
    }

    fun release() {
        locationRepository.release()
    }

    fun activateEmergencyMode() {
        _appState.value = AppState.SEARCHING
        fetchCurrentLocation()
    }

    private fun fetchCurrentLocation() {
        viewModelScope.launch {
            try {
                val location = locationRepository.getCurrentLocation()
                if (location != null) {
                    _currentLatitude.value = location.latitude
                    _currentLongitude.value = location.longitude
                    _isLocationReady.value = true
                } else {
                    _errorMessage.value = "위치를 가져올 수 없습니다"
                    _isLocationReady.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "위치 획득 실패: ${e.message}"
                _isLocationReady.value = false
            }
        }
    }

    fun setNavigating() {
        _appState.value = AppState.NAVIGATING
    }

    fun resetToStandby() {
        _appState.value = AppState.STANDBY
        _isLocationReady.value = false
        _errorMessage.value = null
    }
}
