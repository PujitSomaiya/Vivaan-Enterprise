package com.vivaanenterprise.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus

@Entity(
    tableName = "business_documents",
    indices = [
        Index("documentType"),
        Index("documentDate"),
        Index("clientId"),
        Index("status"),
        Index("updatedAt"),
        Index("syncStatus"),
        Index("isDeleted")
    ]
)
data class BusinessDocumentEntity(
    @PrimaryKey
    val id: String,
    val documentType: DocumentType,
    val documentNumber: String,
    val documentDate: Long,
    val status: DocumentStatus,
    val clientId: String,

    // Seller snapshot
    val sellerBusinessNameSnapshot: String? = null,
    val sellerAddressLine1Snapshot: String? = null,
    val sellerAddressLine2Snapshot: String? = null,
    val sellerCityStatePincodeSnapshot: String? = null,
    val sellerGstinSnapshot: String? = null,
    val sellerMobileSnapshot: String? = null,
    val sellerEmailSnapshot: String? = null,
    val sellerPanSnapshot: String? = null,
    val sellerBankAccountNameSnapshot: String? = null,
    val sellerBankNameSnapshot: String? = null,
    val sellerBankAccountNumberSnapshot: String? = null,
    val sellerBankIfscSnapshot: String? = null,
    val sellerBankBranchSnapshot: String? = null,
    val sellerDeclarationSnapshot: String? = null,
    val sellerAuthorisedSignatorySnapshot: String? = null,

    // Client snapshot
    val clientCompanyNameSnapshot: String? = null,
    val clientAddressSnapshot: String? = null,
    val clientGstinSnapshot: String? = null,
    val clientStateSnapshot: String? = null,
    val clientStateCodeSnapshot: String? = null,
    val clientEmailSnapshot: String? = null,
    val clientPhoneSnapshot: String? = null,
    val clientPanSnapshot: String? = null,
    val clientIecSnapshot: String? = null,
    val clientOtherDetailsSnapshot: String? = null,

    // Optional metadata
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

    // Financial totals (Paise)
    val taxTreatment: String? = null,
    val taxableAmountPaise: Long = 0L,
    val cgstAmountPaise: Long = 0L,
    val sgstAmountPaise: Long = 0L,
    val igstAmountPaise: Long = 0L,
    val totalTaxAmountPaise: Long = 0L,
    val grandTotalPaise: Long = 0L,
    val amountInWords: String? = null,
    val taxAmountInWords: String? = null,

    // Lifecycle timestamps
    val createdAt: Long,
    val updatedAt: Long,
    val finalizedAt: Long? = null,
    val cancelledAt: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,

    // Sync metadata
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncedAt: Long? = null,
    val syncError: String? = null
)
