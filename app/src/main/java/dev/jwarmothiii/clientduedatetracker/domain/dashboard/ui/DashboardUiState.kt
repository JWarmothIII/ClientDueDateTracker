package dev.jwarmothiii.clientduedatetracker.domain.dashboard.ui

import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.DashboardRequirement
import dev.jwarmothiii.clientduedatetracker.domain.notes.api.DashboardNote

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    data class Error(
        val message: String,
    ) : DashboardUiState

    data class Data(
        val activeClientCount: Int,
        val overdue: List<DashboardRequirement>,
        val dueSoon: List<DashboardRequirement>,
        val recentlyCompleted: List<DashboardRequirement>,
        val pinnedNotes: List<DashboardNote>,
    ) : DashboardUiState
}
