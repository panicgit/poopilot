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

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestLocationPermission()
        handleGleoIntent(intent)
    }

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
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleGleoIntent(intent)
    }

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

sealed class GleoCommand {
    object Activate : GleoCommand()
    object Cancel : GleoCommand()
}

object GleoCommandBus {
    private val _commands = kotlinx.coroutines.flow.MutableSharedFlow<GleoCommand>(replay = 1, extraBufferCapacity = 5)
    val commands: kotlinx.coroutines.flow.SharedFlow<GleoCommand> = _commands

    fun emit(command: GleoCommand) {
        _commands.tryEmit(command)
    }
}
