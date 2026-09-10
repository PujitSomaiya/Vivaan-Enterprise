package com.vivaanenterprise.app.feature.purchaseorder.presentation

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
import androidx.compose.ui.text.font.FontWeight
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
import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.model.DocumentCalculationResult
import com.vivaanenterprise.app.domain.model.DocumentLineCalculation
import com.vivaanenterprise.app.domain.model.IndianState
import com.vivaanenterprise.app.domain.model.Product
import com.vivaanenterprise.app.domain.model.TaxTreatment
import com.vivaanenterprise.app.feature.document.presentation.components.ClientSelectorBottomSheet
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentAdditionalDetailsSection
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentCalculationSummary
import com.vivaanenterprise.app.feature.document.presentation.components.DocumentLineItemCard
import com.vivaanenterprise.app.feature.document.presentation.components.PlaceOfSupplyBottomSheet
import com.vivaanenterprise.app.feature.document.presentation.model.DocumentLineUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    BackHandler(enabled = uiState.isDirty) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Unsaved Changes?") },
            text = { Text("You have unsaved changes in this purchase order. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = AppTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    if (uiState.showFinalizeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(PurchaseOrderUiIntent.OnDismissFinalizeConfirm) },
            title = { Text("Finalize Purchase Order") },
            text = {
                Text(
                    "Are you sure you want to finalize this purchase order? " +
                            "Once finalized, line items and supplier details will be frozen and cannot be modified."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onIntent(PurchaseOrderUiIntent.OnConfirmFinalize) }
                ) {
                    Text("Finalize PO", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(PurchaseOrderUiIntent.OnDismissFinalizeConfirm) }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.documentDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePickerDialog = false
                        datePickerState.selectedDateMillis?.let { selected ->
                            onIntent(PurchaseOrderUiIntent.OnDocumentDateChange(selected))
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
                                onRetryClick = { onIntent(PurchaseOrderUiIntent.OnClearGeneralError) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                        AppSectionHeader(title = "Header Information")

                        Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                        // PO Number
                        AppTextField(
                            value = uiState.documentNumber,
                            onValueChange = { onIntent(PurchaseOrderUiIntent.OnDocumentNumberChange(it)) },
                            label = "PO Number *",
                            errorText = uiState.documentNumberError,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                        // PO Date
                        val formattedDate = remember(uiState.documentDate) {
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(uiState.documentDate))
                        }
                        AppTextField(
                            value = formattedDate,
                            onValueChange = {},
                            label = "PO Date *",
                            readOnly = true,
                            trailingIcon = {
                                Text(
                                    text = "Change",
                                    style = AppTheme.typography.labelMedium,
                                    color = AppTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(end = AppTheme.spacing.xs)
                                        .clickable { showDatePickerDialog = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePickerDialog = true }
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                        // Supplier / Client Picker
                        AppTextField(
                            value = uiState.selectedClient?.companyName ?: "",
                            onValueChange = {},
                            label = "Supplier / Client *",
                            readOnly = true,
                            errorText = uiState.clientError,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Supplier / Client",
                                    modifier = Modifier.clickable { showClientSheet = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showClientSheet = true }
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                        // Delivery / Factory Address (PO specific)
                        AppTextField(
                            value = uiState.deliveryFactoryAddress,
                            onValueChange = { onIntent(PurchaseOrderUiIntent.OnDeliveryFactoryAddressChange(it)) },
                            label = "Delivery / Factory Address",
                            errorText = uiState.deliveryFactoryAddressError,
                            modifier = Modifier.fillMaxWidth()
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
                                    modifier = Modifier.clickable { showPosSheet = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPosSheet = true }
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
                            TextButton(onClick = { onIntent(PurchaseOrderUiIntent.OnAddLineItem) }) {
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                        ) {
                            AppPrimaryButton(
                                text = "Finalize PO",
                                onClick = { onIntent(PurchaseOrderUiIntent.OnRequestFinalize) },
                                isLoading = uiState.isFinalizing,
                                enabled = !uiState.isSavingDraft && !uiState.isFinalizing,
                                modifier = Modifier.fillMaxWidth()
                            )

                            AppSecondaryButton(
                                text = "Save Draft",
                                onClick = { onIntent(PurchaseOrderUiIntent.OnSaveDraft) },
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



