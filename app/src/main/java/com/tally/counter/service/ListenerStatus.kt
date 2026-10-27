package com.tally.counter.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Live connection state of [TallyListenerService] (same process as the UI). */
object ListenerStatus {
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    fun setConnected(value: Boolean) {
        _connected.value = value
    }
}

object ListenerControl {

    private const val TAG = "TallyListener"

    fun component(context: Context) = ComponentName(context, TallyListenerService::class.java)

    /** Asks the system to bind the listener again. Safe to call any time. */
    fun requestRebind(context: Context) {
        try {
            NotificationListenerService.requestRebind(component(context))
        } catch (e: Exception) {
            Log.w(TAG, "requestRebind failed", e)
        }
    }

    /**
     * Stronger fix for a stuck listener: briefly disables and re-enables the service component,
     * which forces the system to drop and re-create the binding, then requests a rebind.
     */
    fun forceRestart(context: Context) {
        val pm = context.packageManager
        val component = component(context)
        try {
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            Log.w(TAG, "toggle component failed", e)
        }
        requestRebind(context)
    }
}
