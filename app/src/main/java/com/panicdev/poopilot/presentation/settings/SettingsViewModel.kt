package com.panicdev.poopilot.presentation.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.panicdev.poopilot.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 설정 화면(SettingsFragment)의 비즈니스 로직을 담당하는 ViewModel입니다.
 *
 * 사용자가 변경한 설정값을 [SettingsRepository]를 통해 즉시 저장하고,
 * 현재 설정값을 LiveData로 제공하여 화면이 항상 최신 상태를 표시하도록 합니다.
 *
 * @param settingsRepository 설정값을 SharedPreferences에 읽고 쓰는 저장소
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /** 현재 설정된 화장실 검색 반경(미터 단위)입니다. 예: 500, 1000, 2000 */
    private val _searchRadius = MutableLiveData(settingsRepository.searchRadius)
    val searchRadius: LiveData<Int> = _searchRadius

    /** 도착 시 차량 운전석 문 잠금 해제 기능의 활성화 여부입니다. */
    private val _doorUnlockEnabled = MutableLiveData(settingsRepository.doorUnlockEnabled)
    val doorUnlockEnabled: LiveData<Boolean> = _doorUnlockEnabled

    /** 음성 명령으로 급똥 모드를 활성화하는 기능의 활성화 여부입니다. */
    private val _voiceCommandEnabled = MutableLiveData(settingsRepository.voiceCommandEnabled)
    val voiceCommandEnabled: LiveData<Boolean> = _voiceCommandEnabled

    /** 길 안내 중 음성 안내(TTS) 기능의 활성화 여부입니다. */
    private val _ttsEnabled = MutableLiveData(settingsRepository.ttsEnabled)
    val ttsEnabled: LiveData<Boolean> = _ttsEnabled

    /**
     * 검색 반경을 변경하고 즉시 저장합니다.
     *
     * @param radius 새로 설정할 검색 반경(미터 단위). 예: 500, 1000, 2000
     */
    fun setSearchRadius(radius: Int) {
        _searchRadius.value = radius
        settingsRepository.searchRadius = radius
    }

    /**
     * 도착 시 차량 문 잠금 해제 기능을 켜거나 끄고 즉시 저장합니다.
     *
     * @param enabled true이면 기능 활성화, false이면 비활성화
     */
    fun setDoorUnlockEnabled(enabled: Boolean) {
        _doorUnlockEnabled.value = enabled
        settingsRepository.doorUnlockEnabled = enabled
    }

    /**
     * 음성 명령 기능을 켜거나 끄고 즉시 저장합니다.
     *
     * @param enabled true이면 기능 활성화, false이면 비활성화
     */
    fun setVoiceCommandEnabled(enabled: Boolean) {
        _voiceCommandEnabled.value = enabled
        settingsRepository.voiceCommandEnabled = enabled
    }

    /**
     * TTS(음성 안내) 기능을 켜거나 끄고 즉시 저장합니다.
     *
     * @param enabled true이면 기능 활성화, false이면 비활성화
     */
    fun setTtsEnabled(enabled: Boolean) {
        _ttsEnabled.value = enabled
        settingsRepository.ttsEnabled = enabled
    }
}
