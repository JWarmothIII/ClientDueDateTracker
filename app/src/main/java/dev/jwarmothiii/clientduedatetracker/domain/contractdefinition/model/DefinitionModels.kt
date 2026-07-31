package dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@JvmInline
value class ContractTypeId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "ContractTypeId cannot be blank." }
    }
}

@JvmInline
value class RequirementTemplateId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "RequirementTemplateId cannot be blank." }
    }
}

enum class DeadlineAnchor {
    INTAKE,
    SCHEDULED_ASSESSMENT,
    ACTUAL_ASSESSMENT_COMPLETION,
    PLANNED_EXIT,
    ACTUAL_EXIT,
    CONTRACT_EVENT,
    LAST_PLAN_COMPLETION,
}

enum class ContractEventKind {
    CLIENT_INTERACTION,
    PLAN_CHANGE,
    TERMINATION_NOTICE,
    BILLING_EVENT,
    OTHER,
}

enum class RecurrencePolicy {
    NONE,
    WEEKLY,
    MONTHLY_FIRST_DAY,
    MONTHLY_END,
    EVERY_90_DAYS,
}

enum class WeekendAdjustment {
    NONE,
    NEXT_WEEKDAY,
}

data class DeadlinePolicy(
    val anchor: DeadlineAnchor,
    val offsetDays: Int = 0,
    val dayNumber: Int? = null,
    val monthOffset: Int? = null,
    val endOfMonth: Boolean = false,
    val recurrence: RecurrencePolicy = RecurrencePolicy.NONE,
    val eventKind: ContractEventKind? = null,
    val weekendAdjustment: WeekendAdjustment = WeekendAdjustment.NONE,
) {
    init {
        require(dayNumber == null || dayNumber > 0) { "Day number must be positive." }
        require(anchor == DeadlineAnchor.CONTRACT_EVENT || eventKind == null) {
            "Event kind requires a contract-event anchor."
        }
        require(anchor != DeadlineAnchor.CONTRACT_EVENT || eventKind != null) {
            "Contract-event anchor requires an event kind."
        }
    }

    fun calculate(anchorDate: LocalDate): LocalDate {
        val base =
            when {
                dayNumber != null -> {
                    anchorDate.plusDays(dayNumber.toLong() - 1)
                }

                monthOffset != null -> {
                    val month = YearMonth.from(anchorDate).plusMonths(monthOffset.toLong())
                    if (endOfMonth) month.atEndOfMonth() else month.atDay(1)
                }

                endOfMonth -> {
                    YearMonth.from(anchorDate).atEndOfMonth()
                }

                else -> {
                    anchorDate.plusDays(offsetDays.toLong())
                }
            }
        return base.adjustForWeekend(weekendAdjustment)
    }
}

fun LocalDate.adjustForWeekend(adjustment: WeekendAdjustment): LocalDate =
    if (adjustment == WeekendAdjustment.NEXT_WEEKDAY) {
        when (dayOfWeek) {
            DayOfWeek.SATURDAY -> plusDays(2)
            DayOfWeek.SUNDAY -> plusDays(1)
            else -> this
        }
    } else {
        this
    }

data class ContractType(
    val id: ContractTypeId,
    val lineageId: String,
    val version: Int,
    val name: String,
    val active: Boolean,
) {
    init {
        require(lineageId.isNotBlank())
        require(version > 0)
        require(name.isNotBlank())
    }
}

data class RequirementTemplate(
    val id: RequirementTemplateId,
    val contractTypeId: ContractTypeId,
    val lineageId: String,
    val version: Int,
    val title: String,
    val instructions: String,
    val policy: DeadlinePolicy,
    val notificationLeadDays: Int,
    val active: Boolean,
) {
    init {
        require(lineageId.isNotBlank())
        require(version > 0)
        require(title.isNotBlank())
        require(notificationLeadDays >= 0)
    }
}

enum class ReplacementScope {
    FUTURE_CLIENTS_ONLY,
    FUTURE_AND_ACTIVE_CLIENTS,
}
