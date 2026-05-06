package com.unifiedhub.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unifiedhub.app.data.local.entity.TimelineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineDao {

    @Query(
        """
        SELECT * FROM timeline_items
        WHERE type IN (:types)
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getTimelineItems(
        types: List<String>,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<List<TimelineEntity>>

    @Query(
        """
        SELECT * FROM timeline_items
        WHERE timestamp BETWEEN :startMillis AND :endMillis
        AND type IN (:types)
        ORDER BY timestamp DESC
        """
    )
    fun getItemsInRange(
        startMillis: Long,
        endMillis: Long,
        types: List<String>
    ): Flow<List<TimelineEntity>>

    @Query(
        """
        SELECT * FROM timeline_items
        WHERE timestamp BETWEEN :startMillis AND :endMillis
        AND type IN (:types)
        ORDER BY timestamp DESC
        """
    )
    suspend fun getItemsInRangeOnce(
        startMillis: Long,
        endMillis: Long,
        types: List<String>
    ): List<TimelineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TimelineEntity>)

    @Query("DELETE FROM timeline_items WHERE type = :type")
    suspend fun deleteByType(type: String)

    @Query("DELETE FROM timeline_items WHERE timestamp < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long)

    @Query("SELECT COUNT(*) FROM timeline_items WHERE type = :type AND timestamp BETWEEN :startMillis AND :endMillis")
    suspend fun countByTypeInRange(type: String, startMillis: Long, endMillis: Long): Int

    @Query(
        """
        SELECT * FROM timeline_items
        WHERE (title LIKE '%' || :query || '%' OR contact LIKE '%' || :query || '%' OR preview LIKE '%' || :query || '%')
        AND type IN (:types)
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    fun search(query: String, types: List<String>, limit: Int = 50): Flow<List<TimelineEntity>>
}
