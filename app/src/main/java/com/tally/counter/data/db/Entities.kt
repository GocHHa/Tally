package com.tally.counter.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Current unread count for one app. */
@Entity(tableName = "app_counts")
data class AppCountEntity(
    @PrimaryKey val packageName: String,
    val unreadCount: Int,
    /** Post time of the newest counted notification (wall clock, ms). */
    val lastNotificationAt: Long,
    /** When the count was last reset to zero (wall clock, ms). */
    val lastResetAt: Long
)

/**
 * One row per notification key we've seen. Used to de-duplicate re-posts and updates.
 */
@Entity(
    tableName = "notification_keys",
    indices = [Index(value = ["packageName"])]
)
data class NotificationKeyEntity(
    @PrimaryKey val notificationKey: String,
    val packageName: String,
    /** Hash of title/text/subtext/lines, used for non-messaging notifications. */
    val contentHash: Int,
    /** Notification.when of the last version we saw. */
    val whenTime: Long,
    /** Newest MessagingStyle message timestamp already counted for this key. */
    val lastMessageTime: Long,
    /** True once the notification has left the panel (for any reason). */
    val removed: Boolean,
    val updatedAt: Long
)

/** Recent notification history. Title/text are null unless "Store notification text" is on. */
@Entity(
    tableName = "history",
    indices = [Index(value = ["packageName"]), Index(value = ["postedAt"])]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postedAt: Long
)
