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

/**
 * 길 안내 화면(NavigationFragment)의 비즈니스 로직을 담당하는 ViewModel입니다.
 *
 * 카카오 내비게이션 SDK 이벤트를 수신하여 남은 거리/시간/회전 안내를 업데이트하고,
 * 도착 시 방문 기록 저장 및 차량 문 잠금 해제를 처리합니다.
 * 주행 중 더 가까운 화장실이 발견되면 경로 변경을 제안하는 기능도 포함합니다.
 *
 * @param navigationRepository 카카오 내비게이션 SDK와 통신하는 저장소
 * @param doorRepository 차량 도어 잠금/해제를 제어하는 저장소
 * @param ttsRepository 음성 안내(TTS)를 처리하는 저장소
 * @param settingsRepository 사용자 설정값을 읽는 저장소
 * @param restroomRepository 주변 화장실을 검색하는 저장소
 * @param favoriteRepository 방문 기록을 저장하는 저장소
 * @param locationRepository 현재 위치 정보를 가져오는 저장소
 */
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

    /** 목적지까지 남은 거리를 문자열로 나타냅니다. 예: "500m", "1.2km" */
    private val _remainingDistance = MutableLiveData("--")
    val remainingDistance: LiveData<String> = _remainingDistance

    /** 목적지까지 남은 예상 시간을 문자열로 나타냅니다. 예: "5분", "1시간 20분" */
    private val _remainingTime = MutableLiveData("--")
    val remainingTime: LiveData<String> = _remainingTime

    /** 현재 안내 중인 목적지의 이름입니다. */
    private val _destinationName = MutableLiveData("")
    val destinationName: LiveData<String> = _destinationName

    /** 현재 안내 중인 목적지의 주소입니다. */
    private val _destinationAddress = MutableLiveData("")
    val destinationAddress: LiveData<String> = _destinationAddress

    /** 목적지에 도착했을 때 true로 설정됩니다. UI에서 소비 후 false로 초기화해야 합니다. */
    private val _hasArrived = MutableLiveData(false)
    val hasArrived: LiveData<Boolean> = _hasArrived

    /** 토스트로 표시할 TTS 음성 안내 메시지입니다. 소비 후 null로 초기화해야 합니다. */
    private val _ttsMessage = MutableLiveData<String?>()
    val ttsMessage: LiveData<String?> = _ttsMessage

    /** 다음 회전 지점에 대한 안내 텍스트입니다. 예: "200m 앞에서 좌회전" */
    private val _tbtDescription = MutableLiveData("")
    val tbtDescription: LiveData<String> = _tbtDescription

    /** 현재 길 안내가 진행 중인지 여부를 나타냅니다. */
    private val _isNavigating = MutableLiveData(false)
    val isNavigating: LiveData<Boolean> = _isNavigating

    /** TBT(회전 안내 정보)를 주기적으로 가져오는 코루틴 Job입니다. */
    private var tbtPollingJob: Job? = null
    /** 더 가까운 화장실을 주기적으로 재탐색하는 코루틴 Job입니다. */
    private var reSearchJob: Job? = null

    /** 더 가까운 화장실 제안 메시지입니다. 없으면 null입니다. */
    private val _closerPlaceSuggestion = MutableLiveData<String?>()
    val closerPlaceSuggestion: LiveData<String?> = _closerPlaceSuggestion

    /** 현재 목적지의 위도입니다. 더 가까운 화장실 비교에 사용됩니다. */
    private var currentDestLat = 0.0
    /** 현재 목적지의 경도입니다. 더 가까운 화장실 비교에 사용됩니다. */
    private var currentDestLng = 0.0

    init {
        // ViewModel 초기화 시 TTS와 내비게이션 이벤트 리스너를 준비합니다
        ttsRepository.initialize()
        navigationRepository.registerListener()
        observeNavigationEvents()
    }

    /**
     * ViewModel이 소멸될 때 호출됩니다.
     * 폴링 작업을 중단하고 TTS 및 내비게이션 리스너를 정리합니다.
     */
    override fun onCleared() {
        super.onCleared()
        stopTbtPolling()
        stopReSearch()
        ttsRepository.stop()
        navigationRepository.unregisterListener()
    }

    /**
     * 내비게이션 이벤트(경로 시작, TBT 업데이트, 주행 정보 변경, 도착)를 수신하여 처리합니다.
     * 각 이벤트에 따라 UI 데이터를 업데이트하고 필요한 동작을 수행합니다.
     */
    private fun observeNavigationEvents() {
        viewModelScope.launch {
            navigationRepository.navigationEvents.collect { event ->
                when (event) {
                    is NavigationEvent.RouteStarted -> {
                        // 경로 안내가 시작되면 초기 거리/시간을 표시하고 폴링을 시작합니다
                        _remainingDistance.value = formatDistance(event.info.distance)
                        _remainingTime.value = formatTime(event.info.duration)
                        _isNavigating.value = true
                        startTbtPolling()
                        startReSearch()
                        val timeText = formatTime(event.info.duration)
                        speakIfAvailable("${_destinationName.value}까지 $timeText 소요됩니다")
                    }
                    is NavigationEvent.TBTUpdated -> {
                        // 회전 안내 정보가 업데이트되면 첫 번째 안내 설명을 표시합니다
                        val firstTbt = event.tbtList.firstOrNull()
                        if (firstTbt != null) {
                            _tbtDescription.value = firstTbt.description ?: ""
                        }
                    }
                    is NavigationEvent.DrivingInfoUpdated -> {
                        // 주행 중 남은 거리와 시간을 실시간으로 업데이트합니다
                        val dest = event.info.destination
                        _remainingDistance.value = formatDistance(dest.distance)
                        _remainingTime.value = formatTime(dest.duration)
                    }
                    is NavigationEvent.DestinationArrived -> {
                        // 목적지에 도착하면 방문 기록을 저장하고 설정에 따라 차량 문을 엽니다
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

    /**
     * 지정한 목적지로 길 안내를 시작합니다.
     * ViewModel에 목적지 정보를 저장하고 내비게이션 SDK에 경로 탐색을 요청합니다.
     *
     * @param name 목적지 이름
     * @param address 목적지 주소
     * @param lat 목적지 위도
     * @param lng 목적지 경도
     */
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

    /**
     * 현재 진행 중인 길 안내를 취소합니다.
     * 폴링 작업을 중단하고 내비게이션 SDK에 경로 취소를 요청합니다.
     */
    fun cancelNavigation() {
        stopTbtPolling()
        stopReSearch()
        _isNavigating.value = false
        navigationRepository.cancelRoute()
    }

    /**
     * 주기적으로 현재 위치 주변의 더 가까운 화장실을 재탐색합니다.
     * 길 안내 시작 1분 후 첫 탐색을 시작하고, 이후 2분 간격으로 반복합니다.
     */
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

    /** 더 가까운 화장실 재탐색 작업을 중단합니다. */
    private fun stopReSearch() {
        reSearchJob?.cancel()
        reSearchJob = null
    }

    /**
     * 현재 위치에서 더 가까운 화장실이 있는지 확인합니다.
     * 현재 목적지보다 약 50m 이상 다른 위치에 더 가까운 화장실이 있으면
     * [_closerPlaceSuggestion]에 제안 메시지를 저장합니다.
     *
     * @param lat 현재 위치의 위도
     * @param lng 현재 위치의 경도
     */
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

    /** 사용자가 더 가까운 화장실로 경로를 변경하는 것을 수락했을 때 호출됩니다. */
    fun acceptReRoute() {
        _closerPlaceSuggestion.value = null
        navigationRepository.requestReRoute()
        speakIfAvailable("더 가까운 경로로 변경합니다")
    }

    /** 더 가까운 화장실 경로 변경 제안을 무시합니다. */
    fun dismissReRouteSuggestion() {
        _closerPlaceSuggestion.value = null
    }

    /**
     * 현재 목적지 화장실의 방문 기록을 데이터베이스에 저장합니다.
     * 도착 이벤트 발생 시 자동으로 호출됩니다.
     */
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

    /**
     * TBT(Turn-By-Turn) 회전 안내 정보를 3초마다 가져오는 폴링을 시작합니다.
     * 경로 안내가 시작되면 자동으로 호출됩니다.
     */
    private fun startTbtPolling() {
        tbtPollingJob?.cancel()
        tbtPollingJob = viewModelScope.launch {
            while (true) {
                delay(3_000L)
                navigationRepository.getTBTInfo()
            }
        }
    }

    /** TBT 폴링 작업을 중단합니다. */
    private fun stopTbtPolling() {
        tbtPollingJob?.cancel()
        tbtPollingJob = null
    }

    /** TTS 메시지 이벤트를 소비(초기화)합니다. Fragment에서 메시지를 표시한 후 반드시 호출해야 합니다. */
    fun onTtsMessageConsumed() {
        _ttsMessage.value = null
    }

    /** 도착 이벤트를 소비(초기화)합니다. Fragment에서 도착 처리 후 반드시 호출해야 합니다. */
    fun onArrivalConsumed() {
        _hasArrived.value = false
    }

    /**
     * TTS 설정이 켜져 있을 때만 음성 안내를 재생합니다.
     *
     * @param text 음성으로 읽을 텍스트
     */
    private fun speakIfAvailable(text: String) {
        if (settingsRepository.ttsEnabled) {
            ttsRepository.speak(text)
        }
    }

    /**
     * 미터(m) 단위의 거리를 사람이 읽기 쉬운 형태로 변환합니다.
     * 1km 이상이면 "X.Xkm", 미만이면 "Xm" 형태로 반환합니다.
     *
     * @param meters 미터 단위의 거리
     * @return 사람이 읽기 쉬운 거리 문자열
     */
    private fun formatDistance(meters: Int): String {
        return if (meters >= 1000) {
            String.format("%.1fkm", meters / 1000.0)
        } else {
            "${meters}m"
        }
    }

    /**
     * 초(seconds) 단위의 시간을 사람이 읽기 쉬운 형태로 변환합니다.
     * 예: 0초 → "도착", 30초 → "30초", 5분 → "5분", 90분 → "1시간 30분"
     *
     * @param seconds 초 단위의 시간
     * @return 사람이 읽기 쉬운 시간 문자열
     */
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
