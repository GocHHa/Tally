package com.tally.counter.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CountDao {

    @Query("SELECT * FROM app_counts WHERE unreadCount > 0 ORDER BY unreadCount DESC, lastNotificationAt DESC")
    fun observeActive(): Flow<List<AppCountEntity>>

    @Query("SELECT * FROM app_counts WHERE unreadCount > 0")
    suspend fun getActive(): List<AppCountEntity>

    @Query("SELECT * FROM app_counts WHERE packageName = :packageName LIMIT 1")
    suspend fun get(packageName: String): AppCountEntity?

    @Query("SELECT * FROM app_counts WHERE packageName = :packageName LIMIT 1")
    fun observe(packageName: String): Flow<AppCountEntity?>

    @Query("SELECT packageName FROM app_counts")
    suspend fun allPackages(): List<String>

    @Upsert
    suspend fun upsert(entity: AppCountEntity)

    @Query("UPDATE app_counts SET unreadCount = 0, lastResetAt = :now WHERE packageName = :packageName AND unreadCount > 0")
    suspend fun reset(packageName: String, now: Long): Int

    @Query("UPDATE app_counts SET unreadCount = 0, lastResetAt = :now WHERE unreadCount > 0")
    suspend fun resetAll(now: Long): Int
}

@Dao
interface NotificationKeyDao {

    @Query("SELECT * FROM notification_keys WHERE notificationKey = :key LIMIT 1")
    suspend fun get(key: String): NotificationKeyEntity?

    @Upsert
    suspend fun upsert(entity: NotificationKeyEntity)

    @Query("UPDATE notification_keys SET removed = 1, updatedAt = :now WHERE notificationKey = :key")
    suspend fun markRemoved(key: String, now: Long)

    @Query("SELECT notificationKey FROM notification_keys WHERE removed = 0")
    suspend fun liveKeys(): List<String>

    @Query("SELECT DISTINCT packageName FROM notification_keys")
    suspend fun packages(): List<String>

    @Query("DELETE FROM notification_keys WHERE removed = 1 AND updatedAt < :cutoff")
    suspend fun deleteRemovedOlderThan(cutoff: Long)

    @Query("DELETE FROM notification_keys WHERE updatedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

@Dao
interface HistoryDao {

    @Insert
    suspend fun insertAll(rows: List<HistoryEntity>)

    @Query("SELECT * FROM history WHERE packageName = :packageName ORDER BY postedAt DESC LIMIT 50")
    fun observeRecent(packageName: String): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history WHERE postedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query(
        "DELETE FROM history WHERE packageName = :packageName AND id NOT IN " +
            "(SELECT id FROM history WHERE packageName = :packageName ORDER BY postedAt DESC LIMIT 50)"
    )
    suspend fun trim(packageName: String)

    @Query("SELECT DISTINCT packageName FROM history")
    suspend fun packages(): List<String>

    @Query("UPDATE history SET title = NULL, text = NULL")
    suspend fun clearAllText()

    @Query("DELETE FROM history WHERE packageName = :packageName")
    suspend fun deleteFor(packageName: String)
}
