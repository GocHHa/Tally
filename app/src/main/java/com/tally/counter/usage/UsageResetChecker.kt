package com.tally.counter.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.tally.counter.data.repo.CountRepository
import com.tally.counter.data.settings.SettingsRepository
import com.tally.counter.util.PermissionChecker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Resets an app's count when you opened it after its newest notification, using
 * UsageStatsManager ACTIVITY_RESUMED events. Silently does nothing without Usage access.
 */
class UsageResetChecker(
    context: Context,
    private val countRepository: CountRepository,
    private val settingsRepository: SettingsRepository
) {
    private val appContext = context.applicationContext
    private val usageStatsManager = appContext.getSystemService(UsageStatsManager::class.java)
    private val mutex = Mutex()

    @Volatile
    private var lastRunAt = 0L

    /**
     * @param force skip the short throttle (used when Tally opens and from the periodic job).
     */
    suspend fun check(force: Boolean = false) {
        val manager = usageStatsManager ?: return
        if (!PermissionChecker.hasUsageAccess(appContext)) return
        if (!settingsRepository.current().resetOnOpen) return

        mutex.withLock {
            val now = System.currentTimeMillis()
            if (!force && now - lastRunAt < MIN_INTERVAL_MS) return
            lastRunAt = now

            val active = countRepository.activeCounts()
            if (active.isEmpty()) return

            val watched = active.mapTo(HashSet()) { it.packageName }
            val begin = active.minOf { it.lastNotificationAt }
                .coerceAtLeast(now - TimeUnit.DAYS.toMillis(7))
                .coerceAtMost(now - 1)

            val lastOpened = withContext(Dispatchers.IO) { readResumes(manager, begin, now, watched) }
            countRepository.resetOpenedApps(lastOpened)
        }
    }

    private fun readResumes(
        manager: UsageStatsManager,
        begin: Long,
        end: Long,
        watched: Set<String>
    ): Map<String, Long> {
        val result = HashMap<String, Long>()
        try {
            val events = manager.queryEvents(begin, end) ?: return result
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType != UsageEvents.Event.ACTIVITY_RESUMED) continue
                val pkg = event.packageName ?: continue
                if (pkg !in watched) continue
                val previous = result[pkg] ?: 0L
                if (event.timeStamp > previous) result[pkg] = event.timeStamp
            }
        } catch (e: Exception) {
            Log.w("TallyUsage", "queryEvents failed", e)
        }
        return result
    }

    private companion object {
        const val MIN_INTERVAL_MS = 1_500L
    }
}
