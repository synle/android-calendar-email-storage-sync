// =============================================================================
// TimelineItem.kt — The unified data model
// =============================================================================
// WHY DO WE NEED THIS?
//
// Our app reads data from 4 DIFFERENT sources:
//   - Calendar events (have: title, start time, end time, location)
//   - SMS messages (have: sender, body, date)
//   - Call logs (have: number, type, duration, date)
//   - Emails (have: subject, sender, body, date)
//
// Each source has DIFFERENT fields and formats. But our timeline screen
// needs to display them ALL in ONE list, sorted by time.
//
// SOLUTION: Convert everything into ONE common format — TimelineItem.
// This is like translating different languages into English so everyone
// can understand each other.
//
// WHAT IS A data class?
// A special Kotlin class that automatically gives you:
//   - equals() — compare two items
//   - hashCode() — use items in sets/maps
//   - toString() — print items nicely
//   - copy() — create a modified copy
// Perfect for "data containers" like this.
// =============================================================================

package com.example.unifiedhub.data.model

// --- The type of each timeline item ---
// WHY an enum? Because the type is a FIXED set of options.
// Enums prevent typos (you can't write "emial" by accident).
enum class ItemType {
    EMAIL,
    SMS,
    CALL,
    CALENDAR_EVENT
}

// --- The unified timeline item ---
data class TimelineItem(
    val id: String,           // Unique identifier (so we can tell items apart)
    val title: String,        // Main text: "Meeting with Bob" / "Mom" / "John"
    val description: String,  // Details: email body / SMS text / call duration
    val timestamp: Long,      // When it happened (milliseconds since Jan 1, 1970)
    val type: ItemType,       // What kind of item this is
    val extraInfo: String = "" // Optional extra data (e.g., "Missed call", "Inbox")
)

// =============================================================================
// ABOUT TIMESTAMPS:
//
// We store time as a Long (number of milliseconds since January 1, 1970).
// This is called a "Unix timestamp" or "epoch time".
//
// WHY? Because:
//   1. It's easy to SORT (bigger number = later time)
//   2. It's timezone-independent
//   3. All Android APIs use it
//
// Example: 1709164800000 = February 28, 2024, 12:00 PM UTC
//
// We convert to human-readable format only when DISPLAYING to the user.
// =============================================================================
