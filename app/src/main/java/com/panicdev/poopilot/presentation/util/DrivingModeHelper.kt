package com.panicdev.poopilot.presentation.util

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * 운전 중 안전한 UI 사용을 위해 화면 요소를 조정하는 유틸리티 객체입니다.
 *
 * 운전 중에는 작은 글씨나 좁은 터치 영역이 위험할 수 있으므로,
 * 텍스트 크기를 최소 16sp 이상으로 키우고 터치 영역을 최소 76dp 이상으로 확대합니다.
 * 뷰 계층 구조(ViewGroup)를 재귀적으로 순회하여 모든 자식 뷰에 적용합니다.
 */
object DrivingModeHelper {

    /**
     * 운전 중일 때 주어진 루트 뷰와 그 하위 모든 뷰에 운전 모드 제한을 적용합니다.
     * [isDriving]이 false이면 아무 작업도 수행하지 않습니다.
     *
     * @param rootView 제한을 적용할 최상위 뷰
     * @param isDriving 현재 운전 중인지 여부
     */
    fun applyDrivingRestrictions(rootView: View, isDriving: Boolean) {
        if (!isDriving) return
        increaseTextSizes(rootView)
        increaseTouchTargets(rootView)
    }

    /**
     * 주어진 뷰와 그 하위 모든 TextView의 글씨 크기를 최소 16sp로 키웁니다.
     * 이미 16sp 이상인 경우에는 변경하지 않습니다.
     * ViewGroup이면 자식 뷰들을 재귀적으로 순회합니다.
     *
     * @param view 글씨 크기를 검사하고 조정할 뷰
     */
    private fun increaseTextSizes(view: View) {
        if (view is TextView) {
            // 현재 글씨 크기를 sp 단위로 변환하여 확인합니다
            val currentSize = view.textSize / view.resources.displayMetrics.scaledDensity
            if (currentSize < 16f) {
                view.textSize = 16f
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                increaseTextSizes(view.getChildAt(i))
            }
        }
    }

    /**
     * 클릭 가능한 뷰의 최소 터치 영역을 76dp 이상으로 확대합니다.
     * 운전 중에는 손가락으로 정확히 탭하기 어려우므로 넓은 터치 영역이 필요합니다.
     * ViewGroup이면 자식 뷰들을 재귀적으로 순회합니다.
     *
     * @param view 터치 영역을 검사하고 조정할 뷰
     */
    private fun increaseTouchTargets(view: View) {
        if (view.isClickable) {
            // 76dp를 픽셀로 변환합니다
            val minSize = (76 * view.resources.displayMetrics.density).toInt()
            if (view.minimumHeight < minSize) {
                view.minimumHeight = minSize
            }
            if (view.minimumWidth < minSize) {
                view.minimumWidth = minSize
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                increaseTouchTargets(view.getChildAt(i))
            }
        }
    }
}
