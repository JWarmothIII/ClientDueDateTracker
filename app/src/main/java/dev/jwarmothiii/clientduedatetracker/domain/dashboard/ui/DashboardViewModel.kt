package dev.jwarmothiii.clientduedatetracker.domain.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jwarmothiii.clientduedatetracker.data.database.PersistanceTestTableEntity
import dev.jwarmothiii.clientduedatetracker.data.repository.PersistanceTestTableRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        persistanceTestTableRepository: PersistanceTestTableRepository,
    ) : ViewModel() {
        val persistanceTestTables: StateFlow<List<PersistanceTestTableEntity>> =
            persistanceTestTableRepository
                .observeTestTables()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList(),
                )

        init {
            viewModelScope.launch {
                persistanceTestTableRepository.ensureStartupTestTable()
            }
        }
    }
