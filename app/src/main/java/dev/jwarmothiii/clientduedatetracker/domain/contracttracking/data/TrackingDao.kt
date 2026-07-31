package dev.jwarmothiii.clientduedatetracker.domain.contracttracking.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.jwarmothiii.clientduedatetracker.database.entity.ContractDefinitionAssignmentEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.ContractEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.ContractEventEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.RequirementEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.RequirementSuppressionEntity
import kotlinx.coroutines.flow.Flow

data class ContractWithPublicIds(
    val id: Long,
    val publicId: String,
    val clientPublicId: String,
    val contractTypePublicId: String,
    val startDate: Long,
    val active: Boolean,
    val plannedExitDate: Long?,
    val actualExitDate: Long?,
    val exitKind: String?,
    val scheduledAssessmentDate: Long,
    val actualAssessmentCompletedDate: Long?,
)

data class RequirementWithPublicIds(
    val id: Long,
    val publicId: String,
    val contractPublicId: String,
    val templatePublicId: String?,
    val origin: String,
    val occurrenceKey: String?,
    val title: String,
    val instructions: String,
    val dueDate: Long,
    val notificationLeadDays: Int,
    val status: String,
    val completedAt: Long?,
    val manuallyCustomized: Boolean,
    val clientInitials: String,
)

data class EventWithContractPublicId(
    val id: Long,
    val publicId: String,
    val contractPublicId: String,
    val kind: String,
    val eventDate: Long,
    val createdAt: Long,
)

data class AssignedTemplateRow(
    val publicId: String,
)

@Dao
interface TrackingDao {
    @Query("SELECT id FROM clients WHERE public_id = :publicId")
    suspend fun clientInternalId(publicId: String): Long?

    @Query("SELECT id FROM contract_types WHERE public_id = :publicId")
    suspend fun contractTypeInternalId(publicId: String): Long?

    @Query("SELECT id FROM requirement_templates WHERE public_id = :publicId")
    suspend fun templateInternalId(publicId: String): Long?

    @Insert
    suspend fun insertContract(entity: ContractEntity): Long

    @Update
    suspend fun updateContract(entity: ContractEntity)

    @Query("SELECT * FROM contracts WHERE public_id = :publicId")
    suspend fun contractEntity(publicId: String): ContractEntity?

    @Query("SELECT * FROM contracts WHERE id = :internalId")
    suspend fun contractEntityByInternalId(internalId: Long): ContractEntity?

    @Query("SELECT id FROM contracts WHERE public_id = :publicId")
    suspend fun contractInternalId(publicId: String): Long?

    @Query(
        """
        UPDATE contracts
        SET planned_exit_date = :plannedExitDate
        WHERE client_id = (SELECT id FROM clients WHERE public_id = :clientPublicId)
        """,
    )
    suspend fun updatePlannedExitForClient(
        clientPublicId: String,
        plannedExitDate: Long?,
    ): Int

    @Query(
        """
        SELECT contracts.id,
               contracts.public_id AS publicId,
               clients.public_id AS clientPublicId,
               contract_types.public_id AS contractTypePublicId,
               contracts.start_date AS startDate,
               contracts.active,
               contracts.planned_exit_date AS plannedExitDate,
               contracts.actual_exit_date AS actualExitDate,
               contracts.exit_kind AS exitKind,
               clients.scheduled_assessment_date AS scheduledAssessmentDate,
               clients.actual_assessment_completed_date AS actualAssessmentCompletedDate
        FROM contracts
        JOIN clients ON clients.id = contracts.client_id
        JOIN contract_types ON contract_types.id = contracts.contract_type_id
        WHERE contracts.public_id = :publicId
        """,
    )
    suspend fun contract(publicId: String): ContractWithPublicIds?

    @Query(
        """
        SELECT contracts.id,
               contracts.public_id AS publicId,
               clients.public_id AS clientPublicId,
               contract_types.public_id AS contractTypePublicId,
               contracts.start_date AS startDate,
               contracts.active,
               contracts.planned_exit_date AS plannedExitDate,
               contracts.actual_exit_date AS actualExitDate,
               contracts.exit_kind AS exitKind,
               clients.scheduled_assessment_date AS scheduledAssessmentDate,
               clients.actual_assessment_completed_date AS actualAssessmentCompletedDate
        FROM contracts
        JOIN clients ON clients.id = contracts.client_id
        JOIN contract_types ON contract_types.id = contracts.contract_type_id
        WHERE contracts.active = 1 OR contracts.actual_exit_date IS NOT NULL
        ORDER BY contracts.id
        """,
    )
    suspend fun activeContracts(): List<ContractWithPublicIds>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAssignment(entity: ContractDefinitionAssignmentEntity): Long

    @Query(
        """
        SELECT requirement_templates.public_id AS publicId
        FROM contract_definition_assignments
        JOIN requirement_templates ON requirement_templates.id = contract_definition_assignments.template_id
        WHERE contract_definition_assignments.contract_id = :contractId
        ORDER BY requirement_templates.id
        """,
    )
    suspend fun assignedTemplates(contractId: Long): List<AssignedTemplateRow>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRequirement(entity: RequirementEntity): Long

    @Update
    suspend fun updateRequirement(entity: RequirementEntity)

    @Query("SELECT * FROM requirements WHERE public_id = :publicId")
    suspend fun requirementEntity(publicId: String): RequirementEntity?

    @Query("SELECT * FROM requirements WHERE occurrence_key = :occurrenceKey")
    suspend fun requirementByOccurrenceKey(occurrenceKey: String): RequirementEntity?

    @Query("SELECT id FROM requirements WHERE public_id = :publicId")
    suspend fun requirementInternalId(publicId: String): Long?

    @Query(
        """
        SELECT requirements.id,
               requirements.public_id AS publicId,
               contracts.public_id AS contractPublicId,
               requirement_templates.public_id AS templatePublicId,
               requirements.origin,
               requirements.occurrence_key AS occurrenceKey,
               requirements.title,
               requirements.instructions,
               requirements.due_date AS dueDate,
               requirements.notification_lead_days AS notificationLeadDays,
               requirements.status,
               requirements.completed_at AS completedAt,
               requirements.manually_customized AS manuallyCustomized,
               clients.initials AS clientInitials
        FROM requirements
        JOIN contracts ON contracts.id = requirements.contract_id
        JOIN clients ON clients.id = contracts.client_id
        LEFT JOIN requirement_templates ON requirement_templates.id = requirements.template_id
        WHERE contracts.id = :contractId
        ORDER BY requirements.due_date
        """,
    )
    suspend fun requirementsForContract(contractId: Long): List<RequirementWithPublicIds>

    @Query(
        """
        SELECT requirements.id,
               requirements.public_id AS publicId,
               contracts.public_id AS contractPublicId,
               requirement_templates.public_id AS templatePublicId,
               requirements.origin,
               requirements.occurrence_key AS occurrenceKey,
               requirements.title,
               requirements.instructions,
               requirements.due_date AS dueDate,
               requirements.notification_lead_days AS notificationLeadDays,
               requirements.status,
               requirements.completed_at AS completedAt,
               requirements.manually_customized AS manuallyCustomized,
               clients.initials AS clientInitials
        FROM requirements
        JOIN contracts ON contracts.id = requirements.contract_id
        JOIN clients ON clients.id = contracts.client_id
        LEFT JOIN requirement_templates ON requirement_templates.id = requirements.template_id
        WHERE requirements.status = 'PENDING'
        ORDER BY requirements.due_date, requirements.id
        """,
    )
    fun observePendingRequirements(): Flow<List<RequirementWithPublicIds>>

    @Query(
        """
        SELECT requirements.id,
               requirements.public_id AS publicId,
               contracts.public_id AS contractPublicId,
               requirement_templates.public_id AS templatePublicId,
               requirements.origin,
               requirements.occurrence_key AS occurrenceKey,
               requirements.title,
               requirements.instructions,
               requirements.due_date AS dueDate,
               requirements.notification_lead_days AS notificationLeadDays,
               requirements.status,
               requirements.completed_at AS completedAt,
               requirements.manually_customized AS manuallyCustomized,
               clients.initials AS clientInitials
        FROM requirements
        JOIN contracts ON contracts.id = requirements.contract_id
        JOIN clients ON clients.id = contracts.client_id
        LEFT JOIN requirement_templates ON requirement_templates.id = requirements.template_id
        WHERE requirements.status = 'PENDING'
        ORDER BY requirements.due_date, requirements.id
        """,
    )
    suspend fun pendingRequirements(): List<RequirementWithPublicIds>

    @Query(
        """
        SELECT requirements.id,
               requirements.public_id AS publicId,
               contracts.public_id AS contractPublicId,
               requirement_templates.public_id AS templatePublicId,
               requirements.origin,
               requirements.occurrence_key AS occurrenceKey,
               requirements.title,
               requirements.instructions,
               requirements.due_date AS dueDate,
               requirements.notification_lead_days AS notificationLeadDays,
               requirements.status,
               requirements.completed_at AS completedAt,
               requirements.manually_customized AS manuallyCustomized,
               clients.initials AS clientInitials
        FROM requirements
        JOIN contracts ON contracts.id = requirements.contract_id
        JOIN clients ON clients.id = contracts.client_id
        LEFT JOIN requirement_templates ON requirement_templates.id = requirements.template_id
        WHERE requirements.status = 'COMPLETED'
        ORDER BY requirements.completed_at DESC
        LIMIT :limit
        """,
    )
    fun observeRecentlyCompletedRequirements(limit: Int): Flow<List<RequirementWithPublicIds>>

    @Query("SELECT COUNT(*) FROM contracts WHERE active = 1")
    fun observeActiveClientCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM contracts")
    suspend fun contractCount(): Int

    @Query("SELECT COUNT(*) FROM requirements")
    suspend fun requirementCount(): Int

    @Query(
        """
        DELETE FROM requirements
        WHERE contract_id = :contractId
          AND origin = 'GENERATED'
          AND status = 'PENDING'
          AND manually_customized = 0
          AND template_id IN (
              SELECT template_id
              FROM contract_definition_assignments
              WHERE contract_definition_assignments.contract_id = :contractId
          )
          AND occurrence_key NOT IN (:desiredKeys)
        """,
    )
    suspend fun deleteObsoleteGeneratedRequirements(
        contractId: Long,
        desiredKeys: List<String>,
    )

    @Query(
        """
        DELETE FROM requirements
        WHERE contract_id = :contractId
          AND origin = 'GENERATED'
          AND status = 'PENDING'
          AND manually_customized = 0
          AND template_id IN (
              SELECT template_id
              FROM contract_definition_assignments
              WHERE contract_definition_assignments.contract_id = :contractId
          )
        """,
    )
    suspend fun deleteAllPendingGeneratedRequirements(contractId: Long)

    @Query("SELECT occurrence_key FROM requirement_suppressions WHERE contract_id = :contractId")
    suspend fun suppressedOccurrenceKeys(contractId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSuppression(entity: RequirementSuppressionEntity)

    @Query("DELETE FROM requirements WHERE public_id = :publicId")
    suspend fun deleteRequirement(publicId: String): Int

    @Insert
    suspend fun insertEvent(entity: ContractEventEntity): Long

    @Update
    suspend fun updateEvent(entity: ContractEventEntity)

    @Query("SELECT * FROM contract_events WHERE public_id = :publicId")
    suspend fun eventEntity(publicId: String): ContractEventEntity?

    @Query("DELETE FROM contract_events WHERE public_id = :publicId")
    suspend fun deleteEvent(publicId: String): Int

    @Query(
        """
        SELECT contract_events.id,
               contract_events.public_id AS publicId,
               contracts.public_id AS contractPublicId,
               contract_events.kind,
               contract_events.event_date AS eventDate,
               contract_events.created_at AS createdAt
        FROM contract_events
        JOIN contracts ON contracts.id = contract_events.contract_id
        WHERE contract_events.contract_id = :contractId
        ORDER BY contract_events.event_date
        """,
    )
    suspend fun eventsForContract(contractId: Long): List<EventWithContractPublicId>
}
