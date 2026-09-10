package com.vivaanenterprise.app.core.pdf

import android.graphics.pdf.PdfDocument
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.model.ClientSnapshot
import com.vivaanenterprise.app.domain.model.IndianState
import com.vivaanenterprise.app.domain.model.SellerSnapshot
import com.vivaanenterprise.app.domain.pdf.BusinessDocumentPdfGenerator
import com.vivaanenterprise.app.domain.pdf.PdfGenerationResult
import com.vivaanenterprise.app.domain.util.IndianCurrencyFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native Android implementation of [BusinessDocumentPdfGenerator] using [android.graphics.pdf.PdfDocument].
 * Renders production-grade A4 Tax Invoices and Purchase Orders strictly from frozen historical snapshots without reading current master data.
 */
@Singleton
class AndroidBusinessDocumentPdfGenerator @Inject constructor(
    private val currencyFormatter: IndianCurrencyFormatter
) : BusinessDocumentPdfGenerator {

    private fun createDateFormatter(): SimpleDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }

    override suspend fun generatePdf(document: BusinessDocument): PdfGenerationResult = withContext(Dispatchers.Default) {
        if (document.status != DocumentStatus.FINALIZED) {
            return@withContext PdfGenerationResult.Failure.DocumentNotFinalized
        }
        val seller = document.sellerSnapshot
            ?: return@withContext PdfGenerationResult.Failure.MissingSellerSnapshot
        val client = document.clientSnapshot
            ?: return@withContext PdfGenerationResult.Failure.MissingClientSnapshot
        if (document.lineItems.isEmpty()) {
            return@withContext PdfGenerationResult.Failure.NoLineItems
        }

        val isInterstate = resolveInterstateBranch(document)
            ?: return@withContext PdfGenerationResult.Failure.MissingHistoricalTaxTreatment

        val pdfDoc = PdfDocument()
        try {
            val pageCtx = PdfPageContext(pdfDoc)
            val dateFormatter = createDateFormatter()
            val isInvoice = document.documentType == DocumentType.TAX_INVOICE

            // 1. Top Brand Header with VE Monogram Logo & Document Title Badge
            drawHeaderBlock(pageCtx, document, seller, isInvoice)

            // 2. Metadata Grid Block (Invoice/PO Number, Date, Place of Supply, Buyer Order, etc.)
            drawMetadataGrid(pageCtx, document, isInvoice, dateFormatter)

            // 3. Party Details (Bill To / Supplier & Delivery / Factory Address)
            drawPartyAndDeliveryBlock(pageCtx, document, client, isInvoice)

            // 4. Line Items Table
            drawLineItemsTable(pageCtx, document, isInvoice)

            // 5. Totals & Tax Summary Block
            drawTotalsAndTaxSummary(pageCtx, document, isInterstate)

            // 6. Bank Details, Declaration & Authorised Signatory Block
            drawBankAndDeclarationBlock(pageCtx, seller, isInvoice)

            // 7. Footer
            drawFooter(pageCtx, isInvoice)

            pageCtx.finish()

            val stream = ByteArrayOutputStream()
            pdfDoc.writeTo(stream)
            PdfGenerationResult.Success(stream.toByteArray())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            PdfGenerationResult.Failure.Error(e)
        } finally {
            try {
                pdfDoc.close()
            } catch (_: Exception) {}
        }
    }

    private fun resolveInterstateBranch(document: BusinessDocument): Boolean? {
        val hasIgst = document.igstAmountPaise > 0L || document.lineItems.any { it.igstAmountPaise > 0L }
        val hasCgstSgst = document.cgstAmountPaise > 0L || document.sgstAmountPaise > 0L ||
                document.lineItems.any { it.cgstAmountPaise > 0L || it.sgstAmountPaise > 0L }

        // Reject corrupted contradictory data
        if (hasIgst && hasCgstSgst) return null

        if (document.taxTreatment != null) {
            val isInterstate = document.taxTreatment == com.vivaanenterprise.app.domain.model.TaxTreatment.INTER_STATE
            if (isInterstate && hasCgstSgst) return null
            if (!isInterstate && hasIgst) return null
            return isInterstate
        }

        if (hasIgst) return true
        if (hasCgstSgst) return false

        // Unambiguous tax branch cannot be determined for legacy document with zero tax amounts and no persisted taxTreatment
        return null
    }

    // ── Structural Layout Blocks ───────────────────────────────────────────────

    /**
     * Top header section with native VE Monogram Vector Logo, Seller Identity, and Document Title.
     * Operates strictly in TOP -> BOTTOM coordinate space. Measures all content first before drawing section frame.
     */
    private fun drawHeaderBlock(
        ctx: PdfPageContext,
        doc: BusinessDocument,
        seller: SellerSnapshot,
        isInvoice: Boolean
    ) {
        val startY = ctx.currentY
        val leftX = PdfPageContext.CONTENT_LEFT
        val rightX = PdfPageContext.CONTENT_RIGHT
        val width = PdfPageContext.CONTENT_WIDTH

        // Logo coordinates & size
        val logoSize = 36f
        val logoX = leftX + PdfPageContext.TOKEN_PADDING_MEDIUM
        val logoY = startY + PdfPageContext.TOKEN_PADDING_MEDIUM

        val sellerTextX = logoX + logoSize + 12f
        val sellerTextWidth = (width * 0.55f - logoSize - 20f).toInt()

        val fullAddress = buildString {
            if (seller.addressLine1.isNotBlank()) append(seller.addressLine1)
            if (seller.addressLine2.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append(seller.addressLine2)
            }
            if (seller.cityStatePincode.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append(seller.cityStatePincode)
            }
        }

        // --- PASS 1: Measurement ---
        val titleFontHeight = ctx.measureSingleLineHeight(ctx.textPaintBrandTitle)
        val addressHeight = ctx.measureWrappedTextHeight(fullAddress, ctx.textPaintRegular, sellerTextWidth)
        val gstinHeight = if (seller.gstin.isNotBlank()) ctx.measureSingleLineHeight(ctx.textPaintBold) else 0f
        val mobileHeight = if (seller.mobile.isNotBlank()) ctx.measureSingleLineHeight(ctx.textPaintRegular) else 0f

        var requiredTextHeight = titleFontHeight + PdfPageContext.TOKEN_METADATA_GAP
        if (addressHeight > 0f) {
            requiredTextHeight += addressHeight + PdfPageContext.TOKEN_METADATA_GAP
        }
        if (gstinHeight > 0f) {
            requiredTextHeight += gstinHeight + PdfPageContext.TOKEN_METADATA_GAP
        }
        if (mobileHeight > 0f) {
            requiredTextHeight += mobileHeight + PdfPageContext.TOKEN_METADATA_GAP
        }

        val logoRequiredHeight = logoSize + (PdfPageContext.TOKEN_PADDING_MEDIUM * 2)
        val docTitleFontHeight = ctx.measureSingleLineHeight(ctx.textPaintDocTitle)
        val docSubTitleFontHeight = ctx.measureSingleLineHeight(ctx.textPaintSmall)
        val rightTitleRequiredHeight = docTitleFontHeight + PdfPageContext.TOKEN_METADATA_GAP + docSubTitleFontHeight + (PdfPageContext.TOKEN_PADDING_MEDIUM * 2)

        val headerContentHeight = maxOf(requiredTextHeight, logoRequiredHeight, rightTitleRequiredHeight)
        val headerTotalHeight = maxOf(headerContentHeight + (PdfPageContext.TOKEN_PADDING_MEDIUM * 2), 68f)

        // --- PASS 2: Drawing ---
        // 1. Draw outer section border box
        ctx.canvas.drawRect(leftX, startY, rightX, startY + headerTotalHeight, ctx.paintLine)

        // 2. Draw VE Monogram Logo
        ctx.drawVeLogo(logoX, logoY, logoSize)

        // 3. Draw Seller Details (TOP-in, BOTTOM-out)
        var topY = startY + PdfPageContext.TOKEN_PADDING_MEDIUM
        topY = ctx.drawSingleLineFromTop(seller.businessName, sellerTextX, topY, ctx.textPaintBrandTitle)
        topY += PdfPageContext.TOKEN_METADATA_GAP

        if (fullAddress.isNotBlank()) {
            topY = ctx.drawWrappedTextFromTop(fullAddress, sellerTextX, topY, ctx.textPaintRegular, sellerTextWidth)
            topY += PdfPageContext.TOKEN_METADATA_GAP
        }

        if (seller.gstin.isNotBlank()) {
            topY = ctx.drawSingleLineFromTop("GSTIN/UIN: ${seller.gstin}", sellerTextX, topY, ctx.textPaintBold)
            topY += PdfPageContext.TOKEN_METADATA_GAP
        }

        if (seller.mobile.isNotBlank()) {
            topY = ctx.drawSingleLineFromTop("Mobile: ${seller.mobile}", sellerTextX, topY, ctx.textPaintRegular)
            topY += PdfPageContext.TOKEN_METADATA_GAP
        }

        // 4. Draw Right-Side Document Title Badge
        val titleText = if (isInvoice) "TAX INVOICE" else "PURCHASE ORDER"
        val titleX = rightX - PdfPageContext.TOKEN_PADDING_MEDIUM
        var rightTopY = startY + PdfPageContext.TOKEN_PADDING_MEDIUM + 4f

        rightTopY = ctx.drawSingleLineRightAlignedFromTop(titleText, titleX, rightTopY, ctx.textPaintDocTitle)
        rightTopY += PdfPageContext.TOKEN_METADATA_GAP
        ctx.drawSingleLineRightAlignedFromTop("Original for Recipient", titleX, rightTopY, ctx.textPaintSmall)

        ctx.currentY = startY + headerTotalHeight
    }

    /**
     * Continuation page header for multi-page document pagination.
     */
    private fun drawContinuationHeader(ctx: PdfPageContext, docNumber: String, isInvoice: Boolean) {
        val startY = ctx.currentY
        val leftX = PdfPageContext.CONTENT_LEFT
        val rightX = PdfPageContext.CONTENT_RIGHT
        val headerTitle = if (isInvoice) "TAX INVOICE" else "PURCHASE ORDER"
        val headerHeight = 18f

        ctx.canvas.drawRect(leftX, startY, rightX, startY + headerHeight, ctx.paintFillHeader)
        ctx.canvas.drawRect(leftX, startY, rightX, startY + headerHeight, ctx.paintLine)

        val topY = startY + 3f
        ctx.drawSingleLineFromTop("$headerTitle — $docNumber (Contd.)", leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, topY, ctx.textPaintHeaderLabel)
        ctx.drawSingleLineRightAlignedFromTop("Page ${ctx.pageNumber}", rightX - PdfPageContext.TOKEN_PADDING_MEDIUM, topY, ctx.textPaintHeaderLabel)

        ctx.currentY = startY + headerHeight
    }

    /**
     * Structured document metadata grid.
     */
    private fun drawMetadataGrid(
        ctx: PdfPageContext,
        doc: BusinessDocument,
        isInvoice: Boolean,
        dateFormatter: SimpleDateFormat
    ) {
        val startY = ctx.currentY
        val leftX = PdfPageContext.CONTENT_LEFT
        val rightX = PdfPageContext.CONTENT_RIGHT
        val width = PdfPageContext.CONTENT_WIDTH
        val colWidth = width / 2f
        val midX = leftX + colWidth

        val leftItems = mutableListOf<Pair<String, String>>()
        val rightItems = mutableListOf<Pair<String, String>>()

        val docNumLabel = if (isInvoice) "Invoice No." else "PO No."
        leftItems.add(docNumLabel to doc.documentNumber)
        leftItems.add("Dated" to dateFormatter.format(Date(doc.documentDate)))

        // Strict audit: Place of supply uses persisted doc.placeOfSupply without fallback to seller state
        val stateCode = doc.placeOfSupply ?: ""
        val posName = IndianState.ALL_STATES.find { it.code == stateCode }?.name
        val posDisplay = if (!posName.isNullOrBlank()) "$posName ($stateCode)" else stateCode
        if (posDisplay.isNotBlank()) {
            leftItems.add("Place of Supply" to posDisplay)
        }

        if (!doc.deliveryNote.isNullOrBlank()) {
            rightItems.add("Delivery Note" to doc.deliveryNote)
        }
        if (!doc.paymentTerms.isNullOrBlank()) {
            rightItems.add("Payment Terms" to doc.paymentTerms)
        }
        if (!doc.supplierReference.isNullOrBlank()) {
            rightItems.add("Supplier Ref." to doc.supplierReference)
        }
        if (!doc.buyerOrderNumber.isNullOrBlank()) {
            rightItems.add("Buyer Order No." to doc.buyerOrderNumber)
        }
        if (doc.buyerOrderDate != null && doc.buyerOrderDate > 0L) {
            rightItems.add("Order Date" to dateFormatter.format(Date(doc.buyerOrderDate)))
        }
        if (!doc.destination.isNullOrBlank()) {
            rightItems.add("Destination" to doc.destination)
        }

        val rowHeight = maxOf(ctx.measureSingleLineHeight(ctx.textPaintBold), ctx.measureSingleLineHeight(ctx.textPaintRegular)) + PdfPageContext.TOKEN_METADATA_GAP
        val leftTotalHeight = leftItems.size * rowHeight
        val rightTotalHeight = rightItems.size * rowHeight
        val gridHeight = maxOf(leftTotalHeight, rightTotalHeight, 28f) + (PdfPageContext.TOKEN_PADDING_MEDIUM * 2)

        ctx.canvas.drawRect(leftX, startY, rightX, startY + gridHeight, ctx.paintLine)
        if (rightItems.isNotEmpty()) {
            ctx.canvas.drawLine(midX, startY, midX, startY + gridHeight, ctx.paintLine)
        }

        var leftTop = startY + PdfPageContext.TOKEN_PADDING_MEDIUM
        leftItems.forEach { (label, value) ->
            ctx.drawSingleLineFromTop("$label:", leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, leftTop, ctx.textPaintBold)
            ctx.drawSingleLineFromTop(value, leftX + 90f, leftTop, ctx.textPaintRegular)
            leftTop += rowHeight
        }

        var rightTop = startY + PdfPageContext.TOKEN_PADDING_MEDIUM
        rightItems.forEach { (label, value) ->
            ctx.drawSingleLineFromTop("$label:", midX + PdfPageContext.TOKEN_PADDING_MEDIUM, rightTop, ctx.textPaintBold)
            ctx.drawSingleLineFromTop(value, midX + 90f, rightTop, ctx.textPaintRegular)
            rightTop += rowHeight
        }

        ctx.currentY = startY + gridHeight
    }

    /**
     * Buyer / Supplier details block and Delivery / Factory Address.
     */
    private fun drawPartyAndDeliveryBlock(
        ctx: PdfPageContext,
        doc: BusinessDocument,
        client: ClientSnapshot,
        isInvoice: Boolean
    ) {
        val startY = ctx.currentY
        val leftX = PdfPageContext.CONTENT_LEFT
        val rightX = PdfPageContext.CONTENT_RIGHT
        val width = PdfPageContext.CONTENT_WIDTH

        val hasDeliveryAddr = !doc.deliveryFactoryAddress.isNullOrBlank()
        val halfWidth = if (hasDeliveryAddr) width / 2f else width
        val midX = leftX + halfWidth

        // Subheader Banners
        val partyHeaderTitle = if (isInvoice) "BILL TO (BUYER DETAILS)" else "SUPPLIER / VENDOR DETAILS"
        val bannerHeight = 16f

        ctx.canvas.drawRect(leftX, startY, rightX, startY + bannerHeight, ctx.paintFillHeader)
        ctx.canvas.drawRect(leftX, startY, rightX, startY + bannerHeight, ctx.paintLine)
        ctx.drawSingleLineFromTop(partyHeaderTitle, leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, startY + 3f, ctx.textPaintHeaderLabel)

        if (hasDeliveryAddr) {
            ctx.canvas.drawLine(midX, startY, midX, startY + bannerHeight, ctx.paintLine)
            ctx.drawSingleLineFromTop("DELIVERY / FACTORY ADDRESS", midX + PdfPageContext.TOKEN_PADDING_MEDIUM, startY + 3f, ctx.textPaintHeaderLabel)
        }

        val partyTextWidth = (halfWidth - (PdfPageContext.TOKEN_PADDING_MEDIUM * 2)).toInt()

        // --- PASS 1: Calculate Party & Delivery heights ---
        var buyerHeight = ctx.measureSingleLineHeight(ctx.textPaintBold) + PdfPageContext.TOKEN_METADATA_GAP
        if (!client.address.isNullOrBlank()) {
            buyerHeight += ctx.measureWrappedTextHeight(client.address, ctx.textPaintRegular, partyTextWidth) + PdfPageContext.TOKEN_METADATA_GAP
        }

        val stateName = client.state ?: IndianState.ALL_STATES.find { it.code == client.stateCode }?.name ?: ""
        val stateLine = if (stateName.isNotBlank() || !client.stateCode.isNullOrBlank()) {
            "State: $stateName ${if (!client.stateCode.isNullOrBlank()) "(${client.stateCode})" else ""}"
        } else ""

        if (stateLine.isNotBlank()) {
            buyerHeight += ctx.measureSingleLineHeight(ctx.textPaintRegular) + PdfPageContext.TOKEN_METADATA_GAP
        }
        if (!client.gstin.isNullOrBlank()) {
            buyerHeight += ctx.measureSingleLineHeight(ctx.textPaintBold) + PdfPageContext.TOKEN_METADATA_GAP
        }

        val deliveryHeight = if (hasDeliveryAddr) {
            ctx.measureWrappedTextHeight(doc.deliveryFactoryAddress!!, ctx.textPaintRegular, partyTextWidth) + PdfPageContext.TOKEN_METADATA_GAP
        } else 0f

        val bodyHeight = maxOf(buyerHeight, deliveryHeight, 45f) + (PdfPageContext.TOKEN_PADDING_MEDIUM * 2)
        val totalBlockHeight = bannerHeight + bodyHeight

        // --- PASS 2: Draw Party & Delivery Section ---
        ctx.canvas.drawRect(leftX, startY, rightX, startY + totalBlockHeight, ctx.paintLine)
        if (hasDeliveryAddr) {
            ctx.canvas.drawLine(midX, startY + bannerHeight, midX, startY + totalBlockHeight, ctx.paintLine)
        }

        var partyTop = startY + bannerHeight + PdfPageContext.TOKEN_PADDING_MEDIUM
        partyTop = ctx.drawSingleLineFromTop(client.companyName, leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, partyTop, ctx.textPaintBold)
        partyTop += PdfPageContext.TOKEN_METADATA_GAP

        if (!client.address.isNullOrBlank()) {
            partyTop = ctx.drawWrappedTextFromTop(client.address, leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, partyTop, ctx.textPaintRegular, partyTextWidth)
            partyTop += PdfPageContext.TOKEN_METADATA_GAP
        }

        if (stateLine.isNotBlank()) {
            partyTop = ctx.drawSingleLineFromTop(stateLine, leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, partyTop, ctx.textPaintRegular)
            partyTop += PdfPageContext.TOKEN_METADATA_GAP
        }

        if (!client.gstin.isNullOrBlank()) {
            ctx.drawSingleLineFromTop("GSTIN/UIN: ${client.gstin}", leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, partyTop, ctx.textPaintBold)
        }

        if (hasDeliveryAddr) {
            var delTop = startY + bannerHeight + PdfPageContext.TOKEN_PADDING_MEDIUM
            ctx.drawWrappedTextFromTop(doc.deliveryFactoryAddress!!, midX + PdfPageContext.TOKEN_PADDING_MEDIUM, delTop, ctx.textPaintRegular, partyTextWidth)
        }

        ctx.currentY = startY + totalBlockHeight
    }

    /**
     * Line Items table with dynamic height measurement and multi-page handling.
     */
    private fun drawLineItemsTable(
        ctx: PdfPageContext,
        doc: BusinessDocument,
        isInvoice: Boolean
    ) {
        val leftX = PdfPageContext.CONTENT_LEFT
        val rightX = PdfPageContext.CONTENT_RIGHT
        val tableWidth = PdfPageContext.CONTENT_WIDTH

        // Column widths
        val colSl = 30f
        val colHsn = 55f
        val colQty = 45f
        val colRate = 75f
        val colAmount = 85f
        val colDesc = tableWidth - (colSl + colHsn + colQty + colRate + colAmount)

        val xSl = leftX
        val xDesc = xSl + colSl
        val xHsn = xDesc + colDesc
        val xQty = xHsn + colHsn
        val xRate = xQty + colQty
        val xAmount = xRate + colRate

        val drawTableHeaders = {
            if (ctx.pageNumber > 1) {
                drawContinuationHeader(ctx, doc.documentNumber, isInvoice)
            }

            val startY = ctx.currentY
            val headerHeight = 18f
            ctx.canvas.drawRect(leftX, startY, rightX, startY + headerHeight, ctx.paintFillHeader)
            ctx.canvas.drawRect(leftX, startY, rightX, startY + headerHeight, ctx.paintLine)

            val topY = startY + 3f
            ctx.drawSingleLineCenteredFromTop("Sr.", xSl + colSl / 2f, topY, ctx.textPaintHeaderLabel)
            ctx.drawSingleLineFromTop("Description of Goods / Services", xDesc + 4f, topY, ctx.textPaintHeaderLabel)
            ctx.drawSingleLineCenteredFromTop("HSN/SAC", xHsn + colHsn / 2f, topY, ctx.textPaintHeaderLabel)
            ctx.drawSingleLineRightAlignedFromTop("Qty", xQty + colQty - 4f, topY, ctx.textPaintHeaderLabel)
            ctx.drawSingleLineRightAlignedFromTop("Rate (₹)", xRate + colRate - 4f, topY, ctx.textPaintHeaderLabel)
            ctx.drawSingleLineRightAlignedFromTop("Taxable Amt (₹)", rightX - 4f, topY, ctx.textPaintHeaderLabel)

            // Vertical Column Separators
            ctx.canvas.drawLine(xDesc, startY, xDesc, startY + headerHeight, ctx.paintLine)
            ctx.canvas.drawLine(xHsn, startY, xHsn, startY + headerHeight, ctx.paintLine)
            ctx.canvas.drawLine(xQty, startY, xQty, startY + headerHeight, ctx.paintLine)
            ctx.canvas.drawLine(xRate, startY, xRate, startY + headerHeight, ctx.paintLine)
            ctx.canvas.drawLine(xAmount, startY, xAmount, startY + headerHeight, ctx.paintLine)

            ctx.currentY = startY + headerHeight
        }

        drawTableHeaders()

        doc.lineItems.forEachIndexed { index, item ->
            val descHeight = ctx.measureWrappedTextHeight(item.descriptionSnapshot, ctx.textPaintRegular, (colDesc - 8).toInt())
            val rowHeight = maxOf(descHeight + 8f, 18f)

            ctx.ensureSpace(rowHeight, onNewPageHeader = drawTableHeaders)

            val rowStartY = ctx.currentY
            val textTopY = rowStartY + 4f

            ctx.drawSingleLineCenteredFromTop((index + 1).toString(), xSl + colSl / 2f, textTopY, ctx.textPaintRegular)
            ctx.drawWrappedTextFromTop(item.descriptionSnapshot, xDesc + 4f, textTopY, ctx.textPaintRegular, (colDesc - 8).toInt())
            ctx.drawSingleLineCenteredFromTop(item.hsnSacSnapshot ?: "", xHsn + colHsn / 2f, textTopY, ctx.textPaintRegular)
            ctx.drawSingleLineRightAlignedFromTop(item.quantity.toString(), xQty + colQty - 4f, textTopY, ctx.textPaintRegular)
            ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(item.ratePaise), xRate + colRate - 4f, textTopY, ctx.textPaintRegular)
            ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(item.taxableAmountPaise), rightX - 4f, textTopY, ctx.textPaintBold)

            // Draw Row Boundaries & Vertical Grid Separators
            ctx.canvas.drawRect(leftX, rowStartY, rightX, rowStartY + rowHeight, ctx.paintThinLine)
            ctx.canvas.drawLine(xDesc, rowStartY, xDesc, rowStartY + rowHeight, ctx.paintThinLine)
            ctx.canvas.drawLine(xHsn, rowStartY, xHsn, rowStartY + rowHeight, ctx.paintThinLine)
            ctx.canvas.drawLine(xQty, rowStartY, xQty, rowStartY + rowHeight, ctx.paintThinLine)
            ctx.canvas.drawLine(xRate, rowStartY, xRate, rowStartY + rowHeight, ctx.paintThinLine)
            ctx.canvas.drawLine(xAmount, rowStartY, xAmount, rowStartY + rowHeight, ctx.paintThinLine)

            ctx.currentY = rowStartY + rowHeight
        }

        // Draw solid bottom border for line items table
        ctx.canvas.drawLine(leftX, ctx.currentY, rightX, ctx.currentY, ctx.paintLine)
    }

    /**
     * Financial totals, amount in words, and tax summary block.
     */
    private fun drawTotalsAndTaxSummary(
        ctx: PdfPageContext,
        doc: BusinessDocument,
        isInterstate: Boolean
    ) {
        val leftX = PdfPageContext.CONTENT_LEFT
        val rightX = PdfPageContext.CONTENT_RIGHT
        val width = PdfPageContext.CONTENT_WIDTH

        val wordsText = doc.amountInWords ?: currencyFormatter.formatAmountInWords(doc.grandTotalPaise)
        val wordHeight = ctx.measureWrappedTextHeight(wordsText, ctx.textPaintBold, (width - 16).toInt())
        val totalsHeight = 110f + wordHeight

        ctx.ensureSpace(totalsHeight)

        val startY = ctx.currentY
        var topY = startY + PdfPageContext.TOKEN_PADDING_MEDIUM

        // Taxable Value
        ctx.drawSingleLineRightAlignedFromTop("Taxable Amount:", rightX - 130f, topY, ctx.textPaintRegular)
        ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(doc.taxableAmountPaise), rightX - 8f, topY, ctx.textPaintBold)
        topY += 14f

        if (isInterstate) {
            ctx.drawSingleLineRightAlignedFromTop("IGST Amount:", rightX - 130f, topY, ctx.textPaintRegular)
            ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(doc.igstAmountPaise), rightX - 8f, topY, ctx.textPaintRegular)
            topY += 14f
        } else {
            ctx.drawSingleLineRightAlignedFromTop("CGST Amount:", rightX - 130f, topY, ctx.textPaintRegular)
            ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(doc.cgstAmountPaise), rightX - 8f, topY, ctx.textPaintRegular)
            topY += 14f
            ctx.drawSingleLineRightAlignedFromTop("SGST Amount:", rightX - 130f, topY, ctx.textPaintRegular)
            ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(doc.sgstAmountPaise), rightX - 8f, topY, ctx.textPaintRegular)
            topY += 14f
        }

        ctx.drawSingleLineRightAlignedFromTop("Total Tax Amount:", rightX - 130f, topY, ctx.textPaintRegular)
        ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(doc.totalTaxAmountPaise), rightX - 8f, topY, ctx.textPaintBold)
        topY += 16f

        // Grand Total Row with prominent visual emphasis
        ctx.drawSingleLineRightAlignedFromTop("GRAND TOTAL:", rightX - 130f, topY, ctx.textPaintDocTitle)
        ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(doc.grandTotalPaise), rightX - 8f, topY, ctx.textPaintDocTitle)
        topY += 18f

        ctx.drawSingleLineFromTop("Amount Chargeable (in words):", leftX + 8f, topY, ctx.textPaintBold)
        topY += 14f
        topY = ctx.drawWrappedTextFromTop(wordsText, leftX + 8f, topY, ctx.textPaintBold, (width - 16).toInt())
        topY += PdfPageContext.TOKEN_PADDING_MEDIUM

        val boxHeight = topY - startY
        ctx.canvas.drawRect(leftX, startY, rightX, startY + boxHeight, ctx.paintLine)

        ctx.currentY = startY + boxHeight

        // Render HSN Tax Summary Table
        drawTaxSummaryTable(ctx, doc, isInterstate)
    }

    /**
     * HSN/SAC Tax breakdown summary table.
     */
    private fun drawTaxSummaryTable(
        ctx: PdfPageContext,
        doc: BusinessDocument,
        isInterstate: Boolean
    ) {
        val leftX = PdfPageContext.CONTENT_LEFT
        val rightX = PdfPageContext.CONTENT_RIGHT
        val tableWidth = PdfPageContext.CONTENT_WIDTH

        val groupedLines = doc.lineItems.groupBy { it.hsnSacSnapshot ?: "" }
        val numRows = groupedLines.size
        val taxWords = doc.taxAmountInWords ?: currencyFormatter.formatAmountInWords(doc.totalTaxAmountPaise)
        val taxWordsHeight = ctx.measureWrappedTextHeight("Tax Amount (in words): $taxWords", ctx.textPaintBold, (tableWidth - 16).toInt())
        val estimatedHeight = 18f + (numRows * 18f) + taxWordsHeight + 12f

        ctx.ensureSpace(estimatedHeight)

        val startY = ctx.currentY
        val headerHeight = 18f

        if (isInterstate) {
            val colHsn = 115f
            val colTaxable = 140f
            val colIgstRate = 80f
            val colIgstAmt = 100f
            val colTotalTax = tableWidth - (colHsn + colTaxable + colIgstRate + colIgstAmt)

            val xHsn = leftX
            val xTaxable = xHsn + colHsn
            val xRate = xTaxable + colTaxable
            val xAmt = xRate + colIgstRate
            val xTotal = xAmt + colIgstAmt

            val drawHeaders = {
                val hY = ctx.currentY
                ctx.canvas.drawRect(leftX, hY, rightX, hY + headerHeight, ctx.paintFillHeader)
                ctx.canvas.drawRect(leftX, hY, rightX, hY + headerHeight, ctx.paintLine)

                val topY = hY + 3f
                ctx.drawSingleLineCenteredFromTop("HSN/SAC", xHsn + colHsn / 2f, topY, ctx.textPaintHeaderLabel)
                ctx.drawSingleLineRightAlignedFromTop("Taxable Value", xTaxable + colTaxable - 4f, topY, ctx.textPaintHeaderLabel)
                ctx.drawSingleLineRightAlignedFromTop("IGST Rate", xRate + colIgstRate - 4f, topY, ctx.textPaintHeaderLabel)
                ctx.drawSingleLineRightAlignedFromTop("IGST Amt", xAmt + colIgstAmt - 4f, topY, ctx.textPaintHeaderLabel)
                ctx.drawSingleLineRightAlignedFromTop("Total Tax", rightX - 4f, topY, ctx.textPaintHeaderLabel)

                ctx.canvas.drawLine(xTaxable, hY, xTaxable, hY + headerHeight, ctx.paintLine)
                ctx.canvas.drawLine(xRate, hY, xRate, hY + headerHeight, ctx.paintLine)
                ctx.canvas.drawLine(xAmt, hY, xAmt, hY + headerHeight, ctx.paintLine)
                ctx.canvas.drawLine(xTotal, hY, xTotal, hY + headerHeight, ctx.paintLine)

                ctx.currentY = hY + headerHeight
            }

            drawHeaders()

            groupedLines.forEach { (hsn, lines) ->
                val taxableSum = lines.fold(0L) { acc, item -> acc + item.taxableAmountPaise }
                val igstSum = lines.fold(0L) { acc, item -> acc + item.igstAmountPaise }
                val totalTaxSum = lines.fold(0L) { acc, item -> acc + item.totalTaxPaise }
                val rateStr = lines.firstOrNull()?.let { PdfFormattingUtils.formatGstRateBasisPoints(it.gstRateBasisPoints) } ?: "0%"

                ctx.ensureSpace(18f, onNewPageHeader = drawHeaders)
                val rowY = ctx.currentY
                val topY = rowY + 3f

                ctx.drawSingleLineCenteredFromTop(hsn, xHsn + colHsn / 2f, topY, ctx.textPaintRegular)
                ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(taxableSum), xTaxable + colTaxable - 4f, topY, ctx.textPaintRegular)
                ctx.drawSingleLineRightAlignedFromTop(rateStr, xRate + colIgstRate - 4f, topY, ctx.textPaintRegular)
                ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(igstSum), xAmt + colIgstAmt - 4f, topY, ctx.textPaintRegular)
                ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(totalTaxSum), rightX - 4f, topY, ctx.textPaintRegular)

                ctx.canvas.drawRect(leftX, rowY, rightX, rowY + 18f, ctx.paintThinLine)
                ctx.canvas.drawLine(xTaxable, rowY, xTaxable, rowY + 18f, ctx.paintThinLine)
                ctx.canvas.drawLine(xRate, rowY, xRate, rowY + 18f, ctx.paintThinLine)
                ctx.canvas.drawLine(xAmt, rowY, xAmt, rowY + 18f, ctx.paintThinLine)
                ctx.canvas.drawLine(xTotal, rowY, xTotal, rowY + 18f, ctx.paintThinLine)

                ctx.currentY = rowY + 18f
            }
        } else { // INTRA_STATE (CGST + SGST)
            val colHsn = 85f
            val colTaxable = 110f
            val colCgstRate = 55f
            val colCgstAmt = 75f
            val colSgstRate = 55f
            val colSgstAmt = 75f
            val colTotalTax = tableWidth - (colHsn + colTaxable + colCgstRate + colCgstAmt + colSgstRate + colSgstAmt)

            val xHsn = leftX
            val xTaxable = xHsn + colHsn
            val xCgstRate = xTaxable + colTaxable
            val xCgstAmt = xCgstRate + colCgstRate
            val xSgstRate = xCgstAmt + colCgstAmt
            val xSgstAmt = xSgstRate + colSgstRate
            val xTotal = xSgstAmt + colSgstAmt

            val drawHeaders = {
                val hY = ctx.currentY
                ctx.canvas.drawRect(leftX, hY, rightX, hY + headerHeight, ctx.paintFillHeader)
                ctx.canvas.drawRect(leftX, hY, rightX, hY + headerHeight, ctx.paintLine)

                val topY = hY + 3f
                ctx.drawSingleLineCenteredFromTop("HSN/SAC", xHsn + colHsn / 2f, topY, ctx.textPaintHeaderLabel)
                ctx.drawSingleLineRightAlignedFromTop("Taxable Value", xTaxable + colTaxable - 4f, topY, ctx.textPaintHeaderLabel)
                ctx.drawSingleLineRightAlignedFromTop("CGST Rate", xCgstRate + colCgstRate - 4f, topY, ctx.textPaintHeaderLabel)
                ctx.drawSingleLineRightAlignedFromTop("CGST Amt", xCgstAmt + colCgstAmt - 4f, topY, ctx.textPaintHeaderLabel)
                ctx.drawSingleLineRightAlignedFromTop("SGST Rate", xSgstRate + colSgstRate - 4f, topY, ctx.textPaintHeaderLabel)
                ctx.drawSingleLineRightAlignedFromTop("SGST Amt", xSgstAmt + colSgstAmt - 4f, topY, ctx.textPaintHeaderLabel)
                ctx.drawSingleLineRightAlignedFromTop("Total Tax", rightX - 4f, topY, ctx.textPaintHeaderLabel)

                ctx.canvas.drawLine(xTaxable, hY, xTaxable, hY + headerHeight, ctx.paintLine)
                ctx.canvas.drawLine(xCgstRate, hY, xCgstRate, hY + headerHeight, ctx.paintLine)
                ctx.canvas.drawLine(xCgstAmt, hY, xCgstAmt, hY + headerHeight, ctx.paintLine)
                ctx.canvas.drawLine(xSgstRate, hY, xSgstRate, hY + headerHeight, ctx.paintLine)
                ctx.canvas.drawLine(xSgstAmt, hY, xSgstAmt, hY + headerHeight, ctx.paintLine)
                ctx.canvas.drawLine(xTotal, hY, xTotal, hY + headerHeight, ctx.paintLine)

                ctx.currentY = hY + headerHeight
            }

            drawHeaders()

            groupedLines.forEach { (hsn, lines) ->
                val taxableSum = lines.fold(0L) { acc, item -> acc + item.taxableAmountPaise }
                val cgstSum = lines.fold(0L) { acc, item -> acc + item.cgstAmountPaise }
                val sgstSum = lines.fold(0L) { acc, item -> acc + item.sgstAmountPaise }
                val totalTaxSum = lines.fold(0L) { acc, item -> acc + item.totalTaxPaise }
                val halfRateBasis = (lines.firstOrNull()?.gstRateBasisPoints ?: 0) / 2
                val rateStr = PdfFormattingUtils.formatGstRateBasisPoints(halfRateBasis)

                ctx.ensureSpace(18f, onNewPageHeader = drawHeaders)
                val rowY = ctx.currentY
                val topY = rowY + 3f

                ctx.drawSingleLineCenteredFromTop(hsn, xHsn + colHsn / 2f, topY, ctx.textPaintRegular)
                ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(taxableSum), xTaxable + colTaxable - 4f, topY, ctx.textPaintRegular)
                ctx.drawSingleLineRightAlignedFromTop(rateStr, xCgstRate + colCgstRate - 4f, topY, ctx.textPaintRegular)
                ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(cgstSum), xCgstAmt + colCgstAmt - 4f, topY, ctx.textPaintRegular)
                ctx.drawSingleLineRightAlignedFromTop(rateStr, xSgstRate + colSgstRate - 4f, topY, ctx.textPaintRegular)
                ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(sgstSum), xSgstAmt + colSgstAmt - 4f, topY, ctx.textPaintRegular)
                ctx.drawSingleLineRightAlignedFromTop(PdfFormattingUtils.formatPaiseToCurrency(totalTaxSum), rightX - 4f, topY, ctx.textPaintRegular)

                ctx.canvas.drawRect(leftX, rowY, rightX, rowY + 18f, ctx.paintThinLine)
                ctx.canvas.drawLine(xTaxable, rowY, xTaxable, rowY + 18f, ctx.paintThinLine)
                ctx.canvas.drawLine(xCgstRate, rowY, xCgstRate, rowY + 18f, ctx.paintThinLine)
                ctx.canvas.drawLine(xCgstAmt, rowY, xCgstAmt, rowY + 18f, ctx.paintThinLine)
                ctx.canvas.drawLine(xSgstRate, rowY, xSgstRate, rowY + 18f, ctx.paintThinLine)
                ctx.canvas.drawLine(xSgstAmt, rowY, xSgstAmt, rowY + 18f, ctx.paintThinLine)
                ctx.canvas.drawLine(xTotal, rowY, xTotal, rowY + 18f, ctx.paintThinLine)

                ctx.currentY = rowY + 18f
            }
        }

        // Tax Amount in Words Box (wrapped to prevent horizontal overflow)
        val wordBoxStartY = ctx.currentY
        val taxWordsText = "Tax Amount (in words): $taxWords"
        var wordTopY = wordBoxStartY + PdfPageContext.TOKEN_PADDING_SMALL
        wordTopY = ctx.drawWrappedTextFromTop(taxWordsText, leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, wordTopY, ctx.textPaintBold, (tableWidth - 16).toInt())
        wordTopY += PdfPageContext.TOKEN_PADDING_SMALL

        val wordBoxHeight = maxOf(wordTopY - wordBoxStartY, 18f)
        ctx.canvas.drawRect(leftX, wordBoxStartY, rightX, wordBoxStartY + wordBoxHeight, ctx.paintLine)
        ctx.currentY = wordBoxStartY + wordBoxHeight
    }

    /**
     * Bank Details, Declaration, and Authorised Signatory block.
     * Enforces strict multi-line text wrapping within the left column width so text never crosses the vertical divider.
     */
    private fun drawBankAndDeclarationBlock(
        ctx: PdfPageContext,
        seller: SellerSnapshot,
        isInvoice: Boolean
    ) {
        val leftX = PdfPageContext.CONTENT_LEFT
        val rightX = PdfPageContext.CONTENT_RIGHT
        val width = PdfPageContext.CONTENT_WIDTH
        val halfWidth = width / 2f
        val midX = leftX + halfWidth
        val leftColumnTextWidth = (halfWidth - (PdfPageContext.TOKEN_PADDING_MEDIUM * 2)).toInt()

        // --- PASS 1: Measurement ---
        var leftContentHeight = ctx.measureSingleLineHeight(ctx.textPaintBold) + PdfPageContext.TOKEN_METADATA_GAP
        if (seller.bankName.isNotBlank()) leftContentHeight += ctx.measureWrappedTextHeight("Bank Name: ${seller.bankName}", ctx.textPaintRegular, leftColumnTextWidth) + PdfPageContext.TOKEN_METADATA_GAP
        if (seller.bankAccountNumber.isNotBlank()) leftContentHeight += ctx.measureWrappedTextHeight("A/c No.: ${seller.bankAccountNumber}", ctx.textPaintRegular, leftColumnTextWidth) + PdfPageContext.TOKEN_METADATA_GAP
        if (seller.bankIfsc.isNotBlank()) leftContentHeight += ctx.measureWrappedTextHeight("IFSC: ${seller.bankIfsc}", ctx.textPaintRegular, leftColumnTextWidth) + PdfPageContext.TOKEN_METADATA_GAP
        if (seller.bankBranch.isNotBlank()) leftContentHeight += ctx.measureWrappedTextHeight("Branch: ${seller.bankBranch}", ctx.textPaintRegular, leftColumnTextWidth) + PdfPageContext.TOKEN_METADATA_GAP

        leftContentHeight += ctx.measureSingleLineHeight(ctx.textPaintBold) + PdfPageContext.TOKEN_METADATA_GAP
        val decl = seller.declaration.ifBlank { "We declare that this document shows the actual price of the goods described and that all particulars are true and correct." }
        leftContentHeight += ctx.measureWrappedTextHeight(decl, ctx.textPaintSmall, leftColumnTextWidth) + PdfPageContext.TOKEN_METADATA_GAP

        val sigHeader = "For ${seller.businessName}"
        var rightContentHeight = ctx.measureSingleLineHeight(ctx.textPaintBold) + PdfPageContext.TOKEN_SIGNATURE_HEIGHT + ctx.measureSingleLineHeight(ctx.textPaintRegular)

        val totalLegalHeight = maxOf(leftContentHeight, rightContentHeight, 85f) + (PdfPageContext.TOKEN_PADDING_MEDIUM * 2)

        ctx.ensureSpace(totalLegalHeight)

        // --- PASS 2: Drawing ---
        val startY = ctx.currentY
        var leftTop = startY + PdfPageContext.TOKEN_PADDING_MEDIUM

        leftTop = ctx.drawSingleLineFromTop("BANK DETAILS", leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, leftTop, ctx.textPaintBold)
        leftTop += PdfPageContext.TOKEN_METADATA_GAP

        if (seller.bankName.isNotBlank()) {
            leftTop = ctx.drawWrappedTextFromTop("Bank Name: ${seller.bankName}", leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, leftTop, ctx.textPaintRegular, leftColumnTextWidth)
            leftTop += PdfPageContext.TOKEN_METADATA_GAP
        }
        if (seller.bankAccountNumber.isNotBlank()) {
            leftTop = ctx.drawWrappedTextFromTop("A/c No.: ${seller.bankAccountNumber}", leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, leftTop, ctx.textPaintRegular, leftColumnTextWidth)
            leftTop += PdfPageContext.TOKEN_METADATA_GAP
        }
        if (seller.bankIfsc.isNotBlank()) {
            leftTop = ctx.drawWrappedTextFromTop("IFSC: ${seller.bankIfsc}", leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, leftTop, ctx.textPaintRegular, leftColumnTextWidth)
            leftTop += PdfPageContext.TOKEN_METADATA_GAP
        }
        if (seller.bankBranch.isNotBlank()) {
            leftTop = ctx.drawWrappedTextFromTop("Branch: ${seller.bankBranch}", leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, leftTop, ctx.textPaintRegular, leftColumnTextWidth)
            leftTop += PdfPageContext.TOKEN_METADATA_GAP
        }

        leftTop = ctx.drawSingleLineFromTop("DECLARATION", leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, leftTop, ctx.textPaintBold)
        leftTop += PdfPageContext.TOKEN_METADATA_GAP
        ctx.drawWrappedTextFromTop(decl, leftX + PdfPageContext.TOKEN_PADDING_MEDIUM, leftTop, ctx.textPaintSmall, leftColumnTextWidth)

        var rightTop = startY + PdfPageContext.TOKEN_PADDING_MEDIUM
        rightTop = ctx.drawSingleLineRightAlignedFromTop(sigHeader, rightX - PdfPageContext.TOKEN_PADDING_MEDIUM, rightTop, ctx.textPaintBold)
        rightTop += PdfPageContext.TOKEN_SIGNATURE_HEIGHT
        ctx.drawSingleLineRightAlignedFromTop("Authorised Signatory", rightX - PdfPageContext.TOKEN_PADDING_MEDIUM, rightTop, ctx.textPaintRegular)

        ctx.canvas.drawRect(leftX, startY, rightX, startY + totalLegalHeight, ctx.paintLine)
        ctx.canvas.drawLine(midX, startY, midX, startY + totalLegalHeight, ctx.paintLine)

        ctx.currentY = startY + totalLegalHeight
    }

    private fun drawFooter(ctx: PdfPageContext, isInvoice: Boolean) {
        val footerText = if (isInvoice) "This is a Computer Generated Invoice" else "This is a Computer Generated Purchase Order"
        val bannerHeight = 16f

        ctx.canvas.drawRect(
            PdfPageContext.CONTENT_LEFT,
            ctx.currentY,
            PdfPageContext.CONTENT_RIGHT,
            ctx.currentY + bannerHeight,
            ctx.paintFillHeader
        )
        ctx.canvas.drawRect(
            PdfPageContext.CONTENT_LEFT,
            ctx.currentY,
            PdfPageContext.CONTENT_RIGHT,
            ctx.currentY + bannerHeight,
            ctx.paintLine
        )

        ctx.drawSingleLineCenteredFromTop(
            footerText,
            PdfPageContext.PAGE_WIDTH / 2f,
            ctx.currentY + 3f,
            ctx.textPaintSmall
        )
        ctx.currentY += bannerHeight
    }
}



