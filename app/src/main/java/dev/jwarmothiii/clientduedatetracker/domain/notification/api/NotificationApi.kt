package dev.jwarmothiii.clientduedatetracker.domain.notification.api

import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId
import java.time.Instant
import java.time.LocalDate

enum class DeliveryKind {
    LEAD,
    OVERDUE,
}

data class ReminderCandidate(
    val requirementId: RequirementId,
    val kind: DeliveryKind,
)

interface NotificationApi {
    suspend fun reminderCandidates(today: LocalDate): List<ReminderCandidate>

    suspend fun recordDelivered(
        candidates: List<ReminderCandidate>,
        deliveryDate: LocalDate,
        deliveredAt: Instant,
    )
}
