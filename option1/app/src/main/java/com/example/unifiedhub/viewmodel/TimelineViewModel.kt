package com.example.unifiedhub.viewmodel

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.unifiedhub.data.model.ItemType
import com.example.unifiedhub.data.model.TimelineItem
import com.example.unifiedhub.data.repository.TimelineRepository
import com.example.unifiedhub.data.util.TimelineFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tabs surfaced in the UI. Each maps to one underlying ItemType.
 * Call logs are read but not shown in the tabbed UI (the digest still uses them).
 */
enum class TabType(val itemType: ItemType, val label: String) {
    EMAIL(ItemType.EMAIL, "Email"),
    CALENDAR(ItemType.CALENDAR_EVENT, "Calendar"),
    SMS(ItemType.SMS, "SMS")
}

data class TimelineUiState(
    val items: List<TimelineItem> = emptyList(),
    val visibleItems: List<TimelineItem> = emptyList(),
    val isLoading: Boolean = false,
    val selectedTab: TabType = TabType.EMAIL,
    val searchQuery: String = "",
    val sortDescending: Boolean = true,
    val digestText: String? = null
)

class TimelineViewModel(
    contentResolver: ContentResolver
) : ViewModel() {

    private val repository = TimelineRepository(contentResolver)

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    private var grantedPermissions: Set<String> = emptySet()

    fun loadData(permissions: Set<String>) {
        grantedPermissions = permissions
        _uiState.value = _uiState.value.copy(isLoading = true)
        val items = repository.getTimelineItems(permissions)
        _uiState.value = _uiState.value.copy(
            items = items,
            visibleItems = recomputeVisible(
                items,
                _uiState.value.selectedTab,
                _uiState.value.searchQuery,
                _uiState.value.sortDescending
            ),
            isLoading = false
        )
    }

    fun selectTab(tab: TabType) {
        val s = _uiState.value
        _uiState.value = s.copy(
            selectedTab = tab,
            visibleItems = recomputeVisible(s.items, tab, s.searchQuery, s.sortDescending)
        )
    }

    fun setSearchQuery(query: String) {
        val s = _uiState.value
        _uiState.value = s.copy(
            searchQuery = query,
            visibleItems = recomputeVisible(s.items, s.selectedTab, query, s.sortDescending)
        )
    }

    fun toggleSortDirection() {
        val s = _uiState.value
        val newDir = !s.sortDescending
        _uiState.value = s.copy(
            sortDescending = newDir,
            visibleItems = recomputeVisible(s.items, s.selectedTab, s.searchQuery, newDir)
        )
    }

    fun generateDailyDigest() {
        val todayItems = repository.getTodayItems(grantedPermissions)

        val emailCount = todayItems.count { it.type == ItemType.EMAIL }
        val smsCount = todayItems.count { it.type == ItemType.SMS }
        val callCount = todayItems.count { it.type == ItemType.CALL }
        val missedCallCount = todayItems.count {
            it.type == ItemType.CALL && it.extraInfo == "Missed"
        }
        val eventCount = todayItems.count { it.type == ItemType.CALENDAR_EVENT }

        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())

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

            val messages = todayItems.filter { it.type == ItemType.SMS }
            if (messages.isNotEmpty()) {
                appendLine("MESSAGES:")
                messages.forEach { msg ->
                    val time = formatTime(msg.timestamp)
                    val preview = msg.description.take(50)
                    appendLine("  [$time] ${msg.title}: $preview")
                }
                appendLine()
            }

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

    fun clearDigest() {
        _uiState.value = _uiState.value.copy(digestText = null)
    }

    private fun recomputeVisible(
        items: List<TimelineItem>,
        tab: TabType,
        query: String,
        descending: Boolean
    ): List<TimelineItem> = TimelineFilter.recomputeVisible(items, tab, query, descending)

    private fun formatTime(timestamp: Long): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(Date(timestamp))
    }
}

class TimelineViewModelFactory(
    private val contentResolver: ContentResolver
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TimelineViewModel(contentResolver) as T
    }
}
