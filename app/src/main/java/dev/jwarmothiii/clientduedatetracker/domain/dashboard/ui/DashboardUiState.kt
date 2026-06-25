package dev.jwarmothiii.clientduedatetracker.domain.dashboard.ui

import dev.jwarmothiii.clientduedatetracker.data.database.PersistenceTestTableEntity

data class DashboardUiState(
    val persistenceTestTables: List<PersistenceTestTableEntity> = emptyList(),
)
