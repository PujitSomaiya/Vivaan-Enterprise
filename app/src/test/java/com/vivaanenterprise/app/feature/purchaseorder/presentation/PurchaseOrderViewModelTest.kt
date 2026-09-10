package com.vivaanenterprise.app.feature.purchaseorder.presentation

import androidx.lifecycle.SavedStateHandle
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.model.BusinessProfile
import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.model.DocumentFinalizationResult
import com.vivaanenterprise.app.domain.model.DocumentLineItem
import com.vivaanenterprise.app.domain.model.DocumentValidationError
import com.vivaanenterprise.app.domain.model.Product
import com.vivaanenterprise.app.domain.repository.BusinessProfileRepository
import com.vivaanenterprise.app.domain.repository.ClientRepository
import com.vivaanenterprise.app.domain.repository.DocumentRepository
import com.vivaanenterprise.app.domain.repository.ProductRepository
import com.vivaanenterprise.app.domain.util.DocumentCalculator
import com.vivaanenterprise.app.domain.util.IndianCurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseOrderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeClientRepository: FakeClientRepository
    private lateinit var fakeProductRepository: FakeProductRepository
    private lateinit var fakeDocumentRepository: FakeDocumentRepository
    private lateinit var fakeProfileRepository: FakeBusinessProfileRepository
    private lateinit var calculator: DocumentCalculator
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: PurchaseOrderViewModel

    private val testClient = Client(
        id = "client-1",
        companyName = "Acme Corp",
        address = "Ahmedabad, Gujarat",
        gstin = "24AAAAC1234A1Z1",
        state = "Gujarat",
        stateCode = "24",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private val testProduct = Product(
        id = "prod-1",
        name = "3M anti-slip tape",
        hsnSac = "3919",
        defaultGstRateBasisPoints = 1800,
        isActive = true,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private val testProfile = BusinessProfile(
        id = "profile-1",
        businessName = "VIVAAN ENTERPRISE",
        addressLine1 = "Street 4",
        addressLine2 = "Jorawar Nagar",
        cityStatePincode = "Surendranagar",
        gstin = "24CHWPG0910J1ZB",
        mobile = "9737178061",
        pan = "CHWPG0910J",
        bankAccountName = "SHETH JANVI",
        bankName = "HDFC BANK",
        bankAccountNumber = "50100419622062",
        bankIfsc = "HDFC0000299",
        bankBranch = "Paldi",
        declaration = "Declaration",
        authorisedSignatory = "Signatory",
        state = "Gujarat",
        stateCode = "24",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeClientRepository = FakeClientRepository(listOf(testClient))
        fakeProductRepository = FakeProductRepository(listOf(testProduct))
        fakeDocumentRepository = FakeDocumentRepository()
        fakeProfileRepository = FakeBusinessProfileRepository(testProfile)
        calculator = DocumentCalculator(IndianCurrencyFormatter())
        savedStateHandle = SavedStateHandle()

        viewModel = PurchaseOrderViewModel(
            clientRepository = fakeClientRepository,
            productRepository = fakeProductRepository,
            documentRepository = fakeDocumentRepository,
            profileRepository = fakeProfileRepository,
            calculator = calculator,
            savedStateHandle = savedStateHandle
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test1_requestsPurchaseOrderSuggestedNumber() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        assertEquals(DocumentType.PURCHASE_ORDER, fakeDocumentRepository.lastSuggestedType)
        assertEquals("VE/01/2023-24", viewModel.uiState.value.documentNumber)
    }

    @Test
    fun test2_clientSelectionDefaultsDeliveryAddress() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(PurchaseOrderUiIntent.OnSelectClient(testClient))
        testScheduler.advanceUntilIdle()

        assertEquals("Ahmedabad, Gujarat", viewModel.uiState.value.deliveryFactoryAddress)
    }

    @Test
    fun test3_manualDeliveryAddressSurvivesClientChange() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(PurchaseOrderUiIntent.OnSelectClient(testClient))
        viewModel.onIntent(PurchaseOrderUiIntent.OnDeliveryFactoryAddressChange("Custom Factory Address"))

        val clientB = testClient.copy(id = "client-2", companyName = "Beta Ltd", address = "Baroda")
        viewModel.onIntent(PurchaseOrderUiIntent.OnSelectClient(clientB))

        assertEquals("Custom Factory Address", viewModel.uiState.value.deliveryFactoryAddress)
    }

    @Test
    fun test4_blankDeliveryAddressBlocksFinalization() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(PurchaseOrderUiIntent.OnSelectClient(testClient))
        viewModel.onIntent(PurchaseOrderUiIntent.OnDeliveryFactoryAddressChange(""))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(PurchaseOrderUiIntent.OnRateChange(lineId, "1000"))

        viewModel.onIntent(PurchaseOrderUiIntent.OnRequestFinalize)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showFinalizeConfirmDialog)
        assertEquals("Delivery / Factory address is required", viewModel.uiState.value.deliveryFactoryAddressError)
    }

    @Test
    fun test5_saveDraftCallsCreateDraftWithPurchaseOrderType() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(PurchaseOrderUiIntent.OnSelectClient(testClient))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(PurchaseOrderUiIntent.OnRateChange(lineId, "3500"))

        viewModel.onIntent(PurchaseOrderUiIntent.OnSaveDraft)
        testScheduler.advanceUntilIdle()

        assertEquals(1, fakeDocumentRepository.createDraftCalls)
        assertEquals(DocumentType.PURCHASE_ORDER, fakeDocumentRepository.lastCreatedDraftType)
        assertEquals(PurchaseOrderMode.EDIT_DRAFT, viewModel.uiState.value.mode)
        assertFalse(viewModel.uiState.value.isDirty)
    }

    @Test
    fun test8_manuallyEditedNumberSurvivesDateChange() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(PurchaseOrderUiIntent.OnDocumentNumberChange("CUSTOM-PO-99"))
        assertTrue(viewModel.uiState.value.isDocumentNumberManuallyEdited)

        viewModel.onIntent(PurchaseOrderUiIntent.OnDocumentDateChange(1800000000000L))
        testScheduler.advanceUntilIdle()

        assertEquals("CUSTOM-PO-99", viewModel.uiState.value.documentNumber)
    }

    @Test
    fun test9_untouchedAutoNumberRefreshesOnDateChange() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isDocumentNumberManuallyEdited)

        viewModel.onIntent(PurchaseOrderUiIntent.OnDocumentDateChange(1800000000000L))
        testScheduler.advanceUntilIdle()

        assertEquals("VE/01/2023-24", viewModel.uiState.value.documentNumber)
    }

    @Test
    fun test10_threeFieldsPersistIndependently() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(PurchaseOrderUiIntent.OnSelectClient(testClient))
        viewModel.onIntent(PurchaseOrderUiIntent.OnDeliveryFactoryAddressChange("Factory Addr 1"))
        viewModel.onIntent(PurchaseOrderUiIntent.OnMetadataChange(deliveryNote = "Del Note 2", destination = "Dest 3"))

        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(PurchaseOrderUiIntent.OnRateChange(lineId, "1000"))

        viewModel.onIntent(PurchaseOrderUiIntent.OnSaveDraft)
        testScheduler.advanceUntilIdle()

        val savedDoc = fakeDocumentRepository.documents.values.first()
        assertEquals("Factory Addr 1", savedDoc.deliveryFactoryAddress)
        assertEquals("Del Note 2", savedDoc.deliveryNote)
        assertEquals("Dest 3", savedDoc.destination)
    }

    @Test
    fun test11_blankPlaceOfSupplyBlocksFinalization() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(PurchaseOrderUiIntent.OnSelectClient(testClient.copy(stateCode = null)))
        viewModel.onIntent(PurchaseOrderUiIntent.OnDeliveryFactoryAddressChange("Address OK"))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(PurchaseOrderUiIntent.OnRateChange(lineId, "1000"))

        viewModel.onIntent(PurchaseOrderUiIntent.OnRequestFinalize)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showFinalizeConfirmDialog)
        assertEquals("Valid 2-digit place of supply state code required", viewModel.uiState.value.placeOfSupplyError)
    }

    @Test
    fun test12_subsequentEditAfterSaveResetsDirtyToTrue() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(PurchaseOrderUiIntent.OnSelectClient(testClient))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(PurchaseOrderUiIntent.OnRateChange(lineId, "1000"))

        viewModel.onIntent(PurchaseOrderUiIntent.OnSaveDraft)
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isDirty)

        viewModel.onIntent(PurchaseOrderUiIntent.OnQuantityChange(lineId, "10"))
        assertTrue(viewModel.uiState.value.isDirty)
    }

    @Test
    fun test13_duplicateDocumentNumberErrorDisplayedOnFinalize() = runTest(testDispatcher) {
        fakeDocumentRepository.finalizeResultOverride = DocumentFinalizationResult.Invalid(
            listOf(DocumentValidationError.DuplicateDocumentNumber)
        )
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(PurchaseOrderUiIntent.OnSelectClient(testClient))
        viewModel.onIntent(PurchaseOrderUiIntent.OnPlaceOfSupplyChange("24"))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(PurchaseOrderUiIntent.OnRateChange(lineId, "1000"))

        viewModel.onIntent(PurchaseOrderUiIntent.OnConfirmFinalize)
        testScheduler.advanceUntilIdle()

        assertEquals("A purchase order with this number already exists.", viewModel.uiState.value.documentNumberError)
        assertNull(viewModel.uiState.value.generalError)
    }

    @Test
    fun test15_successfulFinalizeEmitsNavigateSuccessDirectlyWithoutSnackbar() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(PurchaseOrderUiIntent.OnSelectClient(testClient))
        viewModel.onIntent(PurchaseOrderUiIntent.OnDeliveryFactoryAddressChange("Factory Address 1"))
        viewModel.onIntent(PurchaseOrderUiIntent.OnPlaceOfSupplyChange("24"))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(PurchaseOrderUiIntent.OnSelectProduct(lineId, testProduct))
        viewModel.onIntent(PurchaseOrderUiIntent.OnRateChange(lineId, "100"))

        val effects = mutableListOf<PurchaseOrderUiEffect>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(PurchaseOrderUiIntent.OnConfirmFinalize)
        testScheduler.advanceUntilIdle()

        assertEquals(1, effects.size)
        assertTrue(effects.first() is PurchaseOrderUiEffect.NavigateSuccess)
        assertEquals("po-doc-1", (effects.first() as PurchaseOrderUiEffect.NavigateSuccess).documentId)
        job.cancel()
    }

    @Test
    fun test16_dirtyExistingDraftMetadataPreservedOnDirectFinalize() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(PurchaseOrderUiIntent.OnSelectClient(testClient))
        viewModel.onIntent(PurchaseOrderUiIntent.OnDeliveryFactoryAddressChange("Factory Address Special"))
        viewModel.onIntent(PurchaseOrderUiIntent.OnPlaceOfSupplyChange("24"))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(PurchaseOrderUiIntent.OnSelectProduct(lineId, testProduct))
        viewModel.onIntent(PurchaseOrderUiIntent.OnRateChange(lineId, "100"))

        viewModel.onIntent(PurchaseOrderUiIntent.OnMetadataChange(
            deliveryNote = "PO Delivery Note",
            destination = "Surat Plant"
        ))

        viewModel.onIntent(PurchaseOrderUiIntent.OnConfirmFinalize)
        testScheduler.advanceUntilIdle()

        val savedDoc = fakeDocumentRepository.documents["po-doc-1"]
        assertNotNull(savedDoc)
        assertEquals("Factory Address Special", savedDoc?.deliveryFactoryAddress)
        assertEquals("PO Delivery Note", savedDoc?.deliveryNote)
        assertEquals("Surat Plant", savedDoc?.destination)
    }

    @Test
    fun test14_finalizedDraftIdCannotBeEdited() = runTest(testDispatcher) {
        val finalizedDoc = BusinessDocument(
            id = "po-finalized-1",
            documentType = DocumentType.PURCHASE_ORDER,
            documentNumber = "PO-FIN-1",
            documentDate = 1000L,
            status = DocumentStatus.FINALIZED,
            clientId = "client-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        fakeDocumentRepository.documents["po-finalized-1"] = finalizedDoc

        val editVm = PurchaseOrderViewModel(
            clientRepository = fakeClientRepository,
            productRepository = fakeProductRepository,
            documentRepository = fakeDocumentRepository,
            profileRepository = fakeProfileRepository,
            calculator = calculator,
            savedStateHandle = SavedStateHandle(mapOf("documentId" to "po-finalized-1"))
        )
        testScheduler.advanceUntilIdle()

        assertEquals("Draft purchase order not found or not editable", editVm.uiState.value.generalError)
    }
}

// ── FAKES ─────────────────────────────────────────────────────────────

private class FakeClientRepository(private val clients: List<Client>) : ClientRepository {
    private val flow = MutableStateFlow(clients)
    override fun observeClients(): Flow<List<Client>> = flow
    override fun observeClientById(id: String): Flow<Client?> = MutableStateFlow(clients.find { it.id == id })
    override suspend fun getClientById(id: String): Client? = clients.find { it.id == id }
    override suspend fun createClient(client: Client): Result<Unit> = Result.success(Unit)
    override suspend fun updateClient(client: Client): Result<Unit> = Result.success(Unit)
    override suspend fun deleteClient(id: String): Result<Unit> = Result.success(Unit)
}

private class FakeProductRepository(private val products: List<Product>) : ProductRepository {
    private val flow = MutableStateFlow(products)
    override fun observeProducts(): Flow<List<Product>> = flow
    override fun observeActiveProducts(): Flow<List<Product>> = MutableStateFlow(products.filter { it.isActive })
    override fun observeProductById(id: String): Flow<Product?> = MutableStateFlow(products.find { it.id == id })
    override suspend fun getProductById(id: String): Product? = products.find { it.id == id }
    override suspend fun createProduct(product: Product): Result<Unit> = Result.success(Unit)
    override suspend fun updateProduct(product: Product): Result<Unit> = Result.success(Unit)
    override suspend fun setProductActive(id: String, isActive: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun deleteProduct(id: String): Result<Unit> = Result.success(Unit)
}

private class FakeBusinessProfileRepository(private var profile: BusinessProfile?) : BusinessProfileRepository {
    override fun observeProfile(): Flow<BusinessProfile?> = MutableStateFlow(profile)
    override suspend fun getProfile(): BusinessProfile? = profile
}

private class FakeDocumentRepository : DocumentRepository {
    var lastSuggestedType: DocumentType? = null
    var createDraftCalls = 0
    var lastCreatedDraftType: DocumentType? = null
    val documents = mutableMapOf<String, BusinessDocument>()

    override fun observeAllDocuments(): Flow<List<BusinessDocument>> = MutableStateFlow(documents.values.toList())
    override fun observeDocumentById(id: String): Flow<BusinessDocument?> = MutableStateFlow(documents[id])
    override fun observeDocumentsByType(type: DocumentType): Flow<List<BusinessDocument>> = MutableStateFlow(documents.values.filter { it.documentType == type })
    override fun observeDocumentsByClient(clientId: String): Flow<List<BusinessDocument>> = MutableStateFlow(documents.values.filter { it.clientId == clientId })
    override suspend fun getDocumentById(id: String): BusinessDocument? = documents[id]
    override suspend fun getLineItemsForDocument(documentId: String): List<DocumentLineItem> = emptyList()

    override suspend fun suggestDocumentNumber(type: DocumentType, documentDate: Long): String {
        lastSuggestedType = type
        return "VE/01/2023-24"
    }

    override suspend fun createDraft(
        type: DocumentType,
        clientId: String,
        documentDate: Long,
        documentNumber: String?,
        lineItems: List<DocumentLineItem>,
        placeOfSupply: String?,
        deliveryFactoryAddress: String?,
        paymentTerms: String?,
        deliveryNote: String?,
        supplierReference: String?,
        otherReferences: String?,
        buyerOrderNumber: String?,
        buyerOrderDate: Long?,
        dispatchDocumentNumber: String?,
        deliveryNoteDate: Long?,
        dispatchThrough: String?,
        destination: String?,
        termsOfDelivery: String?
    ): Result<BusinessDocument> {
        createDraftCalls++
        lastCreatedDraftType = type
        val doc = BusinessDocument(
            id = "po-doc-1",
            documentType = type,
            documentNumber = documentNumber ?: "VE/01/2023-24",
            documentDate = documentDate,
            status = DocumentStatus.DRAFT,
            clientId = clientId,
            lineItems = lineItems,
            placeOfSupply = placeOfSupply,
            deliveryFactoryAddress = deliveryFactoryAddress,
            deliveryNote = deliveryNote,
            destination = destination,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        documents[doc.id] = doc
        return Result.success(doc)
    }

    override suspend fun updateDraft(document: BusinessDocument, lineItems: List<DocumentLineItem>): Result<BusinessDocument> {
        val updated = document.copy(lineItems = lineItems)
        documents[updated.id] = updated
        return Result.success(updated)
    }

    var finalizeResultOverride: DocumentFinalizationResult? = null

    override suspend fun finalizeDocument(documentId: String, overrideDocumentNumber: String?): DocumentFinalizationResult {
        finalizeResultOverride?.let { return it }
        val existing = documents[documentId] ?: return DocumentFinalizationResult.Failure(Exception("Not found"))
        val finalized = existing.copy(status = DocumentStatus.FINALIZED)
        documents[documentId] = finalized
        return DocumentFinalizationResult.Success(finalized)
    }

    override suspend fun cancelDocument(documentId: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteDocument(documentId: String): Result<Unit> = Result.success(Unit)
}
