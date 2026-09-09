package com.vivaanenterprise.app.feature.dashboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.component.AppCard
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.VeLogo
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.designsystem.theme.VivaanEnterpriseTheme

@Composable
fun DashboardRoute(
    onSignOutClick: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToNewInvoice: () -> Unit,
    onNavigateToNewPurchaseOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardScreen(
        onSignOutClick = onSignOutClick,
        onNavigateToClients = onNavigateToClients,
        onNavigateToProducts = onNavigateToProducts,
        onNavigateToNewInvoice = onNavigateToNewInvoice,
        onNavigateToNewPurchaseOrder = onNavigateToNewPurchaseOrder,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSignOutClick: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToNewInvoice: () -> Unit = {},
    onNavigateToNewPurchaseOrder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSignOutDialog by remember { mutableStateOf(false) }

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

    AppScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.sign_out_action)
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
                .padding(AppTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VeLogo(size = AppTheme.sizing.logoMedium)

                    Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                    Text(
                        text = stringResource(R.string.dashboard_welcome),
                        style = AppTheme.typography.headlineSmall,
                        color = AppTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                    Text(
                        text = stringResource(R.string.dashboard_subtitle),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                    com.vivaanenterprise.app.core.designsystem.component.AppPrimaryButton(
                        text = "New Tax Invoice",
                        onClick = onNavigateToNewInvoice,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                    com.vivaanenterprise.app.core.designsystem.component.AppPrimaryButton(
                        text = "New Purchase Order",
                        onClick = onNavigateToNewPurchaseOrder,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                    com.vivaanenterprise.app.core.designsystem.component.AppSecondaryButton(
                        text = stringResource(R.string.clients_title),
                        onClick = onNavigateToClients,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                    com.vivaanenterprise.app.core.designsystem.component.AppSecondaryButton(
                        text = stringResource(R.string.products_title),
                        onClick = onNavigateToProducts,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
