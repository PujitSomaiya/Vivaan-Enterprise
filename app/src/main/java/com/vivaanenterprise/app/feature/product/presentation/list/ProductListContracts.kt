package com.vivaanenterprise.app.feature.product.presentation.list

import com.vivaanenterprise.app.domain.model.Product

enum class ProductFilter {
    ALL,
    ACTIVE,
    INACTIVE
}

data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val filter: ProductFilter = ProductFilter.ALL,
    val isLoading: Boolean = true,
    val errorMessageRes: Int? = null
)

sealed interface ProductListUiIntent {
    data class SearchQueryChanged(val query: String) : ProductListUiIntent
    data class FilterChanged(val filter: ProductFilter) : ProductListUiIntent
    data class ProductClicked(val productId: String) : ProductListUiIntent
    data object AddProductClicked : ProductListUiIntent
    data class ToggleActiveClicked(val productId: String, val isActive: Boolean) : ProductListUiIntent
}

sealed interface ProductListUiEffect {
    data class NavigateToDetail(val productId: String) : ProductListUiEffect
    data object NavigateToAddProduct : ProductListUiEffect
    data class ShowError(val messageRes: Int) : ProductListUiEffect
}
