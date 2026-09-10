package com.vivaanenterprise.app.feature.document.history.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.vivaanenterprise.app.core.designsystem.component.AppEmptyState
import com.vivaanenterprise.app.core.designsystem.component.AppErrorState
import com.vivaanenterprise.app.core.designsystem.component.AppLoadingState
import com.vivaanenterprise.app.core.designsystem.component.AppPrimaryButton
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.AppSearchField
import com.vivaanenterprise.app.core.designsystem.component.AppSyncIndicator
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.pdf.PdfFormattingUtils
import com.vivaanenterprise.app.core.util.DateTimeUtils

@Composable
fun DocumentsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToNewInvoice: () -> Unit,
    onNavigateToNewPurchaseOrder: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DocumentsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToNewInvoice = onNavigateToNewInvoice,
        onNavigateToNewPurchaseOrder = onNavigateToNewPurchaseOrder,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onTypeFilterSelected = viewModel::onTypeFilterSelected,
        onStatusFilterSelected = viewModel::onStatusFilterSelected,
        onRetry = viewModel::onRetry,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    uiState: DocumentsUiState,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToNewInvoice: () -> Unit,
    onNavigateToNewPurchaseOrder: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterSelected: (DocumentTypeFilter) -> Unit,
    onStatusFilterSelected: (DocumentStatusFilter) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.documents_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back_desc)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search and Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm)
            ) {
                AppSearchField(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = stringResource(R.string.search_documents_placeholder),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                // Type Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DocumentTypeFilter.entries) { filter ->
                        val label = when (filter) {
                            DocumentTypeFilter.ALL -> stringResource(R.string.filter_type_all)
                            DocumentTypeFilter.TAX_INVOICE -> stringResource(R.string.filter_type_tax_invoice)
                            DocumentTypeFilter.PURCHASE_ORDER -> stringResource(R.string.filter_type_purchase_order)
                        }
                        FilterChip(
                            selected = uiState.selectedTypeFilter == filter,
                            onClick = { onTypeFilterSelected(filter) },
                            label = { Text(text = label, style = AppTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppTheme.colorScheme.primaryContainer,
                                selectedLabelColor = AppTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Status Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DocumentStatusFilter.entries) { filter ->
                        val label = when (filter) {
                            DocumentStatusFilter.ALL -> stringResource(R.string.filter_status_all)
                            DocumentStatusFilter.DRAFT -> stringResource(R.string.filter_status_draft)
                            DocumentStatusFilter.FINALIZED -> stringResource(R.string.filter_status_finalized)
                        }
                        FilterChip(
                            selected = uiState.selectedStatusFilter == filter,
                            onClick = { onStatusFilterSelected(filter) },
                            label = { Text(text = label, style = AppTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppTheme.colorScheme.primaryContainer,
                                selectedLabelColor = AppTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Content Area
            when {
                uiState.isLoading -> {
                    AppLoadingState(modifier = Modifier.fillMaxSize())
                }

                uiState.errorMessage != null -> {
                    AppErrorState(
                        message = uiState.errorMessage,
                        onRetryClick = onRetry,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.documents.isEmpty() -> {
                    // Overall no documents exist
                    AppEmptyState(
                        title = stringResource(R.string.empty_documents_title),
                        message = stringResource(R.string.empty_documents_description),
                        actionText = "New Tax Invoice",
                        onActionClick = onNavigateToNewInvoice,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.filteredDocuments.isEmpty() -> {
                    // Filter or search returned empty results
                    AppEmptyState(
                        title = stringResource(R.string.empty_documents_search_title),
                        message = stringResource(R.string.empty_documents_search_description),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = AppTheme.spacing.md,
                            vertical = AppTheme.spacing.sm
                        )
                    ) {
                        items(
                            items = uiState.filteredDocuments,
                            key = { it.id }
                        ) { item ->
                            DocumentListItemCard(
                                item = item,
                                onClick = { onNavigateToDetail(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentListItemCard(
    item: DocumentListItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
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
                    DocumentTypeBadge(documentType = item.documentType)
                    Text(
                        text = item.documentNumber,
                        style = AppTheme.typography.titleMedium,
                        color = AppTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                AppSyncIndicator(status = item.syncStatus)
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

            Text(
                text = if (item.clientDisplayName.isNotBlank()) item.clientDisplayName else "—",
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateTimeUtils.formatDocumentDate(item.documentDate),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                ) {
                    DocumentStatusBadge(status = item.status)

                    if (item.documentType == DocumentType.TAX_INVOICE && item.status == DocumentStatus.FINALIZED) {
                        Text(
                            text = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(item.grandTotalPaise)}",
                            style = AppTheme.typography.titleMedium,
                            color = AppTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentTypeBadge(documentType: DocumentType) {
    val text = when (documentType) {
        DocumentType.TAX_INVOICE -> "INV"
        DocumentType.PURCHASE_ORDER -> "PO"
    }
    Surface(
        color = AppTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun DocumentStatusBadge(status: DocumentStatus) {
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
            style = AppTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
