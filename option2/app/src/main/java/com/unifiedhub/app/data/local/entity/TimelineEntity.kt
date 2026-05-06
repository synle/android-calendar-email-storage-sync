package com.unifiedhub.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.unifiedhub.app.data.model.TimelineItem
import com.unifiedhub.app.data.model.TimelineItemType
import java.time.Instant

/**
 * Room entity for caching unified timeline items.
 * Indexed on timestamp for efficient chronological queries and on type for filtering.
 */
@Entity(
    tableName = "timeline_items",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["type"]),
        Index(value = ["sourceId"], unique = true)
    ]
)
data class TimelineEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val title: String,
    val contact: String,
    val timestamp: Long,
    val preview: String,
    val isRead: Boolean,
    val sourceId: String
) {
    fun toDomain(): TimelineItem = TimelineItem(
        id = id,
        type = TimelineItemType.valueOf(type),
        title = title,
        contact = contact,
        timestamp = Instant.ofEpochMilli(timestamp),
        preview = preview,
        isRead = isRead,
        sourceId = sourceId
    )

    companion object {
        fun fromDomain(item: TimelineItem): TimelineEntity = TimelineEntity(
            id = item.id,
            type = item.type.name,
            title = item.title,
            contact = item.contact,
            timestamp = item.timestamp.toEpochMilli(),
            preview = item.preview,
            isRead = item.isRead,
            sourceId = item.sourceId
        )
    }
}
