package dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api

import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractType
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractTypeId
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.DeadlinePolicy
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ReplacementScope
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplate
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplateId
import kotlinx.coroutines.flow.Flow

data class TemplateDraft(
    val title: String,
    val instructions: String,
    val policy: DeadlinePolicy,
    val notificationLeadDays: Int = 3,
)

sealed interface DefinitionCommandResult<out T> {
    data class Success<T>(
        val value: T,
    ) : DefinitionCommandResult<T>

    data class ValidationError(
        val message: String,
    ) : DefinitionCommandResult<Nothing>

    data object NotFound : DefinitionCommandResult<Nothing>

    data object DefinitionInUse : DefinitionCommandResult<Nothing>
}

interface ContractDefinitionApi {
    fun observeActiveContractTypes(): Flow<List<ContractType>>

    suspend fun activeContractTypes(): List<ContractType>

    suspend fun templatesForContractType(contractTypeId: ContractTypeId): List<RequirementTemplate>

    suspend fun template(id: RequirementTemplateId): RequirementTemplate?

    suspend fun ensureSeedDefinitions()

    suspend fun replaceTemplate(
        templateId: RequirementTemplateId,
        replacement: TemplateDraft,
        scope: ReplacementScope,
    ): DefinitionCommandResult<RequirementTemplate>

    suspend fun deactivateTemplate(templateId: RequirementTemplateId): DefinitionCommandResult<Unit>

    suspend fun replaceContractType(
        contractTypeId: ContractTypeId,
        replacementName: String,
        scope: ReplacementScope,
    ): DefinitionCommandResult<ContractType>

    suspend fun deactivateContractType(contractTypeId: ContractTypeId): DefinitionCommandResult<Unit>
}
