package com.vivaanenterprise.app.feature.product.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.component.AppCard
import com.vivaanenterprise.app.core.designsystem.component.AppEmptyState
import com.vivaanenterprise.app.core.designsystem.component.AppLoadingState
import com.vivaanenterprise.app.core.designsystem.component.AppPrimaryButton
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.AppSecondaryButton
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.designsystem.theme.VivaanEnterpriseTheme
import com.vivaanenterprise.app.domain.model.Product
import com.vivaanenterprise.app.feature.product.model.ProductGstUtils

@Composable
fun ProductDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ProductDetailUiEffect.NavigateToEdit -> onNavigateToEdit(effect.productId)
                ProductDetailUiEffect.DeleteSuccess -> onNavigateBack()
                is ProductDetailUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                }
            }
        }
    }

    ProductDetailScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    uiState: ProductDetailUiState,
    onIntent: (ProductDetailUiIntent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val product = uiState.product

    if (uiState.showDeleteDialog && product != null) {
        AlertDialog(
            onDismissRequest = { onIntent(ProductDetailUiIntent.DismissDeleteDialog) },
            title = { Text(text = stringResource(R.string.delete_product_dialog_title)) },
            text = { Text(text = stringResource(R.string.delete_product_dialog_message, product.name)) },
            confirmButton = {
                TextButton(onClick = { onIntent(ProductDetailUiIntent.ConfirmDeleteClicked) }) {
                    Text(text = stringResource(R.string.delete_action), color = AppTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(ProductDetailUiIntent.DismissDeleteDialog) }) {
                    Text(text = stringResource(R.string.cancel_action))
                }
            }
        )
    }

    AppScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.product_detail_title)) },
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
                AppLoadingState(modifier = Modifier.fillMaxSize())
            }
            product == null -> {
                AppEmptyState(
                    title = stringResource(R.string.error_product_not_found),
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(AppTheme.spacing.md)
                        .verticalScroll(rememberScrollState())
                ) {
                    AppCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppTheme.spacing.md)
                        ) {
                            Text(
                                text = product.name,
                                style = AppTheme.typography.headlineSmall,
                                color = AppTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                            DetailRow(
                                label = stringResource(R.string.hsn_sac_label),
                                value = product.hsnSac
                            )

                            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                            DetailRow(
                                label = stringResource(R.string.gst_rate_label),
                                value = "${ProductGstUtils.formatBasisPointsToPercentage(product.defaultGstRateBasisPoints)}%"
                            )

                            Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.is_active_label),
                                    style = AppTheme.typography.bodyMedium,
                                    color = AppTheme.colorScheme.onSurfaceVariant
                                )

                                Switch(
                                    checked = product.isActive,
                                    onCheckedChange = { onIntent(ProductDetailUiIntent.ToggleActiveClicked(it)) },
                                    enabled = !uiState.isDeleting
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
                    ) {
                        AppPrimaryButton(
                            text = stringResource(R.string.edit_action),
                            onClick = { onIntent(ProductDetailUiIntent.EditClicked) },
                            enabled = !uiState.isDeleting,
                            fullWidth = false,
                            modifier = Modifier.weight(1f)
                        )

                        AppSecondaryButton(
                            text = stringResource(R.string.delete_action),
                            onClick = { onIntent(ProductDetailUiIntent.DeleteClicked) },
                            enabled = !uiState.isDeleting,
                            isLoading = uiState.isDeleting,
                            fullWidth = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))
        Text(
            text = value,
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colorScheme.onSurface
        )
    }
}

@Preview(name = "Product Detail Preview")
@Composable
private fun ProductDetailPreview() {
    VivaanEnterpriseTheme {
        ProductDetailScreen(
            uiState = ProductDetailUiState(
                productId = "p-1",
                product = Product(
                    id = "p-1",
                    name = "3M anti-slip 15mm Scotch Tape",
                    hsnSac = "3919",
                    defaultGstRateBasisPoints = 1800,
                    isActive = true,
                    createdAt = 0L,
                    updatedAt = 0L
                ),
                isLoading = false
            ),
            onIntent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
