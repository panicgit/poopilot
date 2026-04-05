package com.panicdev.poopilot.data.repository

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoorRepository @Inject constructor(
    private val vehicle: Vehicle
) {
    private val door by lazy { vehicle.getDoor() }

    fun unlockDriverDoor(onFailed: (Exception) -> Unit = {}) {
        try {
            door.setDoorLock(
                DoorArea(DoorValue.ROW_1_LEFT),
                false,
                { _, _ -> Log.d(TAG, "Driver door unlocked") },
                { e ->
                    Log.e(TAG, "Driver door unlock failed", e)
                    onFailed(e)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Door unlock error", e)
            onFailed(e)
        }
    }

    fun unlockAllDoors(onFailed: (Exception) -> Unit = {}) {
        val areas = listOf(
            DoorValue.ROW_1_LEFT,
            DoorValue.ROW_1_RIGHT,
            DoorValue.ROW_2_LEFT,
            DoorValue.ROW_2_RIGHT
        )
        try {
            areas.forEach { doorValue ->
                door.setDoorLock(
                    DoorArea(doorValue),
                    false,
                    { _, _ -> Log.d(TAG, "Door unlocked: $doorValue") },
                    { e ->
                        Log.e(TAG, "Door unlock failed for $doorValue", e)
                        onFailed(e)
                    }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unlock all doors error", e)
            onFailed(e)
        }
    }

    companion object {
        private const val TAG = "DoorRepository"
    }
}
