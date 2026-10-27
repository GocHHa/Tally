package com.tally.counter.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** Light, system-consistent haptics. Respects the user's "Touch feedback" setting. */
object Haptics {

    /** Subtle tick for taps. */
    fun tap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Crossing a swipe threshold. */
    fun threshold(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /** Completed action (reset, clear all). */
    fun confirm(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    /** Toggle switches. */
    fun toggle(view: View, on: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(
                if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
            )
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
}
