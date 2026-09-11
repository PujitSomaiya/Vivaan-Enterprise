package com.vivaanenterprise.app.feature.document.history

import androidx.lifecycle.SavedStateHandle
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.model.ClientSnapshot
import com.vivaanenterprise.app.domain.model.DocumentFinalizationInput
import com.vivaanenterprise.app.domain.model.DocumentFinalizationResult
import com.vivaanenterprise.app.domain.model.DocumentLineItem
import com.vivaanenterprise.app.domain.repository.ClientRepository
import com.vivaanenterprise.app.domain.repository.DocumentRepository
import com.vivaanenterprise.app.feature.document.history.detail.DocumentDetailViewModel
import com.vivaanenterprise.app.feature.document.history.list.DocumentsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoricalDocumentImmutabilityTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun finalizedDocument_preservesHistoricalClientName_whenClientMasterIsRenamed() = runTest {
        val docId = "doc-101"
        val clientId = "client-1"

        val finalizedDocument = BusinessDocument(
            id = docId,
            documentType = DocumentType.TAX_INVOICE,
            documentNumber = "VE/01/2026-27",
            documentDate = 1700000000000L,
            status = DocumentStatus.FINALIZED,
            clientId = clientId,
            clientSnapshot = ClientSnapshot(
                clientId = clientId,
                companyName = "Original Client",
                address = "123 Business St",
                gstin = "24AAAAC1234A1Z5"
            ),
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            syncStatus = SyncStatus.SYNCED
        )

        val renamedClient = Client(
            id = clientId,
            companyName = "Renamed Client",
            address = "New Address St",
            gstin = "24AAAAC1234A1Z5",
            createdAt = 1700000000000L,
            updatedAt = 1700001000000L
        )

        val docRepo = FakeDocumentRepo(listOf(finalizedDocument))
        val clientRepo = FakeClientRepo(listOf(renamedClient))

        // Test DocumentsViewModel (List)
        val listViewModel = DocumentsViewModel(docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val listUiState = listViewModel.uiState.value
        assertEquals(1, listUiState.documents.size)
        assertEquals("Original Client", listUiState.documents[0].clientDisplayName)

        // Test DocumentDetailViewModel (Detail)
        val savedStateHandle = SavedStateHandle(mapOf("documentId" to docId))
        val detailViewModel = DocumentDetailViewModel(savedStateHandle, docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val detailUiState = detailViewModel.uiState.value
        assertEquals("Original Client", detailUiState.clientName)
        assertEquals("Original Client", detailUiState.document?.clientSnapshot?.companyName)
    }

    private class FakeDocumentRepo(initialDocs: List<BusinessDocument>) : DocumentRepository {
        val flow = MutableStateFlow(initialDocs)
        override fun observeAllDocuments(): Flow<List<BusinessDocument>> = flow
        override fun observeDocumentById(id: String): Flow<BusinessDocument?> = MutableStateFlow(flow.value.firstOrNull { it.id == id })
        override fun observeDocumentsByType(type: DocumentType): Flow<List<BusinessDocument>> = MutableStateFlow(flow.value.filter { it.documentType == type })
        override fun observeDocumentsByClient(clientId: String): Flow<List<BusinessDocument>> = MutableStateFlow(flow.value.filter { it.clientId == clientId })
        override suspend fun getDocumentById(id: String): BusinessDocument? = flow.value.firstOrNull { it.id == id }
        override suspend fun getLineItemsForDocument(documentId: String): List<DocumentLineItem> = emptyList()
        override suspend fun suggestDocumentNumber(type: DocumentType, documentDate: Long): String = "VE/01/2026-27"
        override suspend fun createDraft(type: DocumentType, clientId: String, documentDate: Long, documentNumber: String?, lineItems: List<DocumentLineItem>, placeOfSupply: String?, deliveryFactoryAddress: String?, paymentTerms: String?, deliveryNote: String?, supplierReference: String?, otherReferences: String?, buyerOrderNumber: String?, buyerOrderDate: Long?, dispatchDocumentNumber: String?, deliveryNoteDate: Long?, dispatchThrough: String?, destination: String?, termsOfDelivery: String?): Result<BusinessDocument> = TODO()
        override suspend fun updateDraft(document: BusinessDocument, lineItems: List<DocumentLineItem>): Result<BusinessDocument> = TODO()
        override suspend fun finalizeDocument(documentId: String, overrideDocumentNumber: String?): DocumentFinalizationResult = TODO()
        override suspend fun finalizeDocument(input: DocumentFinalizationInput): DocumentFinalizationResult = TODO()
        override suspend fun cancelDocument(documentId: String): Result<Unit> = TODO()
        override suspend fun deleteDocument(documentId: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeClientRepo(initialClients: List<Client>) : ClientRepository {
        val flow = MutableStateFlow(initialClients)
        override fun observeClients(): Flow<List<Client>> = flow
        override fun observeClientById(id: String): Flow<Client?> = MutableStateFlow(flow.value.firstOrNull { it.id == id })
        override suspend fun getClientById(id: String): Client? = flow.value.firstOrNull { it.id == id }
        override suspend fun createClient(client: Client): Result<Unit> = Result.success(Unit)
        override suspend fun updateClient(client: Client): Result<Unit> = Result.success(Unit)
        override suspend fun deleteClient(id: String): Result<Unit> = Result.success(Unit)
    }
}
