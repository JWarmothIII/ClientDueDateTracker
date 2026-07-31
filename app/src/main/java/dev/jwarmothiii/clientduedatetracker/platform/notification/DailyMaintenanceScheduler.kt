package dev.jwarmothiii.clientduedatetracker.platform.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object DailyMaintenanceScheduler {
    private const val UNIQUE_WORK_NAME = "daily-maintenance-at-10"
    private const val TARGET_HOUR = 10

    fun ensureScheduled(context: Context) {
        enqueue(context, ExistingWorkPolicy.KEEP)
    }

    fun scheduleNext(context: Context) {
        enqueue(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    private fun enqueue(
        context: Context,
        policy: ExistingWorkPolicy,
    ) {
        val now = ZonedDateTime.now()
        var next =
            now
                .withHour(TARGET_HOUR)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request =
            OneTimeWorkRequestBuilder<DailyMaintenanceWorker>()
                .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
                .build()
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, policy, request)
    }
}
