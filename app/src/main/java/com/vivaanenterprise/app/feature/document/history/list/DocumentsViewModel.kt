package com.vivaanenterprise.app.feature.document.history.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.domain.model.BusinessDocument
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
class DocumentsViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val clientRepository: ClientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onTypeFilterSelected(filter: DocumentTypeFilter) {
        _uiState.update { it.copy(selectedTypeFilter = filter) }
    }

    fun onStatusFilterSelected(filter: DocumentStatusFilter) {
        _uiState.update { it.copy(selectedStatusFilter = filter) }
    }

    fun onRetry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadDocuments()
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            combine(
                documentRepository.observeAllDocuments(),
                clientRepository.observeClients()
            ) { documents, clients ->
                val clientMap = clients.associateBy { it.id }
                
                val uiModels = documents.map { doc ->
                    val clientDisplayName = if (doc.status == DocumentStatus.FINALIZED) {
                        // Crucial immutability requirement:
                        // Finalized documents must ONLY use historical clientSnapshot companyName
                        doc.clientSnapshot?.companyName ?: ""
                    } else {
                        // Draft documents use historical snapshot if present, else current client master name
                        doc.clientSnapshot?.companyName
                            ?: clientMap[doc.clientId]?.companyName
                            ?: ""
                    }

                    DocumentListItemUiModel(
                        id = doc.id,
                        documentType = doc.documentType,
                        documentNumber = doc.documentNumber,
                        documentDate = doc.documentDate,
                        status = doc.status,
                        clientDisplayName = clientDisplayName,
                        grandTotalPaise = doc.grandTotalPaise,
                        syncStatus = doc.syncStatus,
                        updatedAt = doc.updatedAt
                    )
                }.sortedWith(
                    compareByDescending<DocumentListItemUiModel> { it.documentDate }
                        .thenByDescending { it.updatedAt }
                        .thenByDescending { it.id }
                )

                uiModels
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Failed to load documents"
                        )
                    }
                }
                .collect { uiModels ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            documents = uiModels,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}
