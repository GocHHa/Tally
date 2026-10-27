package com.tally.counter.util

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.os.Process
import androidx.core.app.NotificationManagerCompat

data class PermissionSnapshot(
    val notificationAccess: Boolean = false,
    val usageAccess: Boolean = false,
    val batteryUnrestricted: Boolean = false
)

object PermissionChecker {

    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    /** On Samsung, "Unrestricted" battery usage == ignoring battery optimisations. */
    fun isBatteryUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true

    fun snapshot(context: Context) = PermissionSnapshot(
        notificationAccess = hasNotificationAccess(context),
        usageAccess = hasUsageAccess(context),
        batteryUnrestricted = isBatteryUnrestricted(context)
    )
}
