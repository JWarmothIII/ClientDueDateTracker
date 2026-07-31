@file:Suppress("ktlint:standard:function-naming")

package dev.jwarmothiii.clientduedatetracker.domain.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }
    OnboardingContent(
        state = state,
        onBack = onBack,
        onInitialsChanged = viewModel::updateInitials,
        onIntakeChanged = viewModel::updateIntakeDate,
        onScheduledAssessmentChanged = viewModel::updateScheduledAssessmentDate,
        onActualAssessmentChanged = viewModel::updateActualAssessmentDate,
        onPlannedExitChanged = viewModel::updatePlannedExitDate,
        onContractTypeSelected = viewModel::selectContractType,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
internal fun OnboardingContent(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onInitialsChanged: (String) -> Unit,
    onIntakeChanged: (String) -> Unit,
    onScheduledAssessmentChanged: (String) -> Unit,
    onActualAssessmentChanged: (String) -> Unit,
    onPlannedExitChanged: (String) -> Unit,
    onContractTypeSelected: (dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractTypeId) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Add client") },
                navigationIcon = {
                    OutlinedButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                        Text("Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Client details", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.initials,
                onValueChange = onInitialsChanged,
                label = { Text("Initials") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            DateField("Intake date", state.intakeDate, onIntakeChanged)
            DateField(
                "Scheduled assessment date",
                state.scheduledAssessmentDate,
                onScheduledAssessmentChanged,
            )
            DateField(
                "Actual assessment completed (optional)",
                state.actualAssessmentCompletedDate,
                onActualAssessmentChanged,
            )
            DateField("Planned exit (optional)", state.plannedExitDate, onPlannedExitChanged)
            Text("Contract type", style = MaterialTheme.typography.titleMedium)
            if (state.loadingDefinitions) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Text("Loading contract types…", modifier = Modifier.padding(start = 12.dp))
                }
            } else {
                state.contractTypes.forEach { type ->
                    FilterChip(
                        selected = state.selectedContractType?.id == type.id,
                        onClick = { onContractTypeSelected(type.id) },
                        label = { Text(type.name) },
                    )
                }
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = onSave,
                enabled = !state.saving && !state.loadingDefinitions,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.saving) {
                    CircularProgressIndicator()
                } else {
                    Text("Save client")
                }
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = { Text("YYYY-MM-DD") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
