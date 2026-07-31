package dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.usecase

import dev.jwarmothiii.clientduedatetracker.database.DatabaseTransactionRunner
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.ContractDefinitionApi
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.DefinitionCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.TemplateDraft
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractType
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractTypeId
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ReplacementScope
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplate
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplateId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.usecase.DailyGenerationUseCase
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class ReplaceDefinitionUseCase
    @Inject
    constructor(
        private val transactionRunner: DatabaseTransactionRunner,
        private val definitionApi: ContractDefinitionApi,
        private val generation: DailyGenerationUseCase,
    ) {
        suspend fun replaceTemplate(
            templateId: RequirementTemplateId,
            replacement: TemplateDraft,
            scope: ReplacementScope,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): DefinitionCommandResult<RequirementTemplate> =
            transactionRunner.runInTransaction {
                val result = definitionApi.replaceTemplate(templateId, replacement, scope)
                if (result is DefinitionCommandResult.Success) {
                    generation(today, zoneId, Clock.systemUTC().instant())
                }
                result
            }

        suspend fun replaceContractType(
            contractTypeId: ContractTypeId,
            replacementName: String,
            scope: ReplacementScope,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): DefinitionCommandResult<ContractType> =
            transactionRunner.runInTransaction {
                val result = definitionApi.replaceContractType(contractTypeId, replacementName, scope)
                if (result is DefinitionCommandResult.Success) {
                    generation(today, zoneId, Clock.systemUTC().instant())
                }
                result
            }
    }
