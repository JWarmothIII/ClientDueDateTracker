package dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api

import dev.jwarmothiii.clientduedatetracker.domain.client.model.ClientId
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractEventKind
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractTypeId
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplate
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplateId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.Contract
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractEvent
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractEventId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ExitKind
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.Requirement
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

sealed interface TrackingCommandResult<out T> {
    data class Success<T>(
        val value: T,
    ) : TrackingCommandResult<T>

    data class ValidationError(
        val message: String,
    ) : TrackingCommandResult<Nothing>

    data object NotFound : TrackingCommandResult<Nothing>

    data class Conflict(
        val message: String,
    ) : TrackingCommandResult<Nothing>
}

data class DashboardRequirement(
    val requirement: Requirement,
    val clientInitials: String,
)

data class GenerationContract(
    val contract: Contract,
    val scheduledAssessmentDate: LocalDate,
    val actualAssessmentCompletedDate: LocalDate?,
    val assignments: List<RequirementTemplate>,
    val events: List<ContractEvent>,
    val requirements: List<Requirement>,
)

data class GeneratedRequirementDraft(
    val templateId: RequirementTemplateId,
    val occurrenceKey: String,
    val title: String,
    val instructions: String,
    val dueDate: LocalDate,
    val notificationLeadDays: Int,
)

interface ContractTrackingApi {
    suspend fun createContract(
        clientId: ClientId,
        contractTypeId: ContractTypeId,
        startDate: LocalDate,
        plannedExitDate: LocalDate?,
    ): TrackingCommandResult<Contract>

    suspend fun assignTemplates(
        contractId: ContractId,
        templateIds: List<RequirementTemplateId>,
    ): TrackingCommandResult<Unit>

    suspend fun generationContracts(): List<GenerationContract>

    suspend fun synchronizeGeneratedRequirements(
        contractId: ContractId,
        desired: List<GeneratedRequirementDraft>,
        generatedAt: Instant,
    )

    suspend fun createAdHocRequirement(
        contractId: ContractId,
        title: String,
        instructions: String,
        dueDate: LocalDate,
        notificationLeadDays: Int = 3,
    ): TrackingCommandResult<Requirement>

    suspend fun editRequirement(
        requirementId: RequirementId,
        title: String,
        instructions: String,
        dueDate: LocalDate,
        notificationLeadDays: Int,
    ): TrackingCommandResult<Requirement>

    suspend fun completeRequirement(
        requirementId: RequirementId,
        completedAt: Instant,
    ): TrackingCommandResult<Requirement>

    suspend fun reopenRequirement(requirementId: RequirementId): TrackingCommandResult<Requirement>

    suspend fun permanentlyDeleteRequirement(
        requirementId: RequirementId,
        confirmedAt: Instant,
    ): TrackingCommandResult<Unit>

    suspend fun recordEvent(
        contractId: ContractId,
        kind: ContractEventKind,
        eventDate: LocalDate,
        createdAt: Instant,
    ): TrackingCommandResult<ContractEvent>

    suspend fun updateEvent(
        eventId: ContractEventId,
        eventDate: LocalDate,
    ): TrackingCommandResult<ContractEvent>

    suspend fun deleteEvent(eventId: ContractEventId): TrackingCommandResult<Unit>

    suspend fun confirmExit(
        contractId: ContractId,
        actualExitDate: LocalDate,
        kind: ExitKind,
    ): TrackingCommandResult<Contract>

    suspend fun correctExit(contractId: ContractId): TrackingCommandResult<Contract>

    suspend fun updatePlannedExitForClient(
        clientId: ClientId,
        plannedExitDate: LocalDate?,
    ): TrackingCommandResult<Unit>

    fun observeActiveClientCount(): Flow<Int>

    fun observePendingRequirements(): Flow<List<DashboardRequirement>>

    suspend fun pendingRequirements(): List<DashboardRequirement>

    fun observeRecentlyCompletedRequirements(limit: Int = 5): Flow<List<DashboardRequirement>>
}
