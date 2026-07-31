package dev.jwarmothiii.clientduedatetracker.domain.contracttracking.usecase

import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.DeadlineAnchor
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RecurrencePolicy
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplate
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.adjustForWeekend
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.GeneratedRequirementDraft
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.GenerationContract
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

class DeadlineGenerator
    @Inject
    constructor() {
        fun generate(
            source: GenerationContract,
            throughDate: LocalDate,
            zoneId: ZoneId,
        ): List<GeneratedRequirementDraft> =
            source.assignments
                .flatMap { template -> generateTemplate(source, template, throughDate, zoneId) }
                .distinctBy { it.occurrenceKey }

        private fun generateTemplate(
            source: GenerationContract,
            template: RequirementTemplate,
            throughDate: LocalDate,
            zoneId: ZoneId,
        ): List<GeneratedRequirementDraft> {
            val policy = template.policy
            val templateThroughDate =
                if (!source.contract.active && policy.anchor != DeadlineAnchor.ACTUAL_EXIT) {
                    minOf(throughDate, source.contract.actualExitDate ?: throughDate)
                } else {
                    throughDate
                }
            if (policy.anchor == DeadlineAnchor.CONTRACT_EVENT) {
                return source.events
                    .filter { it.kind == policy.eventKind }
                    .mapNotNull { event ->
                        policy
                            .calculate(event.eventDate)
                            .takeIf { it <= templateThroughDate }
                            ?.let { dueDate ->
                                template.draft(source.contract.id.value, dueDate, "event:${event.id.value}")
                            }
                    }
            }
            if (policy.recurrence == RecurrencePolicy.EVERY_90_DAYS) {
                return generatePlanReviews(source, template, templateThroughDate, zoneId)
            }
            val anchor = source.anchorDate(policy.anchor) ?: return emptyList()
            return when (policy.recurrence) {
                RecurrencePolicy.NONE -> {
                    listOfNotNull(
                        policy
                            .calculate(anchor)
                            .takeIf { it <= templateThroughDate }
                            ?.let {
                                template.draft(source.contract.id.value, it, "anchor:${policy.anchor.name}")
                            },
                    )
                }

                RecurrencePolicy.WEEKLY -> {
                    generateSequence(policy.calculate(anchor)) { it.plusWeeks(1) }
                        .takeWhile { it <= templateThroughDate }
                        .filter { it >= source.contract.startDate }
                        .map {
                            template.draft(source.contract.id.value, it, "week:${it.toEpochDay()}")
                        }.toList()
                }

                RecurrencePolicy.MONTHLY_FIRST_DAY,
                RecurrencePolicy.MONTHLY_END,
                -> {
                    generateMonthly(source, template, templateThroughDate)
                }

                RecurrencePolicy.EVERY_90_DAYS -> {
                    error("Handled above.")
                }
            }
        }

        private fun generateMonthly(
            source: GenerationContract,
            template: RequirementTemplate,
            throughDate: LocalDate,
        ): List<GeneratedRequirementDraft> {
            val policy = template.policy
            return generateSequence(YearMonth.from(source.contract.startDate)) { it.plusMonths(1) }
                .map { month ->
                    val unadjusted =
                        if (policy.recurrence == RecurrencePolicy.MONTHLY_END) {
                            month.atEndOfMonth()
                        } else {
                            month.atDay(1)
                        }
                    unadjusted.adjustForWeekend(policy.weekendAdjustment)
                }.takeWhile { it <= throughDate }
                .filter { it >= source.contract.startDate }
                .map {
                    template.draft(source.contract.id.value, it, "month:${YearMonth.from(it)}")
                }.toList()
        }

        private fun generatePlanReviews(
            source: GenerationContract,
            template: RequirementTemplate,
            throughDate: LocalDate,
            zoneId: ZoneId,
        ): List<GeneratedRequirementDraft> {
            val planChange = source.events.filter { it.kind.name == "PLAN_CHANGE" }.maxByOrNull { it.eventDate }
            val lastCompletion =
                source.requirements
                    .filter { it.templateId == template.id && it.status == RequirementStatus.COMPLETED }
                    .mapNotNull { it.completedAt?.atZone(zoneId)?.toLocalDate() }
                    .maxOrNull()
            val restartDate =
                listOfNotNull(planChange?.eventDate, lastCompletion)
                    .maxOrNull()
                    ?: source.actualAssessmentCompletedDate
                    ?: source.contract.startDate
            val drafts = mutableListOf<GeneratedRequirementDraft>()
            if (planChange != null && (lastCompletion == null || planChange.eventDate > lastCompletion)) {
                drafts +=
                    template.draft(
                        source.contract.id.value,
                        planChange.eventDate,
                        "plan-change:${planChange.id.value}",
                    )
            }
            generateSequence(restartDate.plusDays(90)) { it.plusDays(90) }
                .takeWhile { it <= throughDate }
                .forEach {
                    drafts += template.draft(source.contract.id.value, it, "review:${it.toEpochDay()}")
                }
            return drafts
        }
    }

private fun GenerationContract.anchorDate(anchor: DeadlineAnchor): LocalDate? =
    when (anchor) {
        DeadlineAnchor.INTAKE -> contract.startDate

        DeadlineAnchor.SCHEDULED_ASSESSMENT -> scheduledAssessmentDate

        DeadlineAnchor.ACTUAL_ASSESSMENT_COMPLETION -> actualAssessmentCompletedDate

        DeadlineAnchor.PLANNED_EXIT -> contract.plannedExitDate

        DeadlineAnchor.ACTUAL_EXIT -> contract.actualExitDate

        DeadlineAnchor.CONTRACT_EVENT,
        DeadlineAnchor.LAST_PLAN_COMPLETION,
        -> null
    }

private fun RequirementTemplate.draft(
    contractPublicId: String,
    dueDate: LocalDate,
    suffix: String,
): GeneratedRequirementDraft =
    GeneratedRequirementDraft(
        templateId = id,
        occurrenceKey = "$contractPublicId|$lineageId|$suffix",
        title = title,
        instructions = instructions,
        dueDate = dueDate,
        notificationLeadDays = notificationLeadDays,
    )
