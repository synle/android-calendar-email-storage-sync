package com.unifiedhub.app.data.model

import java.time.Instant

/**
 * Unified domain model representing any item in the timeline.
 * Each data source maps its native format into this common model.
 */
data class TimelineItem(
    val id: String,
    val type: TimelineItemType,
    val title: String,
    val contact: String,
    val timestamp: Instant,
    val preview: String,
    val isRead: Boolean = true,
    val sourceId: String = "",
    val metadata: Map<String, String> = emptyMap()
)
