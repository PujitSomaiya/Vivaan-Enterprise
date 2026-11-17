package com.vivaanenterprise.app.feature.purchaseorder.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivaanenterprise.app.core.designsystem.component.AppConfirmationDialog
import com.vivaanenterprise.app.core.designsystem.component.AppDatePickerDialog
import com.vivaanenterprise.app.core.designsystem.component.AppErrorDialog
import com.vivaanenterprise.app.core.designsystem.component.AppErrorState
import com.vivaanenterprise.app.core.designsystem.component.AppLoadingState
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.AppTextField
import com.vivaanenterprise.app.core.designsystem.component.AppTopBar
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.designsystem.theme.VivaanEnterpriseTheme
import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.model.DocumentCalculationResult
import com.vivaanenterprise.app.domain.model.DocumentLineCalculation
import com.vivaanenterprise.app.domain.model.Product
import com.vivaanenterprise.app.domain.model.TaxTreatment
import com.vivaanenterprise.app.feature.document.presentation.components.ClientSelectorBottomSheet
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentAdditionalDetailsSection
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentCalculationSummary
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentFormActionButtons
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentHeaderSection
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentLineItemCard
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentLineItemsHeader
import com.vivaanenterprise.app.feature.document.presentation.components.PlaceOfSupplyBottomSheet
import com.vivaanenterprise.app.feature.document.presentation.model.DocumentLineUiState

@Composable
fun PurchaseOrderRoute(
    onNavigateBack: () -> Unit,
    onSaveSuccess: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is PurchaseOrderUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                PurchaseOrderUiEffect.NavigateBack -> onNavigateBack()
                is PurchaseOrderUiEffect.NavigateSuccess -> onSaveSuccess(effect.documentId)
            }
        }
    }

    PurchaseOrderScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderScreen(
    uiState: PurchaseOrderUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (PurchaseOrderUiIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showClientSheet by remember { mutableStateOf(false) }
    var showPosSheet by remember { mutableStateOf(false) }
    var isMetadataExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    BackHandler(enabled = uiState.isDirty) {
        showDiscardDialog = true
    }

    // Non-field operation error dialog
    uiState.generalError?.let { err ->
        if (uiState.availableClients.isNotEmpty()) {
            AppErrorDialog(
                message = err,
                onDismiss = { onIntent(PurchaseOrderUiIntent.OnClearGeneralError) }
            )
        }
    }

    // Auto-scroll to first field error if any validation error occurs
    val hasFieldErrors = uiState.documentNumberError != null ||
            uiState.clientError != null ||
            uiState.deliveryFactoryAddressError != null ||
            uiState.placeOfSupplyError != null ||
            uiState.lineItems.any { it.productError != null || it.quantityError != null || it.rateError != null }

    LaunchedEffect(hasFieldErrors) {
        if (hasFieldErrors) {
            when {
                uiState.documentNumberError != null || uiState.clientError != null || uiState.deliveryFactoryAddressError != null || uiState.placeOfSupplyError != null -> {
                    listState.animateScrollToItem(0)
                }
                uiState.lineItems.any { it.productError != null || it.quantityError != null || it.rateError != null } -> {
                    val firstInvalidIndex = uiState.lineItems.indexOfFirst {
                        it.productError != null || it.quantityError != null || it.rateError != null
                    }
                    if (firstInvalidIndex >= 0) {
                        listState.animateScrollToItem(firstInvalidIndex + 2)
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AppConfirmationDialog(
            title = "Discard Unsaved Changes?",
            message = "You have unsaved changes in this purchase order. Are you sure you want to leave?",
            confirmLabel = "Discard",
            cancelLabel = "Continue Editing",
            isDestructive = true,
            onConfirm = {
                showDiscardDialog = false
                onNavigateBack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }

    if (uiState.showFinalizeConfirmDialog) {
        AppConfirmationDialog(
            title = "Finalize Purchase Order",
            message = "Are you sure you want to finalize this purchase order? Once finalized, line items and supplier details will be frozen and cannot be modified.",
            confirmLabel = "Finalize PO",
            onConfirm = { onIntent(PurchaseOrderUiIntent.OnConfirmFinalize) },
            onDismiss = { onIntent(PurchaseOrderUiIntent.OnDismissFinalizeConfirm) }
        )
    }

    if (showDatePickerDialog) {
        AppDatePickerDialog(
            initialSelectedDateMillis = uiState.documentDate,
            onDateSelected = { onIntent(PurchaseOrderUiIntent.OnDocumentDateChange(it)) },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    if (showClientSheet) {
        ClientSelectorBottomSheet(
            clients = uiState.availableClients,
            selectedClient = uiState.selectedClient,
            onSelectClient = { client ->
                showClientSheet = false
                onIntent(PurchaseOrderUiIntent.OnSelectClient(client))
            },
            onDismiss = { showClientSheet = false }
        )
    }

    if (showPosSheet) {
        PlaceOfSupplyBottomSheet(
            selectedStateCode = uiState.placeOfSupplyStateCode,
            onSelectState = { indianState ->
                showPosSheet = false
                onIntent(PurchaseOrderUiIntent.OnPlaceOfSupplyChange(indianState.code))
            },
            onDismiss = { showPosSheet = false }
        )
    }

    val title = if (uiState.mode == PurchaseOrderMode.NEW) "New Purchase Order" else "Edit Purchase Order Draft"

    AppScaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = title,
                onBackClick = {
                    if (uiState.isDirty) {
                        showDiscardDialog = true
                    } else {
                        onNavigateBack()
                    }
                }
            )
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        when {
            uiState.isInitialLoading -> {
                AppLoadingState(modifier = Modifier.padding(innerPadding))
            }
            uiState.generalError != null && uiState.availableClients.isEmpty() -> {
                AppErrorState(
                    message = uiState.generalError,
                    onRetryClick = { onIntent(PurchaseOrderUiIntent.OnClearGeneralError) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = AppTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
                ) {
                    item {
                        DocumentHeaderSection(
                            documentNumber = uiState.documentNumber,
                            onDocumentNumberChange = { onIntent(PurchaseOrderUiIntent.OnDocumentNumberChange(it)) },
                            documentNumberLabel = "PO Number *",
                            documentNumberError = uiState.documentNumberError,
                            documentDate = uiState.documentDate,
                            onDateClick = { showDatePickerDialog = true },
                            dateLabel = "PO Date *",
                            selectedClientName = uiState.selectedClient?.companyName ?: "",
                            onClientClick = { showClientSheet = true },
                            clientLabel = "Supplier / Client *",
                            clientError = uiState.clientError,
                            deliveryFactoryAddress = uiState.deliveryFactoryAddress,
                            onDeliveryFactoryAddressChange = { onIntent(PurchaseOrderUiIntent.OnDeliveryFactoryAddressChange(it)) },
                            deliveryFactoryAddressError = uiState.deliveryFactoryAddressError,
                            placeOfSupplyStateCode = uiState.placeOfSupplyStateCode,
                            onPlaceOfSupplyClick = { showPosSheet = true },
                            placeOfSupplyError = uiState.placeOfSupplyError
                        )
                    }

                    // Line Items Section Header
                    item {
                        DocumentLineItemsHeader(onAddLineItem = { onIntent(PurchaseOrderUiIntent.OnAddLineItem) })
                    }

                    // Line Items List
                    itemsIndexed(
                        items = uiState.lineItems,
                        key = { _, item -> item.id }
                    ) { index, line ->
                        val lineCalc = uiState.calculationPreview?.lineCalculations?.find { it.lineItemId == line.id }
                        DocumentLineItemCard(
                            position = index + 1,
                            lineState = line,
                            lineCalc = lineCalc,
                            availableProducts = uiState.availableProducts,
                            onSelectProduct = { prod -> onIntent(PurchaseOrderUiIntent.OnSelectProduct(line.id, prod)) },
                            onQuantityChange = { qty -> onIntent(PurchaseOrderUiIntent.OnQuantityChange(line.id, qty)) },
                            onRateChange = { rate -> onIntent(PurchaseOrderUiIntent.OnRateChange(line.id, rate)) },
                            onRemoveLine = { onIntent(PurchaseOrderUiIntent.OnRemoveLineItem(line.id)) },
                            canRemove = uiState.lineItems.size > 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Live Tax Calculation Summary Section
                    item {
                        DocumentCalculationSummary(preview = uiState.calculationPreview)
                    }

                    // Optional PO Metadata Section
                    item {
                        DocumentAdditionalDetailsSection(
                            expanded = isMetadataExpanded,
                            onExpandedChange = { isMetadataExpanded = it }
                        ) {
                            AppTextField(
                                value = uiState.paymentTerms,
                                onValueChange = { onIntent(PurchaseOrderUiIntent.OnMetadataChange(paymentTerms = it)) },
                                label = "Mode / Terms of Payment",
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = uiState.supplierReference,
                                onValueChange = { onIntent(PurchaseOrderUiIntent.OnMetadataChange(supplierReference = it)) },
                                label = "Supplier Reference",
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = uiState.otherReferences,
                                onValueChange = { onIntent(PurchaseOrderUiIntent.OnMetadataChange(otherReferences = it)) },
                                label = "Other Reference(s)",
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = uiState.dispatchThrough,
                                onValueChange = { onIntent(PurchaseOrderUiIntent.OnMetadataChange(dispatchThrough = it)) },
                                label = "Dispatch Through",
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = uiState.destination,
                                onValueChange = { onIntent(PurchaseOrderUiIntent.OnMetadataChange(destination = it)) },
                                label = "Destination",
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = uiState.termsOfDelivery,
                                onValueChange = { onIntent(PurchaseOrderUiIntent.OnMetadataChange(termsOfDelivery = it)) },
                                label = "Terms of Delivery",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Action Buttons (Save Draft & Finalize PO)
                    item {
                        DocumentFormActionButtons(
                            primaryButtonText = "Finalize PO",
                            onRequestFinalize = { onIntent(PurchaseOrderUiIntent.OnRequestFinalize) },
                            onSaveDraft = { onIntent(PurchaseOrderUiIntent.OnSaveDraft) },
                            isFinalizing = uiState.isFinalizing,
                            isSavingDraft = uiState.isSavingDraft
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Purchase Order Screen Preview", showBackground = true)
@Composable
private fun PurchaseOrderScreenPreview() {
    val sampleClient = Client(
        id = "client_1",
        companyName = "Apex Industrial Supplies",
        address = "123 Industrial Area, Phase 1, Ahmedabad",
        gstin = "24AAACA1234A1ZP",
        state = "Gujarat",
        stateCode = "24",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    val sampleProduct = Product(
        id = "product_1",
        name = "Industrial Steel Bearings",
        hsnSac = "8482",
        defaultGstRateBasisPoints = 1800,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    val sampleLineItems = listOf(
        DocumentLineUiState(
            id = "line_1",
            selectedProduct = sampleProduct,
            quantityInput = "10",
            rateInput = "1500"
        )
    )

    val sampleCalculation = DocumentCalculationResult(
        lineCalculations = listOf(
            DocumentLineCalculation(
                lineItemId = "line_1",
                taxableAmountPaise = 1500000L,
                igstRateBasisPoints = 0,
                igstAmountPaise = 0L,
                cgstRateBasisPoints = 900,
                cgstAmountPaise = 135000L,
                sgstRateBasisPoints = 900,
                sgstAmountPaise = 135000L,
                totalTaxPaise = 270000L,
                lineTotalPaise = 1770000L
            )
        ),
        taxTreatment = TaxTreatment.INTRA_STATE,
        taxableAmountPaise = 1500000L,
        cgstAmountPaise = 135000L,
        sgstAmountPaise = 135000L,
        igstAmountPaise = 0L,
        totalTaxAmountPaise = 270000L,
        grandTotalPaise = 1770000L,
        amountInWords = "RUPEES SEVENTEEN THOUSAND SEVEN HUNDRED ONLY",
        taxAmountInWords = "RUPEES TWO THOUSAND SEVEN HUNDRED ONLY"
    )

    VivaanEnterpriseTheme {
        PurchaseOrderScreen(
            uiState = PurchaseOrderUiState(
                documentNumber = "PO-2026-001",
                selectedClient = sampleClient,
                deliveryFactoryAddress = "Plot 45, GIDC Naroda, Ahmedabad",
                placeOfSupplyStateCode = "24",
                lineItems = sampleLineItems,
                calculationPreview = sampleCalculation,
                availableClients = listOf(sampleClient),
                availableProducts = listOf(sampleProduct),
                isInitialLoading = false
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onNavigateBack = {}
        )
    }
}



