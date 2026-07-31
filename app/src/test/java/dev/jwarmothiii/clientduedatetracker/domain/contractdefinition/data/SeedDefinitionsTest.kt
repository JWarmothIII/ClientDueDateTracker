package dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.data

import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.DeadlineAnchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class SeedDefinitionsTest {
    @Test
    fun matrixSeedsAllThreeContractTypesAndSeparateStatePrtAction() {
        assertEquals(listOf("CTS", "USPO", "State / RSUD"), SeedDefinitions.all.map { it.name })
        val stateTitles =
            SeedDefinitions.all
                .single { it.code == "state-rsud" }
                .templates
                .map { it.title }
        assert(stateTitles.contains("Initial treatment plan"))
        assert(stateTitles.contains("First PRT action"))
    }

    @Test
    fun acceptedAssessmentAndOriginalPlanDatesAreEncoded() {
        val intake = LocalDate.parse("2026-01-20")
        val scheduled = LocalDate.parse("2026-02-02")
        val actual = LocalDate.parse("2026-02-12")

        val cts = SeedDefinitions.all.single { it.code == "cts" }.templates
        assertEquals(
            LocalDate.parse("2026-02-03"),
            cts.single { it.title == "Assessment" }.policy.calculate(intake),
        )
        assertEquals(
            LocalDate.parse("2026-02-28"),
            cts.single { it.title == "Original treatment plan" }.policy.calculate(intake),
        )

        val uspo = SeedDefinitions.all.single { it.code == "uspo" }.templates
        assertEquals(
            LocalDate.parse("2026-02-12"),
            uspo.single { it.title == "Assessment" }.policy.calculate(scheduled),
        )
        assertEquals(
            LocalDate.parse("2026-02-28"),
            uspo.single { it.title == "Original treatment plan" }.policy.calculate(actual),
        )
    }

    @Test
    fun eventDrivenMatrixRulesHaveExplicitEventAnchors() {
        SeedDefinitions.all
            .flatMap { it.templates }
            .filter { it.title == "Case note" || it.title == "Termination Prob 45" }
            .forEach {
                assertEquals(DeadlineAnchor.CONTRACT_EVENT, it.policy.anchor)
                assertNotNull(it.policy.eventKind)
            }
    }
}
