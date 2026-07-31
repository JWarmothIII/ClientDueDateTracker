package dev.jwarmothiii.clientduedatetracker.domain.model

import dev.jwarmothiii.clientduedatetracker.domain.client.model.Client
import dev.jwarmothiii.clientduedatetracker.domain.client.model.ClientId
import dev.jwarmothiii.clientduedatetracker.domain.client.model.normalizedInitials
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.Requirement
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementOrigin
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BusinessModelTest {
    @Test
    fun typedIdsRejectBlankValues() {
        assertThrows(IllegalArgumentException::class.java) { ClientId(" ") }
        assertThrows(IllegalArgumentException::class.java) { RequirementId("") }
    }

    @Test
    fun initialsAreTrimmedAndUppercaseWithoutRemovingPunctuation() {
        assertEquals("A-B", " a-b ".normalizedInitials())
    }

    @Test
    fun clientDatesCannotPrecedeIntake() {
        assertThrows(IllegalArgumentException::class.java) {
            Client(
                id = ClientId("client"),
                initials = "AB",
                intakeDate = LocalDate.parse("2026-07-10"),
                scheduledAssessmentDate = LocalDate.parse("2026-07-09"),
                actualAssessmentCompletedDate = null,
                plannedExitDate = null,
            )
        }
    }

    @Test
    fun reopeningClearsCompletionTimestamp() {
        val completed =
            requirement(
                status = RequirementStatus.COMPLETED,
                completedAt = Instant.parse("2026-07-20T12:00:00Z"),
            )
        val reopened = completed.copy(status = RequirementStatus.PENDING, completedAt = null)

        assertEquals(RequirementStatus.PENDING, reopened.status)
        assertNull(reopened.completedAt)
    }

    private fun requirement(
        status: RequirementStatus,
        completedAt: Instant?,
    ) = Requirement(
        id = RequirementId("requirement"),
        contractId = ContractId("contract"),
        templateId = null,
        origin = RequirementOrigin.AD_HOC,
        occurrenceKey = null,
        title = "Title",
        instructions = "",
        dueDate = LocalDate.parse("2026-07-20"),
        notificationLeadDays = 3,
        status = status,
        completedAt = completedAt,
        manuallyCustomized = true,
    )
}
