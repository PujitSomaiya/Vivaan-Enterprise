package com.vivaanenterprise.app.feature.purchaseorder.presentation

import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.model.DocumentCalculationResult
import com.vivaanenterprise.app.domain.model.Product

import com.vivaanenterprise.app.feature.document.presentation.model.DocumentLineUiState

enum class PurchaseOrderMode {
    NEW,
    EDIT_DRAFT
}

typealias PurchaseOrderLineUiState = DocumentLineUiState

data class PurchaseOrderUiState(
    val mode: PurchaseOrderMode = PurchaseOrderMode.NEW,
    val documentId: String? = null,
    val documentNumber: String = "",
    val isDocumentNumberManuallyEdited: Boolean = false,
    val documentDate: Long = System.currentTimeMillis(),
    val isDocumentDateTouched: Boolean = false,
    val selectedClient: Client? = null,
    val deliveryFactoryAddress: String = "",
    val isDeliveryFactoryAddressManuallyEdited: Boolean = false,
    val placeOfSupplyStateCode: String = "",
    val isPlaceOfSupplyManuallyEdited: Boolean = false,
    val lineItems: List<PurchaseOrderLineUiState> = emptyList(),

    // Optional PO metadata
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
    val deliveryFactoryAddressError: String? = null,
    val placeOfSupplyError: String? = null,
    val generalError: String? = null,
    val showFinalizeConfirmDialog: Boolean = false
)

sealed interface PurchaseOrderUiIntent {
    data class OnDocumentNumberChange(val number: String) : PurchaseOrderUiIntent
    data class OnDocumentDateChange(val dateMillis: Long) : PurchaseOrderUiIntent
    data class OnSelectClient(val client: Client) : PurchaseOrderUiIntent
    data class OnDeliveryFactoryAddressChange(val address: String) : PurchaseOrderUiIntent
    data class OnPlaceOfSupplyChange(val stateCode: String) : PurchaseOrderUiIntent

    data object OnAddLineItem : PurchaseOrderUiIntent
    data class OnRemoveLineItem(val lineId: String) : PurchaseOrderUiIntent
    data class OnSelectProduct(val lineId: String, val product: Product) : PurchaseOrderUiIntent
    data class OnQuantityChange(val lineId: String, val quantity: String) : PurchaseOrderUiIntent
    data class OnRateChange(val lineId: String, val rate: String) : PurchaseOrderUiIntent

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
    ) : PurchaseOrderUiIntent

    data object OnSaveDraft : PurchaseOrderUiIntent
    data object OnRequestFinalize : PurchaseOrderUiIntent
    data object OnConfirmFinalize : PurchaseOrderUiIntent
    data object OnDismissFinalizeConfirm : PurchaseOrderUiIntent
    data object OnClearGeneralError : PurchaseOrderUiIntent
}

sealed interface PurchaseOrderUiEffect {
    data class ShowSnackbar(val message: String) : PurchaseOrderUiEffect
    data object NavigateBack : PurchaseOrderUiEffect
    data class NavigateSuccess(val documentId: String) : PurchaseOrderUiEffect
}
