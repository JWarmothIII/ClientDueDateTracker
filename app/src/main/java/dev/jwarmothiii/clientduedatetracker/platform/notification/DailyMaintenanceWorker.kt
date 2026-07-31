package dev.jwarmothiii.clientduedatetracker.platform.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.usecase.DailyGenerationUseCase
import dev.jwarmothiii.clientduedatetracker.domain.notification.usecase.SendDailySummaryUseCase

@HiltWorker
class DailyMaintenanceWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParameters: WorkerParameters,
        private val dailyGeneration: DailyGenerationUseCase,
        private val sendDailySummary: SendDailySummaryUseCase,
    ) : CoroutineWorker(appContext, workerParameters) {
        override suspend fun doWork(): Result =
            try {
                dailyGeneration()
                sendDailySummary()
                DailyMaintenanceScheduler.scheduleNext(applicationContext)
                Result.success()
            } catch (_: Exception) {
                Result.retry()
            }
    }
