package dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.data

import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.TemplateDraft
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractEventKind
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.DeadlineAnchor
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.DeadlinePolicy
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RecurrencePolicy
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.WeekendAdjustment

internal data class ContractTypeSeed(
    val code: String,
    val name: String,
    val templates: List<TemplateDraft>,
)

internal object SeedDefinitions {
    val all =
        listOf(
            ContractTypeSeed(
                code = "cts",
                name = "CTS",
                templates =
                    listOf(
                        draft(
                            "Intake paperwork",
                            "Complete the three SOW-specific intake forms.",
                            DeadlinePolicy(DeadlineAnchor.INTAKE, offsetDays = 10),
                        ),
                        draft(
                            "Assessment",
                            "Complete the assessment.",
                            DeadlinePolicy(DeadlineAnchor.INTAKE, offsetDays = 14),
                        ),
                        draft(
                            "Assessment write-up and submission",
                            "Write the completed assessment and submit it for approval.",
                            DeadlinePolicy(DeadlineAnchor.ACTUAL_ASSESSMENT_COMPLETION, offsetDays = 7),
                        ),
                        draft(
                            "Original treatment plan",
                            "Complete the original treatment plan by the second billing cycle.",
                            DeadlinePolicy(DeadlineAnchor.INTAKE, monthOffset = 1, endOfMonth = true),
                        ),
                        draft(
                            "Treatment plan review",
                            "Complete the 90-day treatment plan review.",
                            DeadlinePolicy(
                                DeadlineAnchor.LAST_PLAN_COMPLETION,
                                offsetDays = 90,
                                recurrence = RecurrencePolicy.EVERY_90_DAYS,
                            ),
                        ),
                        draft(
                            "Monthly progress report",
                            "Complete the report and submit billing to Megan.",
                            DeadlinePolicy(
                                DeadlineAnchor.INTAKE,
                                recurrence = RecurrencePolicy.MONTHLY_END,
                                weekendAdjustment = WeekendAdjustment.NEXT_WEEKDAY,
                            ),
                        ),
                        draft(
                            "Planned discharge summary",
                            "Complete the termination or discharge summary.",
                            DeadlinePolicy(DeadlineAnchor.PLANNED_EXIT, offsetDays = -14),
                        ),
                        draft(
                            "Unplanned discharge summary",
                            "Complete the termination or discharge summary.",
                            DeadlinePolicy(DeadlineAnchor.ACTUAL_EXIT, offsetDays = 14),
                        ),
                        draft(
                            "Case note",
                            "Complete the case note after seeing the client.",
                            eventPolicy(ContractEventKind.CLIENT_INTERACTION, 7),
                        ),
                    ),
            ),
            ContractTypeSeed(
                code = "uspo",
                name = "USPO",
                templates =
                    listOf(
                        draft(
                            "Intake paperwork",
                            "For in-house clients, complete intake paperwork before assessment.",
                            DeadlinePolicy(DeadlineAnchor.SCHEDULED_ASSESSMENT, offsetDays = -1),
                        ),
                        draft(
                            "Assessment",
                            "Complete the assessment.",
                            DeadlinePolicy(DeadlineAnchor.SCHEDULED_ASSESSMENT, offsetDays = 10),
                        ),
                        draft(
                            "Original treatment plan",
                            "Complete by month-end and submit with billing.",
                            DeadlinePolicy(DeadlineAnchor.ACTUAL_ASSESSMENT_COMPLETION, endOfMonth = true),
                        ),
                        draft(
                            "Treatment plan review",
                            "Complete the 90-day review and submit it with billing.",
                            DeadlinePolicy(
                                DeadlineAnchor.LAST_PLAN_COMPLETION,
                                offsetDays = 90,
                                recurrence = RecurrencePolicy.EVERY_90_DAYS,
                            ),
                        ),
                        draft(
                            "Monthly progress report",
                            "Complete by end of day and submit with billing.",
                            DeadlinePolicy(
                                DeadlineAnchor.INTAKE,
                                recurrence = RecurrencePolicy.MONTHLY_FIRST_DAY,
                                weekendAdjustment = WeekendAdjustment.NEXT_WEEKDAY,
                            ),
                        ),
                        draft(
                            "Termination Prob 45",
                            "Send to the agent within one week and submit with billing.",
                            eventPolicy(ContractEventKind.TERMINATION_NOTICE, 7),
                        ),
                        draft(
                            "Case note",
                            "Complete the case note in InSync.",
                            eventPolicy(ContractEventKind.CLIENT_INTERACTION, 7),
                        ),
                    ),
            ),
            ContractTypeSeed(
                code = "state-rsud",
                name = "State / RSUD",
                templates =
                    listOf(
                        draft(
                            "Intake paperwork",
                            "Complete intake paperwork before assessment.",
                            DeadlinePolicy(DeadlineAnchor.SCHEDULED_ASSESSMENT, offsetDays = -1),
                        ),
                        draft(
                            "Assessment",
                            "Complete ASAM and BIO with signatures.",
                            DeadlinePolicy(DeadlineAnchor.INTAKE, dayNumber = 2),
                        ),
                        draft(
                            "Initial treatment plan",
                            "Complete the treatment plan.",
                            DeadlinePolicy(DeadlineAnchor.INTAKE, dayNumber = 5),
                        ),
                        draft(
                            "First PRT action",
                            "Bring the initial treatment plan to PRT.",
                            DeadlinePolicy(DeadlineAnchor.INTAKE, dayNumber = 8),
                        ),
                        draft(
                            "Treatment plan review",
                            "Complete the weekly review and bring it to PRT.",
                            DeadlinePolicy(
                                DeadlineAnchor.INTAKE,
                                dayNumber = 8,
                                recurrence = RecurrencePolicy.WEEKLY,
                            ),
                        ),
                        draft(
                            "Discharge summary",
                            "Email the probation officer and include the success plan.",
                            DeadlinePolicy(DeadlineAnchor.ACTUAL_EXIT, offsetDays = 3),
                        ),
                        draft(
                            "Case note",
                            "Complete the case note in InSync.",
                            eventPolicy(ContractEventKind.CLIENT_INTERACTION, 7),
                        ),
                    ),
            ),
        )

    private fun draft(
        title: String,
        instructions: String,
        policy: DeadlinePolicy,
    ) = TemplateDraft(title, instructions, policy)

    private fun eventPolicy(
        kind: ContractEventKind,
        offsetDays: Int,
    ) = DeadlinePolicy(
        anchor = DeadlineAnchor.CONTRACT_EVENT,
        offsetDays = offsetDays,
        eventKind = kind,
    )
}
