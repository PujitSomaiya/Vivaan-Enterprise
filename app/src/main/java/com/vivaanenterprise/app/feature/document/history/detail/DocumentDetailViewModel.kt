package com.vivaanenterprise.app.feature.document.history.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.domain.repository.ClientRepository
import com.vivaanenterprise.app.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val documentRepository: DocumentRepository,
    private val clientRepository: ClientRepository
) : ViewModel() {

    val documentId: String = checkNotNull(savedStateHandle["documentId"])

    private val _uiState = MutableStateFlow(DocumentDetailUiState())
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    init {
        loadDocumentDetail()
    }

    fun onRetry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadDocumentDetail()
    }

    private fun loadDocumentDetail() {
        viewModelScope.launch {
            combine(
                documentRepository.observeDocumentById(documentId),
                clientRepository.observeClients()
            ) { document, clients ->
                if (document == null) {
                    return@combine Pair(null, "")
                }

                val clientMap = clients.associateBy { it.id }
                val clientDisplayName = if (document.status == DocumentStatus.FINALIZED) {
                    // Crucial snapshot immutability enforcement:
                    // Finalized documents must render from historical clientSnapshot ONLY
                    document.clientSnapshot?.companyName ?: ""
                } else {
                    document.clientSnapshot?.companyName
                        ?: clientMap[document.clientId]?.companyName
                        ?: ""
                }

                Pair(document, clientDisplayName)
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Failed to load document detail"
                        )
                    }
                }
                .collect { (doc, clientName) ->
                    if (doc == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                document = null,
                                clientName = "",
                                errorMessage = "Document not found"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                document = doc,
                                clientName = clientName,
                                errorMessage = null
                            )
                        }
                    }
                }
        }
    }
}
