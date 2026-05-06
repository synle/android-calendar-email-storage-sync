// =============================================================================
// TimelineRepository.kt — The "single source of truth" for timeline data
// =============================================================================
//
// WHAT IS A REPOSITORY?
//
// A Repository is a class that COMBINES multiple data sources into one
// clean interface. Think of it as a WAITER at a restaurant:
//
//   - You (the UI) don't go to the kitchen (data sources) directly
//   - The waiter (repository) takes your order and brings you food
//   - The waiter knows which chef (reader) to ask for each dish
//
// WHY USE A REPOSITORY?
//
//   1. CLEAN SEPARATION: The UI doesn't need to know about ContentResolvers
//   2. EASY TO CHANGE: Want to add a real email API later? Change only here.
//   3. SINGLE PLACE: All data fetching logic is in one place
//   4. TESTABLE: You can create a fake repository for testing
//
// Our Repository combines Calendar, SMS, Call Log, and Email readers
// into one unified timeline.
// =============================================================================

package com.example.unifiedhub.data.repository

import android.content.ContentResolver
import com.example.unifiedhub.data.model.ItemType
import com.example.unifiedhub.data.model.TimelineItem
import com.example.unifiedhub.data.reader.CalendarReader
import com.example.unifiedhub.data.reader.CallLogReader
import com.example.unifiedhub.data.reader.EmailReader
import com.example.unifiedhub.data.reader.SmsReader

class TimelineRepository(contentResolver: ContentResolver) {

    // Create all our readers
    // Each reader knows how to fetch ONE type of data
    private val calendarReader = CalendarReader(contentResolver)
    private val smsReader = SmsReader(contentResolver)
    private val callLogReader = CallLogReader(contentResolver)
    private val emailReader = EmailReader()

    // --- Fetch ALL timeline items, sorted by time ---
    // This is the main function the UI calls.
    //
    // The 'permissions' parameter tells us which permissions the user granted.
    // WHY? Because the user might deny SMS permission but allow Calendar.
    // We should still show calendar events even if SMS is denied!
    fun getTimelineItems(grantedPermissions: Set<String>): List<TimelineItem> {
        val allItems = mutableListOf<TimelineItem>()

        // --- Read each data source (only if permission was granted) ---

        // Calendar events
        if ("android.permission.READ_CALENDAR" in grantedPermissions) {
            try {
                allItems.addAll(calendarReader.readEvents())
            } catch (e: Exception) {
                // If something goes wrong, log it but don't crash
                // WHY try/catch? Because a Content Provider could throw
                // an exception (e.g., if the calendar app was uninstalled)
                e.printStackTrace()
            }
        }

        // SMS messages
        if ("android.permission.READ_SMS" in grantedPermissions) {
            try {
                allItems.addAll(smsReader.readMessages())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Call logs
        if ("android.permission.READ_CALL_LOG" in grantedPermissions) {
            try {
                allItems.addAll(callLogReader.readCallLogs())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Emails (mock data — no permission needed)
        try {
            allItems.addAll(emailReader.readEmails())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // --- Sort everything by timestamp (newest first) ---
        // WHY sortedByDescending? Because users want to see the LATEST
        // items at the top. "Descending" means biggest number first,
        // and bigger timestamps = more recent.
        return allItems.sortedByDescending { it.timestamp }
    }

    // --- Get only today's items (for the Daily Digest) ---
    fun getTodayItems(grantedPermissions: Set<String>): List<TimelineItem> {
        val allItems = getTimelineItems(grantedPermissions)

        // Calculate the start of today (midnight)
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Filter to only items from today
        return allItems.filter { it.timestamp >= todayStart }
    }
}
