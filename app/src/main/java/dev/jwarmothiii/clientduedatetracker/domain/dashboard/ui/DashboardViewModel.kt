package dev.jwarmothiii.clientduedatetracker.domain.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jwarmothiii.clientduedatetracker.data.repository.PersistenceTestTableRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        persistenceTestTableRepository: PersistenceTestTableRepository,
    ) : ViewModel() {
        val uiState: StateFlow<DashboardUiState> =
            persistenceTestTableRepository
                .observeTestTables()
                .map { persistenceTestTables ->
                    DashboardUiState(persistenceTestTables = persistenceTestTables)
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = DashboardUiState(),
                )

        init {
            viewModelScope.launch {
                persistenceTestTableRepository.ensureStartupTestTable()
            }
        }
    }
