package dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.data

import androidx.room.withTransaction
import dev.jwarmothiii.clientduedatetracker.database.ClientDueDateDatabase
import dev.jwarmothiii.clientduedatetracker.database.entity.ContractTypeEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.RequirementTemplateEntity
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.ContractDefinitionApi
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.DefinitionCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.TemplateDraft
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractType
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractTypeId
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ReplacementScope
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplate
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplateId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomContractDefinitionApi
    @Inject
    constructor(
        private val database: ClientDueDateDatabase,
        private val definitionDao: DefinitionDao,
    ) : ContractDefinitionApi {
        override fun observeActiveContractTypes(): Flow<List<ContractType>> =
            definitionDao.observeActiveContractTypes().map { rows -> rows.map(ContractTypeEntity::toModel) }

        override suspend fun activeContractTypes(): List<ContractType> =
            definitionDao.activeContractTypes().map(ContractTypeEntity::toModel)

        override suspend fun templatesForContractType(contractTypeId: ContractTypeId): List<RequirementTemplate> =
            definitionDao.activeTemplates(contractTypeId.value).map(TemplateWithContractPublicId::toModel)

        override suspend fun template(id: RequirementTemplateId): RequirementTemplate? = definitionDao.template(id.value)?.toModel()

        override suspend fun ensureSeedDefinitions() {
            database.withTransaction {
                if (definitionDao.contractTypeCount() != 0) return@withTransaction
                SeedDefinitions.all.forEach { seed ->
                    val typePublicId = stableUuid("contract-type:${seed.code}")
                    val typeId =
                        definitionDao.insertContractType(
                            ContractTypeEntity(
                                publicId = typePublicId,
                                lineageId = typePublicId,
                                version = 1,
                                name = seed.name,
                                active = true,
                            ),
                        )
                    seed.templates.forEachIndexed { index, draft ->
                        val templatePublicId = stableUuid("template:${seed.code}:$index")
                        definitionDao.insertTemplate(
                            RequirementTemplateEntity(
                                publicId = templatePublicId,
                                contractTypeId = typeId,
                                lineageId = templatePublicId,
                                version = 1,
                                title = draft.title,
                                instructions = draft.instructions,
                                deadlinePolicyJson = DeadlinePolicyJson.encode(draft.policy),
                                notificationLeadDays = draft.notificationLeadDays,
                                active = true,
                            ),
                        )
                    }
                }
            }
        }

        override suspend fun replaceTemplate(
            templateId: RequirementTemplateId,
            replacement: TemplateDraft,
            scope: ReplacementScope,
        ): DefinitionCommandResult<RequirementTemplate> {
            val validation = replacement.validationError()
            if (validation != null) return DefinitionCommandResult.ValidationError(validation)
            return database.withTransaction {
                val old = definitionDao.templateEntity(templateId.value) ?: return@withTransaction DefinitionCommandResult.NotFound
                definitionDao.updateTemplate(old.copy(active = false))
                val replacementEntity =
                    old.copy(
                        id = 0,
                        publicId = UUID.randomUUID().toString(),
                        version = definitionDao.maximumTemplateVersion(old.lineageId) + 1,
                        title = replacement.title.trim(),
                        instructions = replacement.instructions.trim(),
                        deadlinePolicyJson = DeadlinePolicyJson.encode(replacement.policy),
                        notificationLeadDays = replacement.notificationLeadDays,
                        active = true,
                    )
                val replacementId = definitionDao.insertTemplate(replacementEntity)
                if (scope == ReplacementScope.FUTURE_AND_ACTIVE_CLIENTS) {
                    definitionDao.migrateActiveAssignments(old.id, replacementId)
                }
                val model =
                    definitionDao
                        .template(replacementEntity.publicId)
                        ?.toModel()
                        ?: throw DataIntegrityException("Missing replacement after insert.")
                DefinitionCommandResult.Success(model)
            }
        }

        override suspend fun deactivateTemplate(templateId: RequirementTemplateId): DefinitionCommandResult<Unit> {
            val entity = definitionDao.templateEntity(templateId.value) ?: return DefinitionCommandResult.NotFound
            database.withTransaction {
                definitionDao.updateTemplate(entity.copy(active = false))
                definitionDao.removeAssignments(entity.id)
            }
            return DefinitionCommandResult.Success(Unit)
        }

        override suspend fun replaceContractType(
            contractTypeId: ContractTypeId,
            replacementName: String,
            scope: ReplacementScope,
        ): DefinitionCommandResult<ContractType> {
            if (replacementName.isBlank()) {
                return DefinitionCommandResult.ValidationError("Contract type name is required.")
            }
            return database.withTransaction {
                val old =
                    definitionDao.contractType(contractTypeId.value)
                        ?: return@withTransaction DefinitionCommandResult.NotFound
                val oldTemplates = definitionDao.activeTemplates(old.publicId)
                definitionDao.updateContractType(old.copy(active = false))
                val replacement =
                    old.copy(
                        id = 0,
                        publicId = UUID.randomUUID().toString(),
                        version = definitionDao.maximumContractTypeVersion(old.lineageId) + 1,
                        name = replacementName.trim(),
                        active = true,
                    )
                val replacementId = definitionDao.insertContractType(replacement)
                val replacements =
                    oldTemplates.associate { oldTemplate ->
                        val newEntity =
                            RequirementTemplateEntity(
                                publicId = UUID.randomUUID().toString(),
                                contractTypeId = replacementId,
                                lineageId = oldTemplate.lineageId,
                                version = definitionDao.maximumTemplateVersion(oldTemplate.lineageId) + 1,
                                title = oldTemplate.title,
                                instructions = oldTemplate.instructions,
                                deadlinePolicyJson = oldTemplate.deadlinePolicyJson,
                                notificationLeadDays = oldTemplate.notificationLeadDays,
                                active = true,
                            )
                        oldTemplate.id to definitionDao.insertTemplate(newEntity)
                    }
                if (scope == ReplacementScope.FUTURE_AND_ACTIVE_CLIENTS) {
                    definitionDao.migrateActiveContracts(old.id, replacementId)
                    replacements.forEach { (oldTemplateId, replacementTemplateId) ->
                        definitionDao.migrateActiveAssignments(oldTemplateId, replacementTemplateId)
                    }
                }
                DefinitionCommandResult.Success(replacement.toModel())
            }
        }

        override suspend fun deactivateContractType(contractTypeId: ContractTypeId): DefinitionCommandResult<Unit> {
            val contractType = definitionDao.contractType(contractTypeId.value) ?: return DefinitionCommandResult.NotFound
            database.withTransaction {
                definitionDao.updateContractType(contractType.copy(active = false))
                definitionDao.activeTemplates(contractType.publicId).forEach { template ->
                    definitionDao.removeAssignments(template.id)
                }
            }
            return DefinitionCommandResult.Success(Unit)
        }

        private fun TemplateDraft.validationError(): String? =
            when {
                title.isBlank() -> "Template title is required."
                notificationLeadDays < 0 -> "Notification lead cannot be negative."
                else -> null
            }
    }

private fun ContractTypeEntity.toModel(): ContractType =
    ContractType(
        id = ContractTypeId(publicId),
        lineageId = lineageId,
        version = version,
        name = name,
        active = active,
    )

internal fun TemplateWithContractPublicId.toModel(): RequirementTemplate =
    RequirementTemplate(
        id = RequirementTemplateId(publicId),
        contractTypeId = ContractTypeId(contractTypePublicId),
        lineageId = lineageId,
        version = version,
        title = title,
        instructions = instructions,
        policy = DeadlinePolicyJson.decode(deadlinePolicyJson),
        notificationLeadDays = notificationLeadDays,
        active = active,
    )

private fun stableUuid(value: String): String = UUID.nameUUIDFromBytes(value.toByteArray(StandardCharsets.UTF_8)).toString()
