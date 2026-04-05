package com.panicdev.poopilot.presentation.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panicdev.poopilot.data.repository.DoorRepository
import com.panicdev.poopilot.data.repository.NavigationEvent
import com.panicdev.poopilot.data.repository.NavigationRepository
import com.panicdev.poopilot.data.repository.TtsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val navigationRepository: NavigationRepository,
    private val doorRepository: DoorRepository,
    private val ttsRepository: TtsRepository
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

    init {
        ttsRepository.initialize()
        navigationRepository.registerListener()
        observeNavigationEvents()
    }

    override fun onCleared() {
        super.onCleared()
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
                        val timeText = formatTime(event.info.duration)
                        speakIfAvailable("${_destinationName.value}까지 $timeText 소요됩니다")
                    }
                    is NavigationEvent.TBTUpdated -> {
                        _remainingDistance.value = formatDistance(event.info.remainDistance)
                        _remainingTime.value = formatTime(event.info.remainTime)
                    }
                    is NavigationEvent.DestinationArrived -> {
                        _hasArrived.value = true
                        doorRepository.unlockDriverDoor()
                        val message = "도착했습니다! 문이 열렸습니다!"
                        _ttsMessage.value = message
                        speakIfAvailable(message)
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
        navigationRepository.cancelRoute()
    }

    fun onTtsMessageConsumed() {
        _ttsMessage.value = null
    }

    fun onArrivalConsumed() {
        _hasArrived.value = false
    }

    private fun speakIfAvailable(text: String) {
        ttsRepository.speak(text)
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
