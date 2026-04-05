package com.panicdev.poopilot.data.repository

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.DrivingInfo
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

/**
 * 내비게이션 중 발생하는 이벤트를 나타내는 sealed class입니다.
 *
 * 경로 시작, 취소, 목적지 도착, 방향 안내(TBT), 주행 정보 업데이트 등
 * 내비게이션의 다양한 상태 변화를 타입 안전하게 표현합니다.
 */
sealed class NavigationEvent {
    /** 경로 안내가 시작되었을 때 발생하는 이벤트 */
    data class RouteStarted(val info: RouteStartInfo) : NavigationEvent()

    /** 경로 안내가 취소되었을 때 발생하는 이벤트 */
    object RouteCancelled : NavigationEvent()

    /** 목적지(화장실)에 도착했을 때 발생하는 이벤트 */
    data class DestinationArrived(val info: DestinationArrivedInfo) : NavigationEvent()

    /** 다음 방향 안내(Turn-By-Turn) 정보가 업데이트될 때 발생하는 이벤트 */
    data class TBTUpdated(val tbtList: List<TBTInfo>) : NavigationEvent()

    /** 현재 주행 정보(속도, 남은 거리 등)가 업데이트될 때 발생하는 이벤트 */
    data class DrivingInfoUpdated(val info: DrivingInfo) : NavigationEvent()
}

/**
 * 차량 내비게이션 기능을 담당하는 Repository입니다.
 *
 * 화장실로의 경로 요청, 경유지 추가, 경로 취소, 재탐색 등의 내비게이션 제어 기능과
 * 경로 시작/도착/방향 안내 등의 실시간 이벤트를 Flow 형태로 제공합니다.
 */
@Singleton
class NavigationRepository @Inject constructor(
    /** 차량 내비게이션 시스템에 접근하기 위한 NaviHelper */
    private val naviHelper: NaviHelper
) {
    /**
     * 내비게이션 이벤트(경로 시작, 도착, 취소 등)를 외부로 전달하는 SharedFlow.
     * 구독자가 없을 때도 최대 10개의 이벤트를 버퍼에 보관합니다.
     */
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 10)
    /** 내비게이션 이벤트 스트림 (읽기 전용으로 외부에 노출) */
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents

    /**
     * 현재 방향 안내(TBT, Turn-By-Turn) 정보를 실시간으로 유지하는 StateFlow.
     * 방향 전환 안내가 없으면 null입니다.
     */
    private val _currentTBT = MutableStateFlow<TBTInfo?>(null)
    /** 현재 방향 안내 정보 (읽기 전용으로 외부에 노출) */
    val currentTBT: StateFlow<TBTInfo?> = _currentTBT

    /**
     * NaviHelper로부터 내비게이션 이벤트를 수신하여 Flow로 전달하는 리스너.
     * [registerListener]로 등록하고 [unregisterListener]로 해제합니다.
     */
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

        override fun onTBTInfo(info: List<TBTInfo>) {
            // 첫 번째 TBT 항목을 현재 방향 안내로 설정
            _currentTBT.value = info.firstOrNull()
            _navigationEvents.tryEmit(NavigationEvent.TBTUpdated(info))
        }

        override fun onDrivingInfo(info: DrivingInfo) {
            _navigationEvents.tryEmit(NavigationEvent.DrivingInfoUpdated(info))
        }
    }

    /**
     * 내비게이션 이벤트 리스너를 NaviHelper에 등록합니다.
     * 내비게이션 기능을 사용하기 전에 반드시 호출해야 합니다.
     */
    fun registerListener() {
        naviHelper.addListener(eventListener)
    }

    /**
     * 내비게이션 이벤트 리스너를 NaviHelper에서 해제합니다.
     * 화면이 사라지거나 내비게이션이 필요 없을 때 호출하여 메모리 누수를 방지합니다.
     */
    fun unregisterListener() {
        naviHelper.removeListener(eventListener)
    }

    /**
     * 지정된 목적지까지의 경로를 새로 요청합니다.
     * 최단 시간(SHORT_TIME) 경로 옵션을 사용합니다.
     *
     * @param latitude 목적지 위도
     * @param longitude 목적지 경도
     * @param poiName 목적지 장소 이름 (예: "서울역 화장실")
     * @param address 목적지 주소
     */
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

    /**
     * 현재 경로에 경유지를 추가합니다.
     * 이미 경로 안내 중일 때 화장실을 중간 경유지로 추가하는 데 사용됩니다.
     *
     * @param latitude 경유지 위도
     * @param longitude 경유지 경도
     * @param poiName 경유지 장소 이름
     */
    suspend fun addWaypoint(
        latitude: Double,
        longitude: Double,
        poiName: String
    ) {
        val waypointInfo = RequestWaypointInfo(
            longitude = longitude,
            latitude = latitude,
            poiName = poiName,
            waypointIndex = WaypointIndex.FIRST,
            poiId = "",
            address = "",
            poiSubId = "0"
        )
        naviHelper.addWaypoint(waypointInfo)
    }

    /**
     * 목적지로의 내비게이션을 시작합니다.
     *
     * 현재 경로 안내 중인지 확인하여:
     * - 이미 경로 안내 중이면: 목적지를 경유지로 추가합니다 ([addWaypoint])
     * - 경로 안내 중이 아니면: 새로운 경로를 요청합니다 ([requestRoute])
     *
     * @param latitude 목적지 위도
     * @param longitude 목적지 경도
     * @param poiName 목적지 장소 이름
     * @param address 목적지 주소
     */
    suspend fun startNavigation(
        latitude: Double,
        longitude: Double,
        poiName: String,
        address: String
    ) {
        // 현재 경로 안내 중인지 확인 (최대 5초 대기)
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

        // 경로 안내 중이면 경유지 추가, 아니면 새 경로 요청
        if (routeState?.isRouting == true) {
            addWaypoint(latitude, longitude, poiName)
        } else {
            requestRoute(latitude, longitude, poiName, address)
        }
    }

    /**
     * 현재 진행 중인 경로 안내를 취소합니다.
     */
    fun cancelRoute() {
        naviHelper.cancelRoute()
    }

    /**
     * 경로를 재탐색합니다. 경로를 벗어났을 때 새로운 경로를 찾는 데 사용됩니다.
     */
    fun requestReRoute() {
        naviHelper.requestReRoute()
    }

    /**
     * 현재 방향 안내(TBT) 정보를 NaviHelper에 요청합니다.
     * 결과는 [eventListener]의 onTBTInfo를 통해 [currentTBT]와 [navigationEvents]로 전달됩니다.
     */
    fun getTBTInfo() {
        naviHelper.getTBTInfo()
    }
}
