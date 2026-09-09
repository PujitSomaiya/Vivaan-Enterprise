package com.vivaanenterprise.app.feature.client.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.repository.ClientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ClientListViewModel @Inject constructor(
    clientRepository: ClientRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _uiEffect = Channel<ClientListUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    val uiState: StateFlow<ClientListUiState> = combine(
        clientRepository.observeClients(),
        _searchQuery
    ) { allClients, query ->
        val filtered = if (query.isBlank()) {
            allClients
        } else {
            val q = query.trim().lowercase()
            allClients.filter { client ->
                client.companyName.lowercase().contains(q) ||
                        (client.gstin?.lowercase()?.contains(q) == true)
            }
        }
        ClientListUiState(
            clients = filtered,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ClientListUiState(isLoading = true)
    )

    fun onIntent(intent: ClientListUiIntent) {
        when (intent) {
            is ClientListUiIntent.SearchQueryChanged -> _searchQuery.value = intent.query
            is ClientListUiIntent.ClientClicked -> _uiEffect.trySend(ClientListUiEffect.NavigateToDetail(intent.clientId))
            is ClientListUiIntent.AddClientClicked -> _uiEffect.trySend(ClientListUiEffect.NavigateToAddClient)
        }
    }
}
