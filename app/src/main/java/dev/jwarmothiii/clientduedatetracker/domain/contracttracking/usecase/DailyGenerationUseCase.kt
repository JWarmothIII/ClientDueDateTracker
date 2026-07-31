package dev.jwarmothiii.clientduedatetracker.domain.contracttracking.usecase

import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.ContractDefinitionApi
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.ContractTrackingApi
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class DailyGenerationUseCase
    @Inject
    constructor(
        private val definitionApi: ContractDefinitionApi,
        private val trackingApi: ContractTrackingApi,
        private val generator: DeadlineGenerator,
    ) {
        suspend operator fun invoke(
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
            generatedAt: Instant = Clock.systemUTC().instant(),
        ) {
            definitionApi.ensureSeedDefinitions()
            val throughDate = today.plusDays(30)
            trackingApi.generationContracts().forEach { contract ->
                val desired = generator.generate(contract, throughDate, zoneId)
                trackingApi.synchronizeGeneratedRequirements(contract.contract.id, desired, generatedAt)
            }
        }
    }
