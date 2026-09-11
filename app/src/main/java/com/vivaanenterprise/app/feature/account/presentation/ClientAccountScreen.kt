package com.vivaanenterprise.app.feature.account.presentation

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.component.AppCard
import com.vivaanenterprise.app.core.designsystem.component.AppEmptyState
import com.vivaanenterprise.app.core.designsystem.component.AppErrorState
import com.vivaanenterprise.app.core.designsystem.component.AppLoadingState
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.AppSyncIndicator
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.pdf.PdfFormattingUtils
import com.vivaanenterprise.app.core.util.DateTimeUtils
import com.vivaanenterprise.app.domain.model.ClientAccountEntry

@Composable
fun ClientAccountRoute(
    onNavigateBack: () -> Unit,
    onOpenDocumentDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClientAccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ClientAccountUiEffect.OpenDocumentDetail -> onOpenDocumentDetail(effect.documentId)
                is ClientAccountUiEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ClientAccountScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientAccountScreen(
    uiState: ClientAccountUiState,
    onIntent: (ClientAccountUiIntent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.client_account_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back_desc)
                        )
                    }
                }
            )
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                AppLoadingState(modifier = Modifier.fillMaxSize().padding(innerPadding))
            }

            uiState.errorMessage != null || uiState.client == null -> {
                AppErrorState(
                    message = uiState.errorMessage ?: stringResource(R.string.error_client_not_found),
                    onRetryClick = { onIntent(ClientAccountUiIntent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(AppTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
                ) {
                    // Top Summary Header Card
                    AccountSummaryCard(
                        clientCompanyName = uiState.client.companyName,
                        invoiceCount = uiState.invoiceCount,
                        totalBilledPaise = uiState.totalBilledPaise
                    )

                    Text(
                        text = stringResource(R.string.invoice_history_title),
                        style = AppTheme.typography.titleMedium,
                        color = AppTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    if (uiState.entries.isEmpty()) {
                        AppEmptyState(
                            title = stringResource(R.string.empty_account_invoices_title),
                            message = stringResource(R.string.empty_account_invoices_description),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)
                        ) {
                            items(
                                items = uiState.entries,
                                key = { it.id }
                            ) { entry ->
                                AccountEntryRow(
                                    entry = entry,
                                    onClick = { onIntent(ClientAccountUiIntent.EntryClicked(entry.documentId)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSummaryCard(
    clientCompanyName: String,
    invoiceCount: Int,
    totalBilledPaise: Long
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.md)
        ) {
            Text(
                text = clientCompanyName,
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.invoice_count_label),
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = invoiceCount.toString(),
                        style = AppTheme.typography.titleMedium,
                        color = AppTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.total_billed_label),
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(totalBilledPaise)}",
                        style = AppTheme.typography.titleMedium,
                        color = AppTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountEntryRow(
    entry: ClientAccountEntry,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)
                ) {
                    Text(
                        text = entry.narration ?: "Tax Invoice",
                        style = AppTheme.typography.bodyLarge,
                        color = AppTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    AppSyncIndicator(status = entry.syncStatus)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = DateTimeUtils.formatDocumentDate(entry.entryDate),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)
            ) {
                Text(
                    text = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(entry.amountPaise)}",
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.view_document_action),
                    tint = AppTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
