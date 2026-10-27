package com.tally.counter.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tally.counter.appContainer
import com.tally.counter.service.ListenerControl
import com.tally.counter.service.ListenerStatus
import com.tally.counter.util.PermissionChecker
import java.util.concurrent.TimeUnit

/**
 * Every 15 minutes: check usage events, clean up old history, and nudge the listener if it
 * isn't connected.
 */
class MaintenanceWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        return try {
            container.usageResetChecker.check(force = true)
            container.countRepository.cleanup()
            if (PermissionChecker.hasNotificationAccess(applicationContext) && !ListenerStatus.connected.value) {
                ListenerControl.requestRebind(applicationContext)
            }
            container.widgetUpdater.request()
            Result.success()
        } catch (e: Exception) {
            Log.w("TallyWork", "Maintenance failed", e)
            Result.retry()
        }
    }
}

object WorkScheduler {
    private const val UNIQUE_NAME = "tally-maintenance"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
