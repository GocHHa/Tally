package com.tally.counter.data.repo

import com.tally.counter.data.db.AppCountEntity
import com.tally.counter.data.db.CountDao
import com.tally.counter.data.db.HistoryDao
import com.tally.counter.data.db.HistoryEntity
import com.tally.counter.data.db.NotificationKeyDao
import com.tally.counter.data.db.NotificationKeyEntity
import com.tally.counter.widget.WidgetUpdater
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One message inside a MessagingStyle notification. */
data class ParsedMessage(
    val timestamp: Long,
    val sender: String?,
    val text: String?,
    val fromUser: Boolean
)

/** A posted notification, already filtered and parsed by NotificationProcessor. */
data class IncomingNotification(
    val key: String,
    val packageName: String,
    val postTime: Long,
    val whenTime: Long,
    val contentHash: Int,
    val onlyAlertOnce: Boolean,
    val title: String?,
    val text: String?,
    /** Non-null only when the app uses MessagingStyle and the app is in "count messages" mode. */
    val messages: List<ParsedMessage>?
)

enum class RemovalKind { USER_DISMISSED, CLICKED, APP_CANCELED, OTHER }

/**
 * All count bookkeeping. Every write goes through [mutex] so notification events, usage checks
 * and manual resets can never interleave.
 */
class CountRepository(
    private val countDao: CountDao,
    private val keyDao: NotificationKeyDao,
    private val historyDao: HistoryDao,
    private val widgetUpdater: WidgetUpdater
) {
    private val mutex = Mutex()

    fun observeActive(): Flow<List<AppCountEntity>> = countDao.observeActive()

    fun observeCount(packageName: String): Flow<AppCountEntity?> = countDao.observe(packageName)

    fun observeHistory(packageName: String): Flow<List<HistoryEntity>> = historyDao.observeRecent(packageName)

    suspend fun activeCounts(): List<AppCountEntity> = countDao.getActive()

    /** Packages Tally has ever seen a notification from. */
    suspend fun knownPackages(): Set<String> =
        (countDao.allPackages() + keyDao.packages() + historyDao.packages()).toSet()

    /**
     * Records a posted (or re-posted / updated) notification.
     * @return how many unread items were added (0 if it was a duplicate or an update without new content).
     */
    suspend fun recordPosted(n: IncomingNotification, storeText: Boolean): Int {
        val added = mutex.withLock {
            val now = System.currentTimeMillis()
            val existing = keyDao.get(n.key)
            val history = ArrayList<HistoryEntity>()
            var newLastMessageTime = existing?.lastMessageTime ?: 0L
            var addedCount = 0

            val messages = n.messages
            if (!messages.isNullOrEmpty()) {
                // Messaging apps: count each message newer than the newest one already counted for this key.
                val since = existing?.lastMessageTime ?: 0L
                val incoming = messages.filter { it.timestamp > since && !it.fromUser }
                addedCount = incoming.size
                incoming.forEach { msg ->
                    history += HistoryEntity(
                        packageName = n.packageName,
                        title = if (storeText) (msg.sender ?: n.title) else null,
                        text = if (storeText) msg.text else null,
                        postedAt = if (msg.timestamp in 1..now) msg.timestamp else n.postTime
                    )
                }
                newLastMessageTime = maxOf(since, messages.maxOf { it.timestamp })
            } else {
                // Everything else: 1 per new notification key, or per re-post with genuinely new content.
                val isNew = when {
                    existing == null -> true
                    existing.contentHash != n.contentHash -> existing.removed || !n.onlyAlertOnce
                    // Same content: only new if it was gone from the panel and is a fresh alert
                    // (apps that re-post the same notification after a reboot keep the same `when`).
                    else -> existing.removed && n.whenTime != 0L && n.whenTime != existing.whenTime
                }
                if (isNew) {
                    addedCount = 1
                    history += HistoryEntity(
                        packageName = n.packageName,
                        title = if (storeText) n.title else null,
                        text = if (storeText) n.text else null,
                        postedAt = n.postTime
                    )
                }
            }

            keyDao.upsert(
                NotificationKeyEntity(
                    notificationKey = n.key,
                    packageName = n.packageName,
                    contentHash = n.contentHash,
                    whenTime = n.whenTime,
                    lastMessageTime = newLastMessageTime,
                    removed = false,
                    updatedAt = now
                )
            )

            if (addedCount > 0) {
                val current = countDao.get(n.packageName)
                val postTime = n.postTime.coerceAtMost(now)
                countDao.upsert(
                    AppCountEntity(
                        packageName = n.packageName,
                        unreadCount = (current?.unreadCount ?: 0) + addedCount,
                        lastNotificationAt = maxOf(postTime, current?.lastNotificationAt ?: 0L),
                        lastResetAt = current?.lastResetAt ?: 0L
                    )
                )
                historyDao.insertAll(history)
                historyDao.trim(n.packageName)
            }
            addedCount
        }
        if (added > 0) widgetUpdater.request()
        return added
    }

    /**
     * Records a removed notification and applies the reset rules.
     * @return true if the app's count was reset.
     */
    suspend fun recordRemoved(
        key: String,
        packageName: String,
        kind: RemovalKind,
        resetOnClick: Boolean,
        resetOnAppCancel: Boolean
    ): Boolean {
        val didReset = mutex.withLock {
            val now = System.currentTimeMillis()
            // Only react to notifications we actually tracked (ignores e.g. ongoing downloads).
            val existing = keyDao.get(key)
            if (existing == null) {
                false
            } else {
                keyDao.markRemoved(key, now)
                val shouldReset = when (kind) {
                    RemovalKind.CLICKED -> resetOnClick
                    RemovalKind.APP_CANCELED -> resetOnAppCancel
                    // Swiped away / "Clear all": keep the count. That's the whole point of Tally.
                    RemovalKind.USER_DISMISSED, RemovalKind.OTHER -> false
                }
                shouldReset && countDao.reset(packageName, now) > 0
            }
        }
        if (didReset) widgetUpdater.request()
        return didReset
    }

    /** After (re)connecting: anything we think is in the panel but isn't anymore was removed while we were away. */
    suspend fun markMissingAsRemoved(activeKeys: Set<String>) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            keyDao.liveKeys().filter { it !in activeKeys }.forEach { keyDao.markRemoved(it, now) }
        }
    }

    suspend fun resetApp(packageName: String) {
        val changed = mutex.withLock { countDao.reset(packageName, System.currentTimeMillis()) > 0 }
        if (changed) widgetUpdater.request()
    }

    suspend fun clearAll() {
        mutex.withLock { countDao.resetAll(System.currentTimeMillis()) }
        widgetUpdater.request()
    }

    /**
     * Resets every app that was opened after its newest notification.
     * @param lastOpened package -> newest ACTIVITY_RESUMED timestamp.
     */
    suspend fun resetOpenedApps(lastOpened: Map<String, Long>): Int {
        if (lastOpened.isEmpty()) return 0
        val resetCount = mutex.withLock {
            val now = System.currentTimeMillis()
            var n = 0
            countDao.getActive().forEach { row ->
                val opened = lastOpened[row.packageName] ?: return@forEach
                if (opened > row.lastNotificationAt) {
                    n += countDao.reset(row.packageName, now)
                }
            }
            n
        }
        if (resetCount > 0) widgetUpdater.request()
        return resetCount
    }

    /** Wipes stored text when the user turns "Store notification text" off. */
    suspend fun clearStoredText() {
        mutex.withLock { historyDao.clearAllText() }
    }

    /** Deletes history older than 7 days, keeps the last 50 per app, and forgets stale keys. */
    suspend fun cleanup() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val weekAgo = now - TimeUnit.DAYS.toMillis(7)
            historyDao.deleteOlderThan(weekAgo)
            historyDao.packages().forEach { historyDao.trim(it) }
            keyDao.deleteRemovedOlderThan(weekAgo)
            keyDao.deleteOlderThan(now - TimeUnit.DAYS.toMillis(30))
        }
    }
}
