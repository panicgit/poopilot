package com.panicdev.poopilot.presentation.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.panicdev.poopilot.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchRadius = MutableLiveData(settingsRepository.searchRadius)
    val searchRadius: LiveData<Int> = _searchRadius

    private val _doorUnlockEnabled = MutableLiveData(settingsRepository.doorUnlockEnabled)
    val doorUnlockEnabled: LiveData<Boolean> = _doorUnlockEnabled

    private val _voiceCommandEnabled = MutableLiveData(settingsRepository.voiceCommandEnabled)
    val voiceCommandEnabled: LiveData<Boolean> = _voiceCommandEnabled

    private val _ttsEnabled = MutableLiveData(settingsRepository.ttsEnabled)
    val ttsEnabled: LiveData<Boolean> = _ttsEnabled

    fun setSearchRadius(radius: Int) {
        _searchRadius.value = radius
        settingsRepository.searchRadius = radius
    }

    fun setDoorUnlockEnabled(enabled: Boolean) {
        _doorUnlockEnabled.value = enabled
        settingsRepository.doorUnlockEnabled = enabled
    }

    fun setVoiceCommandEnabled(enabled: Boolean) {
        _voiceCommandEnabled.value = enabled
        settingsRepository.voiceCommandEnabled = enabled
    }

    fun setTtsEnabled(enabled: Boolean) {
        _ttsEnabled.value = enabled
        settingsRepository.ttsEnabled = enabled
    }
}
