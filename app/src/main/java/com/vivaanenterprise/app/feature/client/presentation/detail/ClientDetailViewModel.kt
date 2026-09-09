package com.vivaanenterprise.app.feature.client.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.domain.repository.ClientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    private val clientRepository: ClientRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val clientId: String = checkNotNull(savedStateHandle["clientId"])

    private val _uiEffect = Channel<ClientDetailUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    val uiState: StateFlow<ClientDetailUiState> = clientRepository.observeClientById(clientId)
        .map { client ->
            ClientDetailUiState(
                client = client,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ClientDetailUiState(isLoading = true)
        )

    fun onIntent(intent: ClientDetailUiIntent) {
        when (intent) {
            is ClientDetailUiIntent.EditClicked -> _uiEffect.trySend(ClientDetailUiEffect.NavigateToEdit(clientId))
            is ClientDetailUiIntent.DeleteClicked -> { /* Dialog shown in UI */ }
            is ClientDetailUiIntent.ConfirmDeleteClicked -> deleteClient()
        }
    }

    private fun deleteClient() {
        viewModelScope.launch {
            try {
                val result = clientRepository.deleteClient(clientId)
                if (result.isSuccess) {
                    _uiEffect.send(ClientDetailUiEffect.DeleteSuccess)
                } else {
                    _uiEffect.send(ClientDetailUiEffect.ShowError(R.string.error_deleting_client))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiEffect.send(ClientDetailUiEffect.ShowError(R.string.error_deleting_client))
            }
        }
    }
}
