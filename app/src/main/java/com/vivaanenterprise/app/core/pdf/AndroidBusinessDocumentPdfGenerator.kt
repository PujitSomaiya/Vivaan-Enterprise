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
 * Renders A4 Tax Invoices and Purchase Orders strictly from frozen historical snapshots without reading current master data.
 */
@Singleton
class AndroidBusinessDocumentPdfGenerator @Inject constructor(
    private val currencyFormatter: IndianCurrencyFormatter
) : BusinessDocumentPdfGenerator {

    private fun createDateFormatter(): SimpleDateFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).apply {
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

            // 1. Title Banner
            drawTitleBanner(pageCtx, if (isInvoice) "TAX INVOICE" else "PURCHASE ORDER")

            // 2. Header Grid (Seller Profile + Metadata Grid)
            drawHeaderGrid(pageCtx, document, seller, isInvoice, dateFormatter)

            // 3. Buyer / Delivery Block
            drawBuyerAndDeliveryBlock(pageCtx, document, client, isInvoice)

            // 4. Line Items Table
            drawLineItemsTable(pageCtx, document)

            // 5. Totals & Tax Summary Block
            drawTotalsAndTaxSummary(pageCtx, document, isInterstate)

            // 6. Bank Details, Declaration & Signature
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

    private fun drawTitleBanner(ctx: PdfPageContext, title: String) {
        val bannerHeight = 22f
        ctx.canvas.drawRect(
            PdfPageContext.MARGIN_LEFT,
            ctx.currentY,
            PdfPageContext.MARGIN_RIGHT,
            ctx.currentY + bannerHeight,
            ctx.paintFillHeader
        )
        ctx.canvas.drawRect(
            PdfPageContext.MARGIN_LEFT,
            ctx.currentY,
            PdfPageContext.MARGIN_RIGHT,
            ctx.currentY + bannerHeight,
            ctx.paintLine
        )

        ctx.drawTextCentered(
            title,
            PdfPageContext.PAGE_WIDTH / 2f,
            ctx.currentY + 15f,
            ctx.textPaintHeaderLabel
        )
        ctx.currentY += bannerHeight
    }

    private fun drawHeaderGrid(
        ctx: PdfPageContext,
        doc: BusinessDocument,
        seller: SellerSnapshot,
        isInvoice: Boolean,
        dateFormatter: SimpleDateFormat
    ) {
        val gridWidth = PdfPageContext.CONTENT_WIDTH
        val halfWidth = gridWidth / 2f
        val leftX = PdfPageContext.MARGIN_LEFT
        val midX = leftX + halfWidth
        val startY = ctx.currentY

        // Draw Left Seller Info
        var leftY = startY + 12f
        ctx.drawText(seller.businessName, leftX + 8f, leftY, ctx.textPaintTitle)
        leftY += 12f
        val addr1 = seller.addressLine1
        if (addr1.isNotBlank()) {
            leftY += ctx.drawWrappedText(addr1, leftX + 8f, leftY, ctx.textPaintRegular, (halfWidth - 16).toInt()) + 2f
        }
        val addr2 = seller.addressLine2
        if (addr2.isNotBlank()) {
            leftY += ctx.drawWrappedText(addr2, leftX + 8f, leftY, ctx.textPaintRegular, (halfWidth - 16).toInt()) + 2f
        }
        val cityState = seller.cityStatePincode
        if (cityState.isNotBlank()) {
            leftY += ctx.drawWrappedText(cityState, leftX + 8f, leftY, ctx.textPaintRegular, (halfWidth - 16).toInt()) + 2f
        }
        ctx.drawText("GSTIN/UIN: ${seller.gstin}", leftX + 8f, leftY + 10f, ctx.textPaintBold)
        leftY += 14f
        ctx.drawText("Mobile: ${seller.mobile}", leftX + 8f, leftY + 10f, ctx.textPaintRegular)
        leftY += 14f

        // Draw Right Metadata Grid
        var rightY = startY + 12f
        val numLabel = if (isInvoice) "Tax Invoice No." else "PO No."
        drawMetadataLine(ctx, numLabel, doc.documentNumber, midX + 8f, rightY)
        rightY += 12f

        val dateStr = dateFormatter.format(Date(doc.documentDate))
        drawMetadataLine(ctx, "Dated", dateStr, midX + 8f, rightY)
        rightY += 12f

        if (!doc.deliveryNote.isNullOrBlank()) {
            drawMetadataLine(ctx, "Delivery Note", doc.deliveryNote, midX + 8f, rightY)
            rightY += 12f
        }
        if (!doc.paymentTerms.isNullOrBlank()) {
            drawMetadataLine(ctx, "Mode/Terms of Payment", doc.paymentTerms, midX + 8f, rightY)
            rightY += 12f
        }
        if (!doc.supplierReference.isNullOrBlank()) {
            drawMetadataLine(ctx, "Supplier's Ref.", doc.supplierReference, midX + 8f, rightY)
            rightY += 12f
        }
        if (!doc.buyerOrderNumber.isNullOrBlank()) {
            drawMetadataLine(ctx, "Buyer's Order No.", doc.buyerOrderNumber, midX + 8f, rightY)
            rightY += 12f
        }
        if (doc.buyerOrderDate != null && doc.buyerOrderDate > 0L) {
            drawMetadataLine(ctx, "Dated", dateFormatter.format(Date(doc.buyerOrderDate)), midX + 8f, rightY)
            rightY += 12f
        }
        if (!doc.destination.isNullOrBlank()) {
            drawMetadataLine(ctx, "Destination", doc.destination, midX + 8f, rightY)
            rightY += 12f
        }

        val boxHeight = maxOf(leftY - startY + 6f, rightY - startY + 6f, 80f)

        // Outer box and dividing vertical line
        ctx.canvas.drawRect(leftX, startY, PdfPageContext.MARGIN_RIGHT, startY + boxHeight, ctx.paintLine)
        ctx.canvas.drawLine(midX, startY, midX, startY + boxHeight, ctx.paintLine)

        ctx.currentY = startY + boxHeight
    }

    private fun drawMetadataLine(ctx: PdfPageContext, label: String, value: String, x: Float, y: Float) {
        ctx.drawText("$label:", x, y, ctx.textPaintBold)
        ctx.drawText(value, x + 95f, y, ctx.textPaintRegular)
    }

    private fun drawBuyerAndDeliveryBlock(
        ctx: PdfPageContext,
        doc: BusinessDocument,
        client: ClientSnapshot,
        isInvoice: Boolean
    ) {
        val startY = ctx.currentY
        val leftX = PdfPageContext.MARGIN_LEFT
        val gridWidth = PdfPageContext.CONTENT_WIDTH

        // Subheader Banner
        val headerTitle = if (isInvoice) "BILL TO" else "SUPPLIER PO DETAILS"
        ctx.canvas.drawRect(leftX, startY, PdfPageContext.MARGIN_RIGHT, startY + 16f, ctx.paintFillHeader)
        ctx.canvas.drawRect(leftX, startY, PdfPageContext.MARGIN_RIGHT, startY + 16f, ctx.paintLine)
        ctx.drawText(headerTitle, leftX + 8f, startY + 11f, ctx.textPaintHeaderLabel)

        var contentY = startY + 26f
        ctx.drawText(client.companyName, leftX + 8f, contentY, ctx.textPaintBold)
        contentY += 14f

        if (!client.address.isNullOrBlank()) {
            val addrHeight = ctx.drawWrappedText(client.address, leftX + 8f, contentY, ctx.textPaintRegular, (gridWidth - 16).toInt())
            contentY += addrHeight + 4f
        }

        val stateName = client.state ?: IndianState.ALL_STATES.find { it.code == client.stateCode }?.name ?: ""
        val stateLine = if (stateName.isNotBlank() || !client.stateCode.isNullOrBlank()) {
            "State: $stateName ${if (!client.stateCode.isNullOrBlank()) "(${client.stateCode})" else ""}"
        } else ""
        if (stateLine.isNotBlank()) {
            ctx.drawText(stateLine, leftX + 8f, contentY, ctx.textPaintRegular)
            contentY += 14f
        }

        if (!client.gstin.isNullOrBlank()) {
            ctx.drawText("GSTIN/UIN: ${client.gstin}", leftX + 8f, contentY, ctx.textPaintBold)
            contentY += 12f
        }

        if (!doc.deliveryFactoryAddress.isNullOrBlank()) {
            ctx.canvas.drawRect(leftX, contentY + 2f, PdfPageContext.MARGIN_RIGHT, contentY + 18f, ctx.paintFillHeader)
            ctx.canvas.drawRect(leftX, contentY + 2f, PdfPageContext.MARGIN_RIGHT, contentY + 18f, ctx.paintLine)
            ctx.drawText("DELIVERY / FACTORY ADDRESS", leftX + 8f, contentY + 13f, ctx.textPaintHeaderLabel)
            contentY += 24f
            contentY += ctx.drawWrappedText(doc.deliveryFactoryAddress, leftX + 8f, contentY, ctx.textPaintRegular, (gridWidth - 16).toInt()) + 4f
        }

        val boxHeight = contentY - startY + 4f
        ctx.canvas.drawRect(leftX, startY, PdfPageContext.MARGIN_RIGHT, startY + boxHeight, ctx.paintLine)
        ctx.currentY = startY + boxHeight
    }

    private fun drawLineItemsTable(
        ctx: PdfPageContext,
        doc: BusinessDocument
    ) {
        val leftX = PdfPageContext.MARGIN_LEFT
        val rightX = PdfPageContext.MARGIN_RIGHT
        val tableWidth = PdfPageContext.CONTENT_WIDTH

        // Column widths
        val colSl = 30f
        val colHsn = 60f
        val colQty = 50f
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
            val startY = ctx.currentY
            val headerHeight = 18f
            ctx.canvas.drawRect(leftX, startY, rightX, startY + headerHeight, ctx.paintFillHeader)
            ctx.canvas.drawRect(leftX, startY, rightX, startY + headerHeight, ctx.paintLine)

            ctx.drawTextCentered("Sl.No", xSl + colSl / 2f, startY + 12f, ctx.textPaintHeaderLabel)
            ctx.drawText("Description of Goods", xDesc + 4f, startY + 12f, ctx.textPaintHeaderLabel)
            ctx.drawTextCentered("HSN/SAC", xHsn + colHsn / 2f, startY + 12f, ctx.textPaintHeaderLabel)
            ctx.drawTextRightAligned("Quantity", xQty + colQty - 4f, startY + 12f, ctx.textPaintHeaderLabel)
            ctx.drawTextRightAligned("Rate", xRate + colRate - 4f, startY + 12f, ctx.textPaintHeaderLabel)
            ctx.drawTextRightAligned("Amount", rightX - 4f, startY + 12f, ctx.textPaintHeaderLabel)

            // Vertical Column Separator Lines
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
            val rowHeight = maxOf(descHeight + 10f, 20f)

            ctx.ensureSpace(rowHeight, onNewPageHeader = drawTableHeaders)

            val rowStartY = ctx.currentY
            val textY = rowStartY + 12f

            ctx.drawTextCentered((index + 1).toString(), xSl + colSl / 2f, textY, ctx.textPaintRegular)
            ctx.drawWrappedText(item.descriptionSnapshot, xDesc + 4f, rowStartY + 4f, ctx.textPaintRegular, (colDesc - 8).toInt())
            ctx.drawTextCentered(item.hsnSacSnapshot ?: "", xHsn + colHsn / 2f, textY, ctx.textPaintRegular)
            ctx.drawTextRightAligned(item.quantity.toString(), xQty + colQty - 4f, textY, ctx.textPaintRegular)
            ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(item.ratePaise), xRate + colRate - 4f, textY, ctx.textPaintRegular)
            ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(item.taxableAmountPaise), rightX - 4f, textY, ctx.textPaintBold)

            // Draw Outer Row Grid Lines
            ctx.canvas.drawRect(leftX, rowStartY, rightX, rowStartY + rowHeight, ctx.paintLine)
            ctx.canvas.drawLine(xDesc, rowStartY, xDesc, rowStartY + rowHeight, ctx.paintLine)
            ctx.canvas.drawLine(xHsn, rowStartY, xHsn, rowStartY + rowHeight, ctx.paintLine)
            ctx.canvas.drawLine(xQty, rowStartY, xQty, rowStartY + rowHeight, ctx.paintLine)
            ctx.canvas.drawLine(xRate, rowStartY, xRate, rowStartY + rowHeight, ctx.paintLine)
            ctx.canvas.drawLine(xAmount, rowStartY, xAmount, rowStartY + rowHeight, ctx.paintLine)

            ctx.currentY = rowStartY + rowHeight
        }
    }

    private fun drawTotalsAndTaxSummary(
        ctx: PdfPageContext,
        doc: BusinessDocument,
        isInterstate: Boolean
    ) {
        val leftX = PdfPageContext.MARGIN_LEFT
        val rightX = PdfPageContext.MARGIN_RIGHT

        ctx.ensureSpace(110f)

        val startY = ctx.currentY
        var lineY = startY + 12f

        // Taxable Value
        ctx.drawTextRightAligned("Taxable Value:", rightX - 120f, lineY, ctx.textPaintRegular)
        ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(doc.taxableAmountPaise), rightX - 8f, lineY, ctx.textPaintBold)
        lineY += 12f

        if (isInterstate) {
            ctx.drawTextRightAligned("IGST Amount:", rightX - 120f, lineY, ctx.textPaintRegular)
            ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(doc.igstAmountPaise), rightX - 8f, lineY, ctx.textPaintRegular)
            lineY += 12f
        } else {
            ctx.drawTextRightAligned("CGST Amount:", rightX - 120f, lineY, ctx.textPaintRegular)
            ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(doc.cgstAmountPaise), rightX - 8f, lineY, ctx.textPaintRegular)
            lineY += 12f
            ctx.drawTextRightAligned("SGST Amount:", rightX - 120f, lineY, ctx.textPaintRegular)
            ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(doc.sgstAmountPaise), rightX - 8f, lineY, ctx.textPaintRegular)
            lineY += 12f
        }

        ctx.drawTextRightAligned("Total Tax Amount:", rightX - 120f, lineY, ctx.textPaintRegular)
        ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(doc.totalTaxAmountPaise), rightX - 8f, lineY, ctx.textPaintBold)
        lineY += 14f

        ctx.drawTextRightAligned("TOTAL AMOUNT:", rightX - 120f, lineY, ctx.textPaintHeaderLabel)
        ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(doc.grandTotalPaise), rightX - 8f, lineY, ctx.textPaintTitle)
        lineY += 16f

        val wordsText = doc.amountInWords ?: currencyFormatter.formatAmountInWords(doc.grandTotalPaise)
        ctx.drawText("Amount Chargeable (in words):", leftX + 8f, lineY, ctx.textPaintBold)
        lineY += 12f
        ctx.drawText(wordsText, leftX + 8f, lineY, ctx.textPaintBold)
        lineY += 16f

        val boxHeight = lineY - startY
        ctx.canvas.drawRect(leftX, startY, rightX, startY + boxHeight, ctx.paintLine)

        ctx.currentY = startY + boxHeight

        // 8. Tax Summary Table & Tax Amount (in words)
        drawTaxSummaryTable(ctx, doc, isInterstate)
    }

    private fun drawTaxSummaryTable(
        ctx: PdfPageContext,
        doc: BusinessDocument,
        isInterstate: Boolean
    ) {
        val leftX = PdfPageContext.MARGIN_LEFT
        val rightX = PdfPageContext.MARGIN_RIGHT
        val tableWidth = PdfPageContext.CONTENT_WIDTH

        // Group line items by HSN/SAC
        val groupedLines = doc.lineItems.groupBy { it.hsnSacSnapshot ?: "" }
        val numRows = groupedLines.size
        val estimatedHeight = 36f + (numRows * 18f) + 30f

        ctx.ensureSpace(estimatedHeight)

        val startY = ctx.currentY
        val headerHeight = 18f

        // Subheader Banner
        ctx.canvas.drawRect(leftX, startY, rightX, startY + headerHeight, ctx.paintFillHeader)
        ctx.canvas.drawRect(leftX, startY, rightX, startY + headerHeight, ctx.paintLine)

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

                ctx.drawTextCentered("HSN/SAC", xHsn + colHsn / 2f, hY + 12f, ctx.textPaintHeaderLabel)
                ctx.drawTextRightAligned("Taxable Value", xTaxable + colTaxable - 4f, hY + 12f, ctx.textPaintHeaderLabel)
                ctx.drawTextRightAligned("IGST Rate", xRate + colIgstRate - 4f, hY + 12f, ctx.textPaintHeaderLabel)
                ctx.drawTextRightAligned("IGST Amt", xAmt + colIgstAmt - 4f, hY + 12f, ctx.textPaintHeaderLabel)
                ctx.drawTextRightAligned("Total Tax", rightX - 4f, hY + 12f, ctx.textPaintHeaderLabel)

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
                val textY = rowY + 12f

                ctx.drawTextCentered(hsn, xHsn + colHsn / 2f, textY, ctx.textPaintRegular)
                ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(taxableSum), xTaxable + colTaxable - 4f, textY, ctx.textPaintRegular)
                ctx.drawTextRightAligned(rateStr, xRate + colIgstRate - 4f, textY, ctx.textPaintRegular)
                ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(igstSum), xAmt + colIgstAmt - 4f, textY, ctx.textPaintRegular)
                ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(totalTaxSum), rightX - 4f, textY, ctx.textPaintRegular)

                ctx.canvas.drawRect(leftX, rowY, rightX, rowY + 18f, ctx.paintLine)
                ctx.canvas.drawLine(xTaxable, rowY, xTaxable, rowY + 18f, ctx.paintLine)
                ctx.canvas.drawLine(xRate, rowY, xRate, rowY + 18f, ctx.paintLine)
                ctx.canvas.drawLine(xAmt, rowY, xAmt, rowY + 18f, ctx.paintLine)
                ctx.canvas.drawLine(xTotal, rowY, xTotal, rowY + 18f, ctx.paintLine)

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

                ctx.drawTextCentered("HSN/SAC", xHsn + colHsn / 2f, hY + 12f, ctx.textPaintHeaderLabel)
                ctx.drawTextRightAligned("Taxable Value", xTaxable + colTaxable - 4f, hY + 12f, ctx.textPaintHeaderLabel)
                ctx.drawTextRightAligned("CGST Rate", xCgstRate + colCgstRate - 4f, hY + 12f, ctx.textPaintHeaderLabel)
                ctx.drawTextRightAligned("CGST Amt", xCgstAmt + colCgstAmt - 4f, hY + 12f, ctx.textPaintHeaderLabel)
                ctx.drawTextRightAligned("SGST Rate", xSgstRate + colSgstRate - 4f, hY + 12f, ctx.textPaintHeaderLabel)
                ctx.drawTextRightAligned("SGST Amt", xSgstAmt + colSgstAmt - 4f, hY + 12f, ctx.textPaintHeaderLabel)
                ctx.drawTextRightAligned("Total Tax", rightX - 4f, hY + 12f, ctx.textPaintHeaderLabel)

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
                val textY = rowY + 12f

                ctx.drawTextCentered(hsn, xHsn + colHsn / 2f, textY, ctx.textPaintRegular)
                ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(taxableSum), xTaxable + colTaxable - 4f, textY, ctx.textPaintRegular)
                ctx.drawTextRightAligned(rateStr, xCgstRate + colCgstRate - 4f, textY, ctx.textPaintRegular)
                ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(cgstSum), xCgstAmt + colCgstAmt - 4f, textY, ctx.textPaintRegular)
                ctx.drawTextRightAligned(rateStr, xSgstRate + colSgstRate - 4f, textY, ctx.textPaintRegular)
                ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(sgstSum), xSgstAmt + colSgstAmt - 4f, textY, ctx.textPaintRegular)
                ctx.drawTextRightAligned(PdfFormattingUtils.formatPaiseToCurrency(totalTaxSum), rightX - 4f, textY, ctx.textPaintRegular)

                ctx.canvas.drawRect(leftX, rowY, rightX, rowY + 18f, ctx.paintLine)
                ctx.canvas.drawLine(xTaxable, rowY, xTaxable, rowY + 18f, ctx.paintLine)
                ctx.canvas.drawLine(xCgstRate, rowY, xCgstRate, rowY + 18f, ctx.paintLine)
                ctx.canvas.drawLine(xCgstAmt, rowY, xCgstAmt, rowY + 18f, ctx.paintLine)
                ctx.canvas.drawLine(xSgstRate, rowY, xSgstRate, rowY + 18f, ctx.paintLine)
                ctx.canvas.drawLine(xSgstAmt, rowY, xSgstAmt, rowY + 18f, ctx.paintLine)
                ctx.canvas.drawLine(xTotal, rowY, xTotal, rowY + 18f, ctx.paintLine)

                ctx.currentY = rowY + 18f
            }
        }

        // Tax Amount in Words Box
        val taxWords = doc.taxAmountInWords ?: currencyFormatter.formatAmountInWords(doc.totalTaxAmountPaise)
        val wordBoxStartY = ctx.currentY
        val wordTextY = wordBoxStartY + 12f

        ctx.drawText("Tax Amount (in words): $taxWords", leftX + 8f, wordTextY, ctx.textPaintBold)

        val wordBoxHeight = 18f
        ctx.canvas.drawRect(leftX, wordBoxStartY, rightX, wordBoxStartY + wordBoxHeight, ctx.paintLine)
        ctx.currentY = wordBoxStartY + wordBoxHeight
    }

    private fun drawBankAndDeclarationBlock(
        ctx: PdfPageContext,
        seller: SellerSnapshot,
        isInvoice: Boolean
    ) {
        ctx.ensureSpace(85f)

        val startY = ctx.currentY
        val leftX = PdfPageContext.MARGIN_LEFT
        val rightX = PdfPageContext.MARGIN_RIGHT
        val width = PdfPageContext.CONTENT_WIDTH
        val halfWidth = width / 2f
        val midX = leftX + halfWidth

        var leftY = startY + 12f
        ctx.drawText("BANK DETAILS", leftX + 8f, leftY, ctx.textPaintBold)
        leftY += 12f
        ctx.drawText("Bank Name: ${seller.bankName}", leftX + 8f, leftY, ctx.textPaintRegular)
        leftY += 10f
        ctx.drawText("A/c No.: ${seller.bankAccountNumber}", leftX + 8f, leftY, ctx.textPaintRegular)
        leftY += 10f
        ctx.drawText("Branch & IFSC: ${seller.bankBranch} (${seller.bankIfsc})", leftX + 8f, leftY, ctx.textPaintRegular)
        leftY += 14f

        ctx.drawText("DECLARATION", leftX + 8f, leftY, ctx.textPaintBold)
        leftY += 10f
        val decl = seller.declaration.ifBlank { "We declare that this invoice shows the actual price of the goods described and that all particulars are true and correct." }
        leftY += ctx.drawWrappedText(decl, leftX + 8f, leftY, ctx.textPaintSmall, (halfWidth - 16).toInt()) + 4f

        var rightY = startY + 12f
        val sigHeader = seller.authorisedSignatory.ifBlank { "For VIVAAN ENTERPRISE" }
        ctx.drawTextRightAligned(sigHeader, rightX - 8f, rightY, ctx.textPaintBold)
        rightY += 45f // Space for physical signature/stamp
        ctx.drawTextRightAligned("Authorised Signatory", rightX - 8f, rightY, ctx.textPaintRegular)
        rightY += 12f

        val boxHeight = maxOf(leftY - startY + 4f, rightY - startY + 4f, 85f)
        ctx.canvas.drawRect(leftX, startY, rightX, startY + boxHeight, ctx.paintLine)
        ctx.canvas.drawLine(midX, startY, midX, startY + boxHeight, ctx.paintLine)

        ctx.currentY = startY + boxHeight
    }

    private fun drawFooter(ctx: PdfPageContext, isInvoice: Boolean) {
        val footerText = if (isInvoice) "This is a Computer Generated Invoice" else "This is a Computer Generated Purchase Order"
        val bannerHeight = 16f

        ctx.canvas.drawRect(
            PdfPageContext.MARGIN_LEFT,
            ctx.currentY,
            PdfPageContext.MARGIN_RIGHT,
            ctx.currentY + bannerHeight,
            ctx.paintFillHeader
        )
        ctx.canvas.drawRect(
            PdfPageContext.MARGIN_LEFT,
            ctx.currentY,
            PdfPageContext.MARGIN_RIGHT,
            ctx.currentY + bannerHeight,
            ctx.paintLine
        )

        ctx.drawTextCentered(
            footerText,
            PdfPageContext.PAGE_WIDTH / 2f,
            ctx.currentY + 11f,
            ctx.textPaintSmall
        )
        ctx.currentY += bannerHeight
    }
}
