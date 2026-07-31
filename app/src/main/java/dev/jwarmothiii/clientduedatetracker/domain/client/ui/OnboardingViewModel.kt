package dev.jwarmothiii.clientduedatetracker.domain.client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jwarmothiii.clientduedatetracker.domain.client.model.CreateClientCommand
import dev.jwarmothiii.clientduedatetracker.domain.client.usecase.OnboardClientUseCase
import dev.jwarmothiii.clientduedatetracker.domain.client.usecase.OnboardingResult
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.ContractDefinitionApi
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractTypeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val definitionApi: ContractDefinitionApi,
        private val onboardClient: OnboardClientUseCase,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(OnboardingUiState())
        val state: StateFlow<OnboardingUiState> = mutableState.asStateFlow()

        init {
            loadDefinitions()
        }

        fun updateInitials(value: String) = update { copy(initials = value, errorMessage = null) }

        fun updateIntakeDate(value: String) = update { copy(intakeDate = value, errorMessage = null) }

        fun updateScheduledAssessmentDate(value: String) = update { copy(scheduledAssessmentDate = value, errorMessage = null) }

        fun updateActualAssessmentDate(value: String) = update { copy(actualAssessmentCompletedDate = value, errorMessage = null) }

        fun updatePlannedExitDate(value: String) = update { copy(plannedExitDate = value, errorMessage = null) }

        fun selectContractType(id: ContractTypeId) =
            update { copy(selectedContractType = contractTypes.firstOrNull { it.id == id }, errorMessage = null) }

        fun save() {
            val current = mutableState.value
            if (current.saving) return
            val command =
                try {
                    CreateClientCommand(
                        initials = current.initials,
                        intakeDate = current.intakeDate.requiredDate("Intake date"),
                        scheduledAssessmentDate =
                            current.scheduledAssessmentDate.requiredDate("Scheduled assessment date"),
                        actualAssessmentCompletedDate =
                            current.actualAssessmentCompletedDate.optionalDate("Actual assessment date"),
                        plannedExitDate = current.plannedExitDate.optionalDate("Planned exit date"),
                    )
                } catch (exception: InvalidDate) {
                    update { copy(errorMessage = exception.message) }
                    return
                }
            val contractType =
                current.selectedContractType
                    ?: run {
                        update { copy(errorMessage = "Select a contract type.") }
                        return
                    }
            update { copy(saving = true, errorMessage = null) }
            viewModelScope.launch {
                when (
                    val result =
                        onboardClient(
                            command = command,
                            contractTypeId = contractType.id,
                            today = LocalDate.now(),
                            now = Clock.systemUTC().instant(),
                            zoneId = java.time.ZoneId.systemDefault(),
                        )
                ) {
                    is OnboardingResult.Success -> {
                        update { copy(saving = false, saved = true) }
                    }

                    is OnboardingResult.ValidationError -> {
                        update { copy(saving = false, errorMessage = result.message) }
                    }

                    is OnboardingResult.Failure -> {
                        update { copy(saving = false, errorMessage = result.message) }
                    }
                }
            }
        }

        private fun loadDefinitions() {
            viewModelScope.launch {
                runCatching {
                    definitionApi.ensureSeedDefinitions()
                    definitionApi.activeContractTypes()
                }.onSuccess { types ->
                    update {
                        copy(
                            contractTypes = types,
                            selectedContractType = selectedContractType ?: types.firstOrNull(),
                            loadingDefinitions = false,
                        )
                    }
                }.onFailure {
                    update {
                        copy(
                            loadingDefinitions = false,
                            errorMessage = it.message ?: "Unable to load contract types.",
                        )
                    }
                }
            }
        }

        private inline fun update(transform: OnboardingUiState.() -> OnboardingUiState) {
            mutableState.value = mutableState.value.transform()
        }
    }

private fun String.requiredDate(label: String): LocalDate {
    if (isBlank()) throw InvalidDate("$label is required.")
    return optionalDate(label) ?: throw InvalidDate("$label is required.")
}

private fun String.optionalDate(label: String): LocalDate? =
    if (isBlank()) {
        null
    } else {
        try {
            LocalDate.parse(trim())
        } catch (_: DateTimeParseException) {
            throw InvalidDate("$label must use YYYY-MM-DD.")
        }
    }

private class InvalidDate(
    message: String,
) : IllegalArgumentException(message)
