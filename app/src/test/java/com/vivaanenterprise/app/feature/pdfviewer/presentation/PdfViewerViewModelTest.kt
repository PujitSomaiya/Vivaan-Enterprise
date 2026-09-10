package com.vivaanenterprise.app.feature.pdfviewer.presentation

import androidx.lifecycle.SavedStateHandle
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.model.ClientSnapshot
import com.vivaanenterprise.app.domain.model.DocumentLineItem
import com.vivaanenterprise.app.domain.model.SellerSnapshot
import com.vivaanenterprise.app.domain.pdf.PdfGenerationResult
import com.vivaanenterprise.app.domain.usecase.GenerateBusinessDocumentPdfUseCase
import com.vivaanenterprise.app.feature.pdfviewer.service.ExportResult
import com.vivaanenterprise.app.feature.pdfviewer.service.PdfCacheManager
import com.vivaanenterprise.app.feature.pdfviewer.service.PdfExportManager
import com.vivaanenterprise.app.feature.pdfviewer.service.PdfFilenameSanitizer
import com.vivaanenterprise.app.feature.pdfviewer.service.PdfRenderSession
import com.vivaanenterprise.app.feature.pdfviewer.service.PdfShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PdfViewerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeGenerateUseCase: FakeGenerateBusinessDocumentPdfUseCase
    private lateinit var fakeCacheManager: FakePdfCacheManager
    private lateinit var fakeShareManager: FakePdfShareManager
    private lateinit var fakeExportManager: FakePdfExportManager
    private lateinit var sanitizer: PdfFilenameSanitizer

    private val sampleDoc = BusinessDocument(
        id = "doc-1",
        documentType = DocumentType.TAX_INVOICE,
        documentNumber = "VE/06/2026-27",
        documentDate = 1788912000000L,
        status = DocumentStatus.FINALIZED,
        clientId = "client-1",
        sellerSnapshot = SellerSnapshot(
            businessName = "VIVAAN ENTERPRISE",
            addressLine1 = "Address",
            addressLine2 = "",
            cityStatePincode = "Surendranagar",
            gstin = "24CHWPG0910J1ZB",
            mobile = "9737178061",
            pan = "CHWPG0910J",
            bankAccountName = "JANVI",
            bankName = "HDFC",
            bankAccountNumber = "1234",
            bankIfsc = "HDFC0000299",
            bankBranch = "Paldi",
            declaration = "Decl",
            authorisedSignatory = "Auth"
        ),
        clientSnapshot = ClientSnapshot(
            clientId = "client-1",
            companyName = "Acme"
        ),
        lineItems = listOf(
            DocumentLineItem(
                id = "l1",
                documentId = "doc-1",
                position = 0,
                descriptionSnapshot = "Item 1",
                quantity = 1,
                ratePaise = 100,
                gstRateBasisPoints = 1800,
                createdAt = 0,
                updatedAt = 0
            )
        ),
        createdAt = 0,
        updatedAt = 0
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGenerateUseCase = FakeGenerateBusinessDocumentPdfUseCase()
        fakeCacheManager = FakePdfCacheManager()
        fakeShareManager = FakePdfShareManager()
        fakeExportManager = FakePdfExportManager()
        sanitizer = PdfFilenameSanitizer()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(documentId: String = "doc-1"): TestablePdfViewerViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("documentId" to documentId))
        return TestablePdfViewerViewModel(
            generatePdfUseCase = fakeGenerateUseCase,
            cacheManager = fakeCacheManager,
            shareManager = fakeShareManager,
            exportManager = fakeExportManager,
            filenameSanitizer = sanitizer,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun testInitialLoad_success_populatesPagesAndState() = runTest {
        fakeGenerateUseCase.doc = sampleDoc
        fakeGenerateUseCase.pdfResult = PdfGenerationResult.Success("PDF_BYTES".toByteArray())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isInitialLoading)
        assertNull(state.errorMessage)
        assertEquals("VE/06/2026-27", state.documentNumber)
        assertEquals(10, state.pageCount)
        assertEquals(10, state.pages.size)
    }

    @Test
    fun testInitialLoad_failure_displaysErrorMessage() = runTest {
        fakeGenerateUseCase.doc = sampleDoc
        fakeGenerateUseCase.pdfResult = PdfGenerationResult.Failure.DocumentNotFinalized

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isInitialLoading)
        assertEquals("Document is not finalized", state.errorMessage)
    }

    @Test
    fun testOnShareClick_triggersShareIntent() = runTest {
        fakeGenerateUseCase.doc = sampleDoc
        fakeGenerateUseCase.pdfResult = PdfGenerationResult.Success("PDF_BYTES".toByteArray())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(PdfViewerUiIntent.OnShareClick)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSharing)
    }

    @Test
    fun testPageEviction_retainsOnlyMaxRenderedPages() = runTest {
        fakeGenerateUseCase.doc = sampleDoc
        fakeGenerateUseCase.pdfResult = PdfGenerationResult.Success("PDF_BYTES".toByteArray())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Load 7 pages sequentially
        for (i in 0..6) {
            viewModel.onIntent(PdfViewerUiIntent.OnLoadPage(i, 500))
            testDispatcher.scheduler.advanceUntilIdle()
        }

        val pages = viewModel.uiState.value.pages
        val readyPages = pages.filterValues { it.bitmap != null }
        // Should cap at 5 rendered pages maximum
        assertTrue("Ready pages count ${readyPages.size} should not exceed 5", readyPages.size <= 5)

        // Evicted page 0 bitmap should now be null
        assertNull(pages[0]?.bitmap)

        // Request page 0 again -> must re-render and become non-null
        viewModel.onIntent(PdfViewerUiIntent.OnLoadPage(0, 500))
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedPages = viewModel.uiState.value.pages
        assertNotNull(updatedPages[0]?.bitmap)
    }

    @Test
    fun testOnLoadPage_invalidPageIndex_ignored() = runTest {
        fakeGenerateUseCase.doc = sampleDoc
        fakeGenerateUseCase.pdfResult = PdfGenerationResult.Success("PDF_BYTES".toByteArray())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(PdfViewerUiIntent.OnLoadPage(-1, 500))
        viewModel.onIntent(PdfViewerUiIntent.OnLoadPage(10, 500))
        testDispatcher.scheduler.advanceUntilIdle()

        val pages = viewModel.uiState.value.pages
        assertNull(pages[-1])
        assertNull(pages[10])
    }

    @Test
    fun testDocumentNotFound_showsErrorMessage() = runTest {
        fakeGenerateUseCase.doc = null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isInitialLoading)
        assertEquals("Document not found", state.errorMessage)
    }

    private class TestablePdfViewerViewModel(
        generatePdfUseCase: GenerateBusinessDocumentPdfUseCase,
        cacheManager: PdfCacheManager,
        shareManager: PdfShareManager,
        exportManager: PdfExportManager,
        filenameSanitizer: PdfFilenameSanitizer,
        savedStateHandle: SavedStateHandle
    ) : PdfViewerViewModel(
        generatePdfUseCase,
        cacheManager,
        shareManager,
        exportManager,
        filenameSanitizer,
        savedStateHandle
    ) {
        override fun openRenderSession(file: File): PdfRenderSession {
            return FakePdfRenderSession(pageCount = 10)
        }
    }

    private class FakePdfRenderSession(override val pageCount: Int) : PdfRenderSession {
        override suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): android.graphics.Bitmap? {
            return try {
                val constructor = android.graphics.Bitmap::class.java.getDeclaredConstructor()
                constructor.isAccessible = true
                constructor.newInstance()
            } catch (_: Exception) {
                // In mockableJar environment, return non-null dummy instance via reflection/unsafe if needed
                val unsafeClass = Class.forName("sun.misc.Unsafe")
                val field = unsafeClass.getDeclaredField("theUnsafe")
                field.isAccessible = true
                val unsafe = field.get(null)
                val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)
                allocateInstance.invoke(unsafe, android.graphics.Bitmap::class.java) as android.graphics.Bitmap
            }
        }
        override fun close() {}
    }

    private class FakeGenerateBusinessDocumentPdfUseCase : GenerateBusinessDocumentPdfUseCase(
        documentRepository = object : com.vivaanenterprise.app.domain.repository.DocumentRepository {
            override fun observeAllDocuments() = TODO()
            override fun observeDocumentById(id: String) = TODO()
            override fun observeDocumentsByType(type: DocumentType) = TODO()
            override fun observeDocumentsByClient(clientId: String) = TODO()
            override suspend fun getDocumentById(id: String): BusinessDocument? = null
            override suspend fun getLineItemsForDocument(documentId: String): List<DocumentLineItem> = emptyList()
            override suspend fun suggestDocumentNumber(type: DocumentType, dateMillis: Long) = ""
            override suspend fun createDraft(type: DocumentType, clientId: String, documentDate: Long, documentNumber: String?, lineItems: List<DocumentLineItem>, placeOfSupply: String?, deliveryFactoryAddress: String?, paymentTerms: String?, deliveryNote: String?, supplierReference: String?, otherReferences: String?, buyerOrderNumber: String?, buyerOrderDate: Long?, dispatchDocumentNumber: String?, deliveryNoteDate: Long?, dispatchThrough: String?, destination: String?, termsOfDelivery: String?) = TODO()
            override suspend fun updateDraft(document: BusinessDocument, lineItems: List<DocumentLineItem>) = TODO()
            override suspend fun finalizeDocument(documentId: String, overrideDocumentNumber: String?) = TODO()
            override suspend fun cancelDocument(documentId: String) = TODO()
            override suspend fun deleteDocument(documentId: String) = TODO()
        },
        pdfGenerator = object : com.vivaanenterprise.app.domain.pdf.BusinessDocumentPdfGenerator {
            override suspend fun generatePdf(document: BusinessDocument) = PdfGenerationResult.Failure.NoLineItems
        }
    ) {
        var doc: BusinessDocument? = null
        var pdfResult: PdfGenerationResult = PdfGenerationResult.Success("PDF_BYTES".toByteArray())

        override suspend fun getDocument(documentId: String): BusinessDocument? {
            return doc
        }

        override suspend fun invoke(documentId: String): PdfGenerationResult {
            return pdfResult
        }
    }

    private class TestContext : android.content.ContextWrapper(null) {
        override fun getCacheDir(): File = File(System.getProperty("java.io.tmpdir"), "test_cache")
        override fun getPackageName(): String = "com.vivaanenterprise.app"
    }

    private class FakePdfCacheManager : PdfCacheManager(context = TestContext(), sanitizer = PdfFilenameSanitizer()) {
        override suspend fun writePdfToCache(documentId: String, pdfBytes: ByteArray): File {
            val file = File.createTempFile("fake_cache_", ".pdf")
            file.writeBytes(pdfBytes)
            return file
        }

        override fun getCachedPdf(documentId: String): File? {
            val file = File.createTempFile("fake_cache_", ".pdf")
            file.writeBytes("DUMMY".toByteArray())
            return file
        }
    }

    private class FakePdfShareManager : PdfShareManager(context = TestContext(), sanitizer = PdfFilenameSanitizer()) {
        override fun createShareIntent(cachedFile: File, document: BusinessDocument): android.content.Intent {
            return android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
            }
        }
    }

    private class FakePdfExportManager : PdfExportManager(context = TestContext(), sanitizer = PdfFilenameSanitizer()) {
        override suspend fun saveToDownloadsApi29(cachedFile: File, document: BusinessDocument): ExportResult {
            return ExportResult.Success("Saved to Downloads")
        }

        override suspend fun copyToUri(cachedFile: File, destinationUri: android.net.Uri): ExportResult {
            return ExportResult.Success("Saved successfully")
        }
    }
}
