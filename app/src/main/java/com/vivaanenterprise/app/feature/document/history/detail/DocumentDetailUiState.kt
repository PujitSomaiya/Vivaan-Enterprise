package com.vivaanenterprise.app.feature.document.history.detail

import com.vivaanenterprise.app.domain.model.BusinessDocument

data class DocumentDetailUiState(
    val isLoading: Boolean = true,
    val document: BusinessDocument? = null,
    val clientName: String = "",
    val errorMessage: String? = null,
    val showDeleteConfirmationDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeletedSuccessfully: Boolean = false
)
