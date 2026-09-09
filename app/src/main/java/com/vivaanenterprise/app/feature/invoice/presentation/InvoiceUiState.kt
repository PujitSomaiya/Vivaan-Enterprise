package com.vivaanenterprise.app.feature.invoice.presentation

import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.model.DocumentCalculationResult
import com.vivaanenterprise.app.domain.model.Product

enum class InvoiceMode {
    NEW,
    EDIT_DRAFT
}

data class InvoiceLineUiState(
    val id: String,
    val selectedProduct: Product? = null,
    val quantityInput: String = "1",
    val rateInput: String = "",
    val quantityError: String? = null,
    val rateError: String? = null,
    val productError: String? = null
)

data class InvoiceUiState(
    val mode: InvoiceMode = InvoiceMode.NEW,
    val documentId: String? = null,
    val documentNumber: String = "",
    val isDocumentNumberManuallyEdited: Boolean = false,
    val documentDate: Long = System.currentTimeMillis(),
    val isDocumentDateTouched: Boolean = false,
    val selectedClient: Client? = null,
    val placeOfSupplyStateCode: String = "", // Unset until selected or auto-derived from valid client stateCode
    val isPlaceOfSupplyManuallyEdited: Boolean = false,
    val lineItems: List<InvoiceLineUiState> = emptyList(),

    // Optional invoice metadata
    val deliveryNote: String = "",
    val paymentTerms: String = "",
    val supplierReference: String = "",
    val otherReferences: String = "",
    val buyerOrderNumber: String = "",
    val buyerOrderDate: Long? = null,
    val dispatchDocumentNumber: String = "",
    val deliveryNoteDate: Long? = null,
    val dispatchThrough: String = "",
    val destination: String = "",
    val termsOfDelivery: String = "",

    // Data lists for selection
    val availableClients: List<Client> = emptyList(),
    val availableProducts: List<Product> = emptyList(),

    // Calculation preview
    val calculationPreview: DocumentCalculationResult? = null,

    // Loading & Action states
    val isInitialLoading: Boolean = true,
    val isSavingDraft: Boolean = false,
    val isFinalizing: Boolean = false,
    val isDirty: Boolean = false,

    // Validation & Error states
    val documentNumberError: String? = null,
    val clientError: String? = null,
    val placeOfSupplyError: String? = null,
    val generalError: String? = null,
    val showFinalizeConfirmDialog: Boolean = false
)

sealed interface InvoiceUiIntent {
    data class OnDocumentNumberChange(val number: String) : InvoiceUiIntent
    data class OnDocumentDateChange(val dateMillis: Long) : InvoiceUiIntent
    data class OnSelectClient(val client: Client) : InvoiceUiIntent
    data class OnPlaceOfSupplyChange(val stateCode: String) : InvoiceUiIntent

    data object OnAddLineItem : InvoiceUiIntent
    data class OnRemoveLineItem(val lineId: String) : InvoiceUiIntent
    data class OnSelectProduct(val lineId: String, val product: Product) : InvoiceUiIntent
    data class OnQuantityChange(val lineId: String, val quantity: String) : InvoiceUiIntent
    data class OnRateChange(val lineId: String, val rate: String) : InvoiceUiIntent

    data class OnMetadataChange(
        val deliveryNote: String? = null,
        val paymentTerms: String? = null,
        val supplierReference: String? = null,
        val otherReferences: String? = null,
        val buyerOrderNumber: String? = null,
        val buyerOrderDate: Long? = null,
        val dispatchDocumentNumber: String? = null,
        val deliveryNoteDate: Long? = null,
        val dispatchThrough: String? = null,
        val destination: String? = null,
        val termsOfDelivery: String? = null
    ) : InvoiceUiIntent

    data object OnSaveDraft : InvoiceUiIntent
    data object OnRequestFinalize : InvoiceUiIntent
    data object OnConfirmFinalize : InvoiceUiIntent
    data object OnDismissFinalizeConfirm : InvoiceUiIntent
    data object OnClearGeneralError : InvoiceUiIntent
}

sealed interface InvoiceUiEffect {
    data class ShowSnackbar(val message: String) : InvoiceUiEffect
    data object NavigateBack : InvoiceUiEffect
    data class NavigateSuccess(val documentId: String) : InvoiceUiEffect
}
