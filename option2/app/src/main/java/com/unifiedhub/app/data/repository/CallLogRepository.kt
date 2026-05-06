package com.unifiedhub.app.data.repository

import android.content.ContentResolver
import android.provider.CallLog
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
class CallLogRepository @Inject constructor(
    private val contentResolver: ContentResolver
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    /**
     * Query call logs from the system CallLog provider.
     */
    suspend fun getCallLogs(sinceMillis: Long, limit: Int = 200): List<TimelineItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<TimelineItem>()

            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
            )

            val selection = "${CallLog.Calls.DATE} >= ?"
            val selectionArgs = arrayOf(sinceMillis.toString())

            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC LIMIT $limit"
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numberIdx = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIdx = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val dateIdx = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durationIdx = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val typeIdx = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)

                while (it.moveToNext()) {
                    val callId = it.getLong(idIdx)
                    val number = it.getString(numberIdx) ?: "Unknown"
                    val name = it.getString(nameIdx)
                    val date = it.getLong(dateIdx)
                    val duration = it.getLong(durationIdx)
                    val callType = it.getInt(typeIdx)

                    val typeLabel = when (callType) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming call"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing call"
                        CallLog.Calls.MISSED_TYPE -> "Missed call"
                        CallLog.Calls.REJECTED_TYPE -> "Rejected call"
                        CallLog.Calls.BLOCKED_TYPE -> "Blocked call"
                        else -> "Call"
                    }

                    val durationText = when {
                        duration == 0L -> "No answer"
                        duration < 60 -> "${duration}s"
                        else -> "${duration / 60}m ${duration % 60}s"
                    }

                    val callInstant = Instant.ofEpochMilli(date)
                    val preview = "${timeFormatter.format(callInstant)} · $durationText"

                    items.add(
                        TimelineItem(
                            id = "call_$callId",
                            type = TimelineItemType.CALL_LOG,
                            title = typeLabel,
                            contact = name ?: number,
                            timestamp = callInstant,
                            preview = preview,
                            isRead = callType != CallLog.Calls.MISSED_TYPE,
                            sourceId = "call_$callId",
                            metadata = mapOf(
                                "number" to number,
                                "callType" to callType.toString()
                            )
                        )
                    )
                }
            }

            items
        }
}
