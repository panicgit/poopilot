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

@Singleton
class LocationRepository @Inject constructor(
    private val naviHelper: NaviHelper,
    @ApplicationContext private val context: Context
) {
    private val isInitialized = AtomicBoolean(false)

    companion object {
        private const val TAG = "LocationRepository"
        private const val NAVI_TIMEOUT_MS = 5_000L
        private const val GPS_TIMEOUT_MS = 10_000L
    }

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

    fun release() {
        if (isInitialized.compareAndSet(true, false)) {
            try {
                naviHelper.release()
            } catch (e: Exception) {
                Log.w(TAG, "NaviHelper release failed", e)
            }
        }
    }

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

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null
            val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
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
