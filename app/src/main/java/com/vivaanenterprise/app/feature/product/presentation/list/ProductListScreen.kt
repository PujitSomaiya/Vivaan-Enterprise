package com.vivaanenterprise.app.feature.product.presentation.list

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.component.AppCard
import com.vivaanenterprise.app.core.designsystem.component.AppEmptyState
import com.vivaanenterprise.app.core.designsystem.component.AppLoadingState
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.AppSearchField
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.designsystem.theme.VivaanEnterpriseTheme
import com.vivaanenterprise.app.domain.model.Product
import com.vivaanenterprise.app.feature.product.model.ProductGstUtils

@Composable
fun ProductListRoute(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ProductListUiEffect.NavigateToDetail -> onNavigateToDetail(effect.productId)
                ProductListUiEffect.NavigateToAddProduct -> onNavigateToAddProduct()
                is ProductListUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                }
            }
        }
    }

    ProductListScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    uiState: ProductListUiState,
    onIntent: (ProductListUiIntent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.products_title)) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onIntent(ProductListUiIntent.AddProductClicked) },
                containerColor = AppTheme.colorScheme.primary,
                contentColor = AppTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_product_action)
                )
            }
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppTheme.spacing.md)
        ) {
            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

            AppSearchField(
                query = uiState.searchQuery,
                onQueryChange = { onIntent(ProductListUiIntent.SearchQueryChanged(it)) },
                placeholder = stringResource(R.string.search_products_placeholder),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)
            ) {
                FilterChip(
                    selected = uiState.filter == ProductFilter.ALL,
                    onClick = { onIntent(ProductListUiIntent.FilterChanged(ProductFilter.ALL)) },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
                FilterChip(
                    selected = uiState.filter == ProductFilter.ACTIVE,
                    onClick = { onIntent(ProductListUiIntent.FilterChanged(ProductFilter.ACTIVE)) },
                    label = { Text(stringResource(R.string.filter_active)) }
                )
                FilterChip(
                    selected = uiState.filter == ProductFilter.INACTIVE,
                    onClick = { onIntent(ProductListUiIntent.FilterChanged(ProductFilter.INACTIVE)) },
                    label = { Text(stringResource(R.string.filter_inactive)) }
                )
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

            when {
                uiState.isLoading -> {
                    AppLoadingState(modifier = Modifier.fillMaxSize())
                }
                uiState.products.isEmpty() && uiState.searchQuery.isBlank() -> {
                    AppEmptyState(
                        title = stringResource(R.string.empty_products_title),
                        message = stringResource(R.string.empty_products_description),
                        actionText = stringResource(R.string.add_product_action),
                        onActionClick = { onIntent(ProductListUiIntent.AddProductClicked) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.products.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    AppEmptyState(
                        title = stringResource(R.string.empty_product_search_title),
                        message = stringResource(R.string.empty_product_search_description),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                    ) {
                        items(
                            items = uiState.products,
                            key = { it.id }
                        ) { product ->
                            ProductListItemCard(
                                product = product,
                                onClick = { onIntent(ProductListUiIntent.ProductClicked(product.id)) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(AppTheme.spacing.xxl))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductListItemCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "HSN/SAC: ${product.hsnSac}",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(AppTheme.spacing.md))
                    val gstFormatted = ProductGstUtils.formatBasisPointsToPercentage(product.defaultGstRateBasisPoints)
                    Text(
                        text = "GST: $gstFormatted%",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppTheme.spacing.sm))

            ProductStatusBadge(isActive = product.isActive)
        }
    }
}

@Composable
private fun ProductStatusBadge(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) {
        AppTheme.colorScheme.secondaryContainer
    } else {
        AppTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isActive) {
        AppTheme.colorScheme.onSecondaryContainer
    } else {
        AppTheme.colorScheme.onSurfaceVariant
    }

    val statusText = if (isActive) {
        stringResource(R.string.status_active)
    } else {
        stringResource(R.string.status_inactive)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs)
    ) {
        Text(
            text = statusText,
            style = AppTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Preview(name = "Product List Screen Content Preview")
@Composable
private fun ProductListScreenContentPreview() {
    VivaanEnterpriseTheme {
        ProductListScreen(
            uiState = ProductListUiState(
                isLoading = false,
                products = listOf(
                    Product(
                        id = "p-1",
                        name = "3M anti-slip 15mm Scotch Tape",
                        hsnSac = "3919",
                        defaultGstRateBasisPoints = 1800,
                        isActive = true,
                        createdAt = 0L,
                        updatedAt = 0L
                    ),
                    Product(
                        id = "p-2",
                        name = "Industrial Packaging Box",
                        hsnSac = "4819",
                        defaultGstRateBasisPoints = 1200,
                        isActive = false,
                        createdAt = 0L,
                        updatedAt = 0L
                    )
                )
            ),
            onIntent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "Product List Screen Empty Preview")
@Composable
private fun ProductListScreenEmptyPreview() {
    VivaanEnterpriseTheme {
        ProductListScreen(
            uiState = ProductListUiState(
                isLoading = false,
                products = emptyList()
            ),
            onIntent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
