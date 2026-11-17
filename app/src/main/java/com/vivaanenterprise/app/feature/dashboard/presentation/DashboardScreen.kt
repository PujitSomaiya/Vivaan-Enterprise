package com.vivaanenterprise.app.feature.dashboard.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.VeLogo
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.designsystem.theme.LightErrorContainer
import com.vivaanenterprise.app.core.designsystem.theme.LightOnErrorContainer
import com.vivaanenterprise.app.core.designsystem.theme.LightOnSuccessContainer
import com.vivaanenterprise.app.core.designsystem.theme.LightOnWarningContainer
import com.vivaanenterprise.app.core.designsystem.theme.LightSuccessContainer
import com.vivaanenterprise.app.core.designsystem.theme.LightWarningContainer
import com.vivaanenterprise.app.core.designsystem.theme.VivaanEnterpriseTheme
import com.vivaanenterprise.app.domain.repository.SyncStatusSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardRoute(
    onSignOutClick: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToNewInvoice: () -> Unit,
    onNavigateToNewPurchaseOrder: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardScreen(
        uiState = uiState,
        onSignOutClick = onSignOutClick,
        onNavigateToClients = onNavigateToClients,
        onNavigateToProducts = onNavigateToProducts,
        onNavigateToNewInvoice = onNavigateToNewInvoice,
        onNavigateToNewPurchaseOrder = onNavigateToNewPurchaseOrder,
        onNavigateToDocuments = onNavigateToDocuments,
        onSyncNowClick = viewModel::onSyncNowClick,
        onShowSyncDiagnostics = viewModel::onShowSyncDiagnostics,
        onDismissSyncDiagnostics = viewModel::onDismissSyncDiagnostics,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState = DashboardUiState(),
    onSignOutClick: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToNewInvoice: () -> Unit = {},
    onNavigateToNewPurchaseOrder: () -> Unit = {},
    onNavigateToDocuments: () -> Unit = {},
    onSyncNowClick: () -> Unit = {},
    onShowSyncDiagnostics: () -> Unit = {},
    onDismissSyncDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSignOutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    // Double-back exit handler on Dashboard (disabled if dialog is showing)
    com.vivaanenterprise.app.core.common.util.DoubleBackToExitHandler(
        snackbarHostState = snackbarHostState,
        enabled = !showSignOutDialog && !uiState.showSyncDiagnosticsDialog
    )

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(text = stringResource(R.string.sign_out_dialog_title)) },
            text = { Text(text = stringResource(R.string.sign_out_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOutClick()
                    }
                ) {
                    Text(text = stringResource(R.string.confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(text = stringResource(R.string.cancel_action))
                }
            }
        )
    }

    if (uiState.showSyncDiagnosticsDialog) {
        SyncDiagnosticsDialog(
            syncSummary = uiState.syncSummary,
            onDismiss = onDismissSyncDiagnostics,
            onSyncNow = {
                onDismissSyncDiagnostics()
                onSyncNowClick()
            }
        )
    }

    AppScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name), style = AppTheme.typography.titleLarge) },
                actions = {
                    // Sync action button
                    IconButton(
                        onClick = onSyncNowClick,
                        enabled = !uiState.syncSummary.isSyncing && !uiState.isManualSyncing
                    ) {
                        if (uiState.syncSummary.isSyncing || uiState.isManualSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AppTheme.colorScheme.primary
                            )
                        } else {
                            val tint = if (uiState.syncSummary.failedCount > 0) {
                                AppTheme.colorScheme.error
                            } else {
                                AppTheme.colorScheme.onSurfaceVariant
                            }
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.sync_action_now),
                                tint = tint
                            )
                        }
                    }

                    // Sign Out action button
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.sign_out_action),
                            tint = AppTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
        ) {
            // Hero Brand & Welcome Card
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VeLogo(size = AppTheme.sizing.logoMedium)
                    Spacer(modifier = Modifier.height(AppTheme.spacing.sm))
                    Text(
                        text = stringResource(R.string.dashboard_welcome),
                        style = AppTheme.typography.headlineSmall,
                        color = AppTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))
                    Text(
                        text = stringResource(R.string.dashboard_subtitle),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sync Status Banner Card
            DashboardSyncStatusCard(
                syncSummary = uiState.syncSummary,
                isManualSyncing = uiState.isManualSyncing,
                onSyncNow = onSyncNowClick,
                onViewDetails = onShowSyncDiagnostics
            )

            // Restrained KPI Summary Section
            DashboardSummarySection(
                finalizedInvoiceCount = uiState.summary.finalizedInvoiceCount,
                totalBilledPaise = uiState.summary.totalBilledPaise
            )

            // Primary Actions Section
            Text(
                text = "Quick Actions",
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = AppTheme.spacing.xs, top = AppTheme.spacing.xs)
            )

            com.vivaanenterprise.app.core.designsystem.component.AppPrimaryButton(
                text = "New Tax Invoice",
                onClick = onNavigateToNewInvoice,
                modifier = Modifier.fillMaxWidth()
            )

            com.vivaanenterprise.app.core.designsystem.component.AppPrimaryButton(
                text = "New Purchase Order",
                onClick = onNavigateToNewPurchaseOrder,
                modifier = Modifier.fillMaxWidth()
            )

            com.vivaanenterprise.app.core.designsystem.component.AppSecondaryButton(
                text = stringResource(R.string.documents_title),
                onClick = onNavigateToDocuments,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
            ) {
                com.vivaanenterprise.app.core.designsystem.component.AppSecondaryButton(
                    text = stringResource(R.string.clients_title),
                    onClick = onNavigateToClients,
                    modifier = Modifier.weight(1f)
                )

                com.vivaanenterprise.app.core.designsystem.component.AppSecondaryButton(
                    text = stringResource(R.string.products_title),
                    onClick = onNavigateToProducts,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

            com.vivaanenterprise.app.core.designsystem.component.DeveloperCredit()
        }
    }
}

@Composable
private fun DashboardSyncStatusCard(
    syncSummary: SyncStatusSummary,
    isManualSyncing: Boolean,
    onSyncNow: () -> Unit,
    onViewDetails: () -> Unit
) {
    val isBusy = syncSummary.isSyncing || isManualSyncing
    val hasFailed = syncSummary.failedCount > 0
    val hasPending = syncSummary.unsyncedCount > 0

    val (bgColor, textColor, icon, label) = when {
        isBusy -> {
            Tuple4(
                AppTheme.colorScheme.primaryContainer,
                AppTheme.colorScheme.onPrimaryContainer,
                Icons.Default.Refresh,
                stringResource(R.string.sync_status_syncing)
            )
        }
        hasFailed -> {
            Tuple4(
                LightErrorContainer,
                LightOnErrorContainer,
                Icons.Default.Warning,
                stringResource(R.string.sync_status_failed, syncSummary.failedCount)
            )
        }
        hasPending -> {
            Tuple4(
                LightWarningContainer,
                LightOnWarningContainer,
                Icons.Default.Info,
                stringResource(R.string.sync_status_pending, syncSummary.unsyncedCount)
            )
        }
        else -> {
            Tuple4(
                LightSuccessContainer,
                LightOnSuccessContainer,
                Icons.Default.CheckCircle,
                stringResource(R.string.sync_status_synced)
            )
        }
    }

    Surface(
        color = bgColor,
        shape = AppTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetails)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                modifier = Modifier.weight(1f)
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = textColor
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = label,
                        style = AppTheme.typography.bodyMedium,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (syncSummary.lastSyncTimestamp > 0L) {
                        Text(
                            text = "Last synced: ${formatTimestamp(syncSummary.lastSyncTimestamp)}",
                            style = AppTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            TextButton(onClick = onSyncNow, enabled = !isBusy) {
                Text(
                    text = stringResource(R.string.sync_action_now),
                    style = AppTheme.typography.labelMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SyncDiagnosticsDialog(
    syncSummary: SyncStatusSummary,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.sync_diagnostics_title),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                Text(
                    text = stringResource(R.string.sync_diagnostics_status, syncSummary.lastSyncStatus),
                    style = AppTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        R.string.sync_diagnostics_last_sync,
                        if (syncSummary.lastSyncTimestamp > 0) formatTimestamp(syncSummary.lastSyncTimestamp) else stringResource(R.string.sync_diagnostics_never)
                    ),
                    style = AppTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(
                        R.string.sync_diagnostics_last_attempt,
                        if (syncSummary.lastSyncAttemptTimestamp > 0) formatTimestamp(syncSummary.lastSyncAttemptTimestamp) else stringResource(R.string.sync_diagnostics_never)
                    ),
                    style = AppTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.sync_diagnostics_unsynced_items, syncSummary.unsyncedCount),
                    style = AppTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.sync_diagnostics_failed_items, syncSummary.failedCount),
                    style = AppTheme.typography.bodySmall,
                    color = if (syncSummary.failedCount > 0) AppTheme.colorScheme.error else AppTheme.colorScheme.onSurface
                )
                if (!syncSummary.lastSyncError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))
                    Text(
                        text = stringResource(R.string.sync_diagnostics_error, syncSummary.lastSyncError),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSyncNow) {
                Text(text = stringResource(R.string.sync_action_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.confirm_action))
            }
        }
    )
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun DashboardSummarySection(
    finalizedInvoiceCount: Int,
    totalBilledPaise: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
    ) {
        AppCard(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.dashboard_finalized_invoices_label),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                Text(
                    text = finalizedInvoiceCount.toString(),
                    style = AppTheme.typography.titleLarge,
                    color = AppTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AppCard(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.dashboard_total_billed_label),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                Text(
                    text = "₹ ${com.vivaanenterprise.app.core.pdf.PdfFormattingUtils.formatPaiseToCurrency(totalBilledPaise)}",
                    style = AppTheme.typography.titleLarge,
                    color = AppTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(name = "Dashboard Screen Light")
@Composable
private fun DashboardScreenLightPreview() {
    VivaanEnterpriseTheme(darkTheme = false) {
        DashboardScreen(onSignOutClick = {}, onNavigateToClients = {}, onNavigateToProducts = {})
    }
}

@Preview(name = "Dashboard Screen Dark")
@Composable
private fun DashboardScreenDarkPreview() {
    VivaanEnterpriseTheme(darkTheme = true) {
        DashboardScreen(onSignOutClick = {}, onNavigateToClients = {}, onNavigateToProducts = {})
    }
}
