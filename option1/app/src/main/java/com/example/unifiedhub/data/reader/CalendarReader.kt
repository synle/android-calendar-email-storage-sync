// =============================================================================
// CalendarReader.kt — Reads calendar events from the device
// =============================================================================
// WHAT IS A ContentResolver?
//
// Android stores data (contacts, calendar, SMS, etc.) in "Content Providers".
// Think of Content Providers as DATABASES that other apps can access.
//
// A ContentResolver is your TOOL to talk to these databases. It's like a
// librarian — you ask "give me all calendar events", and the ContentResolver
// goes to the Calendar Content Provider, fetches the data, and gives it back.
//
// HOW IT WORKS:
//   1. You tell the ContentResolver WHICH provider (e.g., CalendarContract)
//   2. You specify WHAT columns you want (e.g., title, start time)
//   3. You can add filters (e.g., "only events from this week")
//   4. It returns a Cursor (like a pointer that moves through rows of data)
//
// WHAT IS A Cursor?
//
// A Cursor is like a FINGER pointing at rows in a table.
// Imagine a spreadsheet:
//
//   | Title          | Start Time | End Time   |
//   | Meeting        | 10:00 AM   | 11:00 AM   |  <-- Cursor points here first
//   | Lunch with Bob | 12:00 PM   | 1:00 PM    |  <-- Then here
//   | Dentist        | 3:00 PM    | 3:30 PM    |  <-- Then here
//
// You move the cursor row by row with cursor.moveToNext(),
// and read values from the current row.
// =============================================================================

package com.example.unifiedhub.data.reader

import android.content.ContentResolver
import android.provider.CalendarContract
import com.example.unifiedhub.data.model.ItemType
import com.example.unifiedhub.data.model.TimelineItem
import java.util.Calendar

class CalendarReader(
    // We receive the ContentResolver from the Activity/ViewModel
    // WHY? Because ContentResolver comes from a Context (like an Activity),
    // and we don't want this class to depend on Activity directly.
    // This makes the code cleaner and easier to test.
    private val contentResolver: ContentResolver
) {
    // --- Read calendar events and return them as TimelineItems ---
    fun readEvents(daysBack: Int = 7, daysForward: Int = 7): List<TimelineItem> {
        val items = mutableListOf<TimelineItem>()

        // --- Define the time range ---
        // We want events from [daysBack ago] to [daysForward from now]
        val now = Calendar.getInstance()

        val startTime = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysBack)  // Go back in time
        }.timeInMillis

        val endTime = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysForward)  // Go forward in time
        }.timeInMillis

        // --- Define WHAT data we want from the calendar ---
        // These are the "columns" we want to read, like choosing columns
        // in a spreadsheet.
        val projection = arrayOf(
            CalendarContract.Events._ID,          // Unique event ID
            CalendarContract.Events.TITLE,         // Event title ("Meeting")
            CalendarContract.Events.DESCRIPTION,   // Event description
            CalendarContract.Events.DTSTART,       // Start time (as timestamp)
            CalendarContract.Events.DTEND,         // End time (as timestamp)
            CalendarContract.Events.EVENT_LOCATION  // Location ("Conference Room A")
        )

        // --- Define our FILTER (WHERE clause, like SQL) ---
        // We only want events that overlap with our time range
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND " +
                "${CalendarContract.Events.DTSTART} <= ?"
        val selectionArgs = arrayOf(startTime.toString(), endTime.toString())

        // --- Sort by start time (newest first) ---
        val sortOrder = "${CalendarContract.Events.DTSTART} DESC"

        // --- Actually query the calendar ---
        // This is where the ContentResolver does its magic!
        val cursor = contentResolver.query(
            CalendarContract.Events.CONTENT_URI,  // WHERE to look (calendar provider)
            projection,     // WHAT columns to get
            selection,      // FILTER (WHERE clause)
            selectionArgs,  // FILTER values (replace the ? marks)
            sortOrder       // HOW to sort
        )

        // --- Read the results ---
        // cursor?.use { } automatically closes the cursor when we're done
        // WHY close it? Because cursors use memory. Not closing = memory leak!
        cursor?.use { c ->
            // Find the column positions (they might not be in the order we asked)
            val idIndex = c.getColumnIndex(CalendarContract.Events._ID)
            val titleIndex = c.getColumnIndex(CalendarContract.Events.TITLE)
            val descIndex = c.getColumnIndex(CalendarContract.Events.DESCRIPTION)
            val startIndex = c.getColumnIndex(CalendarContract.Events.DTSTART)
            val locationIndex = c.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)

            // Move through each row of results
            while (c.moveToNext()) {
                val title = c.getString(titleIndex) ?: "No Title"
                val description = c.getString(descIndex) ?: ""
                val location = c.getString(locationIndex) ?: ""
                val start = c.getLong(startIndex)

                // Build a nice description string
                val fullDescription = buildString {
                    if (description.isNotBlank()) append(description)
                    if (location.isNotBlank()) {
                        if (isNotBlank()) append("\n")
                        append("Location: $location")
                    }
                }

                // Convert to our unified TimelineItem
                items.add(
                    TimelineItem(
                        id = "cal_${c.getLong(idIndex)}",
                        title = title,
                        description = fullDescription,
                        timestamp = start,
                        type = ItemType.CALENDAR_EVENT,
                        extraInfo = location
                    )
                )
            }
        }

        return items
    }
}
