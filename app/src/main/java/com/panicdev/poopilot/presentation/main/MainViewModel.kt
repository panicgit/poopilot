package com.panicdev.poopilot.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panicdev.poopilot.GleoCommand
import com.panicdev.poopilot.GleoCommandBus
import com.panicdev.poopilot.data.repository.LocationRepository
import com.panicdev.poopilot.data.service.VoiceActivationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import javax.inject.Inject

enum class AppState {
    STANDBY,
    SEARCHING,
    NAVIGATING
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val voiceActivationService: VoiceActivationService
) : ViewModel() {

    private val _appState = MutableLiveData(AppState.STANDBY)
    val appState: LiveData<AppState> = _appState

    private val _currentLatitude = MutableLiveData(0.0)
    val currentLatitude: LiveData<Double> = _currentLatitude

    private val _currentLongitude = MutableLiveData(0.0)
    val currentLongitude: LiveData<Double> = _currentLongitude

    private val _navigateToSearch = MutableLiveData(false)
    val navigateToSearch: LiveData<Boolean> = _navigateToSearch

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _voiceActivated = MutableLiveData(false)
    val voiceActivated: LiveData<Boolean> = _voiceActivated

    init {
        locationRepository.initialize()
        startVoiceActivation()
        observeGleoCommands()
    }

    override fun onCleared() {
        super.onCleared()
        voiceActivationService.stop()
        locationRepository.release()
    }

    private fun startVoiceActivation() {
        voiceActivationService.start(viewModelScope)
        viewModelScope.launch {
            voiceActivationService.activationEvents.collect { keyword ->
                if (_appState.value == AppState.STANDBY) {
                    _voiceActivated.value = true
                    activateEmergencyMode()
                }
            }
        }
    }

    fun onVoiceActivatedConsumed() {
        _voiceActivated.value = false
    }

    fun restartVoiceActivation() {
        if (!voiceActivationService.isRunning()) {
            voiceActivationService.start(viewModelScope)
        }
    }

    private fun observeGleoCommands() {
        viewModelScope.launch {
            GleoCommandBus.commands.collect { command ->
                when (command) {
                    is GleoCommand.Activate -> {
                        if (_appState.value == AppState.STANDBY) {
                            activateEmergencyMode()
                        }
                    }
                    is GleoCommand.Cancel -> {
                        resetToStandby()
                    }
                }
            }
        }
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
                    _navigateToSearch.value = true
                } else {
                    _errorMessage.value = "위치를 가져올 수 없습니다"
                    _appState.value = AppState.STANDBY
                }
            } catch (e: TimeoutCancellationException) {
                _errorMessage.value = "위치 획득 시간 초과. 다시 시도해주세요."
                _appState.value = AppState.STANDBY
            } catch (e: Exception) {
                _errorMessage.value = "위치 획득 실패: ${e.message}"
                _appState.value = AppState.STANDBY
            }
        }
    }

    fun onNavigateToSearchConsumed() {
        _navigateToSearch.value = false
    }

    fun setNavigating() {
        _appState.value = AppState.NAVIGATING
    }

    fun resetToStandby() {
        _appState.value = AppState.STANDBY
        _navigateToSearch.value = false
        _errorMessage.value = null
        restartVoiceActivation()
    }
}
