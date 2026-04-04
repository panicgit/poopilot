package com.panicdev.poopilot.data.repository

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.constants.WaypointIndex
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed class NavigationEvent {
    data class RouteStarted(val info: RouteStartInfo) : NavigationEvent()
    object RouteCancelled : NavigationEvent()
    data class DestinationArrived(val info: DestinationArrivedInfo) : NavigationEvent()
    data class TBTUpdated(val info: TBTInfo) : NavigationEvent()
}

@Singleton
class NavigationRepository @Inject constructor(
    private val naviHelper: NaviHelper
) {
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 10)
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents

    private val _currentTBT = MutableStateFlow<TBTInfo?>(null)
    val currentTBT: StateFlow<TBTInfo?> = _currentTBT

    private val eventListener = object : NaviHelperEventListener {
        override fun onRouteStarted(info: RouteStartInfo) {
            _navigationEvents.tryEmit(NavigationEvent.RouteStarted(info))
        }

        override fun onRouteCancelled() {
            _navigationEvents.tryEmit(NavigationEvent.RouteCancelled)
        }

        override fun onDestinationArrived(info: DestinationArrivedInfo) {
            _navigationEvents.tryEmit(NavigationEvent.DestinationArrived(info))
        }

        override fun onTBTInfo(info: TBTInfo) {
            _currentTBT.value = info
            _navigationEvents.tryEmit(NavigationEvent.TBTUpdated(info))
        }
    }

    fun registerListener() {
        naviHelper.addListener(eventListener)
    }

    fun unregisterListener() {
        naviHelper.removeListener(eventListener)
    }

    suspend fun requestRoute(
        latitude: Double,
        longitude: Double,
        poiName: String,
        address: String
    ) {
        val routeInfo = RouteInfo(
            latitude = latitude,
            longitude = longitude,
            poiId = "",
            poiName = poiName,
            poiSubId = "0",
            address = address,
            routeOption = RouteOption.SHORT_TIME
        )
        naviHelper.requestRoute(routeInfo)
    }

    suspend fun addWaypoint(
        latitude: Double,
        longitude: Double,
        poiName: String
    ) {
        val waypointInfo = RequestWaypointInfo(
            latitude = latitude,
            longitude = longitude,
            waypointIndex = WaypointIndex.FIRST,
            poiName = poiName
        )
        naviHelper.addWaypoint(waypointInfo)
    }

    suspend fun startNavigation(
        latitude: Double,
        longitude: Double,
        poiName: String,
        address: String
    ) {
        val routeState = try {
            withTimeout(5_000L) {
                suspendCancellableCoroutine<RouteStateInfo?> { continuation ->
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
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            // 타임아웃 시 기존 경로 없는 것으로 간주
            null
        } catch (e: Exception) {
            throw e  // 예상치 못한 에러는 호출자에게 전파
        }

        if (routeState?.isRouting == true) {
            addWaypoint(latitude, longitude, poiName)
        } else {
            requestRoute(latitude, longitude, poiName, address)
        }
    }

    fun cancelRoute() {
        naviHelper.cancelRoute()
    }

    fun requestReRoute() {
        naviHelper.requestReRoute()
    }

    fun getTBTInfo() {
        naviHelper.getTBTInfo()
    }
}
