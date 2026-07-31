package dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.jwarmothiii.clientduedatetracker.database.entity.ContractTypeEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.RequirementTemplateEntity
import kotlinx.coroutines.flow.Flow

data class TemplateWithContractPublicId(
    val id: Long,
    val publicId: String,
    val contractTypeId: Long,
    val contractTypePublicId: String,
    val lineageId: String,
    val version: Int,
    val title: String,
    val instructions: String,
    val deadlinePolicyJson: String,
    val notificationLeadDays: Int,
    val active: Boolean,
)

@Dao
interface DefinitionDao {
    @Insert
    suspend fun insertContractType(entity: ContractTypeEntity): Long

    @Update
    suspend fun updateContractType(entity: ContractTypeEntity)

    @Insert
    suspend fun insertTemplate(entity: RequirementTemplateEntity): Long

    @Update
    suspend fun updateTemplate(entity: RequirementTemplateEntity)

    @Query("SELECT * FROM contract_types WHERE active = 1 ORDER BY name")
    fun observeActiveContractTypes(): Flow<List<ContractTypeEntity>>

    @Query("SELECT * FROM contract_types WHERE active = 1 ORDER BY name")
    suspend fun activeContractTypes(): List<ContractTypeEntity>

    @Query("SELECT * FROM contract_types WHERE public_id = :publicId")
    suspend fun contractType(publicId: String): ContractTypeEntity?

    @Query("SELECT id FROM contract_types WHERE public_id = :publicId")
    suspend fun contractTypeInternalId(publicId: String): Long?

    @Query("SELECT COUNT(*) FROM contract_types")
    suspend fun contractTypeCount(): Int

    @Query("SELECT COALESCE(MAX(version), 0) FROM contract_types WHERE lineage_id = :lineageId")
    suspend fun maximumContractTypeVersion(lineageId: String): Int

    @Query(
        """
        SELECT requirement_templates.id,
               requirement_templates.public_id AS publicId,
               requirement_templates.contract_type_id AS contractTypeId,
               contract_types.public_id AS contractTypePublicId,
               requirement_templates.lineage_id AS lineageId,
               requirement_templates.version,
               requirement_templates.title,
               requirement_templates.instructions,
               requirement_templates.deadline_policy_json AS deadlinePolicyJson,
               requirement_templates.notification_lead_days AS notificationLeadDays,
               requirement_templates.active
        FROM requirement_templates
        JOIN contract_types ON contract_types.id = requirement_templates.contract_type_id
        WHERE contract_types.public_id = :contractTypePublicId
          AND requirement_templates.active = 1
        ORDER BY requirement_templates.title
        """,
    )
    suspend fun activeTemplates(contractTypePublicId: String): List<TemplateWithContractPublicId>

    @Query(
        """
        SELECT requirement_templates.id,
               requirement_templates.public_id AS publicId,
               requirement_templates.contract_type_id AS contractTypeId,
               contract_types.public_id AS contractTypePublicId,
               requirement_templates.lineage_id AS lineageId,
               requirement_templates.version,
               requirement_templates.title,
               requirement_templates.instructions,
               requirement_templates.deadline_policy_json AS deadlinePolicyJson,
               requirement_templates.notification_lead_days AS notificationLeadDays,
               requirement_templates.active
        FROM requirement_templates
        JOIN contract_types ON contract_types.id = requirement_templates.contract_type_id
        WHERE requirement_templates.public_id = :publicId
        """,
    )
    suspend fun template(publicId: String): TemplateWithContractPublicId?

    @Query("SELECT * FROM requirement_templates WHERE public_id = :publicId")
    suspend fun templateEntity(publicId: String): RequirementTemplateEntity?

    @Query("SELECT id FROM requirement_templates WHERE public_id = :publicId")
    suspend fun templateInternalId(publicId: String): Long?

    @Query("SELECT COALESCE(MAX(version), 0) FROM requirement_templates WHERE lineage_id = :lineageId")
    suspend fun maximumTemplateVersion(lineageId: String): Int

    @Query(
        """
        UPDATE contract_definition_assignments
        SET template_id = :replacementTemplateId
        WHERE template_id = :oldTemplateId
          AND contract_id IN (SELECT id FROM contracts WHERE active = 1)
        """,
    )
    suspend fun migrateActiveAssignments(
        oldTemplateId: Long,
        replacementTemplateId: Long,
    )

    @Query("DELETE FROM contract_definition_assignments WHERE template_id = :templateId")
    suspend fun removeAssignments(templateId: Long)

    @Query(
        """
        UPDATE contracts
        SET contract_type_id = :replacementContractTypeId
        WHERE contract_type_id = :oldContractTypeId
          AND active = 1
        """,
    )
    suspend fun migrateActiveContracts(
        oldContractTypeId: Long,
        replacementContractTypeId: Long,
    )
}
