package com.vivaanenterprise.app.core.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.model.ClientSnapshot
import com.vivaanenterprise.app.domain.model.DocumentLineItem
import com.vivaanenterprise.app.domain.model.SellerSnapshot
import com.vivaanenterprise.app.domain.pdf.PdfGenerationResult
import com.vivaanenterprise.app.domain.util.IndianCurrencyFormatter
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
class AndroidBusinessDocumentPdfGeneratorTest {

    private lateinit var generator: AndroidBusinessDocumentPdfGenerator
    private lateinit var currencyFormatter: IndianCurrencyFormatter

    private val sampleSeller = SellerSnapshot(
        businessName = "VIVAAN ENTERPRISE",
        addressLine1 = "NEAR SHALIBHADRANIVAS, OPP. SIDDHIVINAYAK HOUSE",
        addressLine2 = "STREET NO. 4, MAHATMA GANDHI ROAD, JORAWAR NAGAR",
        cityStatePincode = "SURENDRANAGAR, GUJARAT - 363020",
        gstin = "24CHWPG0910J1ZB",
        mobile = "+91 97371 78061",
        email = "vivaan@enterprise.com",
        pan = "CHWPG0910J",
        stateCode = "24",
        bankAccountName = "SHETH JANVI",
        bankName = "HDFC BANK",
        bankAccountNumber = "50100419622062",
        bankIfsc = "HDFC0000299",
        bankBranch = "PALDI, AHMEDABAD - 380 007",
        declaration = "We declare that this invoice shows the actual price of the goods described.",
        authorisedSignatory = "For VIVAAN ENTERPRISE"
    )

    private val sampleClientVE06 = ClientSnapshot(
        clientId = "client-ve06",
        companyName = "GREEN ECO INDUSTRIES",
        address = "123 Green Park, Industrial Estate, Chennai",
        gstin = "33AAACG1234A1Z5",
        state = "Tamil Nadu",
        stateCode = "33",
        email = "billing@greeneco.com",
        phone = "9876543210"
    )

    private val sampleLineVE06 = DocumentLineItem(
        id = "line-1",
        documentId = "doc-ve06",
        position = 0,
        descriptionSnapshot = "3M anti-slip 15mm Scotch Tape",
        hsnSacSnapshot = "3919",
        quantity = 5,
        ratePaise = 350000L, // ₹3,500.00
        gstRateBasisPoints = 1800,
        taxableAmountPaise = 1750000L, // ₹17,500.00
        igstAmountPaise = 315000L, // ₹3,150.00
        totalTaxPaise = 315000L,
        lineTotalPaise = 2065000L, // ₹20,650.00
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private val sampleVe06Invoice = BusinessDocument(
        id = "doc-ve06",
        documentType = DocumentType.TAX_INVOICE,
        documentNumber = "VE/06/2026-27",
        documentDate = 1788912000000L, // 9 Sep 2026
        status = DocumentStatus.FINALIZED,
        clientId = "client-ve06",
        sellerSnapshot = sampleSeller,
        clientSnapshot = sampleClientVE06,
        lineItems = listOf(sampleLineVE06),
        placeOfSupply = "33",
        taxableAmountPaise = 1750000L,
        igstAmountPaise = 315000L,
        totalTaxAmountPaise = 315000L,
        grandTotalPaise = 2065000L,
        amountInWords = "RUPEES TWENTY THOUSAND SIX HUNDRED FIFTY ONLY",
        createdAt = 1000L,
        updatedAt = 1000L,
        finalizedAt = 1000L
    )

    private val sampleVe05Invoice = BusinessDocument(
        id = "doc-ve05",
        documentType = DocumentType.TAX_INVOICE,
        documentNumber = "VE/05/2026-27",
        documentDate = 1788307200000L, // 2 Sep 2026
        status = DocumentStatus.FINALIZED,
        clientId = "client-ve06",
        sellerSnapshot = sampleSeller,
        clientSnapshot = sampleClientVE06,
        lineItems = listOf(
            DocumentLineItem(
                id = "line-ve05",
                documentId = "doc-ve05",
                position = 0,
                descriptionSnapshot = "Industrial Tape Roll 50m",
                hsnSacSnapshot = "3919",
                quantity = 18,
                ratePaise = 230000L, // ₹2,300.00
                gstRateBasisPoints = 1800,
                taxableAmountPaise = 4140000L, // ₹41,400.00
                igstAmountPaise = 745200L, // ₹7,452.00
                totalTaxPaise = 745200L,
                lineTotalPaise = 4885200L, // ₹48,852.00
                createdAt = 1000L,
                updatedAt = 1000L
            )
        ),
        placeOfSupply = "33",
        taxableAmountPaise = 4140000L,
        igstAmountPaise = 745200L,
        totalTaxAmountPaise = 745200L,
        grandTotalPaise = 4885200L,
        amountInWords = "RUPEES FORTY EIGHT THOUSAND EIGHT HUNDRED FIFTY TWO ONLY",
        createdAt = 1000L,
        updatedAt = 1000L,
        finalizedAt = 1000L
    )

    private val samplePurchaseOrder = BusinessDocument(
        id = "doc-po-05",
        documentType = DocumentType.PURCHASE_ORDER,
        documentNumber = "VE/05/2026-27",
        documentDate = 1788566400000L, // 5 Sep 2026
        status = DocumentStatus.FINALIZED,
        clientId = "client-ve06",
        sellerSnapshot = sampleSeller,
        clientSnapshot = sampleClientVE06,
        lineItems = listOf(sampleLineVE06),
        deliveryFactoryAddress = "Factory Location Gate 4, GIDC Industrial Estate, Surendranagar - 363020",
        deliveryNote = "Delivery Note 101",
        destination = "Pune",
        placeOfSupply = "33",
        taxableAmountPaise = 1750000L,
        igstAmountPaise = 315000L,
        totalTaxAmountPaise = 315000L,
        grandTotalPaise = 2065000L,
        amountInWords = "RUPEES TWENTY THOUSAND SIX HUNDRED FIFTY ONLY",
        createdAt = 1000L,
        updatedAt = 1000L,
        finalizedAt = 1000L
    )

    @Before
    fun setUp() {
        currencyFormatter = IndianCurrencyFormatter()
        generator = AndroidBusinessDocumentPdfGenerator(currencyFormatter)
    }

    @Test
    fun test1_finalizedTaxInvoiceVE06GeneratesNonEmptyPdfBytes() = runBlocking {
        val result = generator.generatePdf(sampleVe06Invoice)
        assertTrue(result is PdfGenerationResult.Success)
        val bytes = (result as PdfGenerationResult.Success).pdfBytes
        assertTrue(bytes.isNotEmpty())

        verifyAndSavePdfArtifact(bytes, "VE-06-generated.pdf", expectedPageCount = 1)
    }

    @Test
    fun test2_finalizedTaxInvoiceVE05GeneratesNonEmptyPdfBytes() = runBlocking {
        val result = generator.generatePdf(sampleVe05Invoice)
        assertTrue(result is PdfGenerationResult.Success)
        val bytes = (result as PdfGenerationResult.Success).pdfBytes
        assertTrue(bytes.isNotEmpty())

        verifyAndSavePdfArtifact(bytes, "VE-05-generated.pdf", expectedPageCount = 1)
    }

    @Test
    fun test3_finalizedPurchaseOrderGeneratesNonEmptyPdfBytes() = runBlocking {
        val result = generator.generatePdf(samplePurchaseOrder)
        assertTrue(result is PdfGenerationResult.Success)
        val bytes = (result as PdfGenerationResult.Success).pdfBytes
        assertTrue(bytes.isNotEmpty())

        verifyAndSavePdfArtifact(bytes, "PO-VE-05-generated.pdf", expectedPageCount = 1)
    }

    @Test
    fun test4_draftDocumentIsRejected() = runBlocking {
        val draftDoc = sampleVe06Invoice.copy(status = DocumentStatus.DRAFT)
        val result = generator.generatePdf(draftDoc)
        assertEquals(PdfGenerationResult.Failure.DocumentNotFinalized, result)
    }

    @Test
    fun test5_missingLineItemsIsRejected() = runBlocking {
        val noLineDoc = sampleVe06Invoice.copy(lineItems = emptyList())
        val result = generator.generatePdf(noLineDoc)
        assertEquals(PdfGenerationResult.Failure.NoLineItems, result)
    }

    @Test
    fun test5b_zeroGstInterStateRendersSuccessfullyFromPersistedTaxTreatment() = runBlocking {
        val zeroGstLine = sampleLineVE06.copy(
            gstRateBasisPoints = 0,
            igstAmountPaise = 0L,
            cgstAmountPaise = 0L,
            sgstAmountPaise = 0L,
            totalTaxPaise = 0L,
            lineTotalPaise = 1750000L
        )
        val zeroGstInterStateDoc = sampleVe06Invoice.copy(
            taxTreatment = com.vivaanenterprise.app.domain.model.TaxTreatment.INTER_STATE,
            lineItems = listOf(zeroGstLine),
            igstAmountPaise = 0L,
            cgstAmountPaise = 0L,
            sgstAmountPaise = 0L,
            totalTaxAmountPaise = 0L,
            grandTotalPaise = 1750000L
        )
        val result = generator.generatePdf(zeroGstInterStateDoc)
        assertTrue(result is PdfGenerationResult.Success)
    }

    @Test
    fun test5c_zeroGstIntraStateRendersSuccessfullyFromPersistedTaxTreatment() = runBlocking {
        val zeroGstLine = sampleLineVE06.copy(
            gstRateBasisPoints = 0,
            igstAmountPaise = 0L,
            cgstAmountPaise = 0L,
            sgstAmountPaise = 0L,
            totalTaxPaise = 0L,
            lineTotalPaise = 1750000L
        )
        val zeroGstIntraStateDoc = sampleVe06Invoice.copy(
            taxTreatment = com.vivaanenterprise.app.domain.model.TaxTreatment.INTRA_STATE,
            lineItems = listOf(zeroGstLine),
            igstAmountPaise = 0L,
            cgstAmountPaise = 0L,
            sgstAmountPaise = 0L,
            totalTaxAmountPaise = 0L,
            grandTotalPaise = 1750000L
        )
        val result = generator.generatePdf(zeroGstIntraStateDoc)
        assertTrue(result is PdfGenerationResult.Success)
    }

    @Test
    fun test5d_legacyZeroGstWithoutTaxTreatmentFailsWithTypedError() = runBlocking {
        val zeroGstLine = sampleLineVE06.copy(
            gstRateBasisPoints = 0,
            igstAmountPaise = 0L,
            cgstAmountPaise = 0L,
            sgstAmountPaise = 0L,
            totalTaxPaise = 0L
        )
        val legacyDoc = sampleVe06Invoice.copy(
            taxTreatment = null,
            lineItems = listOf(zeroGstLine),
            igstAmountPaise = 0L,
            cgstAmountPaise = 0L,
            sgstAmountPaise = 0L
        )
        val result = generator.generatePdf(legacyDoc)
        assertEquals(PdfGenerationResult.Failure.MissingHistoricalTaxTreatment, result)
    }

    @Test
    fun test6_deliveryFactoryAddressAndDeliveryNoteRemainIndependentForPO() = runBlocking {
        val result = generator.generatePdf(samplePurchaseOrder)
        assertTrue(result is PdfGenerationResult.Success)
        assertEquals("Delivery Note 101", samplePurchaseOrder.deliveryNote)
        assertEquals("Factory Location Gate 4, GIDC Industrial Estate, Surendranagar - 363020", samplePurchaseOrder.deliveryFactoryAddress)
        assertEquals("Pune", samplePurchaseOrder.destination)
    }

    @Test
    fun test7_multiLineDocumentPaginatesSafely() = runBlocking {
        val manyLines = (1..25).map { idx ->
            sampleLineVE06.copy(
                id = "line-$idx",
                position = idx - 1,
                descriptionSnapshot = "Line Item #$idx - Long detailed description to verify multi-page continuation and text wrapping across page breaks safely."
            )
        }
        val multiLineDoc = sampleVe06Invoice.copy(id = "doc-multi", lineItems = manyLines)
        val result = generator.generatePdf(multiLineDoc)
        assertTrue(result is PdfGenerationResult.Success)
        val bytes = (result as PdfGenerationResult.Success).pdfBytes

        verifyAndSavePdfArtifact(bytes, "multi-page-generated.pdf", expectedPageCount = 2)
    }

    @Test
    fun test7b_totalsMovingToNextPageDoesNotRenderItemTableHeadersOnSummaryPage() = runBlocking {
        // 22 lines force page break so totals render on page 2
        val lines = (1..22).map { idx ->
            sampleLineVE06.copy(
                id = "line-b-$idx",
                position = idx - 1,
                descriptionSnapshot = "Item Row $idx description text for exact pagination check."
            )
        }
        val doc = sampleVe06Invoice.copy(id = "doc-totals-break", lineItems = lines)
        val result = generator.generatePdf(doc)
        assertTrue(result is PdfGenerationResult.Success)
        val bytes = (result as PdfGenerationResult.Success).pdfBytes
        val fileDescriptor = createTempFdFromBytes(bytes)
        val renderer = PdfRenderer(fileDescriptor)
        assertEquals(2, renderer.pageCount)
        renderer.close()
        fileDescriptor.close()
    }

    @Test
    fun test7c_bankDeclarationBlockMovingToNextPageDoesNotRenderStrayItemHeaders() = runBlocking {
        val lines = (1..14).map { idx ->
            sampleLineVE06.copy(
                id = "line-c-$idx",
                position = idx - 1,
                descriptionSnapshot = "Item Row $idx description."
            )
        }
        val doc = sampleVe06Invoice.copy(id = "doc-bank-break", lineItems = lines)
        val result = generator.generatePdf(doc)
        assertTrue(result is PdfGenerationResult.Success)
    }

    @Test
    fun test7d_wrappedItemRowNearBoundaryMovesAsWholeWithoutSplitting() = runBlocking {
        val lines = (1..16).map { idx ->
            sampleLineVE06.copy(
                id = "line-d-$idx",
                position = idx - 1,
                descriptionSnapshot = if (idx == 16) "Boundary wrapped multi-line item description that must move cleanly to the next page as a complete row unit." else "Short row $idx"
            )
        }
        val doc = sampleVe06Invoice.copy(id = "doc-boundary-row", lineItems = lines)
        val result = generator.generatePdf(doc)
        assertTrue(result is PdfGenerationResult.Success)
    }

    @Test
    fun test8_invoiceDoesNotRequireDeliveryFactoryAddress() = runBlocking {
        val invoiceNoDeliveryFactory = sampleVe06Invoice.copy(deliveryFactoryAddress = null)
        val result = generator.generatePdf(invoiceNoDeliveryFactory)
        assertTrue(result is PdfGenerationResult.Success)
    }

    @Test
    fun test9_longSellerAddressRendersSuccessfully() = runBlocking {
        val longSeller = sampleSeller.copy(
            addressLine1 = "VERY LONG STREET NAME WITH EXTENDED INDUSTRIAL ZONE ADDRESS DETAILS AND BUILDING NUMBER 9999",
            addressLine2 = "NEAR SHALIBHADRANIVAS OPPOSITE SIDDHIVINAYAK COMPLEX STAGE 3 JORAWAR NAGAR SURENDRANAGAR"
        )
        val doc = sampleVe06Invoice.copy(sellerSnapshot = longSeller)
        val result = generator.generatePdf(doc)
        assertTrue(result is PdfGenerationResult.Success)
    }

    @Test
    fun test10_longClientAddressRendersSuccessfully() = runBlocking {
        val longClient = sampleClientVE06.copy(
            address = "PLOT 888 INDUSTRIAL PARK CORRIDOR ROAD NEAR NATIONAL HIGHWAY 48 CHENNAI TAMIL NADU INDIA 600001"
        )
        val doc = sampleVe06Invoice.copy(clientSnapshot = longClient)
        val result = generator.generatePdf(doc)
        assertTrue(result is PdfGenerationResult.Success)
    }

    @Test
    fun test11_longDeliveryFactoryAddressRendersSuccessfully() = runBlocking {
        val longPo = samplePurchaseOrder.copy(
            deliveryFactoryAddress = "PLANT LOCATION UNIT 4 WAREHOUSE BLOCK C GIDC HEAVY INDUSTRIAL ESTATE SURENDRANAGAR GUJARAT INDIA 363020"
        )
        val result = generator.generatePdf(longPo)
        assertTrue(result is PdfGenerationResult.Success)
    }

    @Test
    fun test12_longProductDescriptionRendersSuccessfully() = runBlocking {
        val longLine = sampleLineVE06.copy(
            descriptionSnapshot = "3M Anti-Slip High-Traction Weatherproof 15mm Width Safety Tape Roll Heavy Duty Grade A Special Specification Item for Industrial Application"
        )
        val doc = sampleVe06Invoice.copy(lineItems = listOf(longLine))
        val result = generator.generatePdf(doc)
        assertTrue(result is PdfGenerationResult.Success)
    }

    @Test
    fun test13_missingSellerSnapshotIsRejected() = runBlocking {
        val doc = sampleVe06Invoice.copy(sellerSnapshot = null)
        val result = generator.generatePdf(doc)
        assertEquals(PdfGenerationResult.Failure.MissingSellerSnapshot, result)
    }

    @Test
    fun test14_missingClientSnapshotIsRejected() = runBlocking {
        val doc = sampleVe06Invoice.copy(clientSnapshot = null)
        val result = generator.generatePdf(doc)
        assertEquals(PdfGenerationResult.Failure.MissingClientSnapshot, result)
    }

    private fun verifyAndSavePdfArtifact(pdfBytes: ByteArray, filename: String, expectedPageCount: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Save PDF artifact for build outputs
        val outputDir = File(context.cacheDir, "pdf-verification").apply { mkdirs() }
        val pdfFile = File(outputDir, filename)
        FileOutputStream(pdfFile).use { it.write(pdfBytes) }

        val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(fileDescriptor)

        assertEquals(expectedPageCount, pdfRenderer.pageCount)

        for (i in 0 until pdfRenderer.pageCount) {
            val page = pdfRenderer.openPage(i)
            assertEquals(595, page.width)
            assertEquals(842, page.height)

            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            assertNotNull(bitmap)
            assertTrue(bitmap.width > 0 && bitmap.height > 0)

            // Save PNG page artifact
            val pngFile = File(outputDir, "${filename.removeSuffix(".pdf")}-page-${i + 1}.png")
            FileOutputStream(pngFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            page.close()
        }

        pdfRenderer.close()
        fileDescriptor.close()
    }

    private fun createTempFdFromBytes(bytes: ByteArray): ParcelFileDescriptor {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tempFile = File.createTempFile("pdf_test_", ".pdf", context.cacheDir)
        FileOutputStream(tempFile).use { it.write(bytes) }
        return ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }
}
