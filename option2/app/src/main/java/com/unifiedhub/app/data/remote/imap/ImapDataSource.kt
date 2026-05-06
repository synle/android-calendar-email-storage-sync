package com.unifiedhub.app.data.remote.imap

import com.unifiedhub.app.data.model.TimelineItem

interface ImapDataSource {
    suspend fun fetchEmails(sinceMillis: Long, limit: Int): List<TimelineItem>
}
