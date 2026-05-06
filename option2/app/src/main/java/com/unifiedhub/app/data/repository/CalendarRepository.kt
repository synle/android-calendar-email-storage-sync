package com.unifiedhub.app.data.repository

import android.content.ContentResolver
import android.database.Cursor
import android.provider.CalendarContract
import com.unifiedhub.app.data.model.TimelineItem
import com.unifiedhub.app.data.model.TimelineItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    private val contentResolver: ContentResolver
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    /**
     * Query device calendar events within the given time range.
     * Uses CalendarContract.Instances for recurring event expansion.
     */
    suspend fun getEvents(startMillis: Long, endMillis: Long): List<TimelineItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<TimelineItem>()

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.ORGANIZER,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME
            )

            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(startMillis.toString())
                .appendPath(endMillis.toString())
                .build()

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val locationIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
                val descIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
                val allDayIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val organizerIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.ORGANIZER)
                val calNameIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)

                while (it.moveToNext()) {
                    val eventId = it.getLong(idIdx)
                    val title = it.getString(titleIdx) ?: "No Title"
                    val begin = it.getLong(beginIdx)
                    val end = it.getLong(endIdx)
                    val location = it.getString(locationIdx) ?: ""
                    val description = it.getString(descIdx) ?: ""
                    val isAllDay = it.getInt(allDayIdx) == 1
                    val organizer = it.getString(organizerIdx) ?: ""
                    val calName = it.getString(calNameIdx) ?: ""

                    val beginInstant = Instant.ofEpochMilli(begin)
                    val endInstant = Instant.ofEpochMilli(end)

                    val timeRange = if (isAllDay) {
                        "All day"
                    } else {
                        "${timeFormatter.format(beginInstant)} – ${timeFormatter.format(endInstant)}"
                    }

                    val preview = buildString {
                        append(timeRange)
                        if (location.isNotBlank()) append(" · $location")
                        if (description.isNotBlank()) append(" · ${description.take(100)}")
                    }

                    items.add(
                        TimelineItem(
                            id = "cal_${eventId}_$begin",
                            type = TimelineItemType.CALENDAR_EVENT,
                            title = title,
                            contact = organizer.ifBlank { calName },
                            timestamp = beginInstant,
                            preview = preview,
                            sourceId = "cal_${eventId}_$begin"
                        )
                    )
                }
            }

            items
        }
}
