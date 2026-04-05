package com.panicdev.poopilot.presentation.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

/**
 * 앱 업데이트 여부를 확인하고 사용자에게 업데이트를 안내하는 유틸리티 객체입니다.
 *
 * 서버에서 받아온 최신 버전 코드와 현재 설치된 앱의 버전 코드를 비교하여
 * 업데이트가 필요한 경우 플레이스토어로 안내하는 다이얼로그를 표시합니다.
 */
object AppUpdateChecker {

    private const val TAG = "AppUpdateChecker"
    /** 플레이스토어 앱으로 바로 이동하는 URI (앱이 설치된 경우) */
    private const val MARKET_URI = "market://details?id=com.panicdev.poopilot"
    /** 플레이스토어 웹 페이지 URI (앱이 없을 때 웹 브라우저로 열기 위한 폴백용) */
    private const val PLAY_STORE_URI = "https://play.google.com/store/apps/details?id=com.panicdev.poopilot"

    /**
     * 현재 설치된 앱 버전과 최신 버전을 비교하여 업데이트가 필요하면 안내 다이얼로그를 표시합니다.
     *
     * @param activity 다이얼로그를 표시할 Activity
     * @param latestVersionCode 서버에서 받아온 최신 버전 코드
     */
    fun checkForUpdate(activity: Activity, latestVersionCode: Int) {
        val currentVersionCode = getCurrentVersionCode(activity)
        if (currentVersionCode < latestVersionCode) {
            showUpdateDialog(activity)
        }
    }

    /**
     * 현재 기기에 설치된 앱의 버전 코드를 가져옵니다.
     * 패키지 정보를 찾지 못하면 0을 반환합니다.
     *
     * @param activity 패키지 정보를 조회할 Activity
     * @return 현재 설치된 앱의 버전 코드. 조회 실패 시 0
     */
    private fun getCurrentVersionCode(activity: Activity): Int {
        return try {
            val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            packageInfo.longVersionCode.toInt()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package not found", e)
            0
        }
    }

    /**
     * 새 버전 업데이트를 안내하는 다이얼로그를 표시합니다.
     * 사용자가 "업데이트" 버튼을 누르면 플레이스토어로 이동하고,
     * "나중에" 버튼을 누르면 다이얼로그를 닫습니다.
     * 뒤로가기 버튼으로는 닫을 수 없습니다(setCancelable false).
     *
     * @param activity 다이얼로그를 표시할 Activity
     */
    private fun showUpdateDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("업데이트 안내")
            .setMessage("새로운 버전이 출시되었습니다.\n안정적인 사용을 위해 업데이트해주세요.")
            .setPositiveButton("업데이트") { _, _ ->
                openMarket(activity)
            }
            .setNegativeButton("나중에") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 플레이스토어 앱 또는 웹 브라우저로 앱 상세 페이지를 엽니다.
     * 플레이스토어 앱이 설치되어 있으면 앱을 우선 열고,
     * 없으면 웹 브라우저로 폴백합니다.
     *
     * @param activity 인텐트를 실행할 Activity
     */
    private fun openMarket(activity: Activity) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI)))
        } catch (e: Exception) {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URI)))
        }
    }
}
