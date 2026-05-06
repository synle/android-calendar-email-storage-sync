package com.example.unifiedhub.data.util

import com.example.unifiedhub.data.model.ItemType
import com.example.unifiedhub.data.model.TimelineItem
import com.example.unifiedhub.viewmodel.TabType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineFilterTest {

    private fun item(
        id: String,
        type: ItemType,
        timestamp: Long,
        title: String = "",
        description: String = "",
        extraInfo: String = ""
    ) = TimelineItem(
        id = id,
        title = title,
        description = description,
        timestamp = timestamp,
        type = type,
        extraInfo = extraInfo
    )

    private val email1 = item("e1", ItemType.EMAIL, 1000L, title = "Standup notes", description = "Hi team", extraInfo = "Sarah")
    private val email2 = item("e2", ItemType.EMAIL, 3000L, title = "Invoice #4521", description = "February", extraInfo = "billing@workspace.com")
    private val email3 = item("e3", ItemType.EMAIL, 2000L, title = "Weekend plans?", description = "Are you free Saturday", extraInfo = "Mike")
    private val sms1 = item("s1", ItemType.SMS, 1500L, title = "+15551234567", description = "Code is 123456", extraInfo = "Received")
    private val sms2 = item("s2", ItemType.SMS, 2500L, title = "Mom", description = "Call me when you can", extraInfo = "Received")
    private val cal1 = item("c1", ItemType.CALENDAR_EVENT, 1800L, title = "Team meeting", description = "Quarterly review", extraInfo = "Conf Room A")
    private val call1 = item("ca1", ItemType.CALL, 1600L, title = "+15559876543", extraInfo = "Missed")

    private val all = listOf(email1, email2, email3, sms1, sms2, cal1, call1)

    // ---------- Tab filter (happy path) ----------

    @Test
    fun `EMAIL tab returns only email items, sorted newest-first`() {
        val result = TimelineFilter.recomputeVisible(all, TabType.EMAIL, query = "", sortDescending = true)
        assertEquals(listOf("e2", "e3", "e1"), result.map { it.id })
    }

    @Test
    fun `SMS tab returns only sms items`() {
        val result = TimelineFilter.recomputeVisible(all, TabType.SMS, query = "", sortDescending = true)
        assertEquals(listOf("s2", "s1"), result.map { it.id })
    }

    @Test
    fun `CALENDAR tab returns only calendar events (call items are hidden)`() {
        val result = TimelineFilter.recomputeVisible(all, TabType.CALENDAR, query = "", sortDescending = true)
        assertEquals(listOf("c1"), result.map { it.id })
    }

    @Test
    fun `CALL items never show up in any of the three tabs`() {
        for (tab in TabType.entries) {
            val result = TimelineFilter.recomputeVisible(all, tab, query = "", sortDescending = true)
            assertTrue(result.none { it.type == ItemType.CALL })
        }
    }

    // ---------- Sort direction ----------

    @Test
    fun `sortDescending=false yields oldest-first`() {
        val result = TimelineFilter.recomputeVisible(all, TabType.EMAIL, query = "", sortDescending = false)
        assertEquals(listOf("e1", "e3", "e2"), result.map { it.id })
    }

    // ---------- Search ----------

    @Test
    fun `search matches title (case-insensitive)`() {
        val result = TimelineFilter.recomputeVisible(all, TabType.EMAIL, query = "INVOICE", sortDescending = true)
        assertEquals(listOf("e2"), result.map { it.id })
    }

    @Test
    fun `search matches description text`() {
        val result = TimelineFilter.recomputeVisible(all, TabType.EMAIL, query = "saturday", sortDescending = true)
        assertEquals(listOf("e3"), result.map { it.id })
    }

    @Test
    fun `search matches extraInfo (sender, location)`() {
        val emailMatch = TimelineFilter.recomputeVisible(all, TabType.EMAIL, query = "Sarah", sortDescending = true)
        assertEquals(listOf("e1"), emailMatch.map { it.id })

        val calMatch = TimelineFilter.recomputeVisible(all, TabType.CALENDAR, query = "conf room", sortDescending = true)
        assertEquals(listOf("c1"), calMatch.map { it.id })
    }

    @Test
    fun `search treats whitespace-only query as no filter`() {
        val empty = TimelineFilter.recomputeVisible(all, TabType.EMAIL, query = "", sortDescending = true)
        val spaces = TimelineFilter.recomputeVisible(all, TabType.EMAIL, query = "   ", sortDescending = true)
        assertEquals(empty.map { it.id }, spaces.map { it.id })
    }

    @Test
    fun `search trims surrounding whitespace`() {
        val result = TimelineFilter.recomputeVisible(all, TabType.EMAIL, query = "  invoice  ", sortDescending = true)
        assertEquals(listOf("e2"), result.map { it.id })
    }

    @Test
    fun `search with no matches returns empty list (not the unfiltered list)`() {
        val result = TimelineFilter.recomputeVisible(all, TabType.SMS, query = "spotify", sortDescending = true)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `search applies AFTER tab filter — query in another tab's items doesn't leak`() {
        // "Mom" is the SMS title — search in EMAIL tab should not surface it.
        val result = TimelineFilter.recomputeVisible(all, TabType.EMAIL, query = "Mom", sortDescending = true)
        assertTrue(result.isEmpty())
    }

    // ---------- Edge cases ----------

    @Test
    fun `empty input returns empty result for all tabs`() {
        for (tab in TabType.entries) {
            val result = TimelineFilter.recomputeVisible(emptyList(), tab, query = "", sortDescending = true)
            assertTrue(result.isEmpty())
        }
    }

    @Test
    fun `items with identical timestamps are not deduplicated`() {
        val a = item("a", ItemType.EMAIL, 5000L, title = "A")
        val b = item("b", ItemType.EMAIL, 5000L, title = "B")
        val result = TimelineFilter.recomputeVisible(listOf(a, b), TabType.EMAIL, query = "", sortDescending = true)
        assertEquals(2, result.size)
    }
}
