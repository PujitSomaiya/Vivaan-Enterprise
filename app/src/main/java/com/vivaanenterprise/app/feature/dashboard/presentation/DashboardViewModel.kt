package com.vivaanenterprise.app.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.domain.repository.ClientAccountRepository
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
    private val accountRepository: ClientAccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun onRetry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadDashboardData()
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
}
