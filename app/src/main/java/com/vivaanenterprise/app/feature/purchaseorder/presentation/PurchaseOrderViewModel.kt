package com.vivaanenterprise.app.feature.purchaseorder.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.model.DocumentCalculationInput
import com.vivaanenterprise.app.domain.model.DocumentFinalizationResult
import com.vivaanenterprise.app.domain.model.DocumentLineItem
import com.vivaanenterprise.app.domain.model.DocumentValidationError
import com.vivaanenterprise.app.domain.model.Product
import com.vivaanenterprise.app.domain.repository.BusinessProfileRepository
import com.vivaanenterprise.app.domain.repository.ClientRepository
import com.vivaanenterprise.app.domain.repository.DocumentRepository
import com.vivaanenterprise.app.domain.repository.ProductRepository
import com.vivaanenterprise.app.domain.util.DocumentCalculator
import com.vivaanenterprise.app.domain.util.ExactCurrencyParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PurchaseOrderViewModel @Inject constructor(
    private val clientRepository: ClientRepository,
    private val productRepository: ProductRepository,
    private val documentRepository: DocumentRepository,
    private val profileRepository: BusinessProfileRepository,
    private val calculator: DocumentCalculator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editingDocumentId: String? = savedStateHandle.get<String>("documentId")

    private val _uiState = MutableStateFlow(PurchaseOrderUiState())
    val uiState: StateFlow<PurchaseOrderUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PurchaseOrderUiEffect>()
    val uiEffect: SharedFlow<PurchaseOrderUiEffect> = _uiEffect.asSharedFlow()

    private var sellerStateCode: String? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitialLoading = true) }

            val profile = profileRepository.getProfile()
            sellerStateCode = profile?.stateCode

            combine(
                clientRepository.observeClients(),
                productRepository.observeProducts()
            ) { clients, products ->
                clients to products.filter { it.isActive }
            }.collect { (clients, activeProducts) ->
                _uiState.update { current ->
                    current.copy(
                        availableClients = clients,
                        availableProducts = activeProducts
                    )
                }

                if (editingDocumentId == null && _uiState.value.isInitialLoading) {
                    initNewPurchaseOrder(clients, activeProducts)
                } else if (editingDocumentId != null && _uiState.value.isInitialLoading) {
                    loadDraftPurchaseOrder(editingDocumentId, clients, activeProducts)
                }
            }
        }
    }

    private suspend fun initNewPurchaseOrder(clients: List<Client>, activeProducts: List<Product>) {
        val suggestedNum = documentRepository.suggestDocumentNumber(
            DocumentType.PURCHASE_ORDER,
            _uiState.value.documentDate
        )

        val initialLines = if (activeProducts.isNotEmpty()) {
            val defaultProduct = activeProducts.firstOrNull { it.hsnSac == "3919" } ?: activeProducts.first()
            listOf(
                PurchaseOrderLineUiState(
                    id = UUID.randomUUID().toString(),
                    selectedProduct = defaultProduct,
                    quantityInput = "1",
                    rateInput = ""
                )
            )
        } else {
            listOf(
                PurchaseOrderLineUiState(
                    id = UUID.randomUUID().toString()
                )
            )
        }

        _uiState.update { current ->
            current.copy(
                mode = PurchaseOrderMode.NEW,
                documentNumber = suggestedNum,
                lineItems = initialLines,
                isInitialLoading = false
            )
        }
        recalculatePreview()
    }

    private suspend fun loadDraftPurchaseOrder(
        docId: String,
        clients: List<Client>,
        activeProducts: List<Product>
    ) {
        val document = documentRepository.getDocumentById(docId)
        if (document == null || document.status != DocumentStatus.DRAFT || document.documentType != DocumentType.PURCHASE_ORDER) {
            _uiState.update { it.copy(isInitialLoading = false, generalError = "Draft purchase order not found or not editable") }
            _uiEffect.emit(PurchaseOrderUiEffect.NavigateBack)
            return
        }

        val selectedClient = clients.firstOrNull { it.id == document.clientId }
        val lines = document.lineItems.map { lineItem ->
            val matchingProduct = activeProducts.firstOrNull { it.id == lineItem.productId }
                ?: Product(
                    id = lineItem.productId ?: "",
                    name = lineItem.descriptionSnapshot,
                    hsnSac = lineItem.hsnSacSnapshot ?: "",
                    defaultGstRateBasisPoints = lineItem.gstRateBasisPoints,
                    isActive = true,
                    createdAt = lineItem.createdAt,
                    updatedAt = lineItem.updatedAt
                )
            val rateHuman = if (lineItem.ratePaise > 0) {
                val rupees = lineItem.ratePaise / 100L
                val paise = lineItem.ratePaise % 100L
                if (paise == 0L) rupees.toString() else String.format("%d.%02d", rupees, paise)
            } else ""

            PurchaseOrderLineUiState(
                id = lineItem.id,
                selectedProduct = matchingProduct,
                quantityInput = lineItem.quantity.toString(),
                rateInput = rateHuman
            )
        }

        _uiState.update { current ->
            current.copy(
                mode = PurchaseOrderMode.EDIT_DRAFT,
                documentId = docId,
                documentNumber = document.documentNumber,
                isDocumentNumberManuallyEdited = true,
                documentDate = document.documentDate,
                selectedClient = selectedClient,
                deliveryFactoryAddress = document.deliveryFactoryAddress ?: selectedClient?.address ?: "",
                isDeliveryFactoryAddressManuallyEdited = !document.deliveryFactoryAddress.isNullOrBlank(),
                placeOfSupplyStateCode = document.placeOfSupply.orEmpty(),
                isPlaceOfSupplyManuallyEdited = document.placeOfSupply != null,
                lineItems = lines,
                deliveryNote = document.deliveryNote ?: "",
                paymentTerms = document.paymentTerms ?: "",
                supplierReference = document.supplierReference ?: "",
                otherReferences = document.otherReferences ?: "",
                buyerOrderNumber = document.buyerOrderNumber ?: "",
                buyerOrderDate = document.buyerOrderDate,
                dispatchDocumentNumber = document.dispatchDocumentNumber ?: "",
                deliveryNoteDate = document.deliveryNoteDate,
                dispatchThrough = document.dispatchThrough ?: "",
                destination = document.destination ?: "",
                termsOfDelivery = document.termsOfDelivery ?: "",
                isInitialLoading = false,
                isDirty = false
            )
        }
        recalculatePreview()
    }

    fun onIntent(intent: PurchaseOrderUiIntent) {
        when (intent) {
            is PurchaseOrderUiIntent.OnDocumentNumberChange -> handleDocumentNumberChange(intent.number)
            is PurchaseOrderUiIntent.OnDocumentDateChange -> handleDocumentDateChange(intent.dateMillis)
            is PurchaseOrderUiIntent.OnSelectClient -> handleSelectClient(intent.client)
            is PurchaseOrderUiIntent.OnDeliveryFactoryAddressChange -> handleDeliveryFactoryAddressChange(intent.address)
            is PurchaseOrderUiIntent.OnPlaceOfSupplyChange -> handlePlaceOfSupplyChange(intent.stateCode)
            is PurchaseOrderUiIntent.OnAddLineItem -> handleAddLineItem()
            is PurchaseOrderUiIntent.OnRemoveLineItem -> handleRemoveLineItem(intent.lineId)
            is PurchaseOrderUiIntent.OnSelectProduct -> handleSelectProduct(intent.lineId, intent.product)
            is PurchaseOrderUiIntent.OnQuantityChange -> handleQuantityChange(intent.lineId, intent.quantity)
            is PurchaseOrderUiIntent.OnRateChange -> handleRateChange(intent.lineId, intent.rate)
            is PurchaseOrderUiIntent.OnMetadataChange -> handleMetadataChange(intent)
            PurchaseOrderUiIntent.OnSaveDraft -> handleSaveDraft()
            PurchaseOrderUiIntent.OnRequestFinalize -> handleRequestFinalize()
            PurchaseOrderUiIntent.OnConfirmFinalize -> handleConfirmFinalize()
            PurchaseOrderUiIntent.OnDismissFinalizeConfirm -> _uiState.update { it.copy(showFinalizeConfirmDialog = false) }
            PurchaseOrderUiIntent.OnClearGeneralError -> _uiState.update { it.copy(generalError = null) }
        }
    }

    private fun handleDocumentNumberChange(number: String) {
        _uiState.update { current ->
            current.copy(
                documentNumber = number,
                isDocumentNumberManuallyEdited = true,
                documentNumberError = null,
                isDirty = true
            )
        }
    }

    private fun handleDocumentDateChange(dateMillis: Long) {
        val current = _uiState.value
        viewModelScope.launch {
            val updatedNum = if (!current.isDocumentNumberManuallyEdited) {
                documentRepository.suggestDocumentNumber(DocumentType.PURCHASE_ORDER, dateMillis)
            } else {
                current.documentNumber
            }

            _uiState.update { state ->
                state.copy(
                    documentDate = dateMillis,
                    isDocumentDateTouched = true,
                    documentNumber = updatedNum,
                    isDirty = true
                )
            }
            recalculatePreview()
        }
    }

    private fun handleSelectClient(client: Client) {
        _uiState.update { current ->
            val updatedStateCode = if (!current.isPlaceOfSupplyManuallyEdited && !client.stateCode.isNullOrBlank() && client.stateCode.length == 2) {
                client.stateCode
            } else {
                current.placeOfSupplyStateCode
            }

            val updatedDeliveryAddress = if (!current.isDeliveryFactoryAddressManuallyEdited && !client.address.isNullOrBlank()) {
                client.address
            } else {
                current.deliveryFactoryAddress
            }

            current.copy(
                selectedClient = client,
                deliveryFactoryAddress = updatedDeliveryAddress,
                placeOfSupplyStateCode = updatedStateCode,
                clientError = null,
                isDirty = true
            )
        }
        recalculatePreview()
    }

    private fun handleDeliveryFactoryAddressChange(address: String) {
        _uiState.update { current ->
            current.copy(
                deliveryFactoryAddress = address,
                isDeliveryFactoryAddressManuallyEdited = true,
                deliveryFactoryAddressError = null,
                isDirty = true
            )
        }
    }

    private fun handlePlaceOfSupplyChange(stateCode: String) {
        _uiState.update { current ->
            current.copy(
                placeOfSupplyStateCode = stateCode,
                isPlaceOfSupplyManuallyEdited = true,
                placeOfSupplyError = null,
                isDirty = true
            )
        }
        recalculatePreview()
    }

    private fun handleAddLineItem() {
        val activeProducts = _uiState.value.availableProducts
        val defaultProduct = activeProducts.firstOrNull()
        val newLine = PurchaseOrderLineUiState(
            id = UUID.randomUUID().toString(),
            selectedProduct = defaultProduct
        )
        _uiState.update { current ->
            current.copy(
                lineItems = current.lineItems + newLine,
                isDirty = true
            )
        }
        recalculatePreview()
    }

    private fun handleRemoveLineItem(lineId: String) {
        val currentLines = _uiState.value.lineItems
        if (currentLines.size <= 1) return
        _uiState.update { current ->
            current.copy(
                lineItems = current.lineItems.filterNot { it.id == lineId },
                isDirty = true
            )
        }
        recalculatePreview()
    }

    private fun handleSelectProduct(lineId: String, product: Product) {
        _uiState.update { current ->
            val updatedLines = current.lineItems.map { line ->
                if (line.id == lineId) {
                    line.copy(selectedProduct = product, productError = null)
                } else line
            }
            current.copy(lineItems = updatedLines, isDirty = true)
        }
        recalculatePreview()
    }

    private fun handleQuantityChange(lineId: String, quantityStr: String) {
        _uiState.update { current ->
            val updatedLines = current.lineItems.map { line ->
                if (line.id == lineId) {
                    line.copy(quantityInput = quantityStr, quantityError = null)
                } else line
            }
            current.copy(lineItems = updatedLines, isDirty = true)
        }
        recalculatePreview()
    }

    private fun handleRateChange(lineId: String, rateStr: String) {
        _uiState.update { current ->
            val updatedLines = current.lineItems.map { line ->
                if (line.id == lineId) {
                    line.copy(rateInput = rateStr, rateError = null)
                } else line
            }
            current.copy(lineItems = updatedLines, isDirty = true)
        }
        recalculatePreview()
    }

    private fun handleMetadataChange(intent: PurchaseOrderUiIntent.OnMetadataChange) {
        _uiState.update { current ->
            current.copy(
                deliveryNote = intent.deliveryNote ?: current.deliveryNote,
                paymentTerms = intent.paymentTerms ?: current.paymentTerms,
                supplierReference = intent.supplierReference ?: current.supplierReference,
                otherReferences = intent.otherReferences ?: current.otherReferences,
                buyerOrderNumber = intent.buyerOrderNumber ?: current.buyerOrderNumber,
                buyerOrderDate = intent.buyerOrderDate ?: current.buyerOrderDate,
                dispatchDocumentNumber = intent.dispatchDocumentNumber ?: current.dispatchDocumentNumber,
                deliveryNoteDate = intent.deliveryNoteDate ?: current.deliveryNoteDate,
                dispatchThrough = intent.dispatchThrough ?: current.dispatchThrough,
                destination = intent.destination ?: current.destination,
                termsOfDelivery = intent.termsOfDelivery ?: current.termsOfDelivery,
                isDirty = true
            )
        }
    }

    private fun recalculatePreview() {
        val state = _uiState.value
        val client = state.selectedClient
        val sellerState = sellerStateCode
        if (client == null || state.lineItems.isEmpty() || sellerState.isNullOrBlank() || sellerState.length != 2) {
            _uiState.update { it.copy(calculationPreview = null) }
            return
        }

        val calcLines = mutableListOf<DocumentCalculationInput.LineInput>()
        for (line in state.lineItems) {
            val prod = line.selectedProduct ?: return clearingPreview()
            val qty = line.quantityInput.toLongOrNull() ?: return clearingPreview()
            if (qty <= 0) return clearingPreview()
            val ratePaise = ExactCurrencyParser.parseToPaise(line.rateInput) ?: return clearingPreview()

            calcLines.add(
                DocumentCalculationInput.LineInput(
                    id = line.id,
                    quantity = qty,
                    ratePaise = ratePaise,
                    gstRateBasisPoints = prod.defaultGstRateBasisPoints
                )
            )
        }

        val input = DocumentCalculationInput(
            sellerStateCode = sellerState,
            placeOfSupplyStateCode = state.placeOfSupplyStateCode,
            lines = calcLines
        )

        val outcome = calculator.calculate(input)
        val previewResult = outcome.getOrNull()
        _uiState.update { it.copy(calculationPreview = previewResult) }
    }

    private fun clearingPreview() {
        _uiState.update { it.copy(calculationPreview = null) }
    }

    private fun validateForm(forFinalization: Boolean = false): Boolean {
        var isValid = true
        val state = _uiState.value

        var docNumErr: String? = null
        if (state.documentNumber.isBlank()) {
            docNumErr = "Document number is required"
            isValid = false
        }

        var clientErr: String? = null
        if (state.selectedClient == null) {
            clientErr = "Supplier / Client is required"
            isValid = false
        }

        var delAddrErr: String? = null
        if (forFinalization && state.deliveryFactoryAddress.isBlank()) {
            delAddrErr = "Delivery / Factory address is required"
            isValid = false
        }

        var posErr: String? = null
        if (state.placeOfSupplyStateCode.isBlank() || state.placeOfSupplyStateCode.length != 2) {
            posErr = "Valid 2-digit place of supply state code required"
            isValid = false
        }

        val updatedLines = state.lineItems.map { line ->
            var lineValid = true
            var prodErr: String? = null
            var qtyErr: String? = null
            var rateErr: String? = null

            if (line.selectedProduct == null) {
                prodErr = "Product is required"
                lineValid = false
            }

            val qty = line.quantityInput.toLongOrNull()
            if (qty == null || qty <= 0) {
                qtyErr = "Quantity must be greater than zero"
                lineValid = false
            }

            val ratePaise = ExactCurrencyParser.parseToPaise(line.rateInput)
            if (ratePaise == null || ratePaise < 0) {
                rateErr = "Enter a valid rate"
                lineValid = false
            }

            if (!lineValid) isValid = false

            line.copy(
                productError = prodErr,
                quantityError = qtyErr,
                rateError = rateErr
            )
        }

        _uiState.update { current ->
            current.copy(
                documentNumberError = docNumErr,
                clientError = clientErr,
                deliveryFactoryAddressError = delAddrErr,
                placeOfSupplyError = posErr,
                lineItems = updatedLines
            )
        }

        return isValid
    }

    private fun handleSaveDraft() {
        if (_uiState.value.isSavingDraft || _uiState.value.isFinalizing) return
        if (!validateForm(forFinalization = false)) return

        val state = _uiState.value
        val client = state.selectedClient ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingDraft = true) }
            try {
                val domainLineItems = state.lineItems.mapIndexed { idx, line ->
                    val prod = line.selectedProduct ?: throw IllegalStateException("Product missing")
                    val ratePaise = ExactCurrencyParser.parseToPaise(line.rateInput) ?: throw IllegalStateException("Rate invalid")
                    DocumentLineItem(
                        id = line.id,
                        documentId = state.documentId ?: "",
                        productId = prod.id,
                        position = idx,
                        descriptionSnapshot = prod.name,
                        hsnSacSnapshot = prod.hsnSac,
                        quantity = line.quantityInput.toLong(),
                        ratePaise = ratePaise,
                        gstRateBasisPoints = prod.defaultGstRateBasisPoints,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }

                val result = if (state.mode == PurchaseOrderMode.NEW || state.documentId == null) {
                    documentRepository.createDraft(
                        type = DocumentType.PURCHASE_ORDER,
                        clientId = client.id,
                        documentDate = state.documentDate,
                        documentNumber = state.documentNumber,
                        lineItems = domainLineItems,
                        placeOfSupply = state.placeOfSupplyStateCode,
                        deliveryFactoryAddress = state.deliveryFactoryAddress.ifBlank { null },
                        paymentTerms = state.paymentTerms.ifBlank { null },
                        deliveryNote = state.deliveryNote.ifBlank { null },
                        supplierReference = state.supplierReference.ifBlank { null },
                        otherReferences = state.otherReferences.ifBlank { null },
                        buyerOrderNumber = state.buyerOrderNumber.ifBlank { null },
                        buyerOrderDate = state.buyerOrderDate,
                        dispatchDocumentNumber = state.dispatchDocumentNumber.ifBlank { null },
                        deliveryNoteDate = state.deliveryNoteDate,
                        dispatchThrough = state.dispatchThrough.ifBlank { null },
                        destination = state.destination.ifBlank { null },
                        termsOfDelivery = state.termsOfDelivery.ifBlank { null }
                    )
                } else {
                    val existingDoc = documentRepository.getDocumentById(state.documentId)
                    if (existingDoc == null) {
                        _uiState.update { it.copy(isSavingDraft = false, generalError = "Draft purchase order not found") }
                        return@launch
                    }
                    val updatedDoc = existingDoc.copy(
                        documentNumber = state.documentNumber,
                        documentDate = state.documentDate,
                        clientId = client.id,
                        placeOfSupply = state.placeOfSupplyStateCode,
                        deliveryFactoryAddress = state.deliveryFactoryAddress.ifBlank { null },
                        deliveryNote = state.deliveryNote.ifBlank { null },
                        paymentTerms = state.paymentTerms.ifBlank { null },
                        supplierReference = state.supplierReference.ifBlank { null },
                        otherReferences = state.otherReferences.ifBlank { null },
                        buyerOrderNumber = state.buyerOrderNumber.ifBlank { null },
                        buyerOrderDate = state.buyerOrderDate,
                        dispatchDocumentNumber = state.dispatchDocumentNumber.ifBlank { null },
                        deliveryNoteDate = state.deliveryNoteDate,
                        dispatchThrough = state.dispatchThrough.ifBlank { null },
                        destination = state.destination.ifBlank { null },
                        termsOfDelivery = state.termsOfDelivery.ifBlank { null }
                    )
                    documentRepository.updateDraft(updatedDoc, domainLineItems)
                }

                if (result.isSuccess) {
                    val savedDoc = result.getOrThrow()
                    _uiState.update { current ->
                        current.copy(
                            mode = PurchaseOrderMode.EDIT_DRAFT,
                            documentId = savedDoc.id,
                            isSavingDraft = false,
                            isDirty = false
                        )
                    }
                    _uiEffect.emit(PurchaseOrderUiEffect.ShowSnackbar("Draft purchase order saved successfully"))
                } else {
                    _uiState.update { it.copy(isSavingDraft = false, generalError = "Unable to save purchase order") }
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                _uiState.update { it.copy(isSavingDraft = false) }
                throw ce
            } catch (e: Exception) {
                _uiState.update { it.copy(isSavingDraft = false, generalError = "Unable to save purchase order") }
            }
        }
    }

    private fun handleRequestFinalize() {
        if (_uiState.value.isSavingDraft || _uiState.value.isFinalizing) return
        if (!validateForm(forFinalization = true)) return

        _uiState.update { it.copy(showFinalizeConfirmDialog = true) }
    }

    private fun handleConfirmFinalize() {
        if (_uiState.value.isFinalizing) return
        _uiState.update { it.copy(showFinalizeConfirmDialog = false, isFinalizing = true) }
        val state = _uiState.value
        val client = state.selectedClient
        if (client == null) {
            _uiState.update { it.copy(isFinalizing = false, generalError = "Please select a client") }
            return
        }

        viewModelScope.launch {
            try {
                val domainLineItems = state.lineItems.mapIndexed { idx, line ->
                    val prod = line.selectedProduct ?: throw IllegalStateException("Product missing")
                    val ratePaise = ExactCurrencyParser.parseToPaise(line.rateInput) ?: throw IllegalStateException("Rate invalid")
                    DocumentLineItem(
                        id = line.id,
                        documentId = state.documentId ?: "",
                        productId = prod.id,
                        position = idx,
                        descriptionSnapshot = prod.name,
                        hsnSacSnapshot = prod.hsnSac,
                        quantity = line.quantityInput.toLong(),
                        ratePaise = ratePaise,
                        gstRateBasisPoints = prod.defaultGstRateBasisPoints,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }

                val input = com.vivaanenterprise.app.domain.model.DocumentFinalizationInput(
                    documentId = state.documentId,
                    documentType = DocumentType.PURCHASE_ORDER,
                    documentNumber = state.documentNumber,
                    documentDate = state.documentDate,
                    clientId = client.id,
                    placeOfSupply = state.placeOfSupplyStateCode,
                    deliveryFactoryAddress = state.deliveryFactoryAddress.ifBlank { null },
                    lineItems = domainLineItems,
                    paymentTerms = state.paymentTerms.ifBlank { null },
                    deliveryNote = state.deliveryNote.ifBlank { null },
                    supplierReference = state.supplierReference.ifBlank { null },
                    otherReferences = state.otherReferences.ifBlank { null },
                    buyerOrderNumber = state.buyerOrderNumber.ifBlank { null },
                    buyerOrderDate = state.buyerOrderDate,
                    dispatchDocumentNumber = state.dispatchDocumentNumber.ifBlank { null },
                    deliveryNoteDate = state.deliveryNoteDate,
                    dispatchThrough = state.dispatchThrough.ifBlank { null },
                    destination = state.destination.ifBlank { null },
                    termsOfDelivery = state.termsOfDelivery.ifBlank { null }
                )

                val finalizationResult = documentRepository.finalizeDocument(input)

                when (finalizationResult) {
                    is DocumentFinalizationResult.Success -> {
                        _uiState.update { it.copy(isFinalizing = false, isDirty = false) }
                        _uiEffect.emit(PurchaseOrderUiEffect.NavigateSuccess(finalizationResult.document.id))
                    }
                    is DocumentFinalizationResult.Invalid -> {
                        val isDuplicate = finalizationResult.errors.contains(DocumentValidationError.DuplicateDocumentNumber)
                        val isMissingProfile = finalizationResult.errors.contains(DocumentValidationError.MissingSellerProfile)

                        if (isDuplicate) {
                            _uiState.update {
                                it.copy(
                                    isFinalizing = false,
                                    documentNumberError = "A purchase order with this number already exists.",
                                    generalError = null
                                )
                            }
                        } else {
                            val msg = if (isMissingProfile) "Seller profile is incomplete." else "Unable to finalize purchase order. Please check all fields."
                            _uiState.update { it.copy(isFinalizing = false, generalError = msg) }
                        }
                    }
                    is DocumentFinalizationResult.Failure -> {
                        _uiState.update { it.copy(isFinalizing = false, generalError = "Unable to finalize purchase order. Please try again.") }
                    }
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                _uiState.update { it.copy(isFinalizing = false) }
                throw ce
            } catch (e: Exception) {
                _uiState.update { it.copy(isFinalizing = false, generalError = "Unable to finalize purchase order. Please try again.") }
            }
        }
    }
}
