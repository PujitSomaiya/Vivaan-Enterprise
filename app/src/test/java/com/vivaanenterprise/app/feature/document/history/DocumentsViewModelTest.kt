package com.vivaanenterprise.app.feature.document.history

import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.model.ClientSnapshot
import com.vivaanenterprise.app.domain.model.DocumentFinalizationResult
import com.vivaanenterprise.app.domain.model.DocumentLineItem
import com.vivaanenterprise.app.domain.repository.ClientRepository
import com.vivaanenterprise.app.domain.repository.DocumentRepository
import com.vivaanenterprise.app.feature.document.history.list.DocumentStatusFilter
import com.vivaanenterprise.app.feature.document.history.list.DocumentTypeFilter
import com.vivaanenterprise.app.feature.document.history.list.DocumentsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleInvoiceDraft = BusinessDocument(
        id = "doc-inv-1",
        documentType = DocumentType.TAX_INVOICE,
        documentNumber = "VE/01/2026-27",
        documentDate = 1700000000000L,
        status = DocumentStatus.DRAFT,
        clientId = "client-1",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L,
        syncStatus = SyncStatus.PENDING
    )

    private val sampleInvoiceFinalized = BusinessDocument(
        id = "doc-inv-2",
        documentType = DocumentType.TAX_INVOICE,
        documentNumber = "VE/02/2026-27",
        documentDate = 1705000000000L,
        status = DocumentStatus.FINALIZED,
        clientId = "client-1",
        clientSnapshot = ClientSnapshot(clientId = "client-1", companyName = "Acme Corp Snapshot"),
        grandTotalPaise = 2500000L,
        createdAt = 1705000000000L,
        updatedAt = 1705000000000L,
        syncStatus = SyncStatus.SYNCED
    )

    private val samplePoDraft = BusinessDocument(
        id = "doc-po-1",
        documentType = DocumentType.PURCHASE_ORDER,
        documentNumber = "VE/PO/01/2026-27",
        documentDate = 1700000000000L,
        status = DocumentStatus.DRAFT,
        clientId = "client-2",
        createdAt = 1700000000000L,
        updatedAt = 1700000500000L, // Newer updatedAt for tie-breaker test
        syncStatus = SyncStatus.PENDING
    )

    private val samplePoFinalized = BusinessDocument(
        id = "doc-po-2",
        documentType = DocumentType.PURCHASE_ORDER,
        documentNumber = "VE/PO/02/2026-27",
        documentDate = 1710000000000L,
        status = DocumentStatus.FINALIZED,
        clientId = "client-2",
        clientSnapshot = ClientSnapshot(
            clientId = "client-2",
            companyName = "Apex Supplies Pvt Ltd"
        ),
        grandTotalPaise = 500000L,
        createdAt = 1710000000000L,
        updatedAt = 1710000000000L,
        syncStatus = SyncStatus.SYNCED
    )

    private val sampleClient1 = Client(
        id = "client-1",
        companyName = "Acme Corp",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L
    )

    private val sampleClient2 = Client(
        id = "client-2",
        companyName = "Apex Supplies Pvt Ltd",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_displaysDocumentsWithNewestDateFirst() = runTest {
        val docRepo = FakeDocumentRepo(listOf(sampleInvoiceDraft, samplePoFinalized))
        val clientRepo = FakeClientRepo(listOf(sampleClient1))

        val viewModel = DocumentsViewModel(docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(2, state.documents.size)

        // samplePoFinalized date (1710000000000) > sampleInvoiceDraft date (1700000000000)
        assertEquals("doc-po-2", state.documents[0].id)
        assertEquals("doc-inv-1", state.documents[1].id)
    }

    @Test
    fun load_equalDocumentDate_usesUpdatedAtAndIdTieBreaker() = runTest {
        // Both sampleInvoiceDraft and samplePoDraft have documentDate = 1700000000000L
        // samplePoDraft has newer updatedAt = 1700000500000L
        val docRepo = FakeDocumentRepo(listOf(sampleInvoiceDraft, samplePoDraft))
        val clientRepo = FakeClientRepo(listOf(sampleClient1, sampleClient2))

        val viewModel = DocumentsViewModel(docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.documents.size)
        assertEquals("doc-po-1", state.documents[0].id)
        assertEquals("doc-inv-1", state.documents[1].id)
    }

    @Test
    fun load_errorInRepository_setsErrorMessageAndSupportsRetry() = runTest {
        val errorRepo = ErrorDocumentRepo()
        val clientRepo = FakeClientRepo(emptyList())

        val viewModel = DocumentsViewModel(errorRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("DB Connection Error", state.errorMessage)

        // Retry with valid flow
        errorRepo.shouldFail = false
        viewModel.onRetry()
        testDispatcher.scheduler.advanceUntilIdle()

        val retriedState = viewModel.uiState.value
        assertFalse(retriedState.isLoading)
        assertNull(retriedState.errorMessage)
        assertEquals(1, retriedState.documents.size)
    }

    @Test
    fun typeFilter_filtersCorrectly() = runTest {
        val docRepo = FakeDocumentRepo(listOf(sampleInvoiceDraft, samplePoFinalized))
        val clientRepo = FakeClientRepo(listOf(sampleClient1))

        val viewModel = DocumentsViewModel(docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        // ALL
        assertEquals(2, viewModel.uiState.value.filteredDocuments.size)

        // TAX_INVOICE
        viewModel.onTypeFilterSelected(DocumentTypeFilter.TAX_INVOICE)
        val invoiceFiltered = viewModel.uiState.value.filteredDocuments
        assertEquals(1, invoiceFiltered.size)
        assertEquals(DocumentType.TAX_INVOICE, invoiceFiltered[0].documentType)

        // PURCHASE_ORDER
        viewModel.onTypeFilterSelected(DocumentTypeFilter.PURCHASE_ORDER)
        val poFiltered = viewModel.uiState.value.filteredDocuments
        assertEquals(1, poFiltered.size)
        assertEquals(DocumentType.PURCHASE_ORDER, poFiltered[0].documentType)
    }

    @Test
    fun statusFilter_filtersCorrectly() = runTest {
        val docRepo = FakeDocumentRepo(listOf(sampleInvoiceDraft, samplePoFinalized))
        val clientRepo = FakeClientRepo(listOf(sampleClient1))

        val viewModel = DocumentsViewModel(docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        // DRAFT
        viewModel.onStatusFilterSelected(DocumentStatusFilter.DRAFT)
        val draftFiltered = viewModel.uiState.value.filteredDocuments
        assertEquals(1, draftFiltered.size)
        assertEquals(DocumentStatus.DRAFT, draftFiltered[0].status)

        // FINALIZED
        viewModel.onStatusFilterSelected(DocumentStatusFilter.FINALIZED)
        val finalizedFiltered = viewModel.uiState.value.filteredDocuments
        assertEquals(1, finalizedFiltered.size)
        assertEquals(DocumentStatus.FINALIZED, finalizedFiltered[0].status)
    }

    @Test
    fun search_matchesDocumentNumberAndClientSnapshotName_caseInsensitiveAndTrimmed() = runTest {
        val docRepo = FakeDocumentRepo(listOf(sampleInvoiceDraft, samplePoFinalized))
        val clientRepo = FakeClientRepo(listOf(sampleClient1))

        val viewModel = DocumentsViewModel(docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        // Search by document number partial case-insensitive with whitespace
        viewModel.onSearchQueryChanged("  po/02  ")
        val search1 = viewModel.uiState.value.filteredDocuments
        assertEquals(1, search1.size)
        assertEquals("doc-po-2", search1[0].id)

        // Search by finalized client company name
        viewModel.onSearchQueryChanged("apex")
        val search2 = viewModel.uiState.value.filteredDocuments
        assertEquals(1, search2.size)
        assertEquals("Apex Supplies Pvt Ltd", search2[0].clientDisplayName)

        // Blank query returns all
        viewModel.onSearchQueryChanged("   ")
        assertEquals(2, viewModel.uiState.value.filteredDocuments.size)

        // No match returns empty filtered list
        viewModel.onSearchQueryChanged("nonexistent")
        assertTrue(viewModel.uiState.value.filteredDocuments.isEmpty())
    }

    @Test
    fun combinedTypeAndStatusFilters_filterExhaustively() = runTest {
        val docRepo = FakeDocumentRepo(listOf(sampleInvoiceDraft, sampleInvoiceFinalized, samplePoDraft, samplePoFinalized))
        val clientRepo = FakeClientRepo(listOf(sampleClient1, sampleClient2))

        val viewModel = DocumentsViewModel(docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        // TAX_INVOICE + DRAFT
        viewModel.onTypeFilterSelected(DocumentTypeFilter.TAX_INVOICE)
        viewModel.onStatusFilterSelected(DocumentStatusFilter.DRAFT)
        val invDraft = viewModel.uiState.value.filteredDocuments
        assertEquals(1, invDraft.size)
        assertEquals("doc-inv-1", invDraft[0].id)

        // TAX_INVOICE + FINALIZED
        viewModel.onStatusFilterSelected(DocumentStatusFilter.FINALIZED)
        val invFin = viewModel.uiState.value.filteredDocuments
        assertEquals(1, invFin.size)
        assertEquals("doc-inv-2", invFin[0].id)

        // PURCHASE_ORDER + DRAFT
        viewModel.onTypeFilterSelected(DocumentTypeFilter.PURCHASE_ORDER)
        viewModel.onStatusFilterSelected(DocumentStatusFilter.DRAFT)
        val poDraft = viewModel.uiState.value.filteredDocuments
        assertEquals(1, poDraft.size)
        assertEquals("doc-po-1", poDraft[0].id)

        // PURCHASE_ORDER + FINALIZED
        viewModel.onStatusFilterSelected(DocumentStatusFilter.FINALIZED)
        val poFin = viewModel.uiState.value.filteredDocuments
        assertEquals(1, poFin.size)
        assertEquals("doc-po-2", poFin[0].id)
    }

    @Test
    fun combinedSearchAndFilters_workTogether() = runTest {
        val docRepo = FakeDocumentRepo(listOf(sampleInvoiceDraft, samplePoFinalized))
        val clientRepo = FakeClientRepo(listOf(sampleClient1))

        val viewModel = DocumentsViewModel(docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        // Search + Type Filter
        viewModel.onTypeFilterSelected(DocumentTypeFilter.TAX_INVOICE)
        viewModel.onSearchQueryChanged("Acme")
        assertEquals(1, viewModel.uiState.value.filteredDocuments.size)

        // Search + Type Filter with no match
        viewModel.onSearchQueryChanged("Apex")
        assertTrue(viewModel.uiState.value.filteredDocuments.isEmpty())
    }

    @Test
    fun reactiveUpdates_flowRefreshesListAndSyncStatus() = runTest {
        val docRepo = FakeDocumentRepo(listOf(sampleInvoiceDraft))
        val clientRepo = FakeClientRepo(listOf(sampleClient1))

        val viewModel = DocumentsViewModel(docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.documents.size)
        assertEquals(SyncStatus.PENDING, viewModel.uiState.value.documents[0].syncStatus)

        // Update flow with synced status
        val updatedDraft = sampleInvoiceDraft.copy(syncStatus = SyncStatus.SYNCED)
        docRepo.docsFlow.value = listOf(updatedDraft, samplePoFinalized)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.documents.size)
        val syncedItem = viewModel.uiState.value.documents.find { it.id == "doc-inv-1" }
        assertEquals(SyncStatus.SYNCED, syncedItem?.syncStatus)
    }

    private class FakeDocumentRepo(initialDocs: List<BusinessDocument>) : DocumentRepository {
        val docsFlow = MutableStateFlow(initialDocs)
        override fun observeAllDocuments(): Flow<List<BusinessDocument>> = docsFlow
        override fun observeDocumentById(id: String): Flow<BusinessDocument?> = MutableStateFlow(docsFlow.value.firstOrNull { it.id == id })
        override fun observeDocumentsByType(type: DocumentType): Flow<List<BusinessDocument>> = MutableStateFlow(docsFlow.value.filter { it.documentType == type })
        override fun observeDocumentsByClient(clientId: String): Flow<List<BusinessDocument>> = MutableStateFlow(docsFlow.value.filter { it.clientId == clientId })
        override suspend fun getDocumentById(id: String): BusinessDocument? = docsFlow.value.firstOrNull { it.id == id }
        override suspend fun getLineItemsForDocument(documentId: String): List<DocumentLineItem> = emptyList()
        override suspend fun suggestDocumentNumber(type: DocumentType, documentDate: Long): String = "VE/01/2026-27"
        override suspend fun createDraft(type: DocumentType, clientId: String, documentDate: Long, documentNumber: String?, lineItems: List<DocumentLineItem>, placeOfSupply: String?, deliveryFactoryAddress: String?, paymentTerms: String?, deliveryNote: String?, supplierReference: String?, otherReferences: String?, buyerOrderNumber: String?, buyerOrderDate: Long?, dispatchDocumentNumber: String?, deliveryNoteDate: Long?, dispatchThrough: String?, destination: String?, termsOfDelivery: String?): Result<BusinessDocument> = TODO()
        override suspend fun updateDraft(document: BusinessDocument, lineItems: List<DocumentLineItem>): Result<BusinessDocument> = TODO()
        override suspend fun finalizeDocument(documentId: String, overrideDocumentNumber: String?): DocumentFinalizationResult = TODO()
        override suspend fun cancelDocument(documentId: String): Result<Unit> = TODO()
    }

    private class ErrorDocumentRepo : DocumentRepository {
        var shouldFail = true
        override fun observeAllDocuments(): Flow<List<BusinessDocument>> = flow {
            if (shouldFail) throw RuntimeException("DB Connection Error")
            else emit(listOf(BusinessDocument(id = "doc-1", documentType = DocumentType.TAX_INVOICE, documentNumber = "VE/01", documentDate = 100L, status = DocumentStatus.DRAFT, clientId = "c1", createdAt = 100L, updatedAt = 100L)))
        }
        override fun observeDocumentById(id: String): Flow<BusinessDocument?> = TODO()
        override fun observeDocumentsByType(type: DocumentType): Flow<List<BusinessDocument>> = TODO()
        override fun observeDocumentsByClient(clientId: String): Flow<List<BusinessDocument>> = TODO()
        override suspend fun getDocumentById(id: String): BusinessDocument? = TODO()
        override suspend fun getLineItemsForDocument(documentId: String): List<DocumentLineItem> = TODO()
        override suspend fun suggestDocumentNumber(type: DocumentType, documentDate: Long): String = TODO()
        override suspend fun createDraft(type: DocumentType, clientId: String, documentDate: Long, documentNumber: String?, lineItems: List<DocumentLineItem>, placeOfSupply: String?, deliveryFactoryAddress: String?, paymentTerms: String?, deliveryNote: String?, supplierReference: String?, otherReferences: String?, buyerOrderNumber: String?, buyerOrderDate: Long?, dispatchDocumentNumber: String?, deliveryNoteDate: Long?, dispatchThrough: String?, destination: String?, termsOfDelivery: String?): Result<BusinessDocument> = TODO()
        override suspend fun updateDraft(document: BusinessDocument, lineItems: List<DocumentLineItem>): Result<BusinessDocument> = TODO()
        override suspend fun finalizeDocument(documentId: String, overrideDocumentNumber: String?): DocumentFinalizationResult = TODO()
        override suspend fun cancelDocument(documentId: String): Result<Unit> = TODO()
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
