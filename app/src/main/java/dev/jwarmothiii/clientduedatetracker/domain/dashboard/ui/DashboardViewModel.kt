package dev.jwarmothiii.clientduedatetracker.domain.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.ContractDefinitionApi
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.ContractTrackingApi
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.TrackingCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.usecase.DailyGenerationUseCase
import dev.jwarmothiii.clientduedatetracker.domain.notes.api.NotesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        private val trackingApi: ContractTrackingApi,
        notesApi: NotesApi,
        private val definitionApi: ContractDefinitionApi,
        private val dailyGeneration: DailyGenerationUseCase,
    ) : ViewModel() {
        private val maintenanceError = MutableStateFlow<String?>(null)

        val uiState: StateFlow<DashboardUiState> =
            combine(
                trackingApi.observeActiveClientCount(),
                trackingApi.observePendingRequirements(),
                trackingApi.observeRecentlyCompletedRequirements(5),
                notesApi.observeRecentlyUpdatedPinned(3),
                maintenanceError,
            ) { activeCount, pending, recentlyCompleted, notes, error ->
                if (error != null) {
                    DashboardUiState.Error(error)
                } else {
                    val today = LocalDate.now()
                    DashboardUiState.Data(
                        activeClientCount = activeCount,
                        overdue =
                            pending
                                .filter { it.requirement.dueDate < today }
                                .sortedBy { it.requirement.dueDate },
                        dueSoon =
                            pending
                                .filter { it.requirement.dueDate in today..today.plusDays(7) }
                                .sortedBy { it.requirement.dueDate },
                        recentlyCompleted = recentlyCompleted.take(5),
                        pinnedNotes = notes.take(3),
                    )
                }
            }.catch { emit(DashboardUiState.Error(it.message ?: "Unable to load the dashboard.")) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = DashboardUiState.Loading,
                )

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                maintenanceError.value = null
                runCatching {
                    definitionApi.ensureSeedDefinitions()
                    dailyGeneration()
                }.onFailure {
                    maintenanceError.value = it.message ?: "Daily maintenance failed."
                }
            }
        }

        fun complete(requirementId: RequirementId) {
            viewModelScope.launch {
                when (val result = trackingApi.completeRequirement(requirementId, Clock.systemUTC().instant())) {
                    is TrackingCommandResult.Success -> Unit
                    is TrackingCommandResult.ValidationError -> maintenanceError.value = result.message
                    is TrackingCommandResult.Conflict -> maintenanceError.value = result.message
                    TrackingCommandResult.NotFound -> maintenanceError.value = "Requirement not found."
                }
            }
        }

        fun reopen(requirementId: RequirementId) {
            viewModelScope.launch {
                when (val result = trackingApi.reopenRequirement(requirementId)) {
                    is TrackingCommandResult.Success -> Unit
                    is TrackingCommandResult.ValidationError -> maintenanceError.value = result.message
                    is TrackingCommandResult.Conflict -> maintenanceError.value = result.message
                    TrackingCommandResult.NotFound -> maintenanceError.value = "Requirement not found."
                }
            }
        }
    }
