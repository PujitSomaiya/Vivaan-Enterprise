package com.vivaanenterprise.app.domain.model

data class SellerSnapshot(
    val businessName: String,
    val addressLine1: String,
    val addressLine2: String,
    val cityStatePincode: String,
    val gstin: String,
    val mobile: String,
    val email: String? = null,
    val pan: String,
    /**
     * Two-digit GST state code derived from the first two characters of [gstin].
     * Null if the GSTIN does not start with two numeric digits (malformed profile data).
     * Used by [DocumentCalculator] to determine the applicable GST branch (CGST+SGST vs IGST).
     */
    val stateCode: String? = null,
    val bankAccountName: String,
    val bankName: String,
    val bankAccountNumber: String,
    val bankIfsc: String,
    val bankBranch: String,
    val declaration: String,
    val authorisedSignatory: String
)

data class ClientSnapshot(
    val clientId: String,
    val companyName: String,
    val address: String? = null,
    val gstin: String? = null,
    val state: String? = null,
    val stateCode: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val pan: String? = null,
    val iec: String? = null,
    val otherDetails: String? = null
)

data class DocumentLineItem(
    val id: String,
    val documentId: String,
    val productId: String? = null,
    val position: Int,
    val descriptionSnapshot: String,
    val hsnSacSnapshot: String? = null,
    val quantity: Long,
    val ratePaise: Long,
    val gstRateBasisPoints: Int,
    val taxableAmountPaise: Long = 0L,
    val cgstAmountPaise: Long = 0L,
    val sgstAmountPaise: Long = 0L,
    val igstAmountPaise: Long = 0L,
    val totalTaxPaise: Long = 0L,
    val lineTotalPaise: Long = 0L,
    val createdAt: Long,
    val updatedAt: Long
)

data class BusinessDocument(
    val id: String,
    val documentType: com.vivaanenterprise.app.core.common.DocumentType,
    val documentNumber: String,
    val documentDate: Long,
    val status: com.vivaanenterprise.app.core.common.DocumentStatus,
    val clientId: String,
    val sellerSnapshot: SellerSnapshot? = null,
    val clientSnapshot: ClientSnapshot? = null,
    val lineItems: List<DocumentLineItem> = emptyList(),

    // Optional dispatch/reference metadata
    val deliveryNote: String? = null,
    val deliveryFactoryAddress: String? = null,
    val paymentTerms: String? = null,
    val supplierReference: String? = null,
    val otherReferences: String? = null,
    val buyerOrderNumber: String? = null,
    val buyerOrderDate: Long? = null,
    val dispatchDocumentNumber: String? = null,
    val deliveryNoteDate: Long? = null,
    val dispatchThrough: String? = null,
    val destination: String? = null,
    val termsOfDelivery: String? = null,
    val placeOfSupply: String? = null,

    // Financial totals (Paise) - to be populated by calculation result
    val taxTreatment: com.vivaanenterprise.app.domain.model.TaxTreatment? = null,
    val taxableAmountPaise: Long = 0L,
    val cgstAmountPaise: Long = 0L,
    val sgstAmountPaise: Long = 0L,
    val igstAmountPaise: Long = 0L,
    val totalTaxAmountPaise: Long = 0L,
    val grandTotalPaise: Long = 0L,
    val amountInWords: String? = null,
    val taxAmountInWords: String? = null,

    // Timestamps
    val createdAt: Long,
    val updatedAt: Long,
    val finalizedAt: Long? = null,
    val cancelledAt: Long? = null,

    // Sync metadata
    val syncStatus: com.vivaanenterprise.app.core.common.SyncStatus = com.vivaanenterprise.app.core.common.SyncStatus.PENDING
)
