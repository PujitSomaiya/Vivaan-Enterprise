package com.vivaanenterprise.app.feature.document.history.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.designsystem.component.AppCard
import com.vivaanenterprise.app.core.designsystem.component.AppErrorState
import com.vivaanenterprise.app.core.designsystem.component.AppLoadingState
import com.vivaanenterprise.app.core.designsystem.component.AppPrimaryButton
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.AppSyncIndicator
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.pdf.PdfFormattingUtils
import com.vivaanenterprise.app.core.util.DateTimeUtils
import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.model.DocumentLineItem

@Composable
fun DocumentDetailRoute(
    onNavigateBack: () -> Unit,
    onEditDraft: (DocumentType, String) -> Unit,
    onViewPdf: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DocumentDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEditDraft = onEditDraft,
        onViewPdf = onViewPdf,
        onRetry = viewModel::onRetry,
        onDeleteClicked = viewModel::onDeleteClicked,
        onDeleteDismissed = viewModel::onDeleteDismissed,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    uiState: DocumentDetailUiState,
    onNavigateBack: () -> Unit,
    onEditDraft: (DocumentType, String) -> Unit,
    onViewPdf: (String) -> Unit,
    onRetry: () -> Unit,
    onDeleteClicked: () -> Unit = {},
    onDeleteDismissed: () -> Unit = {},
    onDeleteConfirmed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    androidx.compose.runtime.LaunchedEffect(uiState.isDeletedSuccessfully) {
        if (uiState.isDeletedSuccessfully) {
            onNavigateBack()
        }
    }

    if (uiState.showDeleteConfirmationDialog && uiState.document != null) {
        val isDraft = uiState.document.status == DocumentStatus.DRAFT
        val dialogTitle = if (isDraft) {
            stringResource(R.string.delete_draft_dialog_title)
        } else {
            stringResource(R.string.delete_finalized_dialog_title)
        }
        val dialogMessage = if (isDraft) {
            stringResource(R.string.delete_draft_dialog_message)
        } else {
            stringResource(R.string.delete_finalized_dialog_message)
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!uiState.isDeleting) onDeleteDismissed() },
            title = { Text(text = dialogTitle) },
            text = { Text(text = dialogMessage) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = onDeleteConfirmed,
                    enabled = !uiState.isDeleting
                ) {
                    Text(
                        text = stringResource(R.string.delete_action),
                        color = AppTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = onDeleteDismissed,
                    enabled = !uiState.isDeleting
                ) {
                    Text(text = stringResource(R.string.cancel_action))
                }
            }
        )
    }

    AppScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.document_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back_desc)
                        )
                    }
                },
                actions = {
                    if (uiState.document != null) {
                        IconButton(
                            onClick = onDeleteClicked,
                            enabled = !uiState.isDeleting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_document_action),
                                tint = AppTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                AppLoadingState(modifier = Modifier.fillMaxSize().padding(innerPadding))
            }

            uiState.errorMessage != null || uiState.document == null -> {
                AppErrorState(
                    message = uiState.errorMessage ?: stringResource(R.string.error_document_not_found),
                    onRetryClick = onRetry,
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                )
            }

            else -> {
                val doc = uiState.document
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(AppTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
                    ) {
                        // Header Card
                        DocumentHeaderCard(doc = doc)

                        // Client Snapshot Card
                        ClientDetailsCard(doc = doc, clientDisplayName = uiState.clientName)

                        // Seller Snapshot Card (for Finalized)
                        if (doc.status == DocumentStatus.FINALIZED && doc.sellerSnapshot != null) {
                            SellerDetailsCard(doc = doc)
                        }

                        // Line Items Table / List Card
                        LineItemsSectionCard(lineItems = doc.lineItems)

                        // Financial Summary Card
                        FinancialSummaryCard(doc = doc)

                        // Metadata Details Card (if non-empty)
                        MetadataDetailsCard(doc = doc)
                    }

                    // Bottom Action Bar
                    BottomActionBar(
                        doc = doc,
                        onEditDraft = { onEditDraft(doc.documentType, doc.id) },
                        onViewPdf = { onViewPdf(doc.id) },
                        onDeleteClicked = onDeleteClicked,
                        isDeleting = uiState.isDeleting
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentHeaderCard(doc: BusinessDocument) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)
                ) {
                    val typeText = when (doc.documentType) {
                        DocumentType.TAX_INVOICE -> "TAX INVOICE"
                        DocumentType.PURCHASE_ORDER -> "PURCHASE ORDER"
                    }
                    Surface(
                        color = AppTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = typeText,
                            style = AppTheme.typography.labelMedium,
                            color = AppTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = doc.documentNumber,
                        style = AppTheme.typography.titleLarge,
                        color = AppTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                AppSyncIndicator(status = doc.syncStatus)
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date: ${DateTimeUtils.formatDocumentDate(doc.documentDate)}",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )

                StatusTag(status = doc.status)
            }

            if (!doc.placeOfSupply.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                Text(
                    text = "Place of Supply: ${doc.placeOfSupply}",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusTag(status: DocumentStatus) {
    val (bgColor, textColor, text) = when (status) {
        DocumentStatus.DRAFT -> Triple(
            AppTheme.colorScheme.surfaceVariant,
            AppTheme.colorScheme.onSurfaceVariant,
            "DRAFT"
        )
        DocumentStatus.FINALIZED -> Triple(
            AppTheme.colorScheme.primaryContainer,
            AppTheme.colorScheme.onPrimaryContainer,
            "FINALIZED"
        )
        DocumentStatus.CANCELLED -> Triple(
            AppTheme.colorScheme.error.copy(alpha = 0.15f),
            AppTheme.colorScheme.error,
            "CANCELLED"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ClientDetailsCard(doc: BusinessDocument, clientDisplayName: String) {
    val snapshot = doc.clientSnapshot
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.md)
        ) {
            Text(
                text = "Client Details",
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

            Text(
                text = if (clientDisplayName.isNotBlank()) clientDisplayName else "—",
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            val gstin = snapshot?.gstin
            if (!gstin.isNullOrBlank()) {
                Text(
                    text = "GSTIN: $gstin",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
            }

            val address = snapshot?.address
            if (!address.isNullOrBlank()) {
                Text(
                    text = address,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
            }

            val state = snapshot?.state
            val stateCode = snapshot?.stateCode
            if (!state.isNullOrBlank() || !stateCode.isNullOrBlank()) {
                Text(
                    text = "State: ${state ?: ""} ${if (!stateCode.isNullOrBlank()) "($stateCode)" else ""}",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
            }

            val phone = snapshot?.phone
            if (!phone.isNullOrBlank()) {
                Text(
                    text = "Phone: $phone",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SellerDetailsCard(doc: BusinessDocument) {
    val seller = doc.sellerSnapshot ?: return
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.md)
        ) {
            Text(
                text = "Seller Profile (Historical Snapshot)",
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

            Text(
                text = seller.businessName,
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            if (seller.gstin.isNotBlank()) {
                Text(
                    text = "GSTIN: ${seller.gstin}",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
            }

            if (seller.addressLine1.isNotBlank()) {
                Text(
                    text = "${seller.addressLine1}, ${seller.addressLine2}",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
            }

            if (seller.cityStatePincode.isNotBlank()) {
                Text(
                    text = seller.cityStatePincode,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LineItemsSectionCard(lineItems: List<DocumentLineItem>) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.md)
        ) {
            Text(
                text = "Line Items (${lineItems.size})",
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

            if (lineItems.isEmpty()) {
                Text(
                    text = "No line items attached.",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
            } else {
                lineItems.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = AppTheme.spacing.sm),
                            color = AppTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${index + 1}. ${item.descriptionSnapshot}",
                                style = AppTheme.typography.bodyLarge,
                                color = AppTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            val totalText = if (item.lineTotalPaise > 0) {
                                "₹ ${PdfFormattingUtils.formatPaiseToCurrency(item.lineTotalPaise)}"
                            } else {
                                "₹ ${PdfFormattingUtils.formatPaiseToCurrency(item.quantity * item.ratePaise)}"
                            }
                            Text(
                                text = totalText,
                                style = AppTheme.typography.bodyLarge,
                                color = AppTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val hsnText = if (!item.hsnSacSnapshot.isNullOrBlank()) "HSN: ${item.hsnSacSnapshot} | " else ""
                            val gstText = PdfFormattingUtils.formatGstRateBasisPoints(item.gstRateBasisPoints)
                            Text(
                                text = "${hsnText}Qty: ${item.quantity} × ₹ ${PdfFormattingUtils.formatPaiseToCurrency(item.ratePaise)} ($gstText GST)",
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialSummaryCard(doc: BusinessDocument) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.md)
        ) {
            Text(
                text = "Financial Summary",
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

            if (doc.taxTreatment != null) {
                SummaryRow(label = "Tax Treatment", value = doc.taxTreatment.name)
            }

            SummaryRow(
                label = "Taxable Amount",
                value = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(doc.taxableAmountPaise)}"
            )

            if (doc.cgstAmountPaise > 0) {
                SummaryRow(
                    label = "CGST",
                    value = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(doc.cgstAmountPaise)}"
                )
            }

            if (doc.sgstAmountPaise > 0) {
                SummaryRow(
                    label = "SGST",
                    value = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(doc.sgstAmountPaise)}"
                )
            }

            if (doc.igstAmountPaise > 0) {
                SummaryRow(
                    label = "IGST",
                    value = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(doc.igstAmountPaise)}"
                )
            }

            SummaryRow(
                label = "Total Tax Amount",
                value = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(doc.totalTaxAmountPaise)}"
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = AppTheme.spacing.xs),
                color = AppTheme.colorScheme.outline.copy(alpha = 0.5f)
            )

            SummaryRow(
                label = "Grand Total",
                value = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(doc.grandTotalPaise)}",
                isBold = true
            )

            if (!doc.amountInWords.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                Text(
                    text = "Amount in words: ${doc.amountInWords}",
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colorScheme.onSurface,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun MetadataDetailsCard(doc: BusinessDocument) {
    val items = listOfNotNull(
        doc.deliveryNote?.takeIf { it.isNotBlank() }?.let { "Delivery Note" to it },
        doc.paymentTerms?.takeIf { it.isNotBlank() }?.let { "Payment Terms" to it },
        doc.supplierReference?.takeIf { it.isNotBlank() }?.let { "Supplier Ref" to it },
        doc.otherReferences?.takeIf { it.isNotBlank() }?.let { "Other References" to it },
        doc.buyerOrderNumber?.takeIf { it.isNotBlank() }?.let { "Buyer Order #" to it },
        doc.buyerOrderDate?.let { "Buyer Order Date" to DateTimeUtils.formatDocumentDate(it) },
        doc.dispatchDocumentNumber?.takeIf { it.isNotBlank() }?.let { "Dispatch Doc #" to it },
        doc.deliveryNoteDate?.let { "Delivery Note Date" to DateTimeUtils.formatDocumentDate(it) },
        doc.dispatchThrough?.takeIf { it.isNotBlank() }?.let { "Dispatch Through" to it },
        doc.destination?.takeIf { it.isNotBlank() }?.let { "Destination" to it },
        doc.termsOfDelivery?.takeIf { it.isNotBlank() }?.let { "Terms of Delivery" to it },
        doc.deliveryFactoryAddress?.takeIf { it.isNotBlank() }?.let { "Delivery Factory Address" to it }
    )

    if (items.isEmpty()) return

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.md)
        ) {
            Text(
                text = "Dispatch & Reference Metadata",
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

            items.forEach { (label, value) ->
                SummaryRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    doc: BusinessDocument,
    onEditDraft: () -> Unit,
    onViewPdf: () -> Unit,
    onDeleteClicked: () -> Unit = {},
    isDeleting: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.md)
        ) {
            when (doc.status) {
                DocumentStatus.DRAFT -> {
                    AppPrimaryButton(
                        text = stringResource(R.string.action_edit_draft),
                        onClick = onEditDraft,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                DocumentStatus.FINALIZED -> {
                    AppPrimaryButton(
                        text = stringResource(R.string.action_view_pdf),
                        onClick = onViewPdf,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                DocumentStatus.CANCELLED -> {
                    Text(
                        text = "This document is cancelled and read-only.",
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
