package com.vivaanenterprise.app.data.remote.model

data class BusinessProfileDto(
    val id: String = "",
    val businessName: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val cityStatePincode: String = "",
    val gstin: String = "",
    val mobile: String = "",
    val email: String? = null,
    val pan: String = "",
    val bankAccountName: String = "",
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val bankIfsc: String = "",
    val bankBranch: String = "",
    val declaration: String = "",
    val authorisedSignatory: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class ClientDto(
    val id: String = "",
    val companyName: String = "",
    val address: String? = null,
    val gstin: String? = null,
    val state: String? = null,
    val stateCode: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val pan: String? = null,
    val iec: String? = null,
    val otherDetails: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

data class ProductDto(
    val id: String = "",
    val name: String = "",
    val hsnSac: String? = null,
    val defaultGstRateBasisPoints: Int? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

data class BusinessDocumentDto(
    val id: String = "",
    val documentType: String = "",
    val documentNumber: String = "",
    val documentDate: Long = 0L,
    val status: String = "",
    val clientId: String = "",

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
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val finalizedAt: Long? = null,
    val cancelledAt: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

data class DocumentLineItemDto(
    val id: String = "",
    val documentId: String = "",
    val productId: String? = null,
    val position: Int = 0,
    val descriptionSnapshot: String = "",
    val hsnSacSnapshot: String? = null,
    val quantity: Long = 0L,
    val ratePaise: Long = 0L,
    val gstRateBasisPoints: Int = 0,
    val taxableAmountPaise: Long = 0L,
    val cgstAmountPaise: Long = 0L,
    val sgstAmountPaise: Long = 0L,
    val igstAmountPaise: Long = 0L,
    val totalTaxPaise: Long = 0L,
    val lineTotalPaise: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class ClientAccountEntryDto(
    val id: String = "",
    val clientId: String = "",
    val documentId: String? = null,
    val entryType: String = "",
    val entryDate: Long = 0L,
    val amountPaise: Long = 0L,
    val narration: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

data class DocumentSequenceDto(
    val documentType: String = "",
    val financialYear: String = "",
    val lastSequenceNumber: Int = 0,
    val updatedAt: Long = 0L
)
