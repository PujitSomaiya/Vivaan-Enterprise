package com.vivaanenterprise.app.feature.account.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.domain.repository.ClientAccountRepository
import com.vivaanenterprise.app.domain.repository.ClientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientAccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clientRepository: ClientRepository,
    private val accountRepository: ClientAccountRepository
) : ViewModel() {

    val clientId: String = checkNotNull(savedStateHandle["clientId"])

    private val _uiState = MutableStateFlow(ClientAccountUiState())
    val uiState: StateFlow<ClientAccountUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ClientAccountUiEffect>()
    val uiEffect: SharedFlow<ClientAccountUiEffect> = _uiEffect.asSharedFlow()

    init {
        loadAccountData()
    }

    fun onIntent(intent: ClientAccountUiIntent) {
        when (intent) {
            ClientAccountUiIntent.Retry -> onRetry()
            is ClientAccountUiIntent.EntryClicked -> handleEntryClicked(intent.documentId)
        }
    }

    private fun onRetry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadAccountData()
    }

    private fun loadAccountData() {
        viewModelScope.launch {
            combine(
                clientRepository.observeClientById(clientId),
                accountRepository.observeAccountEntriesForClient(clientId),
                accountRepository.observeAccountSummaryForClient(clientId)
            ) { client, entries, summary ->
                Triple(client, entries, summary)
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Failed to load client account"
                        )
                    }
                }
                .collect { (client, entries, summary) ->
                    if (client == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                client = null,
                                errorMessage = "Client not found"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                client = client,
                                invoiceCount = summary.invoiceCount,
                                totalBilledPaise = summary.totalBilledPaise,
                                entries = entries,
                                errorMessage = null
                            )
                        }
                    }
                }
        }
    }

    private fun handleEntryClicked(documentId: String?) {
        viewModelScope.launch {
            if (documentId.isNullOrBlank()) {
                _uiEffect.emit(ClientAccountUiEffect.ShowError("No linked document for this billing entry"))
            } else {
                _uiEffect.emit(ClientAccountUiEffect.OpenDocumentDetail(documentId))
            }
        }
    }
}
