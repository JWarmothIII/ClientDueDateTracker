package dev.jwarmothiii.clientduedatetracker.domain.notification.model

import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.DashboardRequirement
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId
import dev.jwarmothiii.clientduedatetracker.domain.notification.api.DeliveryKind
import dev.jwarmothiii.clientduedatetracker.domain.notification.api.ReminderCandidate
import java.time.LocalDate

data class DeliveryHistory(
    val requirementId: RequirementId,
    val kind: DeliveryKind,
    val deliveryDate: LocalDate,
)

object ReminderPlanner {
    fun candidates(
        pending: List<DashboardRequirement>,
        deliveries: List<DeliveryHistory>,
        today: LocalDate,
    ): List<ReminderCandidate> =
        pending.mapNotNull { row ->
            val requirement = row.requirement
            when {
                requirement.dueDate < today -> {
                    val deliveredToday =
                        deliveries.any {
                            it.requirementId == requirement.id &&
                                it.kind == DeliveryKind.OVERDUE &&
                                it.deliveryDate == today
                        }
                    if (deliveredToday) null else ReminderCandidate(requirement.id, DeliveryKind.OVERDUE)
                }

                !requirement.dueDate.minusDays(requirement.notificationLeadDays.toLong()).isAfter(today) -> {
                    val leadDelivered =
                        deliveries.any {
                            it.requirementId == requirement.id && it.kind == DeliveryKind.LEAD
                        }
                    if (leadDelivered) null else ReminderCandidate(requirement.id, DeliveryKind.LEAD)
                }

                else -> {
                    null
                }
            }
        }
}
