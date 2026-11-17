package com.vivaanenterprise.app.feature.invoice.presentation

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
import com.vivaanenterprise.app.feature.document.presentation.components.ClientSelectorBottomSheet
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentAdditionalDetailsSection
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentCalculationSummary
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentFormActionButtons
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentHeaderSection
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentLineItemCard
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentLineItemsHeader
import com.vivaanenterprise.app.feature.document.presentation.components.PlaceOfSupplyBottomSheet

@Composable
fun InvoiceRoute(
    onNavigateBack: () -> Unit,
    onSaveSuccess: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvoiceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is InvoiceUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                InvoiceUiEffect.NavigateBack -> onNavigateBack()
                is InvoiceUiEffect.NavigateSuccess -> onSaveSuccess(effect.documentId)
            }
        }
    }

    InvoiceScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    uiState: InvoiceUiState,
    onIntent: (InvoiceUiIntent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    var showClientSheet by remember { mutableStateOf(false) }
    var showPlaceOfSupplySheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showMetadataSection by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    BackHandler(enabled = uiState.isDirty) {
        showDiscardDialog = true
    }

    // Non-field operation error dialog
    uiState.generalError?.let { err ->
        if (uiState.availableClients.isNotEmpty()) {
            AppErrorDialog(
                message = err,
                onDismiss = { onIntent(InvoiceUiIntent.OnClearGeneralError) }
            )
        }
    }

    // Auto-scroll to first field error if any validation error occurs
    val hasFieldErrors = uiState.documentNumberError != null ||
            uiState.clientError != null ||
            uiState.placeOfSupplyError != null ||
            uiState.lineItems.any { it.productError != null || it.quantityError != null || it.rateError != null }

    LaunchedEffect(hasFieldErrors) {
        if (hasFieldErrors) {
            when {
                uiState.documentNumberError != null || uiState.clientError != null || uiState.placeOfSupplyError != null -> {
                    listState.animateScrollToItem(0)
                }
                uiState.lineItems.any { it.productError != null || it.quantityError != null || it.rateError != null } -> {
                    val firstInvalidIndex = uiState.lineItems.indexOfFirst {
                        it.productError != null || it.quantityError != null || it.rateError != null
                    }
                    if (firstInvalidIndex >= 0) {
                        listState.animateScrollToItem(firstInvalidIndex + 2) // header item = 0, line header = 1
                    }
                }
            }
        }
    }

    if (showClientSheet) {
        ClientSelectorBottomSheet(
            clients = uiState.availableClients,
            selectedClient = uiState.selectedClient,
            onSelectClient = { onIntent(InvoiceUiIntent.OnSelectClient(it)) },
            onDismiss = { showClientSheet = false }
        )
    }

    if (showPlaceOfSupplySheet) {
        PlaceOfSupplyBottomSheet(
            selectedStateCode = uiState.placeOfSupplyStateCode,
            onSelectState = { onIntent(InvoiceUiIntent.OnPlaceOfSupplyChange(it.code)) },
            onDismiss = { showPlaceOfSupplySheet = false }
        )
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialSelectedDateMillis = uiState.documentDate,
            onDateSelected = { onIntent(InvoiceUiIntent.OnDocumentDateChange(it)) },
            onDismiss = { showDatePicker = false }
        )
    }

    if (uiState.showFinalizeConfirmDialog) {
        AppConfirmationDialog(
            title = "Finalize Tax Invoice",
            message = "Are you sure you want to finalize this invoice? Historical invoice records cannot be edited after finalization.",
            confirmLabel = "Finalize",
            onConfirm = { onIntent(InvoiceUiIntent.OnConfirmFinalize) },
            onDismiss = { onIntent(InvoiceUiIntent.OnDismissFinalizeConfirm) }
        )
    }

    if (showDiscardDialog) {
        AppConfirmationDialog(
            title = "Discard Unsaved Changes?",
            message = "You have unsaved changes in this invoice. Are you sure you want to leave?",
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

    val title = if (uiState.mode == InvoiceMode.NEW) "New Tax Invoice" else "Edit Invoice Draft"

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
                    onRetryClick = { onIntent(InvoiceUiIntent.OnClearGeneralError) },
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
                            onDocumentNumberChange = { onIntent(InvoiceUiIntent.OnDocumentNumberChange(it)) },
                            documentNumberLabel = "Invoice Number *",
                            documentNumberError = uiState.documentNumberError,
                            documentDate = uiState.documentDate,
                            onDateClick = { showDatePicker = true },
                            dateLabel = "Invoice Date *",
                            selectedClientName = uiState.selectedClient?.companyName ?: "",
                            onClientClick = { showClientSheet = true },
                            clientLabel = "Client *",
                            clientError = uiState.clientError,
                            deliveryFactoryAddress = uiState.deliveryFactoryAddress,
                            onDeliveryFactoryAddressChange = { onIntent(InvoiceUiIntent.OnDeliveryFactoryAddressChange(it)) },
                            deliveryFactoryAddressError = uiState.deliveryFactoryAddressError,
                            placeOfSupplyStateCode = uiState.placeOfSupplyStateCode,
                            onPlaceOfSupplyClick = { showPlaceOfSupplySheet = true },
                            placeOfSupplyError = uiState.placeOfSupplyError
                        )
                    }

                    // Line Items Section Header
                    item {
                        DocumentLineItemsHeader(onAddLineItem = { onIntent(InvoiceUiIntent.OnAddLineItem) })
                    }

                    // Line Items List
                    itemsIndexed(
                        items = uiState.lineItems,
                        key = { _, item -> item.id }
                    ) { index, lineState ->
                        val lineCalc = uiState.calculationPreview?.lineCalculations?.firstOrNull { it.lineItemId == lineState.id }
                        DocumentLineItemCard(
                            position = index + 1,
                            lineState = lineState,
                            lineCalc = lineCalc,
                            availableProducts = uiState.availableProducts,
                            onSelectProduct = { onIntent(InvoiceUiIntent.OnSelectProduct(lineState.id, it)) },
                            onQuantityChange = { onIntent(InvoiceUiIntent.OnQuantityChange(lineState.id, it)) },
                            onRateChange = { onIntent(InvoiceUiIntent.OnRateChange(lineState.id, it)) },
                            onRemoveLine = { onIntent(InvoiceUiIntent.OnRemoveLineItem(lineState.id)) },
                            canRemove = uiState.lineItems.size > 1
                        )
                    }

                    // Live Calculation Preview Section
                    item {
                        DocumentCalculationSummary(preview = uiState.calculationPreview)
                    }

                    // Expandable Metadata Section
                    item {
                        DocumentAdditionalDetailsSection(
                            expanded = showMetadataSection,
                            onExpandedChange = { showMetadataSection = it }
                        ) {
                            AppTextField(
                                value = uiState.deliveryNote,
                                onValueChange = { onIntent(InvoiceUiIntent.OnMetadataChange(deliveryNote = it)) },
                                label = "Delivery Note",
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = uiState.paymentTerms,
                                onValueChange = { onIntent(InvoiceUiIntent.OnMetadataChange(paymentTerms = it)) },
                                label = "Mode / Terms of Payment",
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = uiState.supplierReference,
                                onValueChange = { onIntent(InvoiceUiIntent.OnMetadataChange(supplierReference = it)) },
                                label = "Supplier Reference",
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = uiState.otherReferences,
                                onValueChange = { onIntent(InvoiceUiIntent.OnMetadataChange(otherReferences = it)) },
                                label = "Other References",
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = uiState.buyerOrderNumber,
                                onValueChange = { onIntent(InvoiceUiIntent.OnMetadataChange(buyerOrderNumber = it)) },
                                label = "Buyer Order No.",
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = uiState.dispatchDocumentNumber,
                                onValueChange = { onIntent(InvoiceUiIntent.OnMetadataChange(dispatchDocumentNumber = it)) },
                                label = "Dispatch Document No.",
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = uiState.dispatchThrough,
                                onValueChange = { onIntent(InvoiceUiIntent.OnMetadataChange(dispatchThrough = it)) },
                                label = "Dispatched Through",
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = uiState.destination,
                                onValueChange = { onIntent(InvoiceUiIntent.OnMetadataChange(destination = it)) },
                                label = "Destination",
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = uiState.termsOfDelivery,
                                onValueChange = { onIntent(InvoiceUiIntent.OnMetadataChange(termsOfDelivery = it)) },
                                label = "Terms of Delivery",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Action Buttons (Save Draft & Finalize)
                    item {
                        DocumentFormActionButtons(
                            primaryButtonText = "Finalize Invoice",
                            onRequestFinalize = { onIntent(InvoiceUiIntent.OnRequestFinalize) },
                            onSaveDraft = { onIntent(InvoiceUiIntent.OnSaveDraft) },
                            isFinalizing = uiState.isFinalizing,
                            isSavingDraft = uiState.isSavingDraft
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Invoice Screen Preview")
@Composable
private fun InvoiceScreenPreview() {
    VivaanEnterpriseTheme {
        InvoiceScreen(
            uiState = InvoiceUiState(
                documentNumber = "VE/07/2026-27",
                isInitialLoading = false
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}
