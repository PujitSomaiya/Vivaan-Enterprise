package com.vivaanenterprise.app.feature.document.presentation.model

import com.vivaanenterprise.app.domain.model.Product

data class DocumentLineUiState(
    val id: String,
    val selectedProduct: Product? = null,
    val quantityInput: String = "1",
    val rateInput: String = "",
    val quantityError: String? = null,
    val rateError: String? = null,
    val productError: String? = null
)
