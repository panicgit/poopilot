package com.panicdev.poopilot.presentation.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

object AppUpdateChecker {

    private const val TAG = "AppUpdateChecker"
    private const val MARKET_URI = "market://details?id=com.panicdev.poopilot"
    private const val PLAY_STORE_URI = "https://play.google.com/store/apps/details?id=com.panicdev.poopilot"

    fun checkForUpdate(activity: Activity, latestVersionCode: Int) {
        val currentVersionCode = getCurrentVersionCode(activity)
        if (currentVersionCode < latestVersionCode) {
            showUpdateDialog(activity)
        }
    }

    private fun getCurrentVersionCode(activity: Activity): Int {
        return try {
            val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            packageInfo.longVersionCode.toInt()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package not found", e)
            0
        }
    }

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

    private fun openMarket(activity: Activity) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI)))
        } catch (e: Exception) {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URI)))
        }
    }
}
