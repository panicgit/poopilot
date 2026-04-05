package com.panicdev.poopilot.presentation.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panicdev.poopilot.data.repository.DoorRepository
import com.panicdev.poopilot.data.repository.FavoriteRepository
import com.panicdev.poopilot.data.repository.LocationRepository
import com.panicdev.poopilot.data.repository.NavigationEvent
import com.panicdev.poopilot.data.repository.NavigationRepository
import com.panicdev.poopilot.data.repository.RestroomRepository
import com.panicdev.poopilot.data.repository.SettingsRepository
import com.panicdev.poopilot.data.repository.TtsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val navigationRepository: NavigationRepository,
    private val doorRepository: DoorRepository,
    private val ttsRepository: TtsRepository,
    private val settingsRepository: SettingsRepository,
    private val restroomRepository: RestroomRepository,
    private val favoriteRepository: FavoriteRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _remainingDistance = MutableLiveData("--")
    val remainingDistance: LiveData<String> = _remainingDistance

    private val _remainingTime = MutableLiveData("--")
    val remainingTime: LiveData<String> = _remainingTime

    private val _destinationName = MutableLiveData("")
    val destinationName: LiveData<String> = _destinationName

    private val _destinationAddress = MutableLiveData("")
    val destinationAddress: LiveData<String> = _destinationAddress

    private val _hasArrived = MutableLiveData(false)
    val hasArrived: LiveData<Boolean> = _hasArrived

    private val _ttsMessage = MutableLiveData<String?>()
    val ttsMessage: LiveData<String?> = _ttsMessage

    private val _tbtDescription = MutableLiveData("")
    val tbtDescription: LiveData<String> = _tbtDescription

    private val _isNavigating = MutableLiveData(false)
    val isNavigating: LiveData<Boolean> = _isNavigating

    private var tbtPollingJob: Job? = null
    private var reSearchJob: Job? = null

    private val _closerPlaceSuggestion = MutableLiveData<String?>()
    val closerPlaceSuggestion: LiveData<String?> = _closerPlaceSuggestion

    private var currentDestLat = 0.0
    private var currentDestLng = 0.0

    init {
        ttsRepository.initialize()
        navigationRepository.registerListener()
        observeNavigationEvents()
    }

    override fun onCleared() {
        super.onCleared()
        stopTbtPolling()
        stopReSearch()
        ttsRepository.stop()
        navigationRepository.unregisterListener()
    }

    private fun observeNavigationEvents() {
        viewModelScope.launch {
            navigationRepository.navigationEvents.collect { event ->
                when (event) {
                    is NavigationEvent.RouteStarted -> {
                        _remainingDistance.value = formatDistance(event.info.distance)
                        _remainingTime.value = formatTime(event.info.duration)
                        _isNavigating.value = true
                        startTbtPolling()
                        startReSearch()
                        val timeText = formatTime(event.info.duration)
                        speakIfAvailable("${_destinationName.value}까지 $timeText 소요됩니다")
                    }
                    is NavigationEvent.TBTUpdated -> {
                        val firstTbt = event.tbtList.firstOrNull()
                        if (firstTbt != null) {
                            _tbtDescription.value = firstTbt.description ?: ""
                        }
                    }
                    is NavigationEvent.DrivingInfoUpdated -> {
                        val dest = event.info.destination
                        _remainingDistance.value = formatDistance(dest.distance)
                        _remainingTime.value = formatTime(dest.duration)
                    }
                    is NavigationEvent.DestinationArrived -> {
                        _hasArrived.value = true
                        _isNavigating.value = false
                        stopTbtPolling()
                        stopReSearch()
                        recordVisit()
                        if (settingsRepository.doorUnlockEnabled) {
                            doorRepository.unlockDriverDoor()
                            val message = "도착했습니다! 문이 열렸습니다!"
                            _ttsMessage.value = message
                            speakIfAvailable(message)
                        } else {
                            val message = "도착했습니다!"
                            _ttsMessage.value = message
                            speakIfAvailable(message)
                        }
                    }
                    is NavigationEvent.RouteCancelled -> {
                        // handled by fragment
                    }
                }
            }
        }
    }

    fun startNavigation(name: String, address: String, lat: Double, lng: Double) {
        _destinationName.value = name
        _destinationAddress.value = address
        currentDestLat = lat
        currentDestLng = lng

        viewModelScope.launch {
            try {
                navigationRepository.startNavigation(lat, lng, name, address)
                val message = "${name}까지 안내를 시작합니다"
                _ttsMessage.value = message
                speakIfAvailable(message)
            } catch (e: Exception) {
                _ttsMessage.value = "경로 설정 실패: ${e.message}"
            }
        }
    }

    fun cancelNavigation() {
        stopTbtPolling()
        stopReSearch()
        _isNavigating.value = false
        navigationRepository.cancelRoute()
    }

    private fun startReSearch() {
        reSearchJob?.cancel()
        reSearchJob = viewModelScope.launch {
            delay(60_000L) // 1분 후 첫 재탐색
            while (_isNavigating.value == true) {
                try {
                    val currentLoc = locationRepository.getCurrentLocation()
                    if (currentLoc != null) {
                        checkForCloserRestroom(currentLoc.latitude, currentLoc.longitude)
                    }
                } catch (e: Exception) {
                    // 위치 획득 실패 시 건너뜀
                }
                delay(120_000L) // 이후 2분 간격
            }
        }
    }

    private fun stopReSearch() {
        reSearchJob?.cancel()
        reSearchJob = null
    }

    private suspend fun checkForCloserRestroom(lat: Double, lng: Double) {
        val result = restroomRepository.searchNearbyRestrooms(lat, lng, settingsRepository.searchRadius)
        result.onSuccess { places ->
            val closer = places.firstOrNull()
            if (closer != null) {
                val closerLat = closer.y.toDoubleOrNull() ?: return@onSuccess
                val closerLng = closer.x.toDoubleOrNull() ?: return@onSuccess
                val closerDist = closer.distance.toIntOrNull() ?: return@onSuccess
                val isDifferentPlace = Math.abs(closerLat - currentDestLat) > 0.0005 ||
                    Math.abs(closerLng - currentDestLng) > 0.0005 // ~50m threshold
                if (isDifferentPlace && closerDist > 0) {
                    _closerPlaceSuggestion.postValue("더 가까운 화장실: ${closer.placeName} (${closerDist}m)")
                }
            }
        }
    }

    fun acceptReRoute() {
        _closerPlaceSuggestion.value = null
        navigationRepository.requestReRoute()
        speakIfAvailable("더 가까운 경로로 변경합니다")
    }

    fun dismissReRouteSuggestion() {
        _closerPlaceSuggestion.value = null
    }

    private fun recordVisit() {
        val name = _destinationName.value ?: return
        viewModelScope.launch {
            favoriteRepository.recordVisit(
                placeName = name,
                addressName = _destinationAddress.value ?: "",
                roadAddressName = _destinationAddress.value ?: "",
                latitude = currentDestLat,
                longitude = currentDestLng,
                categoryName = "",
                phone = ""
            )
        }
    }

    private fun startTbtPolling() {
        tbtPollingJob?.cancel()
        tbtPollingJob = viewModelScope.launch {
            while (true) {
                delay(3_000L)
                navigationRepository.getTBTInfo()
            }
        }
    }

    private fun stopTbtPolling() {
        tbtPollingJob?.cancel()
        tbtPollingJob = null
    }

    fun onTtsMessageConsumed() {
        _ttsMessage.value = null
    }

    fun onArrivalConsumed() {
        _hasArrived.value = false
    }

    private fun speakIfAvailable(text: String) {
        if (settingsRepository.ttsEnabled) {
            ttsRepository.speak(text)
        }
    }

    private fun formatDistance(meters: Int): String {
        return if (meters >= 1000) {
            String.format("%.1fkm", meters / 1000.0)
        } else {
            "${meters}m"
        }
    }

    private fun formatTime(seconds: Int): String {
        if (seconds <= 0) return "도착"
        val minutes = seconds / 60
        return when {
            minutes >= 60 -> "${minutes / 60}시간 ${minutes % 60}분"
            minutes > 0 -> "${minutes}분"
            else -> "${seconds}초"
        }
    }
}
