package dev.jwarmothiii.clientduedatetracker.domain.contracttracking

import dev.jwarmothiii.clientduedatetracker.domain.client.model.ClientId
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractEventKind
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractTypeId
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.DeadlineAnchor
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.DeadlinePolicy
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RecurrencePolicy
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplate
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplateId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.GenerationContract
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.Contract
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractEvent
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractEventId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.usecase.DeadlineGenerator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DeadlineGeneratorTest {
    private val generator = DeadlineGenerator()

    @Test
    fun rollingHorizonIncludesBoundaryAndIsIdempotent() {
        val source =
            source(
                template =
                    template(
                        DeadlinePolicy(
                            DeadlineAnchor.INTAKE,
                            recurrence = RecurrencePolicy.WEEKLY,
                        ),
                    ),
            )

        val first = generator.generate(source, LocalDate.parse("2026-01-31"), ZoneOffset.UTC)
        val second = generator.generate(source, LocalDate.parse("2026-01-31"), ZoneOffset.UTC)

        assertEquals(first, second)
        assertEquals(LocalDate.parse("2026-01-29"), first.last().dueDate)
    }

    @Test
    fun sourceDateCorrectionKeepsOccurrenceKeyAndChangesDueDate() {
        val policy = DeadlinePolicy(DeadlineAnchor.SCHEDULED_ASSESSMENT, offsetDays = -1)
        val first =
            generator.generate(
                source(template(policy), scheduled = LocalDate.parse("2026-01-10")),
                LocalDate.parse("2026-02-01"),
                ZoneOffset.UTC,
            )
        val corrected =
            generator.generate(
                source(template(policy), scheduled = LocalDate.parse("2026-01-12")),
                LocalDate.parse("2026-02-01"),
                ZoneOffset.UTC,
            )

        assertEquals(first.single().occurrenceKey, corrected.single().occurrenceKey)
        assertEquals(LocalDate.parse("2026-01-11"), corrected.single().dueDate)
    }

    @Test
    fun occurrenceKeysAreScopedToTheContractAndTemplateLineage() {
        val policy = DeadlinePolicy(DeadlineAnchor.INTAKE, offsetDays = 1)
        val first =
            generator
                .generate(source(template(policy), contractId = "contract-a"), LocalDate.parse("2026-02-01"), ZoneOffset.UTC)
                .single()
        val second =
            generator
                .generate(source(template(policy), contractId = "contract-b"), LocalDate.parse("2026-02-01"), ZoneOffset.UTC)
                .single()

        assert(first.occurrenceKey != second.occurrenceKey)
    }

    @Test
    fun planChangeCreatesImmediateReviewAndRestartsNinetyDayCycle() {
        val event =
            ContractEvent(
                id = ContractEventId("event"),
                contractId = ContractId("contract"),
                kind = ContractEventKind.PLAN_CHANGE,
                eventDate = LocalDate.parse("2026-02-10"),
                createdAt = Instant.EPOCH,
            )
        val source =
            source(
                template(
                    DeadlinePolicy(
                        DeadlineAnchor.LAST_PLAN_COMPLETION,
                        offsetDays = 90,
                        recurrence = RecurrencePolicy.EVERY_90_DAYS,
                    ),
                ),
                events = listOf(event),
            )

        val generated = generator.generate(source, LocalDate.parse("2026-06-01"), ZoneOffset.UTC)

        assertEquals(
            listOf(LocalDate.parse("2026-02-10"), LocalDate.parse("2026-05-11")),
            generated.map { it.dueDate },
        )
    }

    private fun source(
        template: RequirementTemplate,
        scheduled: LocalDate = LocalDate.parse("2026-01-10"),
        events: List<ContractEvent> = emptyList(),
        contractId: String = "contract",
    ) = GenerationContract(
        contract =
            Contract(
                id = ContractId(contractId),
                clientId = ClientId("client"),
                contractTypeId = ContractTypeId("type"),
                startDate = LocalDate.parse("2026-01-01"),
                active = true,
                plannedExitDate = null,
                actualExitDate = null,
                exitKind = null,
            ),
        scheduledAssessmentDate = scheduled,
        actualAssessmentCompletedDate = LocalDate.parse("2026-01-15"),
        assignments = listOf(template),
        events = events,
        requirements = emptyList(),
    )

    private fun template(policy: DeadlinePolicy) =
        RequirementTemplate(
            id = RequirementTemplateId("template"),
            contractTypeId = ContractTypeId("type"),
            lineageId = "lineage",
            version = 1,
            title = "Requirement",
            instructions = "",
            policy = policy,
            notificationLeadDays = 3,
            active = true,
        )
}
