// =============================================================================
// TimelineViewModel.kt — The "brain" of our app
// =============================================================================
//
// WHAT IS A VIEWMODEL?
//
// A ViewModel is a class that holds and manages the DATA that your UI needs.
//
// WHY do we need it? Imagine this scenario:
//   1. User opens app → data loads → timeline shows 50 items
//   2. User rotates phone → Activity is DESTROYED and RECREATED
//   3. Without ViewModel: data is LOST, must reload everything
//   4. With ViewModel: data SURVIVES rotation, no reload needed!
//
// Think of it like this:
//   - Activity = the TV SCREEN (can be turned off/on)
//   - ViewModel = the DVR/RECORDING (keeps playing even if TV restarts)
//
// WHAT IS STATE?
//
// "State" is the current situation of your UI. For example:
//   - What items are shown in the list? (state)
//   - Is data loading right now? (state)
//   - Which filter is active? (state)
//
// In Compose, when STATE changes, the UI automatically REDRAWS itself.
// This is called "recomposition". You don't manually update views —
// you update the state, and Compose handles the rest.
//
// WHAT IS StateFlow?
//
// StateFlow is like a PIPE that carries your state:
//   - You put new state in one end (the ViewModel)
//   - The UI reads from the other end (the Composable)
//   - Whenever you put new state in, the UI automatically updates
//
// It's similar to LiveData, but more Kotlin-friendly.
// =============================================================================

package com.example.unifiedhub.viewmodel

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.unifiedhub.data.model.ItemType
import com.example.unifiedhub.data.model.TimelineItem
import com.example.unifiedhub.data.repository.TimelineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- The state of our entire UI, bundled into one class ---
// WHY a data class for state? Because Compose can efficiently detect
// when ANY field changes and only redraw what's needed.
data class TimelineUiState(
    val items: List<TimelineItem> = emptyList(),       // All timeline items
    val filteredItems: List<TimelineItem> = emptyList(), // Items after applying filter
    val isLoading: Boolean = false,                     // Are we loading data?
    val activeFilters: Set<ItemType> = ItemType.entries.toSet(), // Which types are shown
    val digestText: String? = null                      // The daily digest text (null = not shown)
)

class TimelineViewModel(
    contentResolver: ContentResolver
) : ViewModel() {

    // --- Our data repository ---
    private val repository = TimelineRepository(contentResolver)

    // --- The UI state ---
    // _uiState is PRIVATE (only the ViewModel can change it)
    // uiState is PUBLIC (the UI can read it, but can't change it directly)
    // WHY this pattern? It enforces ONE-WAY data flow:
    //   ViewModel → changes state → UI updates automatically
    //   UI → calls ViewModel functions → ViewModel changes state
    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    // Store granted permissions so we can use them in refresh
    private var grantedPermissions: Set<String> = emptySet()

    // --- Load data from all sources ---
    fun loadData(permissions: Set<String>) {
        grantedPermissions = permissions

        // Show loading spinner
        _uiState.value = _uiState.value.copy(isLoading = true)

        // Fetch data from all sources
        val items = repository.getTimelineItems(permissions)

        // Update state — this automatically triggers UI update!
        _uiState.value = _uiState.value.copy(
            items = items,
            filteredItems = applyFilters(items, _uiState.value.activeFilters),
            isLoading = false
        )
    }

    // --- Toggle a filter on/off ---
    // Called when the user taps a filter chip (e.g., "SMS")
    fun toggleFilter(type: ItemType) {
        val currentFilters = _uiState.value.activeFilters.toMutableSet()

        // If the filter is active, remove it. Otherwise, add it.
        if (type in currentFilters) {
            currentFilters.remove(type)
        } else {
            currentFilters.add(type)
        }

        // Recompute filtered items
        _uiState.value = _uiState.value.copy(
            activeFilters = currentFilters,
            filteredItems = applyFilters(_uiState.value.items, currentFilters)
        )
    }

    // --- Generate the Daily Digest ---
    fun generateDailyDigest() {
        val todayItems = repository.getTodayItems(grantedPermissions)

        // Count items by type
        val emailCount = todayItems.count { it.type == ItemType.EMAIL }
        val smsCount = todayItems.count { it.type == ItemType.SMS }
        val callCount = todayItems.count { it.type == ItemType.CALL }
        val missedCallCount = todayItems.count {
            it.type == ItemType.CALL && it.extraInfo == "Missed"
        }
        val eventCount = todayItems.count { it.type == ItemType.CALENDAR_EVENT }

        // Format today's date
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())

        // Build the digest text
        val digest = buildString {
            appendLine("========================================")
            appendLine("   DAILY DIGEST — $today")
            appendLine("========================================")
            appendLine()
            appendLine("SUMMARY:")
            appendLine("  Emails:       $emailCount")
            appendLine("  Messages:     $smsCount")
            appendLine("  Calls:        $callCount")
            if (missedCallCount > 0) {
                appendLine("  Missed Calls: $missedCallCount")
            }
            appendLine("  Events:       $eventCount")
            appendLine()

            // --- List events ---
            if (eventCount > 0) {
                appendLine("CALENDAR EVENTS:")
                todayItems.filter { it.type == ItemType.CALENDAR_EVENT }
                    .forEach { event ->
                        val time = formatTime(event.timestamp)
                        appendLine("  [$time] ${event.title}")
                        if (event.extraInfo.isNotBlank()) {
                            appendLine("          Location: ${event.extraInfo}")
                        }
                    }
                appendLine()
            }

            // --- List calls ---
            val calls = todayItems.filter { it.type == ItemType.CALL }
            if (calls.isNotEmpty()) {
                appendLine("CALLS:")
                calls.forEach { call ->
                    val time = formatTime(call.timestamp)
                    appendLine("  [$time] ${call.extraInfo}: ${call.title}")
                }
                appendLine()
            }

            // --- List messages ---
            val messages = todayItems.filter { it.type == ItemType.SMS }
            if (messages.isNotEmpty()) {
                appendLine("MESSAGES:")
                messages.forEach { msg ->
                    val time = formatTime(msg.timestamp)
                    val preview = msg.description.take(50) // First 50 chars
                    appendLine("  [$time] ${msg.title}: $preview")
                }
                appendLine()
            }

            // --- List emails ---
            val emails = todayItems.filter { it.type == ItemType.EMAIL }
            if (emails.isNotEmpty()) {
                appendLine("EMAILS:")
                emails.forEach { email ->
                    val time = formatTime(email.timestamp)
                    appendLine("  [$time] ${email.title}")
                    appendLine("          From: ${email.extraInfo}")
                }
                appendLine()
            }

            appendLine("========================================")
            appendLine("  Generated by Unified Hub")
            appendLine("========================================")
        }

        _uiState.value = _uiState.value.copy(digestText = digest)
    }

    // --- Clear the digest (close the dialog) ---
    fun clearDigest() {
        _uiState.value = _uiState.value.copy(digestText = null)
    }

    // --- Helper: Apply type filters to the item list ---
    private fun applyFilters(items: List<TimelineItem>, filters: Set<ItemType>): List<TimelineItem> {
        return items.filter { it.type in filters }
    }

    // --- Helper: Format timestamp to "HH:mm" ---
    private fun formatTime(timestamp: Long): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(Date(timestamp))
    }
}

// =============================================================================
// ViewModelFactory — needed to pass ContentResolver to the ViewModel
// =============================================================================
// WHY: ViewModels are created by Android, not by us directly. Normally
// they can only have an empty constructor. But we NEED to pass the
// ContentResolver. A Factory lets us customize how the ViewModel is created.
// =============================================================================
class TimelineViewModelFactory(
    private val contentResolver: ContentResolver
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TimelineViewModel(contentResolver) as T
    }
}
