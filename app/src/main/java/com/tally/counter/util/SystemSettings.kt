package com.tally.counter.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Deep links into the system settings screens Tally needs. */
object SystemSettings {

    fun openNotificationAccess(context: Context) {
        context.startSafely(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            fallback = appDetailsIntent(context)
        )
    }

    fun openUsageAccess(context: Context) {
        context.startSafely(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            fallback = appDetailsIntent(context)
        )
    }

    /**
     * Shows the system "Let app always run in background?" dialog, which sets Tally to
     * Unrestricted. If already unrestricted (or the dialog isn't available) opens App info,
     * where Battery → Unrestricted lives on Samsung.
     */
    @SuppressLint("BatteryLife")
    fun openBatterySettings(context: Context) {
        if (PermissionChecker.isBatteryUnrestricted(context)) {
            context.startSafely(appDetailsIntent(context))
            return
        }
        val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
        context.startSafely(request, fallback = appDetailsIntent(context))
    }

    fun openAppDetails(context: Context) {
        context.startSafely(appDetailsIntent(context))
    }

    private fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))

    private fun Context.startSafely(intent: Intent, fallback: Intent? = null) {
        if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            if (fallback != null) {
                if (this !is Activity) fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    startActivity(fallback)
                } catch (ignored: ActivityNotFoundException) {
                }
            }
        } catch (e: SecurityException) {
            if (fallback != null) {
                try {
                    startActivity(fallback)
                } catch (ignored: Exception) {
                }
            }
        }
    }
}
