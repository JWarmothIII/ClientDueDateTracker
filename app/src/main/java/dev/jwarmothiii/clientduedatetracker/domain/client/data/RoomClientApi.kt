package dev.jwarmothiii.clientduedatetracker.domain.client.data

import dev.jwarmothiii.clientduedatetracker.database.entity.ClientEntity
import dev.jwarmothiii.clientduedatetracker.domain.client.api.ClientApi
import dev.jwarmothiii.clientduedatetracker.domain.client.api.ClientCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.client.model.Client
import dev.jwarmothiii.clientduedatetracker.domain.client.model.ClientId
import dev.jwarmothiii.clientduedatetracker.domain.client.model.CreateClientCommand
import dev.jwarmothiii.clientduedatetracker.domain.client.model.normalizedInitials
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomClientApi
    @Inject
    constructor(
        private val clientDao: ClientDao,
    ) : ClientApi {
        override suspend fun create(
            command: CreateClientCommand,
            today: LocalDate,
        ): ClientCommandResult<Client> {
            val validation = validate(command, today)
            if (validation != null) return ClientCommandResult.ValidationError(validation)
            val entity =
                ClientEntity(
                    publicId = UUID.randomUUID().toString(),
                    initials = command.initials.normalizedInitials(),
                    intakeDate = command.intakeDate.toEpochDay(),
                    scheduledAssessmentDate = command.scheduledAssessmentDate.toEpochDay(),
                    actualAssessmentCompletedDate = command.actualAssessmentCompletedDate?.toEpochDay(),
                    plannedExitDate = command.plannedExitDate?.toEpochDay(),
                )
            val rowId = clientDao.insert(entity)
            return ClientCommandResult.Success(entity.copy(id = rowId).toModel())
        }

        override suspend fun find(id: ClientId): Client? = clientDao.findByPublicId(id.value)?.toModel()

        override suspend fun updateDates(
            id: ClientId,
            scheduledAssessmentDate: LocalDate,
            actualAssessmentCompletedDate: LocalDate?,
            plannedExitDate: LocalDate?,
            today: LocalDate,
        ): ClientCommandResult<Client> {
            val current = clientDao.findByPublicId(id.value) ?: return ClientCommandResult.NotFound
            val command =
                CreateClientCommand(
                    initials = current.initials,
                    intakeDate = LocalDate.ofEpochDay(current.intakeDate),
                    scheduledAssessmentDate = scheduledAssessmentDate,
                    actualAssessmentCompletedDate = actualAssessmentCompletedDate,
                    plannedExitDate = plannedExitDate,
                )
            val validation = validate(command, today)
            if (validation != null) return ClientCommandResult.ValidationError(validation)
            val updated =
                current.copy(
                    scheduledAssessmentDate = scheduledAssessmentDate.toEpochDay(),
                    actualAssessmentCompletedDate = actualAssessmentCompletedDate?.toEpochDay(),
                    plannedExitDate = plannedExitDate?.toEpochDay(),
                )
            clientDao.update(updated)
            return ClientCommandResult.Success(updated.toModel())
        }

        override suspend fun permanentlyDelete(id: ClientId): ClientCommandResult<Unit> =
            if (clientDao.delete(id.value) == 1) {
                ClientCommandResult.Success(Unit)
            } else {
                ClientCommandResult.NotFound
            }

        private fun validate(
            command: CreateClientCommand,
            today: LocalDate,
        ): String? =
            when {
                command.initials.normalizedInitials().isBlank() -> {
                    "Initials are required."
                }

                command.scheduledAssessmentDate.isBefore(command.intakeDate) -> {
                    "Scheduled assessment date cannot precede intake."
                }

                command.actualAssessmentCompletedDate?.isBefore(command.intakeDate) == true -> {
                    "Assessment completion date cannot precede intake."
                }

                command.actualAssessmentCompletedDate?.isAfter(today) == true -> {
                    "Assessment completion date cannot be in the future."
                }

                command.plannedExitDate?.isBefore(command.intakeDate) == true -> {
                    "Planned exit date cannot precede intake."
                }

                else -> {
                    null
                }
            }
    }

internal fun ClientEntity.toModel(): Client =
    Client(
        id = ClientId(publicId),
        initials = initials,
        intakeDate = LocalDate.ofEpochDay(intakeDate),
        scheduledAssessmentDate = LocalDate.ofEpochDay(scheduledAssessmentDate),
        actualAssessmentCompletedDate = actualAssessmentCompletedDate?.let(LocalDate::ofEpochDay),
        plannedExitDate = plannedExitDate?.let(LocalDate::ofEpochDay),
    )
