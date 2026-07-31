package dev.jwarmothiii.clientduedatetracker.domain.notification

import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.DashboardRequirement
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.Requirement
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementOrigin
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementStatus
import dev.jwarmothiii.clientduedatetracker.domain.notification.api.DeliveryKind
import dev.jwarmothiii.clientduedatetracker.domain.notification.api.NotificationApi
import dev.jwarmothiii.clientduedatetracker.domain.notification.api.ReminderCandidate
import dev.jwarmothiii.clientduedatetracker.domain.notification.model.DeliveryHistory
import dev.jwarmothiii.clientduedatetracker.domain.notification.model.ReminderPlanner
import dev.jwarmothiii.clientduedatetracker.domain.notification.usecase.NotificationPoster
import dev.jwarmothiii.clientduedatetracker.domain.notification.usecase.SendDailySummaryUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ReminderBehaviorTest {
    private val today = LocalDate.parse("2026-07-30")

    @Test
    fun leadIsSentOnceAndOverdueCanBeSentOnceEachDay() {
        val requirement = pending("one", today.plusDays(3))
        val lead = ReminderPlanner.candidates(listOf(requirement), emptyList(), today).single()
        assertEquals(DeliveryKind.LEAD, lead.kind)

        val afterLead =
            ReminderPlanner.candidates(
                listOf(requirement),
                listOf(DeliveryHistory(requirement.requirement.id, DeliveryKind.LEAD, today)),
                today,
            )
        assertTrue(afterLead.isEmpty())

        val overdue = pending("late", today.minusDays(1))
        val deliveredYesterday =
            DeliveryHistory(overdue.requirement.id, DeliveryKind.OVERDUE, today.minusDays(1))
        assertEquals(
            DeliveryKind.OVERDUE,
            ReminderPlanner.candidates(listOf(overdue), listOf(deliveredYesterday), today).single().kind,
        )
        val deliveredToday = deliveredYesterday.copy(deliveryDate = today)
        assertTrue(ReminderPlanner.candidates(listOf(overdue), listOf(deliveredToday), today).isEmpty())
    }

    @Test
    fun oneSummaryPostsForAllCandidatesAndRecordsOnlyAfterSuccess() =
        runBlocking {
            val candidates =
                listOf(
                    ReminderCandidate(RequirementId("one"), DeliveryKind.LEAD),
                    ReminderCandidate(RequirementId("two"), DeliveryKind.OVERDUE),
                )
            val api = FakeNotificationApi(candidates)
            val failedPoster = FakePoster(succeeds = false)
            SendDailySummaryUseCase(api, failedPoster)(today)
            assertEquals(listOf(2), failedPoster.postedCounts)
            assertTrue(api.recorded.isEmpty())

            val successfulPoster = FakePoster(succeeds = true)
            SendDailySummaryUseCase(api, successfulPoster)(today)
            assertEquals(listOf(2), successfulPoster.postedCounts)
            assertEquals(candidates, api.recorded)
        }

    private fun pending(
        id: String,
        dueDate: LocalDate,
    ) = DashboardRequirement(
        requirement =
            Requirement(
                id = RequirementId(id),
                contractId = ContractId("contract"),
                templateId = null,
                origin = RequirementOrigin.AD_HOC,
                occurrenceKey = null,
                title = "Requirement",
                instructions = "",
                dueDate = dueDate,
                notificationLeadDays = 3,
                status = RequirementStatus.PENDING,
                completedAt = null,
                manuallyCustomized = true,
            ),
        clientInitials = "AB",
    )
}

private class FakeNotificationApi(
    private val candidates: List<ReminderCandidate>,
) : NotificationApi {
    var recorded: List<ReminderCandidate> = emptyList()

    override suspend fun reminderCandidates(today: LocalDate): List<ReminderCandidate> = candidates

    override suspend fun recordDelivered(
        candidates: List<ReminderCandidate>,
        deliveryDate: LocalDate,
        deliveredAt: Instant,
    ) {
        recorded = candidates
    }
}

private class FakePoster(
    private val succeeds: Boolean,
) : NotificationPoster {
    val postedCounts = mutableListOf<Int>()

    override fun postGenericSummary(requirementCount: Int): Boolean {
        postedCounts += requirementCount
        return succeeds
    }
}
