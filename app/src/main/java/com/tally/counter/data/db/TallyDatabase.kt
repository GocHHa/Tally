package com.tally.counter.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AppCountEntity::class, NotificationKeyEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TallyDatabase : RoomDatabase() {

    abstract fun countDao(): CountDao
    abstract fun keyDao(): NotificationKeyDao
    abstract fun historyDao(): HistoryDao

    companion object {
        fun build(context: Context): TallyDatabase =
            Room.databaseBuilder(context, TallyDatabase::class.java, "tally.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
