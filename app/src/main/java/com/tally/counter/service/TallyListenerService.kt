package com.tally.counter.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.tally.counter.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Receives every notification on the phone. Events are queued and processed one at a time so a
 * post followed quickly by a removal is always handled in the right order.
 */
class TallyListenerService : NotificationListenerService() {

    private sealed interface Event {
        data class Posted(val sbn: StatusBarNotification) : Event
        data class Removed(val sbn: StatusBarNotification, val reason: Int) : Event
        data class Sync(val active: List<StatusBarNotification>) : Event
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val events = Channel<Event>(Channel.UNLIMITED)

    override fun onCreate() {
        super.onCreate()
        val container = appContainer
        scope.launch {
            for (event in events) {
                try {
                    when (event) {
                        is Event.Posted -> container.notificationProcessor.onPosted(event.sbn)
                        is Event.Removed -> container.notificationProcessor.onRemoved(event.sbn, event.reason)
                        is Event.Sync -> container.notificationProcessor.syncActive(event.active)
                    }
                    // Any notification event is a good moment to check whether apps were opened.
                    container.usageResetChecker.check(force = event is Event.Sync)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process $event", e)
                }
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        ListenerStatus.setConnected(true)
        val active: List<StatusBarNotification> = try {
            activeNotifications?.toList().orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "getActiveNotifications failed", e)
            emptyList()
        }
        events.trySend(Event.Sync(active))
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        ListenerStatus.setConnected(false)
        ListenerControl.requestRebind(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        events.trySend(Event.Posted(sbn))
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: NotificationListenerService.RankingMap?,
        reason: Int
    ) {
        if (sbn == null) return
        events.trySend(Event.Removed(sbn, reason))
    }

    override fun onDestroy() {
        ListenerStatus.setConnected(false)
        events.close()
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "TallyListener"
    }
}
