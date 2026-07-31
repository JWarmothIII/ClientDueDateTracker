package dev.jwarmothiii.clientduedatetracker.domain.client.api

import dev.jwarmothiii.clientduedatetracker.domain.client.model.Client
import dev.jwarmothiii.clientduedatetracker.domain.client.model.ClientId
import dev.jwarmothiii.clientduedatetracker.domain.client.model.CreateClientCommand
import java.time.LocalDate

sealed interface ClientCommandResult<out T> {
    data class Success<T>(
        val value: T,
    ) : ClientCommandResult<T>

    data class ValidationError(
        val message: String,
    ) : ClientCommandResult<Nothing>

    data object NotFound : ClientCommandResult<Nothing>
}

interface ClientApi {
    suspend fun create(
        command: CreateClientCommand,
        today: LocalDate,
    ): ClientCommandResult<Client>

    suspend fun find(id: ClientId): Client?

    suspend fun updateDates(
        id: ClientId,
        scheduledAssessmentDate: LocalDate,
        actualAssessmentCompletedDate: LocalDate?,
        plannedExitDate: LocalDate?,
        today: LocalDate,
    ): ClientCommandResult<Client>

    suspend fun permanentlyDelete(id: ClientId): ClientCommandResult<Unit>
}
