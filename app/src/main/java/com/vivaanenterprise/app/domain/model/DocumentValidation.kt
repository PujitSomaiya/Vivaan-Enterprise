package com.vivaanenterprise.app.domain.model

sealed interface DocumentValidationError {
    data object BlankDocumentNumber : DocumentValidationError
    data object DuplicateDocumentNumber : DocumentValidationError
    data object MissingSellerProfile : DocumentValidationError
    data object MissingClient : DocumentValidationError
    data object ClientDeleted : DocumentValidationError
    data object ProductNotFound : DocumentValidationError
    data object ProductDeleted : DocumentValidationError
    data object NoLineItems : DocumentValidationError
    data object InvalidQuantity : DocumentValidationError
    data object NegativeRate : DocumentValidationError
    data object DocumentNotDraft : DocumentValidationError
    /** The document's place-of-supply is null, blank, or is not a valid two-digit numeric state code. */
    data object MissingPlaceOfSupply : DocumentValidationError
    /** The internal [DocumentCalculator] returned an error (e.g. arithmetic overflow, invalid GST rate). */
    data object InvalidCalculationResult : DocumentValidationError
    data object FinalizedInvoiceCancellationUnsupported : DocumentValidationError
}

sealed interface DocumentFinalizationResult {
    data class Success(val document: BusinessDocument) : DocumentFinalizationResult
    data class Invalid(val errors: List<DocumentValidationError>) : DocumentFinalizationResult
    data class Failure(val throwable: Throwable) : DocumentFinalizationResult
}
