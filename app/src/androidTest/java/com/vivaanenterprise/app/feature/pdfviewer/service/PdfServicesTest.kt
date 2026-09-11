package com.vivaanenterprise.app.feature.pdfviewer.service

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.model.ClientSnapshot
import com.vivaanenterprise.app.domain.model.DocumentLineItem
import com.vivaanenterprise.app.domain.model.SellerSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class PdfServicesTest {

    private lateinit var context: Context
    private lateinit var sanitizer: PdfFilenameSanitizer
    private lateinit var cacheManager: PdfCacheManager
    private lateinit var shareManager: PdfShareManager
    private lateinit var exportManager: PdfExportManager

    private val sampleDoc = BusinessDocument(
        id = "doc-12345",
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
                documentId = "doc-12345",
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
        context = InstrumentationRegistry.getInstrumentation().targetContext
        sanitizer = PdfFilenameSanitizer()
        cacheManager = PdfCacheManager(context, sanitizer)
        shareManager = PdfShareManager(context, sanitizer)
        exportManager = PdfExportManager(context, sanitizer)
    }

    @Test
    fun testFilenameSanitizer_sanitizesDocumentNumber() {
        val filename = sanitizer.formatUserFacingFilename(sampleDoc)
        assertEquals("Tax_Invoice_VE_06_2026_27_Acme.pdf", filename)

        val poDoc = sampleDoc.copy(documentType = DocumentType.PURCHASE_ORDER, documentNumber = "PO/123/2026-27")
        assertEquals("Purchase_Order_PO_123_2026_27_Acme.pdf", sanitizer.formatUserFacingFilename(poDoc))

        val dirtyNum = "  ABC / 125 : test? * < > |  "
        assertEquals("ABC_125_test", sanitizer.sanitizeFilename(dirtyNum))
    }

    @Test
    fun testPdfCacheManager_writesAndRetrievesCacheFileAtomically() = runBlocking {
        val pdfBytes = "PDF-HEADER-DUMMY_BYTES".toByteArray()
        val file = cacheManager.writePdfToCache("doc-12345", pdfBytes)

        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertEquals("pdf_doc_12345.pdf", file.name)

        val retrieved = cacheManager.getCachedPdf("doc-12345")
        assertNotNull(retrieved)
        assertEquals(file.absolutePath, retrieved?.absolutePath)
    }

    @Test
    fun testPdfShareManager_createsValidContentUriIntent() = runBlocking {
        val file = cacheManager.writePdfToCache("doc-share", "DUMMY".toByteArray())
        val chooserIntent = shareManager.createShareIntent(file, sampleDoc)

        assertNotNull(chooserIntent)
        val targetIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(targetIntent)
        val uri = targetIntent?.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
        assertNotNull(uri)
        assertEquals("content", uri?.scheme)
        assertTrue(uri.toString().contains("fileprovider"))
        val expectedFilename = sanitizer.formatUserFacingFilename(sampleDoc)
        assertTrue("Share URI path should contain user-facing filename: $expectedFilename, got: $uri", uri.toString().contains(expectedFilename))
    }

    @Test
    fun testPdfRenderSession_opensAndRendersPage() {
        runBlocking {
            // Create valid 1-page PDF using android.graphics.pdf.PdfDocument
            val pdfDoc = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            page.canvas.drawText("Test PDF Content", 100f, 100f, android.graphics.Paint())
            pdfDoc.finishPage(page)

            val testPdfFile = File(context.cacheDir, "test_session_doc.pdf")
            FileOutputStream(testPdfFile).use { pdfDoc.writeTo(it) }
            pdfDoc.close()

            val session = DefaultPdfRenderSession.open(testPdfFile)
            assertEquals(1, session.pageCount)

            val bitmap = session.renderPage(0, 300)
            assertNotNull(bitmap)
            assertEquals(300, bitmap?.width)
            assertTrue((bitmap?.height ?: 0) > 0)

            session.close()
            testPdfFile.delete()
        }
    }

    @Test
    fun testPdfExportManager_saveToDownloadsApi29_writesAndDeletesRecord() = runBlocking {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val file = cacheManager.writePdfToCache("doc-export-test", "EXPORT_TEST_PDF_BYTES".toByteArray())
            val result = exportManager.saveToDownloadsApi29(file, sampleDoc)

            assertTrue("Export should succeed on API 29+", result is ExportResult.Success)
            val success = result as ExportResult.Success
            val savedUri = success.uri
            assertNotNull(savedUri)

            // Verify created record in MediaStore
            val resolver = context.contentResolver
            resolver.query(savedUri!!, arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, android.provider.MediaStore.MediaColumns.MIME_TYPE), null, null, null)?.use { cursor ->
                assertTrue(cursor.moveToFirst())
                val displayName = cursor.getString(0)
                val mimeType = cursor.getString(1)
                assertEquals("application/pdf", mimeType)
                assertTrue(displayName.startsWith("Tax_Invoice_VE_06_2026_27"))
            }

            // Cleanup test row from MediaStore so user's Downloads directory stays clean
            resolver.delete(savedUri, null, null)
        }
    }

    @Test
    fun testPdfCacheManager_preventsPathTraversal() = runBlocking {
        val file = cacheManager.writePdfToCache("../../etc/evil", "DUMMY".toByteArray())
        assertTrue("Cache file must remain within cacheDir/pdfs", file.canonicalPath.startsWith(File(context.cacheDir, "pdfs").canonicalPath))
    }
}
