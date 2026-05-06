package com.unifiedhub.app.ui.screen.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifiedhub.app.data.model.TimelineItem
import com.unifiedhub.app.data.model.TimelineItemType
import com.unifiedhub.app.data.repository.UnifiedTimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class TimelineUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val groupedItems: Map<LocalDate, List<TimelineItem>> = emptyMap(),
    val activeFilters: Set<TimelineItemType> = TimelineItemType.entries.toSet(),
    val searchQuery: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: UnifiedTimelineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    private val activeFilters = MutableStateFlow(TimelineItemType.entries.toSet())
    private val searchQuery = MutableStateFlow("")

    val timelineItems: StateFlow<Map<LocalDate, List<TimelineItem>>> =
        combine(activeFilters, searchQuery) { filters, query -> filters to query }
            .flatMapLatest { (filters, query) ->
                if (query.isBlank()) {
                    repository.observeTimeline(filters)
                } else {
                    repository.search(query, filters)
                }
            }
            .combine(MutableStateFlow(Unit)) { items, _ ->
                items.groupBy { item ->
                    item.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                }.toSortedMap(compareByDescending { it })
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        // Observe combined state for the UI
        viewModelScope.launch {
            combine(activeFilters, searchQuery, timelineItems) { filters, query, items ->
                _uiState.value.copy(
                    isLoading = false,
                    groupedItems = items,
                    activeFilters = filters,
                    searchQuery = query
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                val sinceMillis = Instant.now()
                    .minus(30, ChronoUnit.DAYS)
                    .toEpochMilli()
                repository.refreshAll(sinceMillis, activeFilters.value)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun toggleFilter(type: TimelineItemType) {
        activeFilters.update { current ->
            if (type in current) current - type else current + type
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
