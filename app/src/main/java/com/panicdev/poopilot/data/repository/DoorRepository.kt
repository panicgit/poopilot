package com.panicdev.poopilot.data.repository

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 차량 도어(문) 잠금 해제를 담당하는 Repository입니다.
 *
 * 화장실을 찾아 도착했을 때 차 문을 자동으로 열어주는 기능을 제공합니다.
 * 운전석 문만 열거나, 모든 문을 한꺼번에 열 수 있습니다.
 */
@Singleton
class DoorRepository @Inject constructor(
    /** 차량 제어를 위한 Vehicle SDK 인스턴스 */
    private val vehicle: Vehicle
) {
    /** 도어 제어 객체 - 처음 사용할 때 한 번만 초기화됩니다 (lazy) */
    private val door by lazy { vehicle.getDoor() }

    /**
     * 운전석(앞 왼쪽) 문의 잠금을 해제합니다.
     *
     * @param onFailed 잠금 해제에 실패했을 때 호출되는 콜백. 기본값은 아무것도 하지 않습니다.
     */
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

    /**
     * 차량의 모든 문(앞/뒤 좌/우 총 4개)의 잠금을 한꺼번에 해제합니다.
     *
     * @param onFailed 특정 문의 잠금 해제에 실패했을 때 호출되는 콜백. 기본값은 아무것도 하지 않습니다.
     */
    fun unlockAllDoors(onFailed: (Exception) -> Unit = {}) {
        // 잠금 해제할 문 목록: 앞 왼쪽, 앞 오른쪽, 뒤 왼쪽, 뒤 오른쪽
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
