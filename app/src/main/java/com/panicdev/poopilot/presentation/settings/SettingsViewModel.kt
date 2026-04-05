package com.panicdev.poopilot.presentation.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _searchRadius = MutableLiveData(loadSearchRadius())
    val searchRadius: LiveData<Int> = _searchRadius

    private val _doorUnlockEnabled = MutableLiveData(loadDoorUnlock())
    val doorUnlockEnabled: LiveData<Boolean> = _doorUnlockEnabled

    private val _voiceCommandEnabled = MutableLiveData(loadVoiceCommand())
    val voiceCommandEnabled: LiveData<Boolean> = _voiceCommandEnabled

    private val _ttsEnabled = MutableLiveData(loadTtsEnabled())
    val ttsEnabled: LiveData<Boolean> = _ttsEnabled

    fun setSearchRadius(radius: Int) {
        _searchRadius.value = radius
        prefs.edit().putInt(KEY_SEARCH_RADIUS, radius).apply()
    }

    fun setDoorUnlockEnabled(enabled: Boolean) {
        _doorUnlockEnabled.value = enabled
        prefs.edit().putBoolean(KEY_DOOR_UNLOCK, enabled).apply()
    }

    fun setVoiceCommandEnabled(enabled: Boolean) {
        _voiceCommandEnabled.value = enabled
        prefs.edit().putBoolean(KEY_VOICE_COMMAND, enabled).apply()
    }

    fun setTtsEnabled(enabled: Boolean) {
        _ttsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
    }

    private fun loadSearchRadius(): Int = prefs.getInt(KEY_SEARCH_RADIUS, DEFAULT_RADIUS)
    private fun loadDoorUnlock(): Boolean = prefs.getBoolean(KEY_DOOR_UNLOCK, true)
    private fun loadVoiceCommand(): Boolean = prefs.getBoolean(KEY_VOICE_COMMAND, true)
    private fun loadTtsEnabled(): Boolean = prefs.getBoolean(KEY_TTS_ENABLED, true)

    companion object {
        const val PREFS_NAME = "poopilot_settings"
        const val KEY_SEARCH_RADIUS = "search_radius"
        const val KEY_DOOR_UNLOCK = "door_unlock_enabled"
        const val KEY_VOICE_COMMAND = "voice_command_enabled"
        const val KEY_TTS_ENABLED = "tts_enabled"
        const val DEFAULT_RADIUS = 1000
    }
}
