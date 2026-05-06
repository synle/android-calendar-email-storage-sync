package com.unifiedhub.app.data.remote.gmail

import com.unifiedhub.app.data.model.TimelineItem

interface GmailDataSource {
    suspend fun fetchEmails(sinceMillis: Long, limit: Int): List<TimelineItem>
}
