package com.vivaanenterprise.app.feature.account.presentation

import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.model.ClientAccountEntry

data class ClientAccountUiState(
    val isLoading: Boolean = true,
    val client: Client? = null,
    val invoiceCount: Int = 0,
    val totalBilledPaise: Long = 0L,
    val entries: List<ClientAccountEntry> = emptyList(),
    val errorMessage: String? = null
)

sealed interface ClientAccountUiIntent {
    data object Retry : ClientAccountUiIntent
    data class EntryClicked(val documentId: String?) : ClientAccountUiIntent
}

sealed interface ClientAccountUiEffect {
    data class OpenDocumentDetail(val documentId: String) : ClientAccountUiEffect
    data class ShowError(val message: String) : ClientAccountUiEffect
}
