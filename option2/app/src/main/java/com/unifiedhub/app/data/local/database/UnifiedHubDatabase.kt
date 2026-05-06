package com.unifiedhub.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.unifiedhub.app.data.local.dao.TimelineDao
import com.unifiedhub.app.data.local.entity.TimelineEntity

@Database(
    entities = [TimelineEntity::class],
    version = 1,
    exportSchema = true
)
abstract class UnifiedHubDatabase : RoomDatabase() {
    abstract fun timelineDao(): TimelineDao
}
