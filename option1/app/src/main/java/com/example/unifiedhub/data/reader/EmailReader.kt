// =============================================================================
// EmailReader.kt — Simulated email data
// =============================================================================
//
// ⚠️ IMPORTANT: WHY EMAIL IS DIFFERENT FROM SMS/CALLS/CALENDAR
//
// Reading emails is MUCH harder than reading SMS or call logs. Here's why:
//
// 1. NO BUILT-IN EMAIL CONTENT PROVIDER
//    Unlike SMS (Telephony.Sms) and Calls (CallLog.Calls), Android does NOT
//    have a standard way to read emails from the device.
//
// 2. GMAIL API REQUIRES GOOGLE CLOUD SETUP
//    To read Gmail, you would need to:
//    a. Create a Google Cloud project (console.cloud.google.com)
//    b. Enable the Gmail API
//    c. Create OAuth 2.0 credentials
//    d. Add the google-services.json file to your app
//    e. Implement the OAuth login flow
//    f. Use the Gmail API client library
//
//    This is a LOT of setup for a beginner project!
//
// 3. OTHER EMAIL PROVIDERS
//    Outlook, Yahoo, etc. each have their OWN APIs. There's no universal
//    way to read "all emails" on a device.
//
// OUR APPROACH:
// For this learning project, we'll use MOCK (fake) data that looks realistic.
// This lets us build the full UI and architecture without the complexity
// of the Gmail API.
//
// WHEN YOU'RE READY for real email:
// Look into the Gmail API: https://developers.google.com/gmail/api
// Or consider IMAP/SMTP libraries like JavaMail.
// =============================================================================

package com.example.unifiedhub.data.reader

import com.example.unifiedhub.data.model.ItemType
import com.example.unifiedhub.data.model.TimelineItem
import java.util.Calendar

class EmailReader {

    // --- Generate realistic-looking mock email data ---
    // In a real app, this would call the Gmail API or an IMAP server.
    fun readEmails(): List<TimelineItem> {
        val now = Calendar.getInstance()

        // Sample emails that look realistic
        val mockEmails = listOf(
            MockEmail(
                subject = "Team standup notes - Feb 28",
                sender = "Sarah Johnson",
                preview = "Hi team, here are the key points from today's standup...",
                hoursAgo = 1
            ),
            MockEmail(
                subject = "Your Amazon order has shipped!",
                sender = "Amazon",
                preview = "Your package is on its way. Track your delivery...",
                hoursAgo = 3
            ),
            MockEmail(
                subject = "Invoice #4521 - February",
                sender = "billing@workspace.com",
                preview = "Please find attached your invoice for February 2026...",
                hoursAgo = 5
            ),
            MockEmail(
                subject = "Weekend plans?",
                sender = "Mike Chen",
                preview = "Hey! Are you free this Saturday? Was thinking we could...",
                hoursAgo = 8
            ),
            MockEmail(
                subject = "Your weekly GitHub digest",
                sender = "GitHub",
                preview = "Here's what happened in repositories you watch...",
                hoursAgo = 24
            ),
            MockEmail(
                subject = "Meeting rescheduled to 3 PM",
                sender = "David Park",
                preview = "Hi, I need to move our 2 PM meeting to 3 PM. Does that...",
                hoursAgo = 26
            ),
            MockEmail(
                subject = "New comment on your pull request",
                sender = "GitLab",
                preview = "Alex left a comment on PR #182: 'Looks good, just one...'",
                hoursAgo = 30
            )
        )

        // Convert mock emails to TimelineItems
        return mockEmails.mapIndexed { index, email ->
            val timestamp = Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, -email.hoursAgo)
            }.timeInMillis

            TimelineItem(
                id = "email_$index",
                title = email.subject,
                description = "From: ${email.sender}\n${email.preview}",
                timestamp = timestamp,
                type = ItemType.EMAIL,
                extraInfo = email.sender
            )
        }
    }

    // Simple helper class for mock data
    private data class MockEmail(
        val subject: String,
        val sender: String,
        val preview: String,
        val hoursAgo: Int
    )
}
