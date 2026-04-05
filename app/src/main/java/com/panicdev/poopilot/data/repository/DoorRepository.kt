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

    fun unlockDriverDoor(onComplete: () -> Unit = {}, onFailed: (Exception) -> Unit = {}) {
        try {
            door.setDoorLock(
                area = DoorArea(DoorValue.ROW_1_LEFT),
                locked = false,
                onFailed = { e ->
                    Log.e(TAG, "Driver door unlock failed", e)
                    onFailed(e)
                }
            )
            onComplete()
        } catch (e: Exception) {
            Log.e(TAG, "Door unlock error", e)
            onFailed(e)
        }
    }

    fun unlockAllDoors(onComplete: () -> Unit = {}, onFailed: (Exception) -> Unit = {}) {
        val areas = listOf(
            DoorValue.ROW_1_LEFT,
            DoorValue.ROW_1_RIGHT,
            DoorValue.ROW_2_LEFT,
            DoorValue.ROW_2_RIGHT
        )
        try {
            areas.forEach { doorValue ->
                door.setDoorLock(
                    area = DoorArea(doorValue),
                    locked = false,
                    onFailed = { e ->
                        Log.e(TAG, "Door unlock failed for $doorValue", e)
                    }
                )
            }
            onComplete()
        } catch (e: Exception) {
            Log.e(TAG, "Unlock all doors error", e)
            onFailed(e)
        }
    }

    companion object {
        private const val TAG = "DoorRepository"
    }
}
