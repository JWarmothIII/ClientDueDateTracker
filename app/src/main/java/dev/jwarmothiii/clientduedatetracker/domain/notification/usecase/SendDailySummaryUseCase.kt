package dev.jwarmothiii.clientduedatetracker.domain.notification.usecase

import dev.jwarmothiii.clientduedatetracker.domain.notification.api.NotificationApi
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class SendDailySummaryUseCase
    @Inject
    constructor(
        private val notificationApi: NotificationApi,
        private val notificationPoster: NotificationPoster,
    ) {
        suspend operator fun invoke(today: LocalDate = LocalDate.now()) {
            val candidates = notificationApi.reminderCandidates(today)
            if (candidates.isEmpty()) return
            if (notificationPoster.postGenericSummary(candidates.size)) {
                notificationApi.recordDelivered(candidates, today, Clock.systemUTC().instant())
            }
        }
    }

interface NotificationPoster {
    fun postGenericSummary(requirementCount: Int): Boolean
}
