package com.panicdev.poopilot.data.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.panicdev.poopilot.MainActivity

/**
 * Gleo AI로부터 전달되는 브로드캐스트 인텐트를 수신하는 리시버(Receiver)입니다.
 *
 * 외부 AI 서비스(Gleo)에서 "급똥모드 활성화" 또는 "급똥모드 취소" 명령을 전송하면,
 * 이 리시버가 해당 명령을 받아 [MainActivity]를 실행하고 적절한 동작을 수행합니다.
 * 처리 결과는 resultCode와 resultData를 통해 발신자에게 JSON 형식으로 반환됩니다.
 */
class GleoAiReceiver : BroadcastReceiver() {

    /**
     * 브로드캐스트 인텐트를 수신했을 때 호출됩니다.
     *
     * 인텐트의 action에 따라 급똥모드 활성화 또는 취소 명령을 처리합니다.
     * 알 수 없는 action이 들어오면 RESULT_CANCELED를 반환합니다.
     *
     * @param context 브로드캐스트를 수신한 컨텍스트
     * @param intent 수신된 인텐트 (action으로 명령 종류를 구분합니다)
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received Gleo AI intent: ${intent.action}")

        when (intent.action) {
            // 급똥모드 활성화 명령 처리
            ACTION_ACTIVATE_EMERGENCY -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_GLEO_COMMAND, COMMAND_ACTIVATE)
                }
                context.startActivity(launchIntent)
                resultCode = Activity.RESULT_OK
                resultData = "{\"status\":\"success\",\"message\":\"급똥모드 활성화\"}"
            }
            // 급똥모드 취소 명령 처리
            ACTION_CANCEL_EMERGENCY -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_GLEO_COMMAND, COMMAND_CANCEL)
                }
                context.startActivity(launchIntent)
                resultCode = Activity.RESULT_OK
                resultData = "{\"status\":\"success\",\"message\":\"급똥모드 취소\"}"
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
                resultCode = Activity.RESULT_CANCELED
            }
        }
    }

    companion object {
        /** 로그 태그 */
        private const val TAG = "GleoAiReceiver"

        /** 급똥모드 활성화를 요청하는 브로드캐스트 액션 */
        const val ACTION_ACTIVATE_EMERGENCY = "com.panicdev.poopilot.ACTION_ACTIVATE_EMERGENCY"

        /** 급똥모드 취소를 요청하는 브로드캐스트 액션 */
        const val ACTION_CANCEL_EMERGENCY = "com.panicdev.poopilot.ACTION_CANCEL_EMERGENCY"

        /** MainActivity로 전달할 명령 값의 인텐트 키 */
        const val EXTRA_GLEO_COMMAND = "gleo_command"

        /** 활성화 명령 값 */
        const val COMMAND_ACTIVATE = "activate"

        /** 취소 명령 값 */
        const val COMMAND_CANCEL = "cancel"
    }
}
