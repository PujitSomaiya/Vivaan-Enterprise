package com.vivaanenterprise.app.feature.purchaseorder.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.component.AppCard
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
import com.vivaanenterprise.app.domain.util.IndianCurrencyFormatter
import com.vivaanenterprise.app.feature.invoice.presentation.InvoiceLineUiState
import com.vivaanenterprise.app.feature.invoice.presentation.components.ClientSelectorBottomSheet
import com.vivaanenterprise.app.feature.invoice.presentation.components.InvoiceLineItemCard
import com.vivaanenterprise.app.feature.invoice.presentation.components.PlaceOfSupplyBottomSheet
import com.vivaanenterprise.app.feature.invoice.presentation.components.ProductSelectorBottomSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
        onNavigateBackRequest = {
            if (uiState.isDirty) {
                // Handled via discard dialog in screen
            } else {
                onNavigateBack()
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderScreen(
    uiState: PurchaseOrderUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (PurchaseOrderUiIntent) -> Unit,
    onNavigateBackRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showClientSheet by remember { mutableStateOf(false) }
    var showPosSheet by remember { mutableStateOf(false) }
    var selectingProductForLineId by remember { mutableStateOf<String?>(null) }
    var isMetadataExpanded by remember { mutableStateOf(false) }

    fun handleBackAttempt() {
        if (uiState.isDirty) {
            showDiscardDialog = true
        } else {
            onNavigateBackRequest()
        }
    }

    BackHandler(enabled = true) {
        handleBackAttempt()
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
                        onNavigateBackRequest()
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

    selectingProductForLineId?.let { lineId ->
        val currentLine = uiState.lineItems.find { it.id == lineId }
        ProductSelectorBottomSheet(
            products = uiState.availableProducts,
            selectedProduct = currentLine?.selectedProduct,
            onSelectProduct = { product ->
                selectingProductForLineId = null
                onIntent(PurchaseOrderUiIntent.OnSelectProduct(lineId, product))
            },
            onDismiss = { selectingProductForLineId = null }
        )
    }

    val title = if (uiState.mode == PurchaseOrderMode.NEW) "New Purchase Order" else "Edit Purchase Order Draft"

    AppScaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = title,
                onBackClick = { handleBackAttempt() }
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
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(AppTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
                ) {
                    uiState.generalError?.let { err ->
                        AppErrorState(
                            message = err,
                            onRetryClick = { onIntent(PurchaseOrderUiIntent.OnClearGeneralError) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Document Header Card
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                        ) {
                            AppSectionHeader(title = "PO Header & Date")

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                            ) {
                                AppTextField(
                                    value = uiState.documentNumber,
                                    onValueChange = { onIntent(PurchaseOrderUiIntent.OnDocumentNumberChange(it)) },
                                    label = "PO Number",
                                    errorText = uiState.documentNumberError,
                                    modifier = Modifier.weight(1f)
                                )

                                val sdf = remember {
                                    SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).apply {
                                        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                                    }
                                }
                                val formattedDate = sdf.format(Date(uiState.documentDate))

                                AppTextField(
                                    value = formattedDate,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = "PO Date",
                                    trailingIcon = {
                                        IconButton(onClick = { showDatePickerDialog = true }) {
                                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Supplier / Vendor & Delivery Address Card
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                        ) {
                            AppSectionHeader(title = "Supplier & Delivery Information")

                            OutlinedCard(
                                onClick = { showClientSheet = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(AppTheme.spacing.md)) {
                                    Text(
                                        text = "Supplier / Client",
                                        style = AppTheme.typography.labelMedium,
                                        color = AppTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = uiState.selectedClient?.companyName ?: "Tap to select supplier/client",
                                        style = AppTheme.typography.titleMedium,
                                        color = if (uiState.selectedClient != null) AppTheme.colorScheme.onSurface else AppTheme.colorScheme.outline
                                    )
                                    uiState.selectedClient?.let { c ->
                                        Text(
                                            text = "GSTIN: ${c.gstin ?: "N/A"} | State: ${c.state ?: "N/A"} (${c.stateCode ?: "--"})",
                                            style = AppTheme.typography.bodySmall,
                                            color = AppTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            uiState.clientError?.let { err ->
                                Text(text = err, style = AppTheme.typography.bodySmall, color = AppTheme.colorScheme.error)
                            }

                            AppTextField(
                                value = uiState.deliveryFactoryAddress,
                                onValueChange = { onIntent(PurchaseOrderUiIntent.OnDeliveryFactoryAddressChange(it)) },
                                label = "Delivery / Factory Address",
                                errorText = uiState.deliveryFactoryAddressError,
                                modifier = Modifier.fillMaxWidth()
                            )

                            val posStateName = IndianState.findByCode(uiState.placeOfSupplyStateCode)?.displayName
                                ?: "State Code ${uiState.placeOfSupplyStateCode}"

                            OutlinedCard(
                                onClick = { showPosSheet = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(AppTheme.spacing.md)) {
                                    Text(
                                        text = "Place of Supply",
                                        style = AppTheme.typography.labelMedium,
                                        color = AppTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$posStateName (${uiState.placeOfSupplyStateCode})",
                                        style = AppTheme.typography.titleSmall,
                                        color = AppTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            uiState.placeOfSupplyError?.let { err ->
                                Text(text = err, style = AppTheme.typography.bodySmall, color = AppTheme.colorScheme.error)
                            }
                        }
                    }

                    // Line Items Card
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    AppSectionHeader(title = "Items (${uiState.lineItems.size})")
                                }
                                TextButton(onClick = { onIntent(PurchaseOrderUiIntent.OnAddLineItem) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Item")
                                }
                            }

                            uiState.lineItems.forEachIndexed { index, line ->
                                val lineCalc = uiState.calculationPreview?.lineCalculations?.find { it.lineItemId == line.id }
                                val invoiceLineUi = InvoiceLineUiState(
                                    id = line.id,
                                    selectedProduct = line.selectedProduct,
                                    quantityInput = line.quantityInput,
                                    rateInput = line.rateInput,
                                    quantityError = line.quantityError,
                                    rateError = line.rateError,
                                    productError = line.productError
                                )

                                InvoiceLineItemCard(
                                    position = index + 1,
                                    lineState = invoiceLineUi,
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
                        }
                    }

                    // Live Tax Calculation Summary Card
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)
                        ) {
                            AppSectionHeader(title = "Calculation Summary")

                            val preview = uiState.calculationPreview
                            if (preview != null) {
                                CalculationRow(
                                    label = "Taxable Amount",
                                    value = String.format("₹%.2f", preview.taxableAmountPaise / 100.0)
                                )

                                if (preview.taxTreatment == TaxTreatment.INTRA_STATE) {
                                    CalculationRow(
                                        label = "CGST",
                                        value = String.format("₹%.2f", preview.cgstAmountPaise / 100.0)
                                    )
                                    CalculationRow(
                                        label = "SGST",
                                        value = String.format("₹%.2f", preview.sgstAmountPaise / 100.0)
                                    )
                                } else {
                                    CalculationRow(
                                        label = "IGST",
                                        value = String.format("₹%.2f", preview.igstAmountPaise / 100.0)
                                    )
                                }

                                CalculationRow(
                                    label = "Total Tax",
                                    value = String.format("₹%.2f", preview.totalTaxAmountPaise / 100.0)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Grand Total",
                                        style = AppTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "₹ ${com.vivaanenterprise.app.core.pdf.PdfFormattingUtils.formatPaiseToCurrency(preview.grandTotalPaise)}",
                                        style = AppTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.colorScheme.primary
                                    )
                                }

                                preview.amountInWords?.let { words ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = words,
                                        style = AppTheme.typography.bodySmall,
                                        color = AppTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = "Enter valid supplier, place of supply, items, quantity, and rate to view live calculation summary.",
                                    style = AppTheme.typography.bodySmall,
                                    color = AppTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Optional PO Metadata Card
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppTheme.spacing.md)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppSectionHeader(title = "Additional Details")
                                IconButton(onClick = { isMetadataExpanded = !isMetadataExpanded }) {
                                    Icon(
                                        imageVector = if (isMetadataExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isMetadataExpanded) "Collapse" else "Expand"
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isMetadataExpanded) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                                    modifier = Modifier.padding(top = AppTheme.spacing.sm)
                                ) {
                                    AppTextField(
                                        value = uiState.paymentTerms,
                                        onValueChange = { onIntent(PurchaseOrderUiIntent.OnMetadataChange(paymentTerms = it)) },
                                        label = "Mode/Terms of Payment",
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
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
                    ) {
                        AppSecondaryButton(
                            text = if (uiState.isSavingDraft) "Saving..." else "Save Draft",
                            onClick = { onIntent(PurchaseOrderUiIntent.OnSaveDraft) },
                            enabled = !uiState.isSavingDraft && !uiState.isFinalizing,
                            modifier = Modifier.weight(1f)
                        )

                        AppPrimaryButton(
                            text = if (uiState.isFinalizing) "Finalizing..." else "Finalize PO",
                            onClick = { onIntent(PurchaseOrderUiIntent.OnRequestFinalize) },
                            enabled = !uiState.isSavingDraft && !uiState.isFinalizing,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculationRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = AppTheme.typography.bodyMedium)
        Text(text = value, style = AppTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
        PurchaseOrderLineUiState(
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
            onNavigateBackRequest = {}
        )
    }
}


