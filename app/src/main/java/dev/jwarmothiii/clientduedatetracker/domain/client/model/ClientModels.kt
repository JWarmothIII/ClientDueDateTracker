package dev.jwarmothiii.clientduedatetracker.domain.client.model

import java.time.LocalDate

@JvmInline
value class ClientId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "ClientId cannot be blank." }
    }
}

data class Client(
    val id: ClientId,
    val initials: String,
    val intakeDate: LocalDate,
    val scheduledAssessmentDate: LocalDate,
    val actualAssessmentCompletedDate: LocalDate?,
    val plannedExitDate: LocalDate?,
) {
    init {
        require(initials.isNotBlank()) { "Client initials cannot be blank." }
        require(!scheduledAssessmentDate.isBefore(intakeDate)) {
            "Scheduled assessment date cannot precede intake."
        }
        require(actualAssessmentCompletedDate?.isBefore(intakeDate) != true) {
            "Assessment completion date cannot precede intake."
        }
        require(plannedExitDate?.isBefore(intakeDate) != true) {
            "Planned exit date cannot precede intake."
        }
    }
}

data class CreateClientCommand(
    val initials: String,
    val intakeDate: LocalDate,
    val scheduledAssessmentDate: LocalDate,
    val actualAssessmentCompletedDate: LocalDate? = null,
    val plannedExitDate: LocalDate? = null,
)

fun String.normalizedInitials(): String = trim().uppercase()
