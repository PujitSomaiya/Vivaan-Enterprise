package com.vivaanenterprise.app.domain.usecase

import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.pdf.BusinessDocumentPdfGenerator
import com.vivaanenterprise.app.domain.pdf.PdfGenerationResult
import com.vivaanenterprise.app.domain.repository.DocumentRepository
import javax.inject.Inject

/**
 * UseCase to generate PDF byte array for a finalized business document.
 * Coordinates with [DocumentRepository] to fetch finalized line items and snapshot details.
 */
open class GenerateBusinessDocumentPdfUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val pdfGenerator: BusinessDocumentPdfGenerator
) {

    open suspend fun getDocument(documentId: String): BusinessDocument? {
        return documentRepository.getDocumentById(documentId)
    }

    open suspend operator fun invoke(documentId: String): PdfGenerationResult {
        val document = documentRepository.getDocumentById(documentId)
            ?: return PdfGenerationResult.Failure.Error(IllegalArgumentException("Document not found"))
        return pdfGenerator.generatePdf(document)
    }

    suspend operator fun invoke(document: BusinessDocument): PdfGenerationResult {
        val fullDoc = if (document.lineItems.isEmpty()) {
            documentRepository.getDocumentById(document.id) ?: document
        } else {
            document
        }
        return pdfGenerator.generatePdf(fullDoc)
    }
}
