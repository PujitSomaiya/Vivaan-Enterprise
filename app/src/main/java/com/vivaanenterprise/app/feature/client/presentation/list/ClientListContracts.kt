package com.vivaanenterprise.app.feature.client.presentation.list

import com.vivaanenterprise.app.domain.model.Client

data class ClientListUiState(
    val clients: List<Client> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface ClientListUiIntent {
    data class SearchQueryChanged(val query: String) : ClientListUiIntent
    data class ClientClicked(val clientId: String) : ClientListUiIntent
    data object AddClientClicked : ClientListUiIntent
}

sealed interface ClientListUiEffect {
    data class NavigateToDetail(val clientId: String) : ClientListUiEffect
    data object NavigateToAddClient : ClientListUiEffect
}
