package com.vivaanenterprise.app.domain.model

import com.vivaanenterprise.app.core.common.DocumentType

/**
 * Immutable domain input containing current editor values required to finalize a document
 * directly in one atomic repository operation without first persisting an intermediate draft.
 */
data class DocumentFinalizationInput(
    val documentId: String? = null,
    val documentType: DocumentType,
    val documentNumber: String,
    val documentDate: Long,
    val clientId: String,
    val placeOfSupply: String?,
    val deliveryFactoryAddress: String? = null,
    val lineItems: List<DocumentLineItem>,
    val paymentTerms: String? = null,
    val deliveryNote: String? = null,
    val supplierReference: String? = null,
    val otherReferences: String? = null,
    val buyerOrderNumber: String? = null,
    val buyerOrderDate: Long? = null,
    val dispatchDocumentNumber: String? = null,
    val deliveryNoteDate: Long? = null,
    val dispatchThrough: String? = null,
    val destination: String? = null,
    val termsOfDelivery: String? = null
)
