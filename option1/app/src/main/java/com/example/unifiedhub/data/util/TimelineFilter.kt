package com.example.unifiedhub.data.util

import com.example.unifiedhub.data.model.TimelineItem
import com.example.unifiedhub.viewmodel.TabType

/**
 * Pure-Kotlin filter + search + sort pipeline for the tabbed timeline UI.
 * Lives in `data/util/` (not the ViewModel) so it's unit-testable on the JVM.
 */
object TimelineFilter {

    /**
     * Apply tab filter → search filter → sort, in one pass.
     *
     * @param items every item known to the app (regardless of type)
     * @param tab the active tab — only items whose [TimelineItem.type] matches the
     *            tab's underlying [com.example.unifiedhub.data.model.ItemType] are kept
     * @param query free-text search; blank/whitespace = match everything in the tab
     * @param sortDescending true = newest-first, false = oldest-first
     */
    fun recomputeVisible(
        items: List<TimelineItem>,
        tab: TabType,
        query: String,
        sortDescending: Boolean
    ): List<TimelineItem> {
        val q = query.trim()
        val filteredByType = items.filter { it.type == tab.itemType }
        val searched = if (q.isEmpty()) {
            filteredByType
        } else {
            filteredByType.filter { item ->
                item.title.contains(q, ignoreCase = true) ||
                    item.description.contains(q, ignoreCase = true) ||
                    item.extraInfo.contains(q, ignoreCase = true)
            }
        }
        return if (sortDescending) {
            searched.sortedByDescending { it.timestamp }
        } else {
            searched.sortedBy { it.timestamp }
        }
    }
}
