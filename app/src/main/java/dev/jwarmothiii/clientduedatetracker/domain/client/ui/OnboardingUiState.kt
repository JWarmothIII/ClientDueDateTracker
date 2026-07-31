package dev.jwarmothiii.clientduedatetracker.domain.client.ui

import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractType

data class OnboardingUiState(
    val initials: String = "",
    val intakeDate: String = "",
    val scheduledAssessmentDate: String = "",
    val actualAssessmentCompletedDate: String = "",
    val plannedExitDate: String = "",
    val contractTypes: List<ContractType> = emptyList(),
    val selectedContractType: ContractType? = null,
    val loadingDefinitions: Boolean = true,
    val saving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)
