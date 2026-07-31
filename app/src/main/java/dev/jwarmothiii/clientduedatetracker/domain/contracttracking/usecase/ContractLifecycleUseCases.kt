package dev.jwarmothiii.clientduedatetracker.domain.contracttracking.usecase

import dev.jwarmothiii.clientduedatetracker.database.DatabaseTransactionRunner
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractEventKind
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.ContractTrackingApi
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.TrackingCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.Contract
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractEvent
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractEventId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ContractId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.ExitKind
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class RecordContractEventUseCase
    @Inject
    constructor(
        private val transactionRunner: DatabaseTransactionRunner,
        private val trackingApi: ContractTrackingApi,
        private val generation: DailyGenerationUseCase,
    ) {
        suspend operator fun invoke(
            contractId: ContractId,
            kind: ContractEventKind,
            eventDate: LocalDate,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): TrackingCommandResult<ContractEvent> =
            transactionRunner.runInTransaction {
                val now = Clock.systemUTC().instant()
                val result = trackingApi.recordEvent(contractId, kind, eventDate, now)
                if (result is TrackingCommandResult.Success) generation(today, zoneId, now)
                result
            }
    }

class CorrectContractEventUseCase
    @Inject
    constructor(
        private val transactionRunner: DatabaseTransactionRunner,
        private val trackingApi: ContractTrackingApi,
        private val generation: DailyGenerationUseCase,
    ) {
        suspend operator fun invoke(
            eventId: ContractEventId,
            eventDate: LocalDate,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): TrackingCommandResult<ContractEvent> =
            transactionRunner.runInTransaction {
                val result = trackingApi.updateEvent(eventId, eventDate)
                if (result is TrackingCommandResult.Success) {
                    generation(today, zoneId, Clock.systemUTC().instant())
                }
                result
            }

        suspend fun delete(
            eventId: ContractEventId,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): TrackingCommandResult<Unit> =
            transactionRunner.runInTransaction {
                val result = trackingApi.deleteEvent(eventId)
                if (result is TrackingCommandResult.Success) {
                    generation(today, zoneId, Clock.systemUTC().instant())
                }
                result
            }
    }

class ConfirmContractExitUseCase
    @Inject
    constructor(
        private val transactionRunner: DatabaseTransactionRunner,
        private val trackingApi: ContractTrackingApi,
        private val generation: DailyGenerationUseCase,
    ) {
        suspend operator fun invoke(
            contractId: ContractId,
            actualExitDate: LocalDate,
            kind: ExitKind,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): TrackingCommandResult<Contract> =
            transactionRunner.runInTransaction {
                val result = trackingApi.confirmExit(contractId, actualExitDate, kind)
                if (result is TrackingCommandResult.Success) {
                    generation(today, zoneId, Clock.systemUTC().instant())
                }
                result
            }

        suspend fun correct(
            contractId: ContractId,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): TrackingCommandResult<Contract> =
            transactionRunner.runInTransaction {
                val result = trackingApi.correctExit(contractId)
                if (result is TrackingCommandResult.Success) {
                    generation(today, zoneId, Clock.systemUTC().instant())
                }
                result
            }
    }
