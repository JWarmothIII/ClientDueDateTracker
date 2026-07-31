package dev.jwarmothiii.clientduedatetracker.domain.client.usecase

import dev.jwarmothiii.clientduedatetracker.database.DatabaseTransactionRunner
import dev.jwarmothiii.clientduedatetracker.domain.client.api.ClientApi
import dev.jwarmothiii.clientduedatetracker.domain.client.api.ClientCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.client.model.Client
import dev.jwarmothiii.clientduedatetracker.domain.client.model.CreateClientCommand
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.ContractDefinitionApi
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractTypeId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.ContractTrackingApi
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.TrackingCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.usecase.DailyGenerationUseCase
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

sealed interface OnboardingResult {
    data class Success(
        val client: Client,
    ) : OnboardingResult

    data class ValidationError(
        val message: String,
    ) : OnboardingResult

    data class Failure(
        val message: String,
    ) : OnboardingResult
}

class OnboardClientUseCase
    @Inject
    constructor(
        private val transactionRunner: DatabaseTransactionRunner,
        private val clientApi: ClientApi,
        private val definitionApi: ContractDefinitionApi,
        private val trackingApi: ContractTrackingApi,
        private val dailyGeneration: DailyGenerationUseCase,
    ) {
        suspend operator fun invoke(
            command: CreateClientCommand,
            contractTypeId: ContractTypeId,
            today: LocalDate,
            now: Instant,
            zoneId: ZoneId,
        ): OnboardingResult {
            definitionApi.ensureSeedDefinitions()
            return try {
                transactionRunner.runInTransaction {
                    val client =
                        when (val result = clientApi.create(command, today)) {
                            is ClientCommandResult.Success -> {
                                result.value
                            }

                            is ClientCommandResult.ValidationError -> {
                                return@runInTransaction OnboardingResult.ValidationError(result.message)
                            }

                            ClientCommandResult.NotFound -> {
                                return@runInTransaction OnboardingResult.Failure("Client could not be created.")
                            }
                        }
                    val contract =
                        when (
                            val result =
                                trackingApi.createContract(
                                    client.id,
                                    contractTypeId,
                                    command.intakeDate,
                                    command.plannedExitDate,
                                )
                        ) {
                            is TrackingCommandResult.Success -> {
                                result.value
                            }

                            is TrackingCommandResult.ValidationError -> {
                                throw OnboardingRollback(result.message)
                            }

                            is TrackingCommandResult.Conflict -> {
                                throw OnboardingRollback(result.message)
                            }

                            TrackingCommandResult.NotFound -> {
                                throw OnboardingRollback("Contract type was not found.")
                            }
                        }
                    val templates = definitionApi.templatesForContractType(contractTypeId)
                    when (val assignment = trackingApi.assignTemplates(contract.id, templates.map { it.id })) {
                        is TrackingCommandResult.Success -> Unit
                        is TrackingCommandResult.ValidationError -> throw OnboardingRollback(assignment.message)
                        is TrackingCommandResult.Conflict -> throw OnboardingRollback(assignment.message)
                        TrackingCommandResult.NotFound -> throw OnboardingRollback("A requirement template was not found.")
                    }
                    dailyGeneration(today, zoneId, now)
                    OnboardingResult.Success(client)
                }
            } catch (exception: OnboardingRollback) {
                OnboardingResult.Failure(exception.message ?: "Onboarding failed.")
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                OnboardingResult.Failure(exception.message ?: "Onboarding failed.")
            }
        }
    }

private class OnboardingRollback(
    message: String,
) : RuntimeException(message)
