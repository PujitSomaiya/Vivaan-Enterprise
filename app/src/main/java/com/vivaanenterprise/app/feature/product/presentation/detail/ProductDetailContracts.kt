package com.vivaanenterprise.app.feature.product.presentation.detail

import com.vivaanenterprise.app.domain.model.Product

data class ProductDetailUiState(
    val productId: String = "",
    val product: Product? = null,
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val showDeleteDialog: Boolean = false
)

sealed interface ProductDetailUiIntent {
    data object EditClicked : ProductDetailUiIntent
    data class ToggleActiveClicked(val isActive: Boolean) : ProductDetailUiIntent
    data object DeleteClicked : ProductDetailUiIntent
    data object DismissDeleteDialog : ProductDetailUiIntent
    data object ConfirmDeleteClicked : ProductDetailUiIntent
}

sealed interface ProductDetailUiEffect {
    data class NavigateToEdit(val productId: String) : ProductDetailUiEffect
    data object DeleteSuccess : ProductDetailUiEffect
    data class ShowError(val messageRes: Int) : ProductDetailUiEffect
}
