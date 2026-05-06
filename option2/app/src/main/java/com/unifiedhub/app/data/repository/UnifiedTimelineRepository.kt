package com.unifiedhub.app.data.repository

import android.provider.CallLog
import com.unifiedhub.app.data.local.dao.TimelineDao
import com.unifiedhub.app.data.local.entity.TimelineEntity
import com.unifiedhub.app.data.model.DailyDigest
import com.unifiedhub.app.data.model.TimelineItem
import com.unifiedhub.app.data.model.TimelineItemType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central repository that merges all data sources into a unified timeline.
 *
 * Strategy:
 * 1. On refresh, query each source in parallel
 * 2. Merge into Room cache
 * 3. UI observes Room via Flow (single source of truth)
 */
@Singleton
class UnifiedTimelineRepository @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val smsRepository: SmsRepository,
    private val callLogRepository: CallLogRepository,
    private val emailRepository: EmailRepository,
    private val timelineDao: TimelineDao
) {
    /**
     * Observe timeline items from Room cache, filtered by active types.
     */
    fun observeTimeline(
        activeTypes: Set<TimelineItemType>,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<List<TimelineItem>> {
        val typeStrings = activeTypes.map { it.name }
        return timelineDao.getTimelineItems(typeStrings, limit, offset)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Observe timeline items within a date range.
     */
    fun observeTimelineForRange(
        startMillis: Long,
        endMillis: Long,
        activeTypes: Set<TimelineItemType>
    ): Flow<List<TimelineItem>> {
        val typeStrings = activeTypes.map { it.name }
        return timelineDao.getItemsInRange(startMillis, endMillis, typeStrings)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Search across all cached timeline items.
     */
    fun search(
        query: String,
        activeTypes: Set<TimelineItemType>
    ): Flow<List<TimelineItem>> {
        val typeStrings = activeTypes.map { it.name }
        return timelineDao.search(query, typeStrings)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Refresh all sources in parallel and merge into Room.
     * Each source is independently failable — partial results are still persisted.
     */
    suspend fun refreshAll(
        sinceMillis: Long,
        enabledSources: Set<TimelineItemType> = TimelineItemType.entries.toSet()
    ) = coroutineScope {
        val calendarDeferred = if (TimelineItemType.CALENDAR_EVENT in enabledSources) {
            async {
                runCatching {
                    val endMillis = Instant.now().plusSeconds(30L * 24 * 60 * 60).toEpochMilli()
                    calendarRepository.getEvents(sinceMillis, endMillis)
                }
            }
        } else null

        val smsDeferred = if (TimelineItemType.SMS in enabledSources) {
            async { runCatching { smsRepository.getMessages(sinceMillis) } }
        } else null

        val callDeferred = if (TimelineItemType.CALL_LOG in enabledSources) {
            async { runCatching { callLogRepository.getCallLogs(sinceMillis) } }
        } else null

        val emailDeferred = if (TimelineItemType.EMAIL in enabledSources) {
            async { runCatching { emailRepository.getEmails(sinceMillis) } }
        } else null

        // Collect results, ignoring individual failures
        val allItems = mutableListOf<TimelineItem>()

        calendarDeferred?.await()?.getOrNull()?.let { allItems.addAll(it) }
        smsDeferred?.await()?.getOrNull()?.let { allItems.addAll(it) }
        callDeferred?.await()?.getOrNull()?.let { allItems.addAll(it) }
        emailDeferred?.await()?.getOrNull()?.let { allItems.addAll(it) }

        // Persist to cache
        if (allItems.isNotEmpty()) {
            val entities = allItems.map { TimelineEntity.fromDomain(it) }
            timelineDao.insertAll(entities)
        }
    }

    /**
     * Generate a daily digest for the specified date.
     */
    suspend fun generateDailyDigest(date: LocalDate): DailyDigest {
        val zone = ZoneId.systemDefault()
        val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val allTypes = TimelineItemType.entries.map { it.name }
        val items = timelineDao.getItemsInRangeOnce(startMillis, endMillis, allTypes)
            .map { it.toDomain() }

        val emails = items.filter { it.type == TimelineItemType.EMAIL }
        val sms = items.filter { it.type == TimelineItemType.SMS }
        val calls = items.filter { it.type == TimelineItemType.CALL_LOG }
        val events = items.filter { it.type == TimelineItemType.CALENDAR_EVENT }

        val missedCalls = calls.filter { !it.isRead }

        return DailyDigest(
            date = date,
            emailCount = emails.size,
            smsCount = sms.size,
            missedCallCount = missedCalls.size,
            calendarEventCount = events.size,
            topEmails = emails.take(5),
            topMessages = sms.take(5),
            missedCalls = missedCalls,
            upcomingEvents = events
        )
    }

    /**
     * Purge cache entries older than the given threshold.
     */
    suspend fun pruneCache(olderThanMillis: Long) {
        timelineDao.deleteOlderThan(olderThanMillis)
    }
}
