package com.vivaanenterprise.app.data.remote.mapper

import com.vivaanenterprise.app.core.common.AccountEntryType
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.database.entity.BusinessDocumentEntity
import com.vivaanenterprise.app.core.database.entity.BusinessProfileEntity
import com.vivaanenterprise.app.core.database.entity.ClientAccountEntryEntity
import com.vivaanenterprise.app.core.database.entity.ClientEntity
import com.vivaanenterprise.app.core.database.entity.DocumentLineItemEntity
import com.vivaanenterprise.app.core.database.entity.DocumentSequenceEntity
import com.vivaanenterprise.app.core.database.entity.ProductEntity
import com.vivaanenterprise.app.data.remote.model.BusinessDocumentDto
import com.vivaanenterprise.app.data.remote.model.BusinessProfileDto
import com.vivaanenterprise.app.data.remote.model.ClientAccountEntryDto
import com.vivaanenterprise.app.data.remote.model.ClientDto
import com.vivaanenterprise.app.data.remote.model.DocumentLineItemDto
import com.vivaanenterprise.app.data.remote.model.DocumentSequenceDto
import com.vivaanenterprise.app.data.remote.model.ProductDto

// BusinessProfile Mappers
fun BusinessProfileEntity.toDto(): BusinessProfileDto = BusinessProfileDto(
    id = id,
    businessName = businessName,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    cityStatePincode = cityStatePincode,
    gstin = gstin,
    mobile = mobile,
    email = email,
    pan = pan,
    bankAccountName = bankAccountName,
    bankName = bankName,
    bankAccountNumber = bankAccountNumber,
    bankIfsc = bankIfsc,
    bankBranch = bankBranch,
    declaration = declaration,
    authorisedSignatory = authorisedSignatory,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BusinessProfileDto.toEntity(syncedAt: Long): BusinessProfileEntity = BusinessProfileEntity(
    id = id,
    businessName = businessName,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    cityStatePincode = cityStatePincode,
    gstin = gstin,
    mobile = mobile,
    email = email,
    pan = pan,
    bankAccountName = bankAccountName,
    bankName = bankName,
    bankAccountNumber = bankAccountNumber,
    bankIfsc = bankIfsc,
    bankBranch = bankBranch,
    declaration = declaration,
    authorisedSignatory = authorisedSignatory,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED,
    lastSyncedAt = syncedAt,
    syncError = null
)

// Client Mappers
fun ClientEntity.toDto(): ClientDto = ClientDto(
    id = id,
    companyName = companyName,
    address = address,
    gstin = gstin,
    state = state,
    stateCode = stateCode,
    email = email,
    phone = phone,
    pan = pan,
    iec = iec,
    otherDetails = otherDetails,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun ClientDto.toEntity(syncedAt: Long): ClientEntity = ClientEntity(
    id = id,
    companyName = companyName,
    address = address,
    gstin = gstin,
    state = state,
    stateCode = stateCode,
    email = email,
    phone = phone,
    pan = pan,
    iec = iec,
    otherDetails = otherDetails,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED,
    lastSyncedAt = syncedAt,
    syncError = null
)

// Product Mappers
fun ProductEntity.toDto(): ProductDto = ProductDto(
    id = id,
    name = name,
    hsnSac = hsnSac,
    defaultGstRateBasisPoints = defaultGstRateBasisPoints,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun ProductDto.toEntity(syncedAt: Long): ProductEntity = ProductEntity(
    id = id,
    name = name,
    hsnSac = hsnSac,
    defaultGstRateBasisPoints = defaultGstRateBasisPoints,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED,
    lastSyncedAt = syncedAt,
    syncError = null
)

// BusinessDocument Mappers
fun BusinessDocumentEntity.toDto(): BusinessDocumentDto = BusinessDocumentDto(
    id = id,
    documentType = documentType.name,
    documentNumber = documentNumber,
    documentDate = documentDate,
    status = status.name,
    clientId = clientId,
    sellerBusinessNameSnapshot = sellerBusinessNameSnapshot,
    sellerAddressLine1Snapshot = sellerAddressLine1Snapshot,
    sellerAddressLine2Snapshot = sellerAddressLine2Snapshot,
    sellerCityStatePincodeSnapshot = sellerCityStatePincodeSnapshot,
    sellerGstinSnapshot = sellerGstinSnapshot,
    sellerMobileSnapshot = sellerMobileSnapshot,
    sellerEmailSnapshot = sellerEmailSnapshot,
    sellerPanSnapshot = sellerPanSnapshot,
    sellerBankAccountNameSnapshot = sellerBankAccountNameSnapshot,
    sellerBankNameSnapshot = sellerBankNameSnapshot,
    sellerBankAccountNumberSnapshot = sellerBankAccountNumberSnapshot,
    sellerBankIfscSnapshot = sellerBankIfscSnapshot,
    sellerBankBranchSnapshot = sellerBankBranchSnapshot,
    sellerDeclarationSnapshot = sellerDeclarationSnapshot,
    sellerAuthorisedSignatorySnapshot = sellerAuthorisedSignatorySnapshot,
    clientCompanyNameSnapshot = clientCompanyNameSnapshot,
    clientAddressSnapshot = clientAddressSnapshot,
    clientGstinSnapshot = clientGstinSnapshot,
    clientStateSnapshot = clientStateSnapshot,
    clientStateCodeSnapshot = clientStateCodeSnapshot,
    clientEmailSnapshot = clientEmailSnapshot,
    clientPhoneSnapshot = clientPhoneSnapshot,
    clientPanSnapshot = clientPanSnapshot,
    clientIecSnapshot = clientIecSnapshot,
    clientOtherDetailsSnapshot = clientOtherDetailsSnapshot,
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
    taxTreatment = taxTreatment,
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
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun BusinessDocumentDto.toEntity(syncedAt: Long): BusinessDocumentEntity = BusinessDocumentEntity(
    id = id,
    documentType = try { DocumentType.valueOf(documentType) } catch (e: Exception) { DocumentType.TAX_INVOICE },
    documentNumber = documentNumber,
    documentDate = documentDate,
    status = try { DocumentStatus.valueOf(status) } catch (e: Exception) { DocumentStatus.DRAFT },
    clientId = clientId,
    sellerBusinessNameSnapshot = sellerBusinessNameSnapshot,
    sellerAddressLine1Snapshot = sellerAddressLine1Snapshot,
    sellerAddressLine2Snapshot = sellerAddressLine2Snapshot,
    sellerCityStatePincodeSnapshot = sellerCityStatePincodeSnapshot,
    sellerGstinSnapshot = sellerGstinSnapshot,
    sellerMobileSnapshot = sellerMobileSnapshot,
    sellerEmailSnapshot = sellerEmailSnapshot,
    sellerPanSnapshot = sellerPanSnapshot,
    sellerBankAccountNameSnapshot = sellerBankAccountNameSnapshot,
    sellerBankNameSnapshot = sellerBankNameSnapshot,
    sellerBankAccountNumberSnapshot = sellerBankAccountNumberSnapshot,
    sellerBankIfscSnapshot = sellerBankIfscSnapshot,
    sellerBankBranchSnapshot = sellerBankBranchSnapshot,
    sellerDeclarationSnapshot = sellerDeclarationSnapshot,
    sellerAuthorisedSignatorySnapshot = sellerAuthorisedSignatorySnapshot,
    clientCompanyNameSnapshot = clientCompanyNameSnapshot,
    clientAddressSnapshot = clientAddressSnapshot,
    clientGstinSnapshot = clientGstinSnapshot,
    clientStateSnapshot = clientStateSnapshot,
    clientStateCodeSnapshot = clientStateCodeSnapshot,
    clientEmailSnapshot = clientEmailSnapshot,
    clientPhoneSnapshot = clientPhoneSnapshot,
    clientPanSnapshot = clientPanSnapshot,
    clientIecSnapshot = clientIecSnapshot,
    clientOtherDetailsSnapshot = clientOtherDetailsSnapshot,
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
    taxTreatment = taxTreatment,
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
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED,
    lastSyncedAt = syncedAt,
    syncError = null
)

// DocumentLineItem Mappers
fun DocumentLineItemEntity.toDto(): DocumentLineItemDto = DocumentLineItemDto(
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

fun DocumentLineItemDto.toEntity(): DocumentLineItemEntity = DocumentLineItemEntity(
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

// ClientAccountEntry Mappers
fun ClientAccountEntryEntity.toDto(): ClientAccountEntryDto = ClientAccountEntryDto(
    id = id,
    clientId = clientId,
    documentId = documentId,
    entryType = entryType.name,
    entryDate = entryDate,
    amountPaise = amountPaise,
    narration = narration,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun ClientAccountEntryDto.toEntity(syncedAt: Long): ClientAccountEntryEntity = ClientAccountEntryEntity(
    id = id,
    clientId = clientId,
    documentId = documentId,
    entryType = try { AccountEntryType.valueOf(entryType) } catch (e: Exception) { AccountEntryType.INVOICE },
    entryDate = entryDate,
    amountPaise = amountPaise,
    narration = narration,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED,
    lastSyncedAt = syncedAt,
    syncError = null
)

// DocumentSequence Mappers
fun DocumentSequenceEntity.toDto(): DocumentSequenceDto = DocumentSequenceDto(
    documentType = documentType.name,
    financialYear = financialYear,
    lastSequenceNumber = lastSequenceNumber,
    updatedAt = updatedAt
)

fun DocumentSequenceDto.toEntity(): DocumentSequenceEntity = DocumentSequenceEntity(
    documentType = try { DocumentType.valueOf(documentType) } catch (e: Exception) { DocumentType.TAX_INVOICE },
    financialYear = financialYear,
    lastSequenceNumber = lastSequenceNumber,
    updatedAt = updatedAt
)
