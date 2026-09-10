package com.vivaanenterprise.app.domain.model

import com.vivaanenterprise.app.core.common.AccountEntryType
import com.vivaanenterprise.app.core.common.SyncStatus

data class ClientAccountEntry(
    val id: String,
    val clientId: String,
    val documentId: String?,
    val entryType: AccountEntryType,
    val entryDate: Long,
    val amountPaise: Long,
    val narration: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus
)

data class ClientAccountSummary(
    val invoiceCount: Int,
    val totalBilledPaise: Long
)

data class DashboardSummary(
    val finalizedInvoiceCount: Int,
    val totalBilledPaise: Long
)
