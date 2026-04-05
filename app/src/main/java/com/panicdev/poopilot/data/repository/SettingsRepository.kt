package com.panicdev.poopilot.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var searchRadius: Int
        get() = prefs.getInt(KEY_SEARCH_RADIUS, DEFAULT_RADIUS)
        set(value) = prefs.edit().putInt(KEY_SEARCH_RADIUS, value).apply()

    var doorUnlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_DOOR_UNLOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_DOOR_UNLOCK, value).apply()

    var voiceCommandEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_COMMAND, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_COMMAND, value).apply()

    var ttsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()

    companion object {
        private const val PREFS_NAME = "poopilot_settings"
        private const val KEY_SEARCH_RADIUS = "search_radius"
        private const val KEY_DOOR_UNLOCK = "door_unlock_enabled"
        private const val KEY_VOICE_COMMAND = "voice_command_enabled"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val DEFAULT_RADIUS = 1000
    }
}
