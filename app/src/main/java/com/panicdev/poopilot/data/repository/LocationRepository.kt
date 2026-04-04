package com.panicdev.poopilot.data.repository

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationRepository @Inject constructor(
    private val naviHelper: NaviHelper
) {
    private val isInitialized = AtomicBoolean(false)

    companion object {
        private const val TIMEOUT_MS = 10_000L
    }

    fun initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            naviHelper.initialize()
        }
    }

    fun release() {
        if (isInitialized.compareAndSet(true, false)) {
            naviHelper.release()
        }
    }

    suspend fun getCurrentLocation(): CurrentLocationInfo? {
        return withTimeout(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : NaviHelperEventListener {
                    override fun onCurrentLocationInfo(info: CurrentLocationInfo) {
                        naviHelper.removeListener(this)
                        if (continuation.isActive) {
                            continuation.resume(info)
                        }
                    }
                }
                naviHelper.addListener(listener)
                naviHelper.getCurrentLocationInfo()

                continuation.invokeOnCancellation {
                    naviHelper.removeListener(listener)
                }
            }
        }
    }

    suspend fun getRouteState(): RouteStateInfo? {
        return withTimeout(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : NaviHelperEventListener {
                    override fun onRouteStateInfo(info: RouteStateInfo) {
                        naviHelper.removeListener(this)
                        if (continuation.isActive) {
                            continuation.resume(info)
                        }
                    }
                }
                naviHelper.addListener(listener)
                naviHelper.getRouteStateInfo()

                continuation.invokeOnCancellation {
                    naviHelper.removeListener(listener)
                }
            }
        }
    }

    suspend fun getDestinationInfo(): DestinationInfo? {
        return withTimeout(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : NaviHelperEventListener {
                    override fun onDestinationInfo(info: DestinationInfo) {
                        naviHelper.removeListener(this)
                        if (continuation.isActive) {
                            continuation.resume(info)
                        }
                    }
                }
                naviHelper.addListener(listener)
                naviHelper.getDestinationInfo()

                continuation.invokeOnCancellation {
                    naviHelper.removeListener(listener)
                }
            }
        }
    }
}
