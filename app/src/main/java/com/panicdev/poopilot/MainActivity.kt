package com.panicdev.poopilot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.panicdev.poopilot.data.receiver.GleoAiReceiver
import dagger.hilt.android.AndroidEntryPoint

/**
 * 앱의 진입점이 되는 메인 액티비티입니다.
 *
 * 이 액티비티는 앱이 처음 실행될 때 화면을 구성하고, 다음 두 가지 역할을 수행합니다.
 *
 * 1. **위치 권한 요청**: 화장실 검색 등 위치 기반 서비스를 사용하려면
 *    사용자의 위치 정보 접근 권한이 필요합니다. 권한이 없으면 시스템 팝업으로 요청합니다.
 *
 * 2. **Gleo AI 명령 처리**: 외부(예: Gleo AI 어시스턴트)에서 인텐트로 전달된
 *    음성 명령(활성화/취소 등)을 받아 [GleoCommandBus]를 통해 앱 내부로 전달합니다.
 *
 * [@AndroidEntryPoint]는 Hilt가 이 액티비티에 의존성을 자동으로 주입할 수 있도록 해줍니다.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 앱 시작 시 위치 권한을 확인하고, 없으면 사용자에게 요청합니다.
        requestLocationPermission()
        // 앱이 Gleo AI 명령 인텐트로 실행된 경우 해당 명령을 처리합니다.
        handleGleoIntent(intent)
    }

    /**
     * 위치 권한을 요청하는 함수입니다.
     *
     * 정밀 위치([ACCESS_FINE_LOCATION])와 대략적 위치([ACCESS_COARSE_LOCATION]) 권한을
     * 함께 요청합니다. 이미 권한이 허용된 경우에는 아무 동작도 하지 않습니다.
     */
    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    companion object {
        /** 위치 권한 요청 시 결과를 식별하기 위한 요청 코드입니다. */
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    /**
     * 이미 실행 중인 액티비티가 새로운 인텐트를 받았을 때 호출됩니다.
     *
     * 예를 들어, Gleo AI가 앱이 이미 켜져 있는 상태에서 새 명령을 보내면
     * onCreate 대신 이 함수가 호출됩니다.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleGleoIntent(intent)
    }

    /**
     * Gleo AI로부터 전달된 인텐트를 처리하는 함수입니다.
     *
     * 인텐트에서 명령 문자열을 꺼낸 뒤, 명령 종류에 따라
     * [GleoCommandBus]로 [GleoCommand]를 발행(emit)합니다.
     * 처리 후에는 인텐트에서 해당 Extra를 제거해 중복 처리를 방지합니다.
     *
     * @param intent Gleo AI 명령을 담고 있을 수 있는 인텐트. null이면 아무 동작도 하지 않습니다.
     */
    private fun handleGleoIntent(intent: Intent?) {
        val command = intent?.getStringExtra(GleoAiReceiver.EXTRA_GLEO_COMMAND) ?: return
        intent.removeExtra(GleoAiReceiver.EXTRA_GLEO_COMMAND)

        when (command) {
            GleoAiReceiver.COMMAND_ACTIVATE -> {
                // MainFragment의 ViewModel이 이 이벤트를 처리
                GleoCommandBus.emit(GleoCommand.Activate)
            }
            GleoAiReceiver.COMMAND_CANCEL -> {
                GleoCommandBus.emit(GleoCommand.Cancel)
            }
        }
    }
}

/**
 * Gleo AI로부터 받을 수 있는 명령의 종류를 나타내는 sealed class입니다.
 *
 * sealed class를 사용하면 명령의 종류를 컴파일 타임에 모두 알 수 있어,
 * when 표현식에서 누락된 케이스를 컴파일러가 잡아줍니다.
 */
sealed class GleoCommand {
    /** Gleo AI가 앱 기능을 활성화(시작)하도록 요청하는 명령입니다. */
    object Activate : GleoCommand()

    /** Gleo AI가 현재 진행 중인 동작을 취소하도록 요청하는 명령입니다. */
    object Cancel : GleoCommand()
}

/**
 * Gleo AI 명령을 앱 내부의 여러 컴포넌트에 전달하기 위한 이벤트 버스입니다.
 *
 * [kotlinx.coroutines.flow.SharedFlow]를 사용하여 명령을 브로드캐스트합니다.
 * ViewModel 등 구독자가 이 Flow를 collect하면 명령이 전달됩니다.
 *
 * - [replay] = 1: 새로 구독하는 구독자에게 가장 최근 명령을 1개 재전달합니다.
 * - [extraBufferCapacity] = 5: 구독자가 처리하기 전에 최대 5개의 명령을 버퍼에 보관합니다.
 */
object GleoCommandBus {
    private val _commands = kotlinx.coroutines.flow.MutableSharedFlow<GleoCommand>(replay = 1, extraBufferCapacity = 5)

    /** 외부에서 구독할 수 있는 읽기 전용 명령 스트림입니다. */
    val commands: kotlinx.coroutines.flow.SharedFlow<GleoCommand> = _commands

    /**
     * 새로운 Gleo 명령을 버스에 발행합니다.
     *
     * [tryEmit]을 사용하므로 코루틴 없이 어디서든 호출할 수 있습니다.
     *
     * @param command 전달할 [GleoCommand]
     */
    fun emit(command: GleoCommand) {
        _commands.tryEmit(command)
    }
}
