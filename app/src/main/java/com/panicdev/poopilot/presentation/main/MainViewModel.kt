package com.panicdev.poopilot.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panicdev.poopilot.GleoCommand
import com.panicdev.poopilot.GleoCommandBus
import com.panicdev.poopilot.data.db.FavoriteRestroom
import com.panicdev.poopilot.data.repository.FavoriteRepository
import com.panicdev.poopilot.data.repository.LocationRepository
import com.panicdev.poopilot.data.service.VoiceActivationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import javax.inject.Inject

/**
 * 앱 전반의 상태를 나타내는 열거형(enum)입니다.
 *
 * 현재 앱이 어떤 단계에 있는지를 구분하여 UI를 적절하게 업데이트하는 데 사용됩니다.
 */
enum class AppState {
    /** 대기 중 상태. 아무 작업도 진행되지 않고 있습니다. */
    STANDBY,
    /** 화장실 검색 중 상태. 위치를 가져오거나 주변 화장실을 찾고 있습니다. */
    SEARCHING,
    /** 길 안내 중 상태. 목적지로 이동하는 네비게이션이 진행 중입니다. */
    NAVIGATING
}

/**
 * 메인 화면(MainFragment)의 비즈니스 로직을 담당하는 ViewModel입니다.
 *
 * Activity 범위로 생성되어 여러 Fragment에서 공유됩니다.
 * 위치 정보 수집, 음성 명령 감지, 즐겨찾기 목록 관리,
 * 급똥 모드(긴급 화장실 검색) 활성화 등의 역할을 합니다.
 *
 * @param locationRepository 현재 위치 정보를 가져오는 저장소
 * @param voiceActivationService 음성 명령을 감지하는 서비스
 * @param favoriteRepository 즐겨찾기 및 최근 방문 데이터를 관리하는 저장소
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val voiceActivationService: VoiceActivationService,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    /** 현재 앱 상태(대기/검색/길안내)를 나타냅니다. */
    private val _appState = MutableLiveData(AppState.STANDBY)
    val appState: LiveData<AppState> = _appState

    /** 현재 위치의 위도 값입니다. 위치 정보를 가져오기 전에는 0.0입니다. */
    private val _currentLatitude = MutableLiveData(0.0)
    val currentLatitude: LiveData<Double> = _currentLatitude

    /** 현재 위치의 경도 값입니다. 위치 정보를 가져오기 전에는 0.0입니다. */
    private val _currentLongitude = MutableLiveData(0.0)
    val currentLongitude: LiveData<Double> = _currentLongitude

    /** 검색 화면으로 이동해야 할 때 true로 설정됩니다. 이동 후에는 다시 false로 초기화해야 합니다. */
    private val _navigateToSearch = MutableLiveData(false)
    val navigateToSearch: LiveData<Boolean> = _navigateToSearch

    /** 사용자에게 보여줄 오류 메시지입니다. 오류가 없으면 null입니다. */
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /** 음성 명령이 감지되었을 때 true로 설정됩니다. UI에서 소비한 후 false로 초기화해야 합니다. */
    private val _voiceActivated = MutableLiveData(false)
    val voiceActivated: LiveData<Boolean> = _voiceActivated

    /** 즐겨찾기로 등록된 화장실 목록입니다. DB의 변경을 실시간으로 반영합니다. */
    private val _favorites = MutableLiveData<List<FavoriteRestroom>>(emptyList())
    val favorites: LiveData<List<FavoriteRestroom>> = _favorites

    /** 최근 방문한 화장실 목록입니다. DB의 변경을 실시간으로 반영합니다. */
    private val _recentVisits = MutableLiveData<List<FavoriteRestroom>>(emptyList())
    val recentVisits: LiveData<List<FavoriteRestroom>> = _recentVisits

    /** 즐겨찾기 항목을 선택하여 길 안내를 시작할 때 해당 화장실 정보를 담습니다. 소비 후 null로 초기화해야 합니다. */
    private val _navigateToFavorite = MutableLiveData<FavoriteRestroom?>()
    val navigateToFavorite: LiveData<FavoriteRestroom?> = _navigateToFavorite

    init {
        // ViewModel 초기화 시 위치 서비스, 음성 활성화, GleoCommand 수신, 즐겨찾기 로딩을 시작합니다
        locationRepository.initialize()
        startVoiceActivation()
        observeGleoCommands()
        loadFavorites()
    }

    /**
     * 즐겨찾기 목록과 최근 방문 목록을 데이터베이스에서 불러와 LiveData에 반영합니다.
     * Flow를 사용하여 데이터 변경 시 자동으로 업데이트됩니다.
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            favoriteRepository.getFavorites().collectLatest { list ->
                _favorites.value = list
            }
        }
        viewModelScope.launch {
            favoriteRepository.getRecentVisits().collectLatest { list ->
                _recentVisits.value = list
            }
        }
    }

    /** 즐겨찾기 항목을 선택하여 길 안내로 이동하도록 이벤트를 발행합니다. */
    fun navigateToFavorite(restroom: FavoriteRestroom) {
        _navigateToFavorite.value = restroom
    }

    /** 즐겨찾기 이동 이벤트를 소비(초기화)합니다. Fragment에서 화면 이동 후 반드시 호출해야 합니다. */
    fun onNavigateToFavoriteConsumed() {
        _navigateToFavorite.value = null
    }

    /**
     * ViewModel이 소멸될 때 호출됩니다.
     * 음성 서비스와 위치 서비스를 정리하여 리소스 낭비를 방지합니다.
     */
    override fun onCleared() {
        super.onCleared()
        voiceActivationService.stop()
        locationRepository.release()
    }

    /**
     * 음성 활성화 서비스를 시작하고, 음성 명령이 감지되면 급똥 모드를 자동으로 활성화합니다.
     * 앱이 대기 상태일 때만 음성 명령에 반응합니다.
     */
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

    /** 음성 활성화 이벤트를 소비(초기화)합니다. Fragment에서 알림 표시 후 반드시 호출해야 합니다. */
    fun onVoiceActivatedConsumed() {
        _voiceActivated.value = false
    }

    /** 음성 활성화 서비스가 중단된 경우 다시 시작합니다. */
    fun restartVoiceActivation() {
        if (!voiceActivationService.isRunning()) {
            voiceActivationService.start(viewModelScope)
        }
    }

    /**
     * Gleo 차량 시스템으로부터 오는 명령(GleoCommand)을 수신하여 처리합니다.
     * Activate 명령이 오면 급똥 모드를 켜고, Cancel 명령이 오면 대기 상태로 돌아갑니다.
     */
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

    /**
     * 급똥 모드를 활성화합니다.
     * 앱 상태를 SEARCHING으로 변경하고 현재 위치를 가져오기 시작합니다.
     */
    fun activateEmergencyMode() {
        _appState.value = AppState.SEARCHING
        fetchCurrentLocation()
    }

    /**
     * 현재 위치를 비동기로 가져옵니다.
     * 위치를 성공적으로 가져오면 검색 화면으로 이동하고,
     * 실패하거나 시간 초과되면 오류 메시지를 표시하고 대기 상태로 돌아갑니다.
     */
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

    /** 검색 화면 이동 이벤트를 소비(초기화)합니다. Fragment에서 화면 이동 후 반드시 호출해야 합니다. */
    fun onNavigateToSearchConsumed() {
        _navigateToSearch.value = false
    }

    /** 앱 상태를 NAVIGATING(길 안내 중)으로 변경합니다. */
    fun setNavigating() {
        _appState.value = AppState.NAVIGATING
    }

    /**
     * 앱을 초기 대기 상태(STANDBY)로 리셋합니다.
     * 길 안내 취소, 검색 취소 등 홈 화면으로 돌아올 때 사용합니다.
     * 음성 인식도 다시 시작합니다.
     */
    fun resetToStandby() {
        _appState.value = AppState.STANDBY
        _navigateToSearch.value = false
        _errorMessage.value = null
        restartVoiceActivation()
    }
}
