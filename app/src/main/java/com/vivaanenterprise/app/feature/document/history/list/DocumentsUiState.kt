package com.vivaanenterprise.app.feature.document.history.list

import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus

enum class DocumentTypeFilter {
    ALL,
    TAX_INVOICE,
    PURCHASE_ORDER
}

enum class DocumentStatusFilter {
    ALL,
    DRAFT,
    FINALIZED
}

data class DocumentListItemUiModel(
    val id: String,
    val documentType: DocumentType,
    val documentNumber: String,
    val documentDate: Long,
    val status: DocumentStatus,
    val clientDisplayName: String,
    val grandTotalPaise: Long,
    val syncStatus: SyncStatus
)

data class DocumentsUiState(
    val isLoading: Boolean = true,
    val documents: List<DocumentListItemUiModel> = emptyList(),
    val searchQuery: String = "",
    val selectedTypeFilter: DocumentTypeFilter = DocumentTypeFilter.ALL,
    val selectedStatusFilter: DocumentStatusFilter = DocumentStatusFilter.ALL,
    val errorMessage: String? = null
) {
    val filteredDocuments: List<DocumentListItemUiModel>
        get() {
            val query = searchQuery.trim()
            return documents.filter { doc ->
                // Type filter
                val matchesType = when (selectedTypeFilter) {
                    DocumentTypeFilter.ALL -> true
                    DocumentTypeFilter.TAX_INVOICE -> doc.documentType == DocumentType.TAX_INVOICE
                    DocumentTypeFilter.PURCHASE_ORDER -> doc.documentType == DocumentType.PURCHASE_ORDER
                }

                // Status filter
                val matchesStatus = when (selectedStatusFilter) {
                    DocumentStatusFilter.ALL -> true
                    DocumentStatusFilter.DRAFT -> doc.status == DocumentStatus.DRAFT
                    DocumentStatusFilter.FINALIZED -> doc.status == DocumentStatus.FINALIZED
                }

                // Search query matching documentNumber or clientDisplayName (case-insensitive)
                val matchesQuery = if (query.isBlank()) {
                    true
                } else {
                    doc.documentNumber.contains(query, ignoreCase = true) ||
                            doc.clientDisplayName.contains(query, ignoreCase = true)
                }

                matchesType && matchesStatus && matchesQuery
            }
        }
}
