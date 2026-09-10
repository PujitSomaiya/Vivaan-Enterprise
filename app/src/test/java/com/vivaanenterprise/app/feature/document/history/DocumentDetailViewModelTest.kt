package com.vivaanenterprise.app.feature.document.history

import androidx.lifecycle.SavedStateHandle
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.model.ClientSnapshot
import com.vivaanenterprise.app.domain.model.DocumentFinalizationResult
import com.vivaanenterprise.app.domain.model.DocumentLineItem
import com.vivaanenterprise.app.domain.model.SellerSnapshot
import com.vivaanenterprise.app.domain.model.TaxTreatment
import com.vivaanenterprise.app.domain.repository.ClientRepository
import com.vivaanenterprise.app.domain.repository.DocumentRepository
import com.vivaanenterprise.app.feature.document.history.detail.DocumentDetailViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleDraftInvoice = BusinessDocument(
        id = "doc-draft-1",
        documentType = DocumentType.TAX_INVOICE,
        documentNumber = "VE/01/2026-27",
        documentDate = 1700000000000L,
        status = DocumentStatus.DRAFT,
        clientId = "client-1",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L
    )

    private val sampleDraftPo = BusinessDocument(
        id = "doc-draft-po",
        documentType = DocumentType.PURCHASE_ORDER,
        documentNumber = "VE/PO/01/2026-27",
        documentDate = 1700000000000L,
        status = DocumentStatus.DRAFT,
        clientId = "client-1",
        deliveryFactoryAddress = "Factory Gate 2, GIDC Industrial Estate",
        deliveryNote = "Standard delivery",
        destination = "Surat Depot",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L
    )

    private val sampleFinalizedInvoice = BusinessDocument(
        id = "doc-fin-inv",
        documentType = DocumentType.TAX_INVOICE,
        documentNumber = "VE/10/2026-27",
        documentDate = 1700000000000L,
        status = DocumentStatus.FINALIZED,
        clientId = "client-1",
        sellerSnapshot = SellerSnapshot(
            businessName = "Vivaan Enterprise",
            addressLine1 = "101 Trade Center",
            addressLine2 = "Ring Road",
            cityStatePincode = "Surat, Gujarat - 395002",
            gstin = "24AAAAC1234A1Z5",
            mobile = "9876543210",
            pan = "AAAAC1234A",
            bankAccountName = "Vivaan Enterprise",
            bankName = "HDFC Bank",
            bankAccountNumber = "50200012345678",
            bankIfsc = "HDFC0001234",
            bankBranch = "Main Branch",
            declaration = "Goods sold are non-refundable",
            authorisedSignatory = "Pujit Somaiya"
        ),
        clientSnapshot = ClientSnapshot(
            clientId = "client-1",
            companyName = "Historical Client Name Pvt Ltd",
            gstin = "24CHWPG0910J1ZB",
            address = "456 Commerce Tower"
        ),
        lineItems = listOf(
            DocumentLineItem(
                id = "item-1",
                documentId = "doc-fin-inv",
                position = 0,
                descriptionSnapshot = "Industrial Raw Material A",
                hsnSacSnapshot = "2902",
                quantity = 100,
                ratePaise = 250000, // ₹ 2,500.00
                gstRateBasisPoints = 1800, // 18%
                taxableAmountPaise = 25000000,
                cgstAmountPaise = 2250000,
                sgstAmountPaise = 2250000,
                igstAmountPaise = 0,
                totalTaxPaise = 4500000,
                lineTotalPaise = 29500000,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L
            )
        ),
        taxTreatment = TaxTreatment.INTRA_STATE,
        taxableAmountPaise = 25000000,
        cgstAmountPaise = 2250000,
        sgstAmountPaise = 2250000,
        igstAmountPaise = 0,
        totalTaxAmountPaise = 4500000,
        grandTotalPaise = 29500000,
        amountInWords = "RUPEES TWO LAKH NINETY FIVE THOUSAND ONLY",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L,
        finalizedAt = 1700000000000L,
        syncStatus = SyncStatus.SYNCED
    )

    private val sampleCancelledDoc = BusinessDocument(
        id = "doc-cancelled",
        documentType = DocumentType.TAX_INVOICE,
        documentNumber = "VE/99/2026-27",
        documentDate = 1700000000000L,
        status = DocumentStatus.CANCELLED,
        clientId = "client-1",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L
    )

    private val sampleClient = Client(
        id = "client-1",
        companyName = "Renamed Current Client Master Name",
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
    fun load_validDraftInvoice_loadsCorrectly() = runTest {
        val docRepo = FakeDocumentRepo(listOf(sampleDraftInvoice))
        val clientRepo = FakeClientRepo(listOf(sampleClient))

        val savedStateHandle = SavedStateHandle(mapOf("documentId" to "doc-draft-1"))
        val viewModel = DocumentDetailViewModel(savedStateHandle, docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNotNull(state.document)
        assertEquals("doc-draft-1", state.document?.id)
        assertEquals(DocumentStatus.DRAFT, state.document?.status)
        assertEquals("Renamed Current Client Master Name", state.clientName)
    }

    @Test
    fun load_validDraftPo_loadsCorrectlyWithIndependentAddressFields() = runTest {
        val docRepo = FakeDocumentRepo(listOf(sampleDraftPo))
        val clientRepo = FakeClientRepo(listOf(sampleClient))

        val savedStateHandle = SavedStateHandle(mapOf("documentId" to "doc-draft-po"))
        val viewModel = DocumentDetailViewModel(savedStateHandle, docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.document)
        assertEquals("Factory Gate 2, GIDC Industrial Estate", state.document?.deliveryFactoryAddress)
        assertEquals("Standard delivery", state.document?.deliveryNote)
        assertEquals("Surat Depot", state.document?.destination)
    }

    @Test
    fun load_finalizedInvoice_loadsHistoricalSnapshotsWithoutMasterSubstitution() = runTest {
        val docRepo = FakeDocumentRepo(listOf(sampleFinalizedInvoice))
        val clientRepo = FakeClientRepo(listOf(sampleClient))

        val savedStateHandle = SavedStateHandle(mapOf("documentId" to "doc-fin-inv"))
        val viewModel = DocumentDetailViewModel(savedStateHandle, docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.document)
        assertEquals(DocumentStatus.FINALIZED, state.document?.status)

        // Historical client name must NOT be replaced by current master!
        assertEquals("Historical Client Name Pvt Ltd", state.clientName)
        assertEquals("Vivaan Enterprise", state.document?.sellerSnapshot?.businessName)
        assertEquals(TaxTreatment.INTRA_STATE, state.document?.taxTreatment)
        assertEquals(29500000L, state.document?.grandTotalPaise)
    }

    @Test
    fun load_missingDocument_returnsErrorState() = runTest {
        val docRepo = FakeDocumentRepo(emptyList())
        val clientRepo = FakeClientRepo(emptyList())

        val savedStateHandle = SavedStateHandle(mapOf("documentId" to "non-existent"))
        val viewModel = DocumentDetailViewModel(savedStateHandle, docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.document)
        assertEquals("Document not found", state.errorMessage)
    }

    @Test
    fun cancelledDocument_isLoadedAsReadOnly() = runTest {
        val docRepo = FakeDocumentRepo(listOf(sampleCancelledDoc))
        val clientRepo = FakeClientRepo(listOf(sampleClient))

        val savedStateHandle = SavedStateHandle(mapOf("documentId" to "doc-cancelled"))
        val viewModel = DocumentDetailViewModel(savedStateHandle, docRepo, clientRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.document)
        assertEquals(DocumentStatus.CANCELLED, state.document?.status)
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
