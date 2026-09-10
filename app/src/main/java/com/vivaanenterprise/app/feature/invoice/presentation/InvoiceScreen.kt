package com.vivaanenterprise.app.feature.invoice.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.component.AppErrorState
import com.vivaanenterprise.app.core.designsystem.component.AppLoadingState
import com.vivaanenterprise.app.core.designsystem.component.AppPrimaryButton
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.AppSecondaryButton
import com.vivaanenterprise.app.core.designsystem.component.AppSectionHeader
import com.vivaanenterprise.app.core.designsystem.component.AppTextField
import com.vivaanenterprise.app.core.designsystem.component.AppTopBar
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.designsystem.theme.VivaanEnterpriseTheme
import com.vivaanenterprise.app.domain.model.IndianState
import com.vivaanenterprise.app.feature.document.presentation.components.ClientSelectorBottomSheet
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentAdditionalDetailsSection
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentCalculationSummary
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentLineItemCard
import com.vivaanenterprise.app.feature.document.presentation.components.PlaceOfSupplyBottomSheet
import com.vivaanenterprise.app.feature.purchaseorder.presentation.PurchaseOrderUiIntent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    BackHandler(enabled = uiState.isDirty) {
        showDiscardDialog = true
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
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.documentDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onIntent(InvoiceUiIntent.OnDocumentDateChange(it))
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.confirm_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (uiState.showFinalizeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(InvoiceUiIntent.OnDismissFinalizeConfirm) },
            title = { Text("Finalize Tax Invoice") },
            text = { Text("Are you sure you want to finalize this invoice? Historical invoice records cannot be edited after finalization.") },
            confirmButton = {
                TextButton(onClick = { onIntent(InvoiceUiIntent.OnConfirmFinalize) }) {
                    Text("Finalize")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(InvoiceUiIntent.OnDismissFinalizeConfirm) }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Unsaved Changes?") },
            text = { Text("You have unsaved changes in this invoice. Are you sure you want to leave?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onNavigateBack()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Continue Editing")
                }
            }
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = AppTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
                ) {
                    item {
                        uiState.generalError?.let { err ->
                            AppErrorState(
                                message = err,
                                onRetryClick = { onIntent(InvoiceUiIntent.OnClearGeneralError) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                        AppSectionHeader(title = "Header Information")

                        Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                        // Document Number
                        AppTextField(
                            value = uiState.documentNumber,
                            onValueChange = { onIntent(InvoiceUiIntent.OnDocumentNumberChange(it)) },
                            label = "Invoice Number *",
                            errorText = uiState.documentNumberError,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                        // Document Date
                        val formattedDate = remember(uiState.documentDate) {
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(uiState.documentDate))
                        }
                        AppTextField(
                            value = formattedDate,
                            onValueChange = {},
                            label = "Invoice Date *",
                            readOnly = true,
                            trailingIcon = {
                                Text(
                                    text = "Change",
                                    style = AppTheme.typography.labelMedium,
                                    color = AppTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(end = AppTheme.spacing.xs)
                                        .clickable { showDatePicker = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                        // Client Picker
                        AppTextField(
                            value = uiState.selectedClient?.companyName ?: "",
                            onValueChange = {},
                            label = "Client *",
                            readOnly = true,
                            errorText = uiState.clientError,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Client",
                                    modifier = Modifier.clickable { showClientSheet = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showClientSheet = true }
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                        // Place of Supply Picker
                        val posState = remember(uiState.placeOfSupplyStateCode) {
                            IndianState.findByCode(uiState.placeOfSupplyStateCode)
                        }
                        val posDisplayText = posState?.displayName ?: "State Code: ${uiState.placeOfSupplyStateCode}"

                        AppTextField(
                            value = posDisplayText,
                            onValueChange = {},
                            label = "Place of Supply *",
                            readOnly = true,
                            errorText = uiState.placeOfSupplyError,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Place of Supply",
                                    modifier = Modifier.clickable { showPlaceOfSupplySheet = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPlaceOfSupplySheet = true }
                        )
                    }

                    // Line Items Section Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                AppSectionHeader(title = "Line Items")
                            }
                            TextButton(onClick = { onIntent(InvoiceUiIntent.OnAddLineItem) }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.padding(horizontal = AppTheme.spacing.xxs))
                                Text(text = "Add Item")
                            }
                        }
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                        ) {
                            AppPrimaryButton(
                                text = "Finalize Invoice",
                                onClick = { onIntent(InvoiceUiIntent.OnRequestFinalize) },
                                isLoading = uiState.isFinalizing,
                                enabled = !uiState.isSavingDraft && !uiState.isFinalizing,
                                modifier = Modifier.fillMaxWidth()
                            )

                            AppSecondaryButton(
                                text = "Save Draft",
                                onClick = { onIntent(InvoiceUiIntent.OnSaveDraft) },
                                isLoading = uiState.isSavingDraft,
                                enabled = !uiState.isSavingDraft && !uiState.isFinalizing,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(AppTheme.spacing.lg))
                        }
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
