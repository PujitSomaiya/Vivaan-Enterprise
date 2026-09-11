package com.vivaanenterprise.app.feature.dashboard.presentation

import com.vivaanenterprise.app.domain.model.DashboardSummary

data class DashboardUiState(
    val isLoading: Boolean = false,
    val summary: DashboardSummary = DashboardSummary(
        finalizedInvoiceCount = 0,
        totalBilledPaise = 0L
    ),
    val errorMessage: String? = null
)
