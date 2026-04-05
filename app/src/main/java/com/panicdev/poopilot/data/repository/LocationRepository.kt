package com.panicdev.poopilot.data.repository

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 현재 위치 정보를 가져오는 Repository입니다.
 *
 * 위치를 얻기 위해 3단계 폴백(fallback) 전략을 사용합니다.
 *  1. 차량 NaviHelper (가장 정확, 차량 내비게이션 시스템 활용)
 *  2. Android GPS / 네트워크 위치
 *  3. 마지막으로 알려진 위치 (캐시된 값)
 *
 * 또한 현재 경로 상태(주행 중 여부)와 목적지 정보도 조회할 수 있습니다.
 */
@Singleton
class LocationRepository @Inject constructor(
    /** 차량 내비게이션 시스템에 접근하기 위한 NaviHelper */
    private val naviHelper: NaviHelper,
    /** 안드로이드 시스템 서비스(GPS 등)에 접근하기 위한 앱 Context */
    @ApplicationContext private val context: Context
) {
    /**
     * NaviHelper 초기화 완료 여부.
     * 여러 스레드에서 동시에 초기화되는 것을 막기 위해 AtomicBoolean을 사용합니다.
     */
    private val isInitialized = AtomicBoolean(false)

    companion object {
        private const val TAG = "LocationRepository"
        /** NaviHelper로부터 위치를 받을 때 기다리는 최대 시간 (5초) */
        private const val NAVI_TIMEOUT_MS = 5_000L
        /** Android GPS로부터 위치를 받을 때 기다리는 최대 시간 (10초) */
        private const val GPS_TIMEOUT_MS = 10_000L
    }

    /**
     * NaviHelper를 초기화합니다. 이미 초기화된 경우 아무 동작도 하지 않습니다.
     * 초기화에 실패해도 GPS 폴백이 있으므로 앱은 계속 동작합니다.
     */
    fun initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                naviHelper.initialize()
                Log.d(TAG, "NaviHelper initialized")
            } catch (e: Exception) {
                Log.w(TAG, "NaviHelper init failed, will use GPS fallback", e)
            }
        }
    }

    /**
     * NaviHelper 리소스를 해제합니다. 앱 종료 시 호출하여 메모리를 반환합니다.
     */
    fun release() {
        if (isInitialized.compareAndSet(true, false)) {
            try {
                naviHelper.release()
            } catch (e: Exception) {
                Log.w(TAG, "NaviHelper release failed", e)
            }
        }
    }

    /**
     * 현재 위치를 가져옵니다. 3단계 폴백 전략으로 최선의 위치를 반환합니다.
     *
     * 1단계: NaviHelper (차량 시스템) → 2단계: Android GPS/네트워크 → 3단계: 마지막 알려진 위치
     *
     * @return 현재 위치 정보. 모든 방법이 실패하면 null을 반환합니다.
     */
    suspend fun getCurrentLocation(): CurrentLocationInfo? {
        // 1차: NaviHelper에서 위치 획득 시도
        try {
            val naviLocation = getLocationFromNaviHelper()
            if (naviLocation != null) {
                Log.d(TAG, "Location from NaviHelper: ${naviLocation.latitude}, ${naviLocation.longitude}")
                return naviLocation
            }
        } catch (e: Exception) {
            Log.w(TAG, "NaviHelper location failed, trying GPS fallback", e)
        }

        // 2차: Android GPS 폴백
        try {
            val gpsLocation = getLocationFromGps()
            if (gpsLocation != null) {
                Log.d(TAG, "Location from GPS: ${gpsLocation.latitude}, ${gpsLocation.longitude}")
                return CurrentLocationInfo(
                    gpsLocation.longitude,
                    gpsLocation.latitude,
                    "",
                    "",
                    null
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "GPS location also failed", e)
        }

        // 3차: 마지막으로 알려진 위치 사용
        val lastKnown = getLastKnownLocation()
        if (lastKnown != null) {
            Log.d(TAG, "Using last known location: ${lastKnown.latitude}, ${lastKnown.longitude}")
            return CurrentLocationInfo(
                lastKnown.longitude,
                lastKnown.latitude,
                "",
                "",
                null
            )
        }

        Log.e(TAG, "All location methods failed")
        return null
    }

    /**
     * NaviHelper를 통해 현재 위치를 가져옵니다.
     * 최대 [NAVI_TIMEOUT_MS](5초) 안에 응답이 없으면 타임아웃됩니다.
     *
     * @return NaviHelper로부터 받은 현재 위치 정보. 실패 시 null.
     */
    private suspend fun getLocationFromNaviHelper(): CurrentLocationInfo? {
        return withTimeout(NAVI_TIMEOUT_MS) {
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

    /**
     * Android 시스템의 GPS 또는 네트워크 위치 제공자를 통해 현재 위치를 가져옵니다.
     * GPS를 우선 사용하며, GPS를 사용할 수 없으면 네트워크 위치를 사용합니다.
     * 최대 [GPS_TIMEOUT_MS](10초) 안에 응답이 없으면 타임아웃됩니다.
     *
     * @return Android GPS/네트워크에서 받은 위치. 권한 없거나 제공자 비활성화 시 null.
     */
    @SuppressLint("MissingPermission")
    private suspend fun getLocationFromGps(): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!hasGps && !hasNetwork) {
            Log.w(TAG, "No location provider available")
            return null
        }

        // GPS를 우선 사용하고, 없으면 네트워크 위치 사용
        val provider = if (hasGps) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER

        return try {
            withTimeout(GPS_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            locationManager.removeUpdates(this)
                            if (continuation.isActive) {
                                continuation.resume(location)
                            }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    locationManager.requestLocationUpdates(
                        provider, 0L, 0f, listener, Looper.getMainLooper()
                    )
                    continuation.invokeOnCancellation {
                        locationManager.removeUpdates(listener)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission not granted", e)
            null
        }
    }

    /**
     * 시스템에 캐시된 마지막으로 알려진 위치를 가져옵니다.
     * GPS와 네트워크 중 더 최근에 업데이트된 위치를 반환합니다.
     *
     * @return 가장 최근에 캐시된 위치. 없거나 권한이 없으면 null.
     */
    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null
            val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            // GPS와 네트워크 위치 중 더 최근 것을 선택
            when {
                gpsLoc != null && netLoc != null -> if (gpsLoc.time > netLoc.time) gpsLoc else netLoc
                gpsLoc != null -> gpsLoc
                netLoc != null -> netLoc
                else -> null
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission not granted for last known", e)
            null
        }
    }

    /**
     * 현재 내비게이션 경로 상태를 가져옵니다 (예: 경로 안내 중인지 여부).
     * 최대 [NAVI_TIMEOUT_MS](5초) 안에 응답이 없으면 타임아웃됩니다.
     *
     * @return 현재 경로 상태 정보. 실패 시 null.
     */
    suspend fun getRouteState(): RouteStateInfo? {
        return withTimeout(NAVI_TIMEOUT_MS) {
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

    /**
     * 현재 설정된 목적지 정보를 가져옵니다.
     * 최대 [NAVI_TIMEOUT_MS](5초) 안에 응답이 없으면 타임아웃됩니다.
     *
     * @return 현재 목적지 정보. 목적지가 없거나 실패 시 null.
     */
    suspend fun getDestinationInfo(): DestinationInfo? {
        return withTimeout(NAVI_TIMEOUT_MS) {
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
