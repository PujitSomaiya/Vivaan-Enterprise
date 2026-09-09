package com.vivaanenterprise.app.feature.product.presentation.form

enum class ProductFormFieldValidationError {
    NAME_REQUIRED,
    HSN_SAC_REQUIRED,
    GST_RATE_INVALID
}

data class ProductFormUiState(
    val productId: String? = null,
    val name: String = "",
    val hsnSac: String = "",
    val gstRateInput: String = "",
    val isActive: Boolean = true,
    val isLoadingProduct: Boolean = false,
    val isSaving: Boolean = false,
    val nameError: ProductFormFieldValidationError? = null,
    val hsnSacError: ProductFormFieldValidationError? = null,
    val gstRateError: ProductFormFieldValidationError? = null
) {
    val isEditMode: Boolean get() = productId != null
}

sealed interface ProductFormUiIntent {
    data class NameChanged(val value: String) : ProductFormUiIntent
    data class HsnSacChanged(val value: String) : ProductFormUiIntent
    data class GstRateChanged(val value: String) : ProductFormUiIntent
    data class IsActiveChanged(val value: Boolean) : ProductFormUiIntent
    data object SaveClicked : ProductFormUiIntent
}

sealed interface ProductFormUiEffect {
    data object SaveSuccess : ProductFormUiEffect
    data class ShowError(val messageRes: Int) : ProductFormUiEffect
}
