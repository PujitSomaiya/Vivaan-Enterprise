package com.vivaanenterprise.app.domain.repository

import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.model.DocumentFinalizationResult
import com.vivaanenterprise.app.domain.model.DocumentLineItem
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeAllDocuments(): Flow<List<BusinessDocument>>
    fun observeDocumentById(id: String): Flow<BusinessDocument?>
    fun observeDocumentsByType(type: DocumentType): Flow<List<BusinessDocument>>
    fun observeDocumentsByClient(clientId: String): Flow<List<BusinessDocument>>

    suspend fun getDocumentById(id: String): BusinessDocument?
    suspend fun getLineItemsForDocument(documentId: String): List<DocumentLineItem>

    suspend fun suggestDocumentNumber(type: DocumentType, documentDate: Long): String

    suspend fun createDraft(
        type: DocumentType,
        clientId: String,
        documentDate: Long,
        documentNumber: String? = null,
        lineItems: List<DocumentLineItem> = emptyList(),
        placeOfSupply: String? = null,
        deliveryFactoryAddress: String? = null,
        paymentTerms: String? = null,
        deliveryNote: String? = null,
        supplierReference: String? = null,
        otherReferences: String? = null,
        buyerOrderNumber: String? = null,
        buyerOrderDate: Long? = null,
        dispatchDocumentNumber: String? = null,
        deliveryNoteDate: Long? = null,
        dispatchThrough: String? = null,
        destination: String? = null,
        termsOfDelivery: String? = null
    ): Result<BusinessDocument>

    suspend fun updateDraft(
        document: BusinessDocument,
        lineItems: List<DocumentLineItem>
    ): Result<BusinessDocument>

    /**
     * Finalizes a document directly from current editor values in one atomic operation.
     * If documentId is null (NEW document), creates directly as FINALIZED without an intermediate draft.
     * If documentId is non-null (existing DRAFT), updates the existing draft and finalizes atomically.
     */
    suspend fun finalizeDocument(
        input: com.vivaanenterprise.app.domain.model.DocumentFinalizationInput
    ): DocumentFinalizationResult

    /**
     * Finalizes a DRAFT document by ID.
     */
    suspend fun finalizeDocument(
        documentId: String,
        overrideDocumentNumber: String? = null
    ): DocumentFinalizationResult

    suspend fun cancelDocument(documentId: String): Result<Unit>

    suspend fun deleteDocument(documentId: String): Result<Unit>
}

