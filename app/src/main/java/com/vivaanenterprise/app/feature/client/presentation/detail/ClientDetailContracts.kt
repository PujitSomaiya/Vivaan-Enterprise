package com.vivaanenterprise.app.feature.client.presentation.detail

import com.vivaanenterprise.app.domain.model.Client

data class ClientDetailUiState(
    val client: Client? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null
)

sealed interface ClientDetailUiIntent {
    data object EditClicked : ClientDetailUiIntent
    data object DeleteClicked : ClientDetailUiIntent
    data object ConfirmDeleteClicked : ClientDetailUiIntent
}

sealed interface ClientDetailUiEffect {
    data class NavigateToEdit(val clientId: String) : ClientDetailUiEffect
    data object DeleteSuccess : ClientDetailUiEffect
    data class ShowError(val messageRes: Int) : ClientDetailUiEffect
}
