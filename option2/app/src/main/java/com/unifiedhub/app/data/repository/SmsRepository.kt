package com.unifiedhub.app.data.repository

import android.content.ContentResolver
import android.provider.Telephony
import com.unifiedhub.app.data.model.TimelineItem
import com.unifiedhub.app.data.model.TimelineItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRepository @Inject constructor(
    private val contentResolver: ContentResolver
) {
    /**
     * Query SMS messages from the Telephony provider.
     * Handles both SMS and MMS via the unified content://sms URI.
     */
    suspend fun getMessages(sinceMillis: Long, limit: Int = 200): List<TimelineItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<TimelineItem>()

            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ
            )

            val selection = "${Telephony.Sms.DATE} >= ?"
            val selectionArgs = arrayOf(sinceMillis.toString())

            val cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                val readIdx = it.getColumnIndexOrThrow(Telephony.Sms.READ)

                while (it.moveToNext()) {
                    val smsId = it.getLong(idIdx)
                    val address = it.getString(addressIdx) ?: "Unknown"
                    val body = it.getString(bodyIdx) ?: ""
                    val date = it.getLong(dateIdx)
                    val type = it.getInt(typeIdx)
                    val isRead = it.getInt(readIdx) == 1

                    val direction = when (type) {
                        Telephony.Sms.MESSAGE_TYPE_INBOX -> "Received"
                        Telephony.Sms.MESSAGE_TYPE_SENT -> "Sent"
                        Telephony.Sms.MESSAGE_TYPE_DRAFT -> "Draft"
                        else -> ""
                    }

                    items.add(
                        TimelineItem(
                            id = "sms_$smsId",
                            type = TimelineItemType.SMS,
                            title = "$direction message",
                            contact = address,
                            timestamp = Instant.ofEpochMilli(date),
                            preview = body.take(160),
                            isRead = isRead,
                            sourceId = "sms_$smsId"
                        )
                    )
                }
            }

            items
        }
}
