// =============================================================================
// SmsReader.kt — Reads SMS (text) messages from the device
// =============================================================================
// HOW SMS IS STORED ON ANDROID:
//
// Android stores SMS messages in a Content Provider at "content://sms".
// The Telephony.Sms class gives us constants to access it.
//
// Each message has:
//   - _id: Unique ID
//   - address: Phone number of the sender/recipient
//   - body: The actual message text
//   - date: When the message was received/sent (timestamp)
//   - type: 1 = received (inbox), 2 = sent
//
// WHY IS READING SMS A "DANGEROUS" PERMISSION?
//
// SMS messages contain very personal information — conversations with
// friends, bank OTPs, medical info, etc. Android requires the user to
// explicitly approve access. This is a GOOD thing — it protects privacy.
//
// If the user denies permission, our app simply won't show SMS data.
// We should NEVER crash or force the user to grant permission.
// =============================================================================

package com.example.unifiedhub.data.reader

import android.content.ContentResolver
import android.provider.Telephony
import com.example.unifiedhub.data.model.ItemType
import com.example.unifiedhub.data.model.TimelineItem

class SmsReader(
    private val contentResolver: ContentResolver
) {
    // --- Read SMS messages and return as TimelineItems ---
    fun readMessages(limit: Int = 50): List<TimelineItem> {
        val items = mutableListOf<TimelineItem>()

        // --- What columns do we want? ---
        val projection = arrayOf(
            Telephony.Sms._ID,       // Unique message ID
            Telephony.Sms.ADDRESS,   // Phone number
            Telephony.Sms.BODY,      // Message text
            Telephony.Sms.DATE,      // When it was received/sent
            Telephony.Sms.TYPE       // 1 = inbox, 2 = sent
        )

        // --- Sort by date, newest first, and limit results ---
        // WHY limit? Users might have thousands of messages.
        // Loading all of them would make the app slow!
        val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT $limit"

        // --- Query the SMS provider ---
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,  // WHERE: the SMS content provider
            projection,                  // WHAT: columns we want
            null,                        // No filter (get all messages)
            null,                        // No filter arguments
            sortOrder                    // HOW: sorted by date, limited
        )

        cursor?.use { c ->
            val idIndex = c.getColumnIndex(Telephony.Sms._ID)
            val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)
            val typeIndex = c.getColumnIndex(Telephony.Sms.TYPE)

            while (c.moveToNext()) {
                val address = c.getString(addressIndex) ?: "Unknown"
                val body = c.getString(bodyIndex) ?: ""
                val date = c.getLong(dateIndex)
                val type = c.getInt(typeIndex)

                // Determine if it's received or sent
                val direction = when (type) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> "Received"
                    Telephony.Sms.MESSAGE_TYPE_SENT -> "Sent"
                    Telephony.Sms.MESSAGE_TYPE_DRAFT -> "Draft"
                    else -> "Message"
                }

                items.add(
                    TimelineItem(
                        id = "sms_${c.getLong(idIndex)}",
                        title = address,           // Show the phone number/name
                        description = body,        // Show the message text
                        timestamp = date,
                        type = ItemType.SMS,
                        extraInfo = direction      // "Received" or "Sent"
                    )
                )
            }
        }

        return items
    }
}
