package com.vivaanenterprise.app.feature.invoice.presentation

import androidx.lifecycle.SavedStateHandle
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus
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
class InvoiceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeClientRepository: FakeClientRepository
    private lateinit var fakeProductRepository: FakeProductRepository
    private lateinit var fakeDocumentRepository: FakeDocumentRepository
    private lateinit var fakeProfileRepository: FakeBusinessProfileRepository
    private lateinit var calculator: DocumentCalculator
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: InvoiceViewModel

    private val testClient = Client(
        id = "client-1",
        companyName = "Acme Corp",
        address = "Ahmedabad",
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

        viewModel = InvoiceViewModel(
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

    // ── INITIALIZATION ──────────────────────────────────────────────────

    @Test
    fun test1_newInvoiceRequestsSuggestedNumber() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        assertEquals("VE/01/2023-24", viewModel.uiState.value.documentNumber)
    }

    @Test
    fun test2_clientsObserved() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.availableClients.size)
        assertEquals("client-1", viewModel.uiState.value.availableClients.first().id)
    }

    @Test
    fun test3_activeProductsObserved() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.availableProducts.size)
        assertEquals("prod-1", viewModel.uiState.value.availableProducts.first().id)
    }

    @Test
    fun test4_businessProfileStateCodeObservedThroughRepositoryBoundary() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(viewModel.uiState.value.lineItems.first().id, "100"))
        testScheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.calculationPreview)
    }

    // ── NUMBER / DATE ────────────────────────────────────────────────────

    @Test
    fun test5_untouchedSuggestedNumberUpdatesWhenDateChanges() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnDocumentDateChange(1700000000000L))
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isDocumentNumberManuallyEdited)
    }

    @Test
    fun test6_manuallyEditedNumberSurvivesDateChange() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnDocumentNumberChange("CUSTOM-123"))
        assertTrue(viewModel.uiState.value.isDocumentNumberManuallyEdited)
        viewModel.onIntent(InvoiceUiIntent.OnDocumentDateChange(1800000000000L))
        assertEquals("CUSTOM-123", viewModel.uiState.value.documentNumber)
    }

    @Test
    fun test7_editDraftPreservesPersistedNumber() = runTest(testDispatcher) {
        val existingDraft = BusinessDocument(
            id = "draft-99",
            documentType = DocumentType.TAX_INVOICE,
            documentNumber = "PERSISTED-777",
            documentDate = 1000L,
            status = DocumentStatus.DRAFT,
            clientId = "client-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        fakeDocumentRepository.storedDocs["draft-99"] = existingDraft

        val editSavedStateHandle = SavedStateHandle(mapOf("documentId" to "draft-99"))
        val editVm = InvoiceViewModel(
            clientRepository = fakeClientRepository,
            productRepository = fakeProductRepository,
            documentRepository = fakeDocumentRepository,
            profileRepository = fakeProfileRepository,
            calculator = calculator,
            savedStateHandle = editSavedStateHandle
        )
        testScheduler.advanceUntilIdle()

        assertEquals("PERSISTED-777", editVm.uiState.value.documentNumber)
        assertTrue(editVm.uiState.value.isDocumentNumberManuallyEdited)
    }

    // ── CLIENT / PLACE OF SUPPLY ─────────────────────────────────────────

    @Test
    fun test8_selectingClientDefaultsValidStateCode() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        assertEquals("24", viewModel.uiState.value.placeOfSupplyStateCode)
    }

    @Test
    fun test9_manualPlaceOfSupplySurvivesSubsequentClientChange() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnPlaceOfSupplyChange("33"))
        assertTrue(viewModel.uiState.value.isPlaceOfSupplyManuallyEdited)

        val otherClient = testClient.copy(id = "client-2", stateCode = "27")
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(otherClient))
        assertEquals("33", viewModel.uiState.value.placeOfSupplyStateCode)
    }

    @Test
    fun test10_nonManualDefaultUpdatesOnClientChange() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        val mhClient = testClient.copy(id = "client-2", stateCode = "27")
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(mhClient))
        assertEquals("27", viewModel.uiState.value.placeOfSupplyStateCode)
    }

    @Test
    fun test11_savedDraftPlaceOfSupplyRestored() = runTest(testDispatcher) {
        val draft = BusinessDocument(
            id = "draft-1",
            documentType = DocumentType.TAX_INVOICE,
            documentNumber = "VE/01/2023-24",
            documentDate = 1000L,
            status = DocumentStatus.DRAFT,
            clientId = "client-1",
            placeOfSupply = "33",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        fakeDocumentRepository.storedDocs["draft-1"] = draft
        val editVm = InvoiceViewModel(
            clientRepository = fakeClientRepository,
            productRepository = fakeProductRepository,
            documentRepository = fakeDocumentRepository,
            profileRepository = fakeProfileRepository,
            calculator = calculator,
            savedStateHandle = SavedStateHandle(mapOf("documentId" to "draft-1"))
        )
        testScheduler.advanceUntilIdle()
        assertEquals("33", editVm.uiState.value.placeOfSupplyStateCode)
    }

    // ── LINES ────────────────────────────────────────────────────────────

    @Test
    fun test12_initialLineHasStableId() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.lineItems.first().id)
    }

    @Test
    fun test13_productSelectionUpdatesLine() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(lineId, testProduct))
        assertEquals("prod-1", viewModel.uiState.value.lineItems.first().selectedProduct?.id)
    }

    @Test
    fun test14_quantityChangeUpdatesState() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "10"))
        assertEquals("10", viewModel.uiState.value.lineItems.first().quantityInput)
    }

    @Test
    fun test15_rateChangeUpdatesState() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(lineId, "2500"))
        assertEquals("2500", viewModel.uiState.value.lineItems.first().rateInput)
    }

    @Test
    fun test16_addLineCreatesDistinctStableId() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnAddLineItem)
        assertEquals(2, viewModel.uiState.value.lineItems.size)
        assertTrue(viewModel.uiState.value.lineItems[0].id != viewModel.uiState.value.lineItems[1].id)
    }

    @Test
    fun test17_removeLineBehavesCorrectly() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnAddLineItem)
        val firstId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnRemoveLineItem(firstId))
        assertEquals(1, viewModel.uiState.value.lineItems.size)
    }

    @Test
    fun test18_atLeastOneEditableLineRemains() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnRemoveLineItem(lineId))
        assertEquals(1, viewModel.uiState.value.lineItems.size)
    }

    // ── CALCULATION ──────────────────────────────────────────────────────

    @Test
    fun test19_validInputUsesDocumentCalculator() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(lineId, testProduct))
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "5"))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(lineId, "3500"))
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.calculationPreview)
        assertEquals(2065000L, viewModel.uiState.value.calculationPreview?.grandTotalPaise)
    }

    @Test
    fun test20_incompleteRateDoesNotDisplayFakeZeroPreview() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(lineId, testProduct))
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "1"))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(lineId, "3500.5.5"))
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.calculationPreview)
    }

    @Test
    fun test21_invalidQuantityDoesNotDisplayFakeCalculation() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "0"))
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.calculationPreview)
    }

    @Test
    fun test22_sellerStateMissingDoesNotCalculate() = runTest(testDispatcher) {
        fakeProfileRepository = FakeBusinessProfileRepository(testProfile.copy(stateCode = null))
        val noStateVm = InvoiceViewModel(
            clientRepository = fakeClientRepository,
            productRepository = fakeProductRepository,
            documentRepository = fakeDocumentRepository,
            profileRepository = fakeProfileRepository,
            calculator = calculator,
            savedStateHandle = savedStateHandle
        )
        testScheduler.advanceUntilIdle()

        noStateVm.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        val lineId = noStateVm.uiState.value.lineItems.first().id
        noStateVm.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "5"))
        noStateVm.onIntent(InvoiceUiIntent.OnRateChange(lineId, "3500"))
        testScheduler.advanceUntilIdle()

        assertNull(noStateVm.uiState.value.calculationPreview)
    }

    @Test
    fun test23_mixedMultipleLinesPreviewHandledCorrectly() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        val line1Id = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(line1Id, testProduct))
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(line1Id, "1"))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(line1Id, "1000"))

        viewModel.onIntent(InvoiceUiIntent.OnAddLineItem)
        val line2Id = viewModel.uiState.value.lineItems[1].id
        val prod2 = testProduct.copy(id = "prod-2", defaultGstRateBasisPoints = 500)
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(line2Id, prod2))
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(line2Id, "2"))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(line2Id, "500"))
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.calculationPreview)
    }

    // ── SAVE DRAFT ───────────────────────────────────────────────────────

    @Test
    fun test24_firstSaveCallsCreateDraftOnce() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(lineId, testProduct))
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "1"))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(lineId, "100"))

        viewModel.onIntent(InvoiceUiIntent.OnSaveDraft)
        testScheduler.advanceUntilIdle()

        assertEquals(1, fakeDocumentRepository.createDraftCalls)
    }

    @Test
    fun test26_successfulFirstSaveCapturesDocumentId() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(lineId, testProduct))
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "1"))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(lineId, "100"))

        viewModel.onIntent(InvoiceUiIntent.OnSaveDraft)
        testScheduler.advanceUntilIdle()

        assertEquals("doc-1", viewModel.uiState.value.documentId)
        assertEquals(InvoiceMode.EDIT_DRAFT, viewModel.uiState.value.mode)
    }

    @Test
    fun test27_subsequentSaveUpdatesSameDraft() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(lineId, testProduct))
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "1"))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(lineId, "100"))

        viewModel.onIntent(InvoiceUiIntent.OnSaveDraft)
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(InvoiceUiIntent.OnDocumentNumberChange("UPDATED-NUM"))
        viewModel.onIntent(InvoiceUiIntent.OnSaveDraft)
        testScheduler.advanceUntilIdle()

        assertEquals(1, fakeDocumentRepository.createDraftCalls)
        assertEquals(1, fakeDocumentRepository.updateDraftCalls)
    }

    @Test
    fun test28_successfulSaveClearsDirtyBaseline() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        assertTrue(viewModel.uiState.value.isDirty)

        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(lineId, testProduct))
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "1"))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(lineId, "100"))

        viewModel.onIntent(InvoiceUiIntent.OnSaveDraft)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDirty)
    }

    // ── FINALIZATION ─────────────────────────────────────────────────────

    @Test
    fun test35_invalidFormCannotRequestFinalize() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnRequestFinalize)
        assertFalse(viewModel.uiState.value.showFinalizeConfirmDialog)
    }

    @Test
    fun test36_requestFinalizeShowsConfirmationDialog() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(lineId, testProduct))
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "1"))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(lineId, "100"))

        viewModel.onIntent(InvoiceUiIntent.OnRequestFinalize)
        assertTrue(viewModel.uiState.value.showFinalizeConfirmDialog)
    }

    @Test
    fun test37_confirmFinalizeInvokesRepositoryOnce() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(lineId, testProduct))
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "1"))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(lineId, "100"))

        viewModel.onIntent(InvoiceUiIntent.OnConfirmFinalize)
        testScheduler.advanceUntilIdle()

        assertEquals(1, fakeDocumentRepository.finalizeCalls)
    }

    @Test
    fun test38_successfulFinalizeEmitsNavigateSuccessDirectlyWithoutSnackbar() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(lineId, testProduct))
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "1"))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(lineId, "100"))

        val effects = mutableListOf<InvoiceUiEffect>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(InvoiceUiIntent.OnConfirmFinalize)
        testScheduler.advanceUntilIdle()

        assertEquals(1, effects.size)
        assertTrue(effects.first() is InvoiceUiEffect.NavigateSuccess)
        assertEquals("doc-1", (effects.first() as InvoiceUiEffect.NavigateSuccess).documentId)
        job.cancel()
    }

    @Test
    fun test40_newInvoiceDefaultPosIsBlank() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.placeOfSupplyStateCode)
    }

    @Test
    fun test41_editDraftWithNullPosRestoresAsBlankNotGujarat() = runTest(testDispatcher) {
        val draft = BusinessDocument(
            id = "draft-no-pos",
            documentType = DocumentType.TAX_INVOICE,
            documentNumber = "VE/01/2023-24",
            documentDate = 1000L,
            status = DocumentStatus.DRAFT,
            clientId = "client-1",
            placeOfSupply = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        fakeDocumentRepository.storedDocs["draft-no-pos"] = draft
        val editVm = InvoiceViewModel(
            clientRepository = fakeClientRepository,
            productRepository = fakeProductRepository,
            documentRepository = fakeDocumentRepository,
            profileRepository = fakeProfileRepository,
            calculator = calculator,
            savedStateHandle = SavedStateHandle(mapOf("documentId" to "draft-no-pos"))
        )
        testScheduler.advanceUntilIdle()
        assertEquals("", editVm.uiState.value.placeOfSupplyStateCode)
    }

    @Test
    fun test42_dirtyExistingDraftMetadataPreservedOnDirectFinalize() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(lineId, testProduct))
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "1"))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(lineId, "100"))

        viewModel.onIntent(InvoiceUiIntent.OnMetadataChange(
            deliveryNote = "Updated Delivery Note",
            paymentTerms = "Net 30",
            destination = "Mumbai Port"
        ))

        viewModel.onIntent(InvoiceUiIntent.OnConfirmFinalize)
        testScheduler.advanceUntilIdle()

        val savedDoc = fakeDocumentRepository.storedDocs["doc-1"]
        assertNotNull(savedDoc)
        assertEquals("Updated Delivery Note", savedDoc?.deliveryNote)
        assertEquals("Net 30", savedDoc?.paymentTerms)
        assertEquals("Mumbai Port", savedDoc?.destination)
    }

    @Test
    fun test39_duplicateDocumentNumberErrorDisplayed() = runTest(testDispatcher) {
        fakeDocumentRepository.finalizeResultOverride = DocumentFinalizationResult.Invalid(
            listOf(DocumentValidationError.DuplicateDocumentNumber)
        )
        testScheduler.advanceUntilIdle()
        viewModel.onIntent(InvoiceUiIntent.OnSelectClient(testClient))
        val lineId = viewModel.uiState.value.lineItems.first().id
        viewModel.onIntent(InvoiceUiIntent.OnSelectProduct(lineId, testProduct))
        viewModel.onIntent(InvoiceUiIntent.OnQuantityChange(lineId, "1"))
        viewModel.onIntent(InvoiceUiIntent.OnRateChange(lineId, "100"))

        viewModel.onIntent(InvoiceUiIntent.OnConfirmFinalize)
        testScheduler.advanceUntilIdle()

        assertEquals("This invoice number is already in use.", viewModel.uiState.value.generalError)
    }

    // ── Fakes ─────────────────────────────────────────────────────────────

    private class FakeClientRepository(initialClients: List<Client>) : ClientRepository {
        private val flow = MutableStateFlow(initialClients)
        override fun observeClients(): Flow<List<Client>> = flow
        override fun observeClientById(id: String): Flow<Client?> = MutableStateFlow(flow.value.firstOrNull { it.id == id })
        override suspend fun getClientById(id: String): Client? = flow.value.firstOrNull { it.id == id }
        override suspend fun createClient(client: Client): Result<Unit> = Result.success(Unit)
        override suspend fun updateClient(client: Client): Result<Unit> = Result.success(Unit)
        override suspend fun deleteClient(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeProductRepository(initialProducts: List<Product>) : ProductRepository {
        private val flow = MutableStateFlow(initialProducts)
        override fun observeProducts(): Flow<List<Product>> = flow
        override fun observeActiveProducts(): Flow<List<Product>> = MutableStateFlow(flow.value.filter { it.isActive })
        override fun observeProductById(id: String): Flow<Product?> = MutableStateFlow(flow.value.firstOrNull { it.id == id })
        override suspend fun getProductById(id: String): Product? = flow.value.firstOrNull { it.id == id }
        override suspend fun createProduct(product: Product): Result<Unit> = Result.success(Unit)
        override suspend fun updateProduct(product: Product): Result<Unit> = Result.success(Unit)
        override suspend fun setProductActive(id: String, isActive: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun deleteProduct(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeDocumentRepository : DocumentRepository {
        var createDraftCalls = 0
        var updateDraftCalls = 0
        var finalizeCalls = 0
        var finalizeResultOverride: DocumentFinalizationResult? = null
        val storedDocs = mutableMapOf<String, BusinessDocument>()

        override fun observeDocumentById(id: String): Flow<BusinessDocument?> = MutableStateFlow(storedDocs[id])
        override fun observeDocumentsByType(type: DocumentType): Flow<List<BusinessDocument>> = MutableStateFlow(emptyList())
        override fun observeDocumentsByClient(clientId: String): Flow<List<BusinessDocument>> = MutableStateFlow(emptyList())
        override suspend fun getDocumentById(id: String): BusinessDocument? = storedDocs[id]
        override suspend fun getLineItemsForDocument(documentId: String): List<DocumentLineItem> = emptyList()
        override suspend fun suggestDocumentNumber(type: DocumentType, documentDate: Long): String = "VE/01/2023-24"
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
            val doc = BusinessDocument(
                id = "doc-1",
                documentType = type,
                documentNumber = documentNumber ?: "VE/01/2023-24",
                documentDate = documentDate,
                status = DocumentStatus.DRAFT,
                clientId = clientId,
                lineItems = lineItems,
                placeOfSupply = placeOfSupply,
                deliveryFactoryAddress = deliveryFactoryAddress,
                paymentTerms = paymentTerms,
                deliveryNote = deliveryNote,
                supplierReference = supplierReference,
                otherReferences = otherReferences,
                buyerOrderNumber = buyerOrderNumber,
                buyerOrderDate = buyerOrderDate,
                dispatchDocumentNumber = dispatchDocumentNumber,
                deliveryNoteDate = deliveryNoteDate,
                dispatchThrough = dispatchThrough,
                destination = destination,
                termsOfDelivery = termsOfDelivery,
                createdAt = 1000L,
                updatedAt = 1000L,
                syncStatus = SyncStatus.PENDING
            )
            storedDocs[doc.id] = doc
            return Result.success(doc)
        }

        override suspend fun updateDraft(
            document: BusinessDocument,
            lineItems: List<DocumentLineItem>
        ): Result<BusinessDocument> {
            updateDraftCalls++
            storedDocs[document.id] = document
            return Result.success(document)
        }

        override suspend fun finalizeDocument(
            documentId: String,
            overrideDocumentNumber: String?
        ): DocumentFinalizationResult {
            finalizeCalls++
            finalizeResultOverride?.let { return it }
            val doc = BusinessDocument(
                id = documentId,
                documentType = DocumentType.TAX_INVOICE,
                documentNumber = overrideDocumentNumber ?: "VE/01/2023-24",
                documentDate = 1000L,
                status = DocumentStatus.FINALIZED,
                clientId = "client-1",
                createdAt = 1000L,
                updatedAt = 1000L,
                syncStatus = SyncStatus.PENDING
            )
            return DocumentFinalizationResult.Success(doc)
        }

        override suspend fun cancelDocument(documentId: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeBusinessProfileRepository(private val profile: BusinessProfile) : BusinessProfileRepository {
        override fun observeProfile(): Flow<BusinessProfile?> = MutableStateFlow(profile)
        override suspend fun getProfile(): BusinessProfile? = profile
    }
}
