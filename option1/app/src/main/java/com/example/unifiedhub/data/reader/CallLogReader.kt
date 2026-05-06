// =============================================================================
// CallLogReader.kt — Reads phone call history from the device
// =============================================================================
// HOW CALL LOGS WORK:
//
// Every phone call (incoming, outgoing, missed) is stored in the
// CallLog Content Provider. Each entry has:
//   - number: The phone number
//   - type: Incoming (1), Outgoing (2), or Missed (3)
//   - date: When the call happened
//   - duration: How long the call lasted (in seconds)
//   - cachedName: The contact name (if the number is in your contacts)
//
// FUN FACT: The call log also stores rejected calls, blocked calls,
// and voicemails, but we'll focus on the main three types.
// =============================================================================

package com.example.unifiedhub.data.reader

import android.content.ContentResolver
import android.provider.CallLog
import com.example.unifiedhub.data.model.ItemType
import com.example.unifiedhub.data.model.TimelineItem

class CallLogReader(
    private val contentResolver: ContentResolver
) {
    // --- Read call logs and return as TimelineItems ---
    fun readCallLogs(limit: Int = 50): List<TimelineItem> {
        val items = mutableListOf<TimelineItem>()

        // --- What columns do we want? ---
        val projection = arrayOf(
            CallLog.Calls._ID,           // Unique call ID
            CallLog.Calls.NUMBER,        // Phone number
            CallLog.Calls.TYPE,          // Incoming / Outgoing / Missed
            CallLog.Calls.DATE,          // When the call happened
            CallLog.Calls.DURATION,      // Call length in seconds
            CallLog.Calls.CACHED_NAME    // Contact name (if saved)
        )

        // --- Sort by date, newest first ---
        val sortOrder = "${CallLog.Calls.DATE} DESC LIMIT $limit"

        // --- Query the call log provider ---
        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use { c ->
            val idIndex = c.getColumnIndex(CallLog.Calls._ID)
            val numberIndex = c.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIndex = c.getColumnIndex(CallLog.Calls.TYPE)
            val dateIndex = c.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex = c.getColumnIndex(CallLog.Calls.DURATION)
            val nameIndex = c.getColumnIndex(CallLog.Calls.CACHED_NAME)

            while (c.moveToNext()) {
                val number = c.getString(numberIndex) ?: "Unknown"
                val name = c.getString(nameIndex)  // May be null if not in contacts
                val type = c.getInt(typeIndex)
                val date = c.getLong(dateIndex)
                val duration = c.getLong(durationIndex)

                // --- Determine call type ---
                // WHY a when() expression? It's Kotlin's version of switch/case
                // but more powerful — it can return values!
                val callType = when (type) {
                    CallLog.Calls.INCOMING_TYPE -> "Incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                    CallLog.Calls.MISSED_TYPE -> "Missed"
                    CallLog.Calls.REJECTED_TYPE -> "Rejected"
                    else -> "Call"
                }

                // --- Format the duration nicely ---
                // Convert seconds to "Xm Ys" format
                val durationText = when {
                    duration == 0L -> ""                      // Missed calls have 0 duration
                    duration < 60 -> "${duration}s"           // "45s"
                    else -> "${duration / 60}m ${duration % 60}s"  // "2m 30s"
                }

                // Show contact name if available, otherwise phone number
                val displayName = name ?: number

                items.add(
                    TimelineItem(
                        id = "call_${c.getLong(idIndex)}",
                        title = displayName,
                        description = buildString {
                            append("$callType call")
                            if (durationText.isNotBlank()) append(" ($durationText)")
                            // If we have a name, also show the number in description
                            if (name != null) append("\n$number")
                        },
                        timestamp = date,
                        type = ItemType.CALL,
                        extraInfo = callType  // "Incoming", "Outgoing", "Missed"
                    )
                )
            }
        }

        return items
    }
}
