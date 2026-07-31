package dev.jwarmothiii.clientduedatetracker.domain.contractdefinition

import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.data.DataIntegrityException
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.data.DeadlinePolicyJson
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.DeadlineAnchor
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.DeadlinePolicy
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RecurrencePolicy
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.WeekendAdjustment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class DeadlinePolicyTest {
    @Test
    fun dayNumberCountsIntakeAsDayOne() {
        val policy = DeadlinePolicy(DeadlineAnchor.INTAKE, dayNumber = 5)

        assertEquals(LocalDate.parse("2026-07-14"), policy.calculate(LocalDate.parse("2026-07-10")))
    }

    @Test
    fun monthBoundaryHandlesLeapYear() {
        val policy = DeadlinePolicy(DeadlineAnchor.INTAKE, monthOffset = 1, endOfMonth = true)

        assertEquals(LocalDate.parse("2024-02-29"), policy.calculate(LocalDate.parse("2024-01-31")))
    }

    @Test
    fun nextWeekdayMovesWeekendToMonday() {
        val policy =
            DeadlinePolicy(
                DeadlineAnchor.INTAKE,
                offsetDays = 1,
                weekendAdjustment = WeekendAdjustment.NEXT_WEEKDAY,
            )

        assertEquals(LocalDate.parse("2026-08-03"), policy.calculate(LocalDate.parse("2026-07-31")))
    }

    @Test
    fun policyJsonRoundTripsAllStableCodes() {
        val original =
            DeadlinePolicy(
                anchor = DeadlineAnchor.CONTRACT_EVENT,
                offsetDays = 7,
                recurrence = RecurrencePolicy.NONE,
                eventKind =
                    dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractEventKind
                        .TERMINATION_NOTICE,
                weekendAdjustment = WeekendAdjustment.NEXT_WEEKDAY,
            )

        assertEquals(original, DeadlinePolicyJson.decode(DeadlinePolicyJson.encode(original)))
    }

    @Test
    fun unknownPolicyCodeFailsClosed() {
        val json =
            DeadlinePolicyJson
                .encode(DeadlinePolicy(DeadlineAnchor.INTAKE))
                .replace("\"INTAKE\"", "\"UNRECOGNIZED\"")

        assertThrows(DataIntegrityException::class.java) { DeadlinePolicyJson.decode(json) }
    }
}
