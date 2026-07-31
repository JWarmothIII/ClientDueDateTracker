package dev.jwarmothiii.clientduedatetracker.domain.contracttracking.data

import androidx.room.withTransaction
import dev.jwarmothiii.clientduedatetracker.database.ClientDueDateDatabase
import dev.jwarmothiii.clientduedatetracker.database.entity.ContractDefinitionAssignmentEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.ContractEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.ContractEventEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.RequirementEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.RequirementSuppressionEntity
import dev.jwarmothiii.clientduedatetracker.domain.client.model.ClientId
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.ContractDefinitionApi
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractEventKind
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractTypeId
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplateId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.ContractTrackingApi
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.DashboardRequirement
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.GeneratedRequirementDraft
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.GenerationContract
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.TrackingCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.Contract
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractEvent
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractEventId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ExitKind
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.Requirement
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementOrigin
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomContractTrackingApi
    @Inject
    constructor(
        private val database: ClientDueDateDatabase,
        private val definitionApi: ContractDefinitionApi,
        private val trackingDao: TrackingDao,
    ) : ContractTrackingApi {
        override suspend fun createContract(
            clientId: ClientId,
            contractTypeId: ContractTypeId,
            startDate: LocalDate,
            plannedExitDate: LocalDate?,
        ): TrackingCommandResult<Contract> {
            if (plannedExitDate?.isBefore(startDate) == true) {
                return TrackingCommandResult.ValidationError("Planned exit cannot precede intake.")
            }
            val clientInternalId = trackingDao.clientInternalId(clientId.value) ?: return TrackingCommandResult.NotFound
            val typeInternalId =
                trackingDao.contractTypeInternalId(contractTypeId.value)
                    ?: return TrackingCommandResult.NotFound
            val entity =
                ContractEntity(
                    publicId = UUID.randomUUID().toString(),
                    clientId = clientInternalId,
                    contractTypeId = typeInternalId,
                    startDate = startDate.toEpochDay(),
                    active = true,
                    plannedExitDate = plannedExitDate?.toEpochDay(),
                    actualExitDate = null,
                    exitKind = null,
                )
            return try {
                val id = trackingDao.insertContract(entity)
                TrackingCommandResult.Success(entity.copy(id = id).toModel(clientId, contractTypeId))
            } catch (_: android.database.sqlite.SQLiteConstraintException) {
                TrackingCommandResult.Conflict("Client already has a contract.")
            }
        }

        override suspend fun assignTemplates(
            contractId: ContractId,
            templateIds: List<RequirementTemplateId>,
        ): TrackingCommandResult<Unit> =
            database.withTransaction {
                val contractInternalId =
                    trackingDao.contractInternalId(contractId.value)
                        ?: return@withTransaction TrackingCommandResult.NotFound
                for (templateId in templateIds.distinct()) {
                    val templateInternalId =
                        trackingDao.templateInternalId(templateId.value)
                            ?: return@withTransaction TrackingCommandResult.NotFound
                    trackingDao.insertAssignment(
                        ContractDefinitionAssignmentEntity(contractInternalId, templateInternalId),
                    )
                }
                TrackingCommandResult.Success(Unit)
            }

        override suspend fun generationContracts(): List<GenerationContract> =
            trackingDao.activeContracts().map { row ->
                val assignments =
                    trackingDao
                        .assignedTemplates(row.id)
                        .mapNotNull { definitionApi.template(RequirementTemplateId(it.publicId)) }
                val events = trackingDao.eventsForContract(row.id).map(EventWithContractPublicId::toModel)
                val requirements =
                    trackingDao.requirementsForContract(row.id).map(RequirementWithPublicIds::toRequirement)
                GenerationContract(
                    contract = row.toContract(),
                    scheduledAssessmentDate = LocalDate.ofEpochDay(row.scheduledAssessmentDate),
                    actualAssessmentCompletedDate = row.actualAssessmentCompletedDate?.let(LocalDate::ofEpochDay),
                    assignments = assignments,
                    events = events,
                    requirements = requirements,
                )
            }

        override suspend fun synchronizeGeneratedRequirements(
            contractId: ContractId,
            desired: List<GeneratedRequirementDraft>,
            generatedAt: Instant,
        ) {
            database.withTransaction {
                val contractInternalId = trackingDao.contractInternalId(contractId.value) ?: return@withTransaction
                val suppressed = trackingDao.suppressedOccurrenceKeys(contractInternalId).toSet()
                val activeDesired = desired.filterNot { it.occurrenceKey in suppressed }
                activeDesired.forEach { draft ->
                    val templateInternalId =
                        trackingDao.templateInternalId(draft.templateId.value)
                            ?: return@forEach
                    val current = trackingDao.requirementByOccurrenceKey(draft.occurrenceKey)
                    if (current == null) {
                        trackingDao.insertRequirement(
                            draft.toEntity(contractInternalId, templateInternalId),
                        )
                    } else if (
                        current.status == RequirementStatus.PENDING.name &&
                        !current.manuallyCustomized &&
                        current.templateId == templateInternalId
                    ) {
                        trackingDao.updateRequirement(
                            current.copy(
                                title = draft.title,
                                instructions = draft.instructions,
                                dueDate = draft.dueDate.toEpochDay(),
                                notificationLeadDays = draft.notificationLeadDays,
                            ),
                        )
                    }
                }
                val desiredKeys = activeDesired.map { it.occurrenceKey }
                if (desiredKeys.isEmpty()) {
                    trackingDao.deleteAllPendingGeneratedRequirements(contractInternalId)
                } else {
                    trackingDao.deleteObsoleteGeneratedRequirements(contractInternalId, desiredKeys)
                }
            }
        }

        override suspend fun createAdHocRequirement(
            contractId: ContractId,
            title: String,
            instructions: String,
            dueDate: LocalDate,
            notificationLeadDays: Int,
        ): TrackingCommandResult<Requirement> {
            val validation = validateRequirement(title, notificationLeadDays)
            if (validation != null) return TrackingCommandResult.ValidationError(validation)
            val contractInternalId =
                trackingDao.contractInternalId(contractId.value)
                    ?: return TrackingCommandResult.NotFound
            val entity =
                RequirementEntity(
                    publicId = UUID.randomUUID().toString(),
                    contractId = contractInternalId,
                    templateId = null,
                    origin = RequirementOrigin.AD_HOC.name,
                    occurrenceKey = null,
                    title = title.trim(),
                    instructions = instructions.trim(),
                    dueDate = dueDate.toEpochDay(),
                    notificationLeadDays = notificationLeadDays,
                    status = RequirementStatus.PENDING.name,
                    completedAt = null,
                    manuallyCustomized = true,
                )
            val id = trackingDao.insertRequirement(entity)
            return TrackingCommandResult.Success(
                entity.copy(id = id).toRequirement(contractId, null),
            )
        }

        override suspend fun editRequirement(
            requirementId: RequirementId,
            title: String,
            instructions: String,
            dueDate: LocalDate,
            notificationLeadDays: Int,
        ): TrackingCommandResult<Requirement> {
            val validation = validateRequirement(title, notificationLeadDays)
            if (validation != null) return TrackingCommandResult.ValidationError(validation)
            val current =
                trackingDao.requirementEntity(requirementId.value)
                    ?: return TrackingCommandResult.NotFound
            val updated =
                current.copy(
                    title = title.trim(),
                    instructions = instructions.trim(),
                    dueDate = dueDate.toEpochDay(),
                    notificationLeadDays = notificationLeadDays,
                    manuallyCustomized = true,
                )
            trackingDao.updateRequirement(updated)
            return TrackingCommandResult.Success(updated.toRequirementWithLookups())
        }

        override suspend fun completeRequirement(
            requirementId: RequirementId,
            completedAt: Instant,
        ): TrackingCommandResult<Requirement> = updateStatus(requirementId, RequirementStatus.COMPLETED, completedAt)

        override suspend fun reopenRequirement(requirementId: RequirementId): TrackingCommandResult<Requirement> =
            updateStatus(requirementId, RequirementStatus.PENDING, null)

        override suspend fun permanentlyDeleteRequirement(
            requirementId: RequirementId,
            confirmedAt: Instant,
        ): TrackingCommandResult<Unit> =
            database.withTransaction {
                val current =
                    trackingDao.requirementEntity(requirementId.value)
                        ?: return@withTransaction TrackingCommandResult.NotFound
                current.occurrenceKey?.let {
                    trackingDao.insertSuppression(
                        RequirementSuppressionEntity(
                            contractId = current.contractId,
                            occurrenceKey = it,
                            createdAt = confirmedAt.toEpochMilli(),
                        ),
                    )
                }
                trackingDao.deleteRequirement(requirementId.value)
                TrackingCommandResult.Success(Unit)
            }

        override suspend fun recordEvent(
            contractId: ContractId,
            kind: ContractEventKind,
            eventDate: LocalDate,
            createdAt: Instant,
        ): TrackingCommandResult<ContractEvent> {
            val contractInternalId =
                trackingDao.contractInternalId(contractId.value)
                    ?: return TrackingCommandResult.NotFound
            val entity =
                ContractEventEntity(
                    publicId = UUID.randomUUID().toString(),
                    contractId = contractInternalId,
                    kind = kind.name,
                    eventDate = eventDate.toEpochDay(),
                    createdAt = createdAt.toEpochMilli(),
                )
            val id = trackingDao.insertEvent(entity)
            return TrackingCommandResult.Success(entity.copy(id = id).toModel(contractId))
        }

        override suspend fun updateEvent(
            eventId: ContractEventId,
            eventDate: LocalDate,
        ): TrackingCommandResult<ContractEvent> {
            val current = trackingDao.eventEntity(eventId.value) ?: return TrackingCommandResult.NotFound
            val updated = current.copy(eventDate = eventDate.toEpochDay())
            trackingDao.updateEvent(updated)
            val contractPublicId =
                trackingDao.activeContracts().firstOrNull { it.id == current.contractId }?.publicId
                    ?: trackingDao
                        .contractEntityByInternalId(current.contractId)
                        ?.publicId
                    ?: return TrackingCommandResult.NotFound
            return TrackingCommandResult.Success(updated.toModel(ContractId(contractPublicId)))
        }

        override suspend fun deleteEvent(eventId: ContractEventId): TrackingCommandResult<Unit> =
            if (trackingDao.deleteEvent(eventId.value) == 1) {
                TrackingCommandResult.Success(Unit)
            } else {
                TrackingCommandResult.NotFound
            }

        override suspend fun confirmExit(
            contractId: ContractId,
            actualExitDate: LocalDate,
            kind: ExitKind,
        ): TrackingCommandResult<Contract> {
            val current = trackingDao.contractEntity(contractId.value) ?: return TrackingCommandResult.NotFound
            if (actualExitDate.isBefore(LocalDate.ofEpochDay(current.startDate))) {
                return TrackingCommandResult.ValidationError("Actual exit cannot precede intake.")
            }
            val updated =
                current.copy(
                    active = false,
                    actualExitDate = actualExitDate.toEpochDay(),
                    exitKind = kind.name,
                )
            trackingDao.updateContract(updated)
            return TrackingCommandResult.Success(updated.toModelWithLookups())
        }

        override suspend fun correctExit(contractId: ContractId): TrackingCommandResult<Contract> {
            val current = trackingDao.contractEntity(contractId.value) ?: return TrackingCommandResult.NotFound
            val updated = current.copy(active = true, actualExitDate = null, exitKind = null)
            trackingDao.updateContract(updated)
            return TrackingCommandResult.Success(updated.toModelWithLookups())
        }

        override suspend fun updatePlannedExitForClient(
            clientId: ClientId,
            plannedExitDate: LocalDate?,
        ): TrackingCommandResult<Unit> =
            if (trackingDao.updatePlannedExitForClient(clientId.value, plannedExitDate?.toEpochDay()) == 1) {
                TrackingCommandResult.Success(Unit)
            } else {
                TrackingCommandResult.NotFound
            }

        override fun observeActiveClientCount(): Flow<Int> = trackingDao.observeActiveClientCount()

        override fun observePendingRequirements(): Flow<List<DashboardRequirement>> =
            trackingDao.observePendingRequirements().map { rows -> rows.map(RequirementWithPublicIds::toDashboard) }

        override suspend fun pendingRequirements(): List<DashboardRequirement> =
            trackingDao.pendingRequirements().map(RequirementWithPublicIds::toDashboard)

        override fun observeRecentlyCompletedRequirements(limit: Int): Flow<List<DashboardRequirement>> =
            trackingDao
                .observeRecentlyCompletedRequirements(limit)
                .map { rows -> rows.map(RequirementWithPublicIds::toDashboard) }

        private suspend fun updateStatus(
            requirementId: RequirementId,
            status: RequirementStatus,
            completedAt: Instant?,
        ): TrackingCommandResult<Requirement> {
            val current =
                trackingDao.requirementEntity(requirementId.value)
                    ?: return TrackingCommandResult.NotFound
            val updated =
                current.copy(
                    status = status.name,
                    completedAt = completedAt?.toEpochMilli(),
                )
            trackingDao.updateRequirement(updated)
            return TrackingCommandResult.Success(updated.toRequirementWithLookups())
        }

        private suspend fun RequirementEntity.toRequirementWithLookups(): Requirement {
            val row =
                trackingDao
                    .requirementsForContract(contractId)
                    .first { it.id == id }
            return row.toRequirement()
        }

        private suspend fun ContractEntity.toModelWithLookups(): Contract {
            val row = trackingDao.contract(publicId) ?: error("Missing contract relations.")
            return row.toContract()
        }

        private fun validateRequirement(
            title: String,
            notificationLeadDays: Int,
        ): String? =
            when {
                title.isBlank() -> "Requirement title is required."
                notificationLeadDays < 0 -> "Notification lead cannot be negative."
                else -> null
            }
    }

private fun GeneratedRequirementDraft.toEntity(
    contractInternalId: Long,
    templateInternalId: Long,
): RequirementEntity =
    RequirementEntity(
        publicId = UUID.randomUUID().toString(),
        contractId = contractInternalId,
        templateId = templateInternalId,
        origin = RequirementOrigin.GENERATED.name,
        occurrenceKey = occurrenceKey,
        title = title,
        instructions = instructions,
        dueDate = dueDate.toEpochDay(),
        notificationLeadDays = notificationLeadDays,
        status = RequirementStatus.PENDING.name,
        completedAt = null,
        manuallyCustomized = false,
    )

private fun ContractEntity.toModel(
    clientId: ClientId,
    contractTypeId: ContractTypeId,
): Contract =
    Contract(
        id = ContractId(publicId),
        clientId = clientId,
        contractTypeId = contractTypeId,
        startDate = LocalDate.ofEpochDay(startDate),
        active = active,
        plannedExitDate = plannedExitDate?.let(LocalDate::ofEpochDay),
        actualExitDate = actualExitDate?.let(LocalDate::ofEpochDay),
        exitKind = exitKind?.let(ExitKind::valueOf),
    )

private fun ContractWithPublicIds.toContract(): Contract =
    Contract(
        id = ContractId(publicId),
        clientId = ClientId(clientPublicId),
        contractTypeId = ContractTypeId(contractTypePublicId),
        startDate = LocalDate.ofEpochDay(startDate),
        active = active,
        plannedExitDate = plannedExitDate?.let(LocalDate::ofEpochDay),
        actualExitDate = actualExitDate?.let(LocalDate::ofEpochDay),
        exitKind = exitKind?.let(ExitKind::valueOf),
    )

private fun RequirementEntity.toRequirement(
    contractId: ContractId,
    templateId: RequirementTemplateId?,
): Requirement =
    Requirement(
        id = RequirementId(publicId),
        contractId = contractId,
        templateId = templateId,
        origin = RequirementOrigin.valueOf(origin),
        occurrenceKey = occurrenceKey,
        title = title,
        instructions = instructions,
        dueDate = LocalDate.ofEpochDay(dueDate),
        notificationLeadDays = notificationLeadDays,
        status = RequirementStatus.valueOf(status),
        completedAt = completedAt?.let(Instant::ofEpochMilli),
        manuallyCustomized = manuallyCustomized,
    )

private fun RequirementWithPublicIds.toRequirement(): Requirement =
    Requirement(
        id = RequirementId(publicId),
        contractId = ContractId(contractPublicId),
        templateId = templatePublicId?.let(::RequirementTemplateId),
        origin = RequirementOrigin.valueOf(origin),
        occurrenceKey = occurrenceKey,
        title = title,
        instructions = instructions,
        dueDate = LocalDate.ofEpochDay(dueDate),
        notificationLeadDays = notificationLeadDays,
        status = RequirementStatus.valueOf(status),
        completedAt = completedAt?.let(Instant::ofEpochMilli),
        manuallyCustomized = manuallyCustomized,
    )

private fun RequirementWithPublicIds.toDashboard(): DashboardRequirement = DashboardRequirement(toRequirement(), clientInitials)

private fun EventWithContractPublicId.toModel(): ContractEvent =
    ContractEvent(
        id = ContractEventId(publicId),
        contractId = ContractId(contractPublicId),
        kind = ContractEventKind.valueOf(kind),
        eventDate = LocalDate.ofEpochDay(eventDate),
        createdAt = Instant.ofEpochMilli(createdAt),
    )

private fun ContractEventEntity.toModel(contractId: ContractId): ContractEvent =
    ContractEvent(
        id = ContractEventId(publicId),
        contractId = contractId,
        kind = ContractEventKind.valueOf(kind),
        eventDate = LocalDate.ofEpochDay(eventDate),
        createdAt = Instant.ofEpochMilli(createdAt),
    )
