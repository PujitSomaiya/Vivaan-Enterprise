package com.vivaanenterprise.app.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.core.sync.SyncScheduler
import com.vivaanenterprise.app.domain.repository.ClientAccountRepository
import com.vivaanenterprise.app.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: ClientAccountRepository,
    private val syncRepository: SyncRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
        observeSyncStatus()
    }

    fun onRetry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadDashboardData()
    }

    fun onSyncNowClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isManualSyncing = true) }
            try {
                syncScheduler.enqueueSync(forceImmediate = true)
            } finally {
                _uiState.update { it.copy(isManualSyncing = false) }
            }
        }
    }

    fun onShowSyncDiagnostics() {
        _uiState.update { it.copy(showSyncDiagnosticsDialog = true) }
    }

    fun onDismissSyncDiagnostics() {
        _uiState.update { it.copy(showSyncDiagnosticsDialog = false) }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            accountRepository.observeDashboardSummary()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Failed to load dashboard summary"
                        )
                    }
                }
                .collect { summary ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            summary = summary,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            syncRepository.observeSyncStatus()
                .catch { /* Protect against sync flow errors */ }
                .collect { syncSummary ->
                    _uiState.update {
                        it.copy(syncSummary = syncSummary)
                    }
                }
        }
    }
}
