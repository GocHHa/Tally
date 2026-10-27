package com.tally.counter.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.tally.counter.data.apps.AppInfoProvider
import com.tally.counter.data.repo.CountRepository
import com.tally.counter.data.repo.IncomingNotification
import com.tally.counter.data.repo.ParsedMessage
import com.tally.counter.data.repo.RemovalKind
import com.tally.counter.data.settings.SettingsRepository
import com.tally.counter.data.settings.TallySettings

/**
 * Turns raw StatusBarNotifications into count updates. All calls come from a single consumer
 * coroutine in [TallyListenerService], so events are processed strictly in order.
 */
class NotificationProcessor(
    private val ownPackage: String,
    private val countRepository: CountRepository,
    private val settingsRepository: SettingsRepository,
    private val appInfoProvider: AppInfoProvider
) {

    suspend fun onPosted(sbn: StatusBarNotification) {
        val settings = settingsRepository.current()
        if (!shouldTrack(sbn, settings)) return
        countRepository.recordPosted(parse(sbn, settings), settings.storeText)
    }

    suspend fun onRemoved(sbn: StatusBarNotification, reason: Int) {
        if (sbn.packageName == ownPackage) return
        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val settings = settingsRepository.current()
        val kind = when (reason) {
            NotificationListenerService.REASON_CLICK -> RemovalKind.CLICKED
            NotificationListenerService.REASON_APP_CANCEL,
            NotificationListenerService.REASON_APP_CANCEL_ALL -> RemovalKind.APP_CANCELED
            NotificationListenerService.REASON_CANCEL,
            NotificationListenerService.REASON_CANCEL_ALL -> RemovalKind.USER_DISMISSED
            else -> RemovalKind.OTHER
        }
        countRepository.recordRemoved(
            key = sbn.key,
            packageName = sbn.packageName,
            kind = kind,
            resetOnClick = settings.resetOnClick,
            resetOnAppCancel = settings.resetOnAppCancel &&
                sbn.packageName !in settings.appCancelDisabledPackages
        )
    }

    /** Called from onListenerConnected with getActiveNotifications(). Never double-counts. */
    suspend fun syncActive(active: List<StatusBarNotification>) {
        val settings = settingsRepository.current()
        val keys = HashSet<String>(active.size * 2)
        for (sbn in active) {
            keys += sbn.key
            if (shouldTrack(sbn, settings)) {
                countRepository.recordPosted(parse(sbn, settings), settings.storeText)
            }
        }
        countRepository.markMissingAsRemoved(keys)
    }

    private fun shouldTrack(sbn: StatusBarNotification, settings: TallySettings): Boolean {
        val pkg = sbn.packageName ?: return false
        if (pkg == ownPackage) return false
        val n = sbn.notification ?: return false
        val flags = n.flags
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        if (sbn.isOngoing || flags and Notification.FLAG_ONGOING_EVENT != 0) return false
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return false
        if (!sbn.isClearable) return false
        if (n.category?.let { it in IGNORED_CATEGORIES } == true) return false
        return !appInfoProvider.isExcluded(pkg, settings)
    }

    private fun parse(sbn: StatusBarNotification, settings: TallySettings): IncomingNotification {
        val n = sbn.notification
        val extras = n.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val lines = extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n")
        val contentHash = listOf(title, text, subText, lines).joinToString("\u0001").hashCode()

        val countMessages = sbn.packageName !in settings.notificationModePackages
        val messages = if (countMessages) parseMessages(n, title) else null

        return IncomingNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            postTime = sbn.postTime,
            whenTime = n.`when`,
            contentHash = contentHash,
            onlyAlertOnce = n.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0,
            title = title,
            text = text,
            messages = messages?.takeIf { it.isNotEmpty() }
        )
    }

    /** Reads EXTRA_MESSAGES via MessagingStyle. Messages with no sender were sent by you. */
    private fun parseMessages(notification: Notification, fallbackTitle: String?): List<ParsedMessage>? {
        val style = try {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        } catch (e: Exception) {
            null
        } ?: return null

        val user = style.user
        val conversation = style.conversationTitle?.toString()
        return style.messages.map { message ->
            val person = message.person
            val fromUser = person == null ||
                (user.key != null && person.key == user.key) ||
                (user.key == null && person.key == null && user.name != null &&
                    person.name?.toString() == user.name?.toString())
            val senderName = person?.name?.toString()
            ParsedMessage(
                timestamp = message.timestamp,
                sender = when {
                    senderName != null && conversation != null -> "$senderName · $conversation"
                    senderName != null -> senderName
                    else -> fallbackTitle
                },
                text = message.text?.toString(),
                fromUser = fromUser
            )
        }
    }

    private companion object {
        /** Media players, progress bars, navigation, stopwatches, services and system notices. */
        val IGNORED_CATEGORIES = setOf(
            "transport", "progress", "service", "sys", "navigation", "stopwatch", "location_sharing"
        )
    }
}
