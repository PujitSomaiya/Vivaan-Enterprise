package com.vivaanenterprise.app.data.local.mapper

import com.vivaanenterprise.app.core.database.entity.BusinessDocumentEntity
import com.vivaanenterprise.app.core.database.entity.BusinessProfileEntity
import com.vivaanenterprise.app.core.database.entity.ClientEntity
import com.vivaanenterprise.app.core.database.entity.DocumentLineItemEntity
import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.model.ClientSnapshot
import com.vivaanenterprise.app.domain.model.DocumentLineItem
import com.vivaanenterprise.app.domain.model.SellerSnapshot

fun BusinessProfileEntity.toSellerSnapshot(): SellerSnapshot {
    return SellerSnapshot(
        businessName = businessName,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        cityStatePincode = cityStatePincode,
        gstin = gstin,
        mobile = mobile,
        email = email,
        pan = pan,
        stateCode = stateCode,
        bankAccountName = bankAccountName,
        bankName = bankName,
        bankAccountNumber = bankAccountNumber,
        bankIfsc = bankIfsc,
        bankBranch = bankBranch,
        declaration = declaration,
        authorisedSignatory = authorisedSignatory
    )
}

fun ClientEntity.toClientSnapshot(): ClientSnapshot = ClientSnapshot(
    clientId = id,
    companyName = companyName,
    address = address,
    gstin = gstin,
    state = state,
    stateCode = stateCode,
    email = email,
    phone = phone,
    pan = pan,
    iec = iec,
    otherDetails = otherDetails
)

fun DocumentLineItemEntity.toDomain(): DocumentLineItem = DocumentLineItem(
    id = id,
    documentId = documentId,
    productId = productId,
    position = position,
    descriptionSnapshot = descriptionSnapshot,
    hsnSacSnapshot = hsnSacSnapshot,
    quantity = quantity,
    ratePaise = ratePaise,
    gstRateBasisPoints = gstRateBasisPoints,
    taxableAmountPaise = taxableAmountPaise,
    cgstAmountPaise = cgstAmountPaise,
    sgstAmountPaise = sgstAmountPaise,
    igstAmountPaise = igstAmountPaise,
    totalTaxPaise = totalTaxPaise,
    lineTotalPaise = lineTotalPaise,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun DocumentLineItem.toEntity(): DocumentLineItemEntity = DocumentLineItemEntity(
    id = id,
    documentId = documentId,
    productId = productId,
    position = position,
    descriptionSnapshot = descriptionSnapshot,
    hsnSacSnapshot = hsnSacSnapshot,
    quantity = quantity,
    ratePaise = ratePaise,
    gstRateBasisPoints = gstRateBasisPoints,
    taxableAmountPaise = taxableAmountPaise,
    cgstAmountPaise = cgstAmountPaise,
    sgstAmountPaise = sgstAmountPaise,
    igstAmountPaise = igstAmountPaise,
    totalTaxPaise = totalTaxPaise,
    lineTotalPaise = lineTotalPaise,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BusinessDocumentEntity.toDomain(lineItems: List<DocumentLineItem> = emptyList()): BusinessDocument {
    val sellerSnapshot = if (!sellerBusinessNameSnapshot.isNullOrBlank()) {
        SellerSnapshot(
            businessName = sellerBusinessNameSnapshot,
            addressLine1 = sellerAddressLine1Snapshot ?: "",
            addressLine2 = sellerAddressLine2Snapshot ?: "",
            cityStatePincode = sellerCityStatePincodeSnapshot ?: "",
            gstin = sellerGstinSnapshot ?: "",
            mobile = sellerMobileSnapshot ?: "",
            email = sellerEmailSnapshot,
            pan = sellerPanSnapshot ?: "",
            bankAccountName = sellerBankAccountNameSnapshot ?: "",
            bankName = sellerBankNameSnapshot ?: "",
            bankAccountNumber = sellerBankAccountNumberSnapshot ?: "",
            bankIfsc = sellerBankIfscSnapshot ?: "",
            bankBranch = sellerBankBranchSnapshot ?: "",
            declaration = sellerDeclarationSnapshot ?: "",
            authorisedSignatory = sellerAuthorisedSignatorySnapshot ?: ""
        )
    } else null

    val clientSnapshot = if (!clientCompanyNameSnapshot.isNullOrBlank()) {
        ClientSnapshot(
            clientId = clientId,
            companyName = clientCompanyNameSnapshot,
            address = clientAddressSnapshot,
            gstin = clientGstinSnapshot,
            state = clientStateSnapshot,
            stateCode = clientStateCodeSnapshot,
            email = clientEmailSnapshot,
            phone = clientPhoneSnapshot,
            pan = clientPanSnapshot,
            iec = clientIecSnapshot,
            otherDetails = clientOtherDetailsSnapshot
        )
    } else null

    return BusinessDocument(
        id = id,
        documentType = documentType,
        documentNumber = documentNumber,
        documentDate = documentDate,
        status = status,
        clientId = clientId,
        sellerSnapshot = sellerSnapshot,
        clientSnapshot = clientSnapshot,
        lineItems = lineItems,
        deliveryNote = deliveryNote,
        deliveryFactoryAddress = deliveryFactoryAddress,
        paymentTerms = paymentTerms,
        supplierReference = supplierReference,
        otherReferences = otherReferences,
        buyerOrderNumber = buyerOrderNumber,
        buyerOrderDate = buyerOrderDate,
        dispatchDocumentNumber = dispatchDocumentNumber,
        deliveryNoteDate = deliveryNoteDate,
        dispatchThrough = dispatchThrough,
        destination = destination,
        termsOfDelivery = termsOfDelivery,
        placeOfSupply = placeOfSupply,
        taxTreatment = taxTreatment?.let {
            try { com.vivaanenterprise.app.domain.model.TaxTreatment.valueOf(it) } catch (_: Exception) { null }
        },
        taxableAmountPaise = taxableAmountPaise,
        cgstAmountPaise = cgstAmountPaise,
        sgstAmountPaise = sgstAmountPaise,
        igstAmountPaise = igstAmountPaise,
        totalTaxAmountPaise = totalTaxAmountPaise,
        grandTotalPaise = grandTotalPaise,
        amountInWords = amountInWords,
        taxAmountInWords = taxAmountInWords,
        createdAt = createdAt,
        updatedAt = updatedAt,
        finalizedAt = finalizedAt,
        cancelledAt = cancelledAt,
        syncStatus = syncStatus
    )
}

fun BusinessDocument.toEntity(): BusinessDocumentEntity = BusinessDocumentEntity(
    id = id,
    documentType = documentType,
    documentNumber = documentNumber,
    documentDate = documentDate,
    status = status,
    clientId = clientId,
    sellerBusinessNameSnapshot = sellerSnapshot?.businessName,
    sellerAddressLine1Snapshot = sellerSnapshot?.addressLine1,
    sellerAddressLine2Snapshot = sellerSnapshot?.addressLine2,
    sellerCityStatePincodeSnapshot = sellerSnapshot?.cityStatePincode,
    sellerGstinSnapshot = sellerSnapshot?.gstin,
    sellerMobileSnapshot = sellerSnapshot?.mobile,
    sellerEmailSnapshot = sellerSnapshot?.email,
    sellerPanSnapshot = sellerSnapshot?.pan,
    sellerBankAccountNameSnapshot = sellerSnapshot?.bankAccountName,
    sellerBankNameSnapshot = sellerSnapshot?.bankName,
    sellerBankAccountNumberSnapshot = sellerSnapshot?.bankAccountNumber,
    sellerBankIfscSnapshot = sellerSnapshot?.bankIfsc,
    sellerBankBranchSnapshot = sellerSnapshot?.bankBranch,
    sellerDeclarationSnapshot = sellerSnapshot?.declaration,
    sellerAuthorisedSignatorySnapshot = sellerSnapshot?.authorisedSignatory,
    clientCompanyNameSnapshot = clientSnapshot?.companyName,
    clientAddressSnapshot = clientSnapshot?.address,
    clientGstinSnapshot = clientSnapshot?.gstin,
    clientStateSnapshot = clientSnapshot?.state,
    clientStateCodeSnapshot = clientSnapshot?.stateCode,
    clientEmailSnapshot = clientSnapshot?.email,
    clientPhoneSnapshot = clientSnapshot?.phone,
    clientPanSnapshot = clientSnapshot?.pan,
    clientIecSnapshot = clientSnapshot?.iec,
    clientOtherDetailsSnapshot = clientSnapshot?.otherDetails,
    deliveryNote = deliveryNote,
    deliveryFactoryAddress = deliveryFactoryAddress,
    paymentTerms = paymentTerms,
    supplierReference = supplierReference,
    otherReferences = otherReferences,
    buyerOrderNumber = buyerOrderNumber,
    buyerOrderDate = buyerOrderDate,
    dispatchDocumentNumber = dispatchDocumentNumber,
    deliveryNoteDate = deliveryNoteDate,
    dispatchThrough = dispatchThrough,
    destination = destination,
    termsOfDelivery = termsOfDelivery,
    placeOfSupply = placeOfSupply,
    taxTreatment = taxTreatment?.name,
    taxableAmountPaise = taxableAmountPaise,
    cgstAmountPaise = cgstAmountPaise,
    sgstAmountPaise = sgstAmountPaise,
    igstAmountPaise = igstAmountPaise,
    totalTaxAmountPaise = totalTaxAmountPaise,
    grandTotalPaise = grandTotalPaise,
    amountInWords = amountInWords,
    taxAmountInWords = taxAmountInWords,
    createdAt = createdAt,
    updatedAt = updatedAt,
    finalizedAt = finalizedAt,
    cancelledAt = cancelledAt,
    isDeleted = false,
    deletedAt = null,
    syncStatus = syncStatus
)
