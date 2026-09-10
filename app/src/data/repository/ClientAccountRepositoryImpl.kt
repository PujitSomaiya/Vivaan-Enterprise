package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.common.AccountEntryType
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.database.VivaanEnterpriseDatabase
import com.vivaanenterprise.app.data.local.mapper.toDomain
import com.vivaanenterprise.app.domain.model.ClientAccountEntry
import com.vivaanenterprise.app.domain.model.ClientAccountSummary
import com.vivaanenterprise.app.domain.model.DashboardSummary
import com.vivaanenterprise.app.domain.repository.ClientAccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientAccountRepositoryImpl @Inject constructor(
    database: VivaanEnterpriseDatabase
) : ClientAccountRepository {

    private val accountEntryDao = database.clientAccountEntryDao()
    private val documentDao = database.businessDocumentDao()

    override fun observeAccountEntriesForClient(clientId: String): Flow<List<ClientAccountEntry>> {
        return accountEntryDao.observeByClientId(clientId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeAccountSummaryForClient(clientId: String): Flow<ClientAccountSummary> {
        return accountEntryDao.observeByClientId(clientId).map { entities ->
            val invoiceEntries = entities.filter { it.entryType == AccountEntryType.INVOICE }
            val invoiceCount = invoiceEntries.size
            val totalBilledPaise = invoiceEntries.fold(0L) { acc, entry -> acc + entry.amountPaise }
            ClientAccountSummary(
                invoiceCount = invoiceCount,
                totalBilledPaise = totalBilledPaise
            )
        }
    }

    override fun observeDashboardSummary(): Flow<DashboardSummary> {
        return documentDao.observeDocumentsByType(DocumentType.TAX_INVOICE).map { docs ->
            val finalizedInvoices = docs.filter { it.status == DocumentStatus.FINALIZED }
            val count = finalizedInvoices.size
            val totalBilled = finalizedInvoices.fold(0L) { acc, doc -> acc + doc.grandTotalPaise }
            DashboardSummary(
                finalizedInvoiceCount = count,
                totalBilledPaise = totalBilled
            )
        }
    }
}
