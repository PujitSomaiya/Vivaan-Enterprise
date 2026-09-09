package com.vivaanenterprise.app.domain.pdf

import com.vivaanenterprise.app.domain.model.BusinessDocument

/**
 * Result of a PDF generation request.
 */
sealed interface PdfGenerationResult {
    data class Success(val pdfBytes: ByteArray) : PdfGenerationResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Success
            return pdfBytes.contentEquals(other.pdfBytes)
        }

        override fun hashCode(): Int {
            return pdfBytes.contentHashCode()
        }
    }

    sealed interface Failure : PdfGenerationResult {
        data object DocumentNotFinalized : Failure
        data object MissingSellerSnapshot : Failure
        data object MissingClientSnapshot : Failure
        data object NoLineItems : Failure
        data object MissingHistoricalTaxTreatment : Failure
        data class Error(val exception: Throwable) : Failure
    }
}

/**
 * Pure domain contract for generating PDF documents from finalized historical snapshots.
 */
interface BusinessDocumentPdfGenerator {
    suspend fun generatePdf(document: BusinessDocument): PdfGenerationResult
}
