package dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model

import dev.jwarmothiii.clientduedatetracker.domain.client.model.ClientId
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractEventKind
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractTypeId
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplateId
import java.time.Instant
import java.time.LocalDate

@JvmInline
value class ContractId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "ContractId cannot be blank." }
    }
}

@JvmInline
value class ContractEventId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "ContractEventId cannot be blank." }
    }
}

@JvmInline
value class RequirementId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "RequirementId cannot be blank." }
    }
}

enum class RequirementStatus {
    PENDING,
    COMPLETED,
}

enum class RequirementOrigin {
    GENERATED,
    AD_HOC,
}

enum class ExitKind {
    PLANNED,
    UNPLANNED,
}

data class Contract(
    val id: ContractId,
    val clientId: ClientId,
    val contractTypeId: ContractTypeId,
    val startDate: LocalDate,
    val active: Boolean,
    val plannedExitDate: LocalDate?,
    val actualExitDate: LocalDate?,
    val exitKind: ExitKind?,
)

data class ContractEvent(
    val id: ContractEventId,
    val contractId: ContractId,
    val kind: ContractEventKind,
    val eventDate: LocalDate,
    val createdAt: Instant,
)

data class Requirement(
    val id: RequirementId,
    val contractId: ContractId,
    val templateId: RequirementTemplateId?,
    val origin: RequirementOrigin,
    val occurrenceKey: String?,
    val title: String,
    val instructions: String,
    val dueDate: LocalDate,
    val notificationLeadDays: Int,
    val status: RequirementStatus,
    val completedAt: Instant?,
    val manuallyCustomized: Boolean,
) {
    init {
        require(title.isNotBlank())
        require(notificationLeadDays >= 0)
        require(status == RequirementStatus.COMPLETED || completedAt == null) {
            "Pending requirements cannot have a completion timestamp."
        }
        require(status == RequirementStatus.PENDING || completedAt != null) {
            "Completed requirements require a completion timestamp."
        }
    }
}
