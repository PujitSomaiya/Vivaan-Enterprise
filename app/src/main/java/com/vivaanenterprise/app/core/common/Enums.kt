package com.vivaanenterprise.app.core.common

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}

enum class DocumentType {
    TAX_INVOICE,
    PURCHASE_ORDER
}

enum class DocumentStatus {
    DRAFT,
    FINALIZED,
    CANCELLED
}

enum class AccountEntryType {
    INVOICE,
    PAYMENT,
    ADJUSTMENT
}
