package com.unifiedhub.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unifiedhub.app.data.model.TimelineItemType
import com.unifiedhub.app.data.repository.UnifiedTimelineRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@HiltWorker
class DigestWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: UnifiedTimelineRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Refresh data first
            val sinceMillis = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()
            repository.refreshAll(sinceMillis, TimelineItemType.entries.toSet())

            // Generate digest
            val digest = repository.generateDailyDigest(LocalDate.now())
            val text = digest.toFormattedText()

            // Show notification with digest summary
            showDigestNotification(digest.emailCount, digest.smsCount, digest.missedCallCount)

            // Prune old cache (older than 90 days)
            val pruneThreshold = Instant.now().minus(90, ChronoUnit.DAYS).toEpochMilli()
            repository.pruneCache(pruneThreshold)

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun showDigestNotification(emails: Int, messages: Int, missedCalls: Int) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily Digest",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily summary of your communications"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Daily Digest")
            .setContentText("$emails emails, $messages messages, $missedCalls missed calls")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "daily_digest_worker"
        const val CHANNEL_ID = "daily_digest"
        const val NOTIFICATION_ID = 1001
    }
}
