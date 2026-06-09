package com.example.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkScheduler {
    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 1. Daily Audit Worker (Runs every 24 hours, starting around midnight)
        val msUntilMidnight = getMsUntilMidnight()
        val auditRequest = PeriodicWorkRequestBuilder<DailyAuditWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(msUntilMidnight, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "daily_audit",
            ExistingPeriodicWorkPolicy.KEEP,
            auditRequest
        )

        // 2. Distraction Drain Worker (Runs every 15 minutes)
        val drainRequest = PeriodicWorkRequestBuilder<DistractionDrainWorker>(15, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "distraction_drain",
            ExistingPeriodicWorkPolicy.KEEP,
            drainRequest
        )
    }

    private fun getMsUntilMidnight(): Long {
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 24)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (midnight.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    }
}
