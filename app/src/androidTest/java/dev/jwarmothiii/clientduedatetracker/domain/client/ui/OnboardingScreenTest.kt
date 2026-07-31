package dev.jwarmothiii.clientduedatetracker.domain.client.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractType
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractTypeId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun formAcceptsDatesSelectsTypeAndInvokesSave() {
        val type =
            ContractType(
                id = ContractTypeId("cts"),
                lineageId = "cts",
                version = 1,
                name = "CTS",
                active = true,
            )
        var saved = false
        var selected = false
        compose.setContent {
            OnboardingContent(
                state = OnboardingUiState(contractTypes = listOf(type), loadingDefinitions = false),
                onBack = {},
                onInitialsChanged = {},
                onIntakeChanged = {},
                onScheduledAssessmentChanged = {},
                onActualAssessmentChanged = {},
                onPlannedExitChanged = {},
                onContractTypeSelected = { selected = it == type.id },
                onSave = { saved = true },
            )
        }

        compose.onNodeWithText("Initials").performTextInput("ab")
        compose.onNodeWithText("Intake date").performTextInput("2026-07-01")
        compose.onNodeWithText("CTS").performClick()
        compose.onNodeWithText("Save client").performClick()

        assertTrue(selected)
        assertTrue(saved)
    }

    @Test
    fun validationAndSavingStatesAreVisible() {
        compose.setContent {
            OnboardingContent(
                state =
                    OnboardingUiState(
                        loadingDefinitions = false,
                        saving = true,
                        errorMessage = "Assessment completion date cannot be in the future.",
                    ),
                onBack = {},
                onInitialsChanged = {},
                onIntakeChanged = {},
                onScheduledAssessmentChanged = {},
                onActualAssessmentChanged = {},
                onPlannedExitChanged = {},
                onContractTypeSelected = {},
                onSave = {},
            )
        }

        compose
            .onNodeWithText("Assessment completion date cannot be in the future.")
            .assertIsDisplayed()
    }
}
