package com.vivaanenterprise.app.feature.dashboard.presentation

import com.vivaanenterprise.app.domain.model.DashboardSummary
import com.vivaanenterprise.app.domain.repository.SyncStatusSummary

data class DashboardUiState(
    val isLoading: Boolean = false,
    val summary: DashboardSummary = DashboardSummary(
        finalizedInvoiceCount = 0,
        totalBilledPaise = 0L
    ),
    val syncSummary: SyncStatusSummary = SyncStatusSummary(),
    val isManualSyncing: Boolean = false,
    val showSyncDiagnosticsDialog: Boolean = false,
    val errorMessage: String? = null
)
