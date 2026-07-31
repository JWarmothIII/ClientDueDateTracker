package dev.jwarmothiii.clientduedatetracker.domain.client.usecase

import dev.jwarmothiii.clientduedatetracker.database.DatabaseTransactionRunner
import dev.jwarmothiii.clientduedatetracker.domain.client.api.ClientApi
import dev.jwarmothiii.clientduedatetracker.domain.client.api.ClientCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.client.model.Client
import dev.jwarmothiii.clientduedatetracker.domain.client.model.ClientId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.ContractTrackingApi
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.TrackingCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.usecase.DailyGenerationUseCase
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class CorrectClientDatesUseCase
    @Inject
    constructor(
        private val transactionRunner: DatabaseTransactionRunner,
        private val clientApi: ClientApi,
        private val trackingApi: ContractTrackingApi,
        private val dailyGeneration: DailyGenerationUseCase,
    ) {
        suspend operator fun invoke(
            clientId: ClientId,
            scheduledAssessmentDate: LocalDate,
            actualAssessmentCompletedDate: LocalDate?,
            plannedExitDate: LocalDate?,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): ClientCommandResult<Client> =
            transactionRunner.runInTransaction {
                val result =
                    clientApi.updateDates(
                        clientId,
                        scheduledAssessmentDate,
                        actualAssessmentCompletedDate,
                        plannedExitDate,
                        today,
                    )
                if (result is ClientCommandResult.Success) {
                    when (val contractResult = trackingApi.updatePlannedExitForClient(clientId, plannedExitDate)) {
                        is TrackingCommandResult.Success -> Unit
                        is TrackingCommandResult.ValidationError -> error(contractResult.message)
                        is TrackingCommandResult.Conflict -> error(contractResult.message)
                        TrackingCommandResult.NotFound -> error("Client contract was not found.")
                    }
                    dailyGeneration(today, zoneId, Clock.systemUTC().instant())
                }
                result
            }
    }
