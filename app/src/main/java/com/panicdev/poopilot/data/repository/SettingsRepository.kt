package com.panicdev.poopilot.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱의 사용자 설정 값을 저장하고 불러오는 저장소(Repository)입니다.
 *
 * Android의 [SharedPreferences]를 사용하여 앱 설정을 기기에 영구적으로 저장합니다.
 * 검색 반경, 문 잠금 해제 기능, 음성 명령, TTS(음성 안내) 등의 설정을 관리합니다.
 * 각 프로퍼티는 getter/setter를 통해 바로 읽고 쓸 수 있어 편리하게 사용할 수 있습니다.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    /** 설정 값을 저장하는 SharedPreferences 인스턴스 */
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 화장실 검색 반경 설정 (단위: 미터).
     * 기본값은 1000m(1km)이며, 이 범위 안에 있는 화장실을 검색합니다.
     */
    var searchRadius: Int
        get() = prefs.getInt(KEY_SEARCH_RADIUS, DEFAULT_RADIUS)
        set(value) = prefs.edit().putInt(KEY_SEARCH_RADIUS, value).apply()

    /**
     * 문 잠금 해제 기능 활성화 여부.
     * true이면 앱에서 화장실 문 잠금을 해제하는 기능을 사용할 수 있습니다.
     */
    var doorUnlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_DOOR_UNLOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_DOOR_UNLOCK, value).apply()

    /**
     * 음성 명령 기능 활성화 여부.
     * true이면 "급똥모드", "화장실" 등의 키워드로 앱을 음성으로 제어할 수 있습니다.
     */
    var voiceCommandEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_COMMAND, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_COMMAND, value).apply()

    /**
     * TTS(Text-To-Speech) 음성 안내 기능 활성화 여부.
     * true이면 화장실 위치 등을 음성으로 안내해 줍니다.
     */
    var ttsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()

    companion object {
        /** SharedPreferences 파일 이름 */
        private const val PREFS_NAME = "poopilot_settings"
        /** 검색 반경 설정 키 */
        private const val KEY_SEARCH_RADIUS = "search_radius"
        /** 문 잠금 해제 기능 설정 키 */
        private const val KEY_DOOR_UNLOCK = "door_unlock_enabled"
        /** 음성 명령 기능 설정 키 */
        private const val KEY_VOICE_COMMAND = "voice_command_enabled"
        /** TTS 음성 안내 기능 설정 키 */
        private const val KEY_TTS_ENABLED = "tts_enabled"
        /** 검색 반경 기본값 (1000m) */
        private const val DEFAULT_RADIUS = 1000
    }
}
