package com.panicdev.poopilot.presentation.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * 네트워크 연결 상태를 확인하기 위한 유틸리티 객체입니다.
 *
 * API 호출 전에 인터넷 연결 여부를 미리 확인하거나,
 * 오류 발생 시 네트워크 문제인지 판단할 때 사용합니다.
 */
object NetworkUtils {

    /**
     * 현재 기기의 인터넷 연결 여부를 확인합니다.
     *
     * 활성화된 네트워크(Wi-Fi, 모바일 데이터 등)가 있고
     * 실제 인터넷 접근 가능 여부(NET_CAPABILITY_INTERNET)를 검사합니다.
     *
     * @param context 시스템 서비스에 접근하기 위한 Context
     * @return 인터넷에 연결되어 있으면 true, 그렇지 않으면 false
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
