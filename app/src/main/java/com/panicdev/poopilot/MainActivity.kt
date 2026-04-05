package com.panicdev.poopilot

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.panicdev.poopilot.data.receiver.GleoAiReceiver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handleGleoIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleGleoIntent(intent)
    }

    private fun handleGleoIntent(intent: Intent?) {
        val command = intent?.getStringExtra(GleoAiReceiver.EXTRA_GLEO_COMMAND) ?: return
        intent.removeExtra(GleoAiReceiver.EXTRA_GLEO_COMMAND)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

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

sealed class GleoCommand {
    object Activate : GleoCommand()
    object Cancel : GleoCommand()
}

object GleoCommandBus {
    private val _commands = kotlinx.coroutines.flow.MutableSharedFlow<GleoCommand>(extraBufferCapacity = 5)
    val commands: kotlinx.coroutines.flow.SharedFlow<GleoCommand> = _commands

    fun emit(command: GleoCommand) {
        _commands.tryEmit(command)
    }
}
