package com.panicdev.poopilot.presentation.util

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

object DrivingModeHelper {

    fun applyDrivingRestrictions(rootView: View, isDriving: Boolean) {
        if (!isDriving) return
        increaseTextSizes(rootView)
        increaseTouchTargets(rootView)
    }

    private fun increaseTextSizes(view: View) {
        if (view is TextView) {
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

    private fun increaseTouchTargets(view: View) {
        if (view.isClickable) {
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
