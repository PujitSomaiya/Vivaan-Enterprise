package com.vivaanenterprise.app.domain.repository

import com.vivaanenterprise.app.domain.model.ClientAccountEntry
import com.vivaanenterprise.app.domain.model.ClientAccountSummary
import com.vivaanenterprise.app.domain.model.DashboardSummary
import kotlinx.coroutines.flow.Flow

interface ClientAccountRepository {
    fun observeAccountEntriesForClient(clientId: String): Flow<List<ClientAccountEntry>>
    fun observeAccountSummaryForClient(clientId: String): Flow<ClientAccountSummary>
    fun observeDashboardSummary(): Flow<DashboardSummary>
}
