package com.panicdev.poopilot.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.panicdev.poopilot.MainActivity

class GleoAiReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received Gleo AI intent: ${intent.action}")

        when (intent.action) {
            ACTION_ACTIVATE_EMERGENCY -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_GLEO_COMMAND, COMMAND_ACTIVATE)
                }
                context.startActivity(launchIntent)
                setResultCode(RESULT_OK)
                setResultData("{\"status\":\"success\",\"message\":\"급똥모드 활성화\"}")
            }
            ACTION_CANCEL_EMERGENCY -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_GLEO_COMMAND, COMMAND_CANCEL)
                }
                context.startActivity(launchIntent)
                setResultCode(RESULT_OK)
                setResultData("{\"status\":\"success\",\"message\":\"급똥모드 취소\"}")
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
                setResultCode(RESULT_CANCELED)
            }
        }
    }

    companion object {
        private const val TAG = "GleoAiReceiver"
        const val ACTION_ACTIVATE_EMERGENCY = "com.panicdev.poopilot.ACTION_ACTIVATE_EMERGENCY"
        const val ACTION_CANCEL_EMERGENCY = "com.panicdev.poopilot.ACTION_CANCEL_EMERGENCY"
        const val EXTRA_GLEO_COMMAND = "gleo_command"
        const val COMMAND_ACTIVATE = "activate"
        const val COMMAND_CANCEL = "cancel"
        private const val RESULT_OK = 0
        private const val RESULT_CANCELED = 1
    }
}
