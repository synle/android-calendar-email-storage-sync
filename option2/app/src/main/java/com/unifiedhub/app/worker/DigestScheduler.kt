package com.unifiedhub.app.worker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the daily digest WorkManager job.
 * Uses PeriodicWorkRequest with initial delay calculated to target the user's preferred time.
 */
@Singleton
class DigestScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun scheduleDailyDigest(targetTime: LocalTime) {
        val now = LocalDateTime.now()
        var targetDateTime = now.toLocalDate().atTime(targetTime)

        // If the target time has already passed today, schedule for tomorrow
        if (targetDateTime.isBefore(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val initialDelay = Duration.between(now, targetDateTime)

        val workRequest = PeriodicWorkRequestBuilder<DigestWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DigestWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelDailyDigest() {
        workManager.cancelUniqueWork(DigestWorker.WORK_NAME)
    }
}
