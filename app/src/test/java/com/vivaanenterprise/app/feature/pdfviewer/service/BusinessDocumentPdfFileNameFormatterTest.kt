package com.vivaanenterprise.app.feature.pdfviewer.service

import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.model.ClientSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BusinessDocumentPdfFileNameFormatterTest {

    private lateinit var sanitizer: PdfFilenameSanitizer

    private val sampleInvoice = BusinessDocument(
        id = "doc-invoice-1001",
        documentType = DocumentType.TAX_INVOICE,
        documentNumber = "VE/01/2026-27",
        documentDate = 1000L,
        status = DocumentStatus.FINALIZED,
        clientId = "client-1",
        clientSnapshot = ClientSnapshot(
            clientId = "client-1",
            companyName = "ABC Enterprise"
        ),
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private val samplePO = BusinessDocument(
        id = "doc-po-2002",
        documentType = DocumentType.PURCHASE_ORDER,
        documentNumber = "PO/12/2026-27",
        documentDate = 1000L,
        status = DocumentStatus.FINALIZED,
        clientId = "supplier-1",
        clientSnapshot = ClientSnapshot(
            clientId = "supplier-1",
            companyName = "Shree Industrial Tools"
        ),
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @Before
    fun setUp() {
        sanitizer = PdfFilenameSanitizer()
    }

    @Test
    fun test1_taxInvoiceCorrectPrefixAndFormat() {
        val filename = sanitizer.formatUserFacingFilename(sampleInvoice)
        assertEquals("Tax_Invoice_VE_01_2026_27_ABC_Enterprise.pdf", filename)
    }

    @Test
    fun test2_purchaseOrderCorrectPrefixAndFormat() {
        val filename = sanitizer.formatUserFacingFilename(samplePO)
        assertEquals("Purchase_Order_PO_12_2026_27_Shree_Industrial_Tools.pdf", filename)
    }

    @Test
    fun test3_slashesInDocumentNumberSanitized() {
        val doc = sampleInvoice.copy(documentNumber = "VE/09/2025/26")
        val filename = sanitizer.formatUserFacingFilename(doc)
        assertEquals("Tax_Invoice_VE_09_2025_26_ABC_Enterprise.pdf", filename)
    }

    @Test
    fun test4_spacesNormalizedToUnderscores() {
        val doc = sampleInvoice.copy(
            documentNumber = "  VE / 01 / 2026  ",
            clientSnapshot = ClientSnapshot("c1", "Vivaan   Enterprise   Pvt   Ltd")
        )
        val filename = sanitizer.formatUserFacingFilename(doc)
        assertEquals("Tax_Invoice_VE_01_2026_Vivaan_Enterprise_Pvt_Ltd.pdf", filename)
    }

    @Test
    fun test5_unsafeFilesystemCharactersRemoved() {
        val doc = sampleInvoice.copy(
            documentNumber = "VE/01:2026?*<|>\"",
            clientSnapshot = ClientSnapshot("c1", "A/B: Traders & Co.")
        )
        val filename = sanitizer.formatUserFacingFilename(doc)
        assertEquals("Tax_Invoice_VE_01_2026_A_B_Traders_Co.pdf", filename)
    }

    @Test
    fun test6_repeatedSeparatorsCollapsed() {
        val doc = sampleInvoice.copy(
            documentNumber = "VE///01---2026",
            clientSnapshot = ClientSnapshot("c1", "ABC___Enterprise---Limited")
        )
        val filename = sanitizer.formatUserFacingFilename(doc)
        assertEquals("Tax_Invoice_VE_01_2026_ABC_Enterprise_Limited.pdf", filename)
    }

    @Test
    fun test7_blankCompanyNameFallback() {
        val doc = sampleInvoice.copy(clientSnapshot = null)
        val filename = sanitizer.formatUserFacingFilename(doc)
        assertEquals("Tax_Invoice_VE_01_2026_27.pdf", filename)
    }

    @Test
    fun test8_longCompanyNameTruncatedSafely() {
        val longName = "A".repeat(200)
        val doc = sampleInvoice.copy(clientSnapshot = ClientSnapshot("c1", longName))
        val filename = sanitizer.formatUserFacingFilename(doc)

        assertTrue(filename.startsWith("Tax_Invoice_VE_01_2026_27_AAAAA"))
        assertTrue(filename.endsWith(".pdf"))
        assertTrue(filename.length <= 124) // 120 base chars + 4 for .pdf
    }

    @Test
    fun test9_pdfExtensionAppearsExactlyOnce() {
        val doc = sampleInvoice.copy(documentNumber = "VE/01.pdf")
        val filename = sanitizer.formatUserFacingFilename(doc)
        assertEquals("Tax_Invoice_VE_01_pdf_ABC_Enterprise.pdf", filename)
        assertTrue(filename.endsWith(".pdf"))
        assertEquals(1, filename.split(".pdf").size - 1)
    }

    @Test
    fun test10_deterministicResultForSameDocument() {
        val fn1 = sanitizer.formatUserFacingFilename(sampleInvoice)
        val fn2 = sanitizer.formatUserFacingFilename(sampleInvoice)
        assertEquals(fn1, fn2)
    }

    @Test
    fun test11_noUuidShownWhenValidDocumentNumberExists() {
        val filename = sanitizer.formatUserFacingFilename(sampleInvoice)
        assertFalse(filename.contains("doc-invoice-1001"))
    }

    @Test
    fun test12_usesHistoricalClientSnapshotName() {
        val docWithSnapshot = sampleInvoice.copy(
            clientSnapshot = ClientSnapshot("c1", "Historical Snapshot Name")
        )
        val filename = sanitizer.formatUserFacingFilename(docWithSnapshot)
        assertEquals("Tax_Invoice_VE_01_2026_27_Historical_Snapshot_Name.pdf", filename)
    }
}
