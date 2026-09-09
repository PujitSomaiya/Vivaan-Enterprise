package com.vivaanenterprise.app.core.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * Reusable drawing, layout, and pagination context wrapper for native Android [PdfDocument].
 * Uses A4 dimensions: 595 x 842 points (72 DPI).
 */
class PdfPageContext(
    private val pdfDocument: PdfDocument
) {
    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN_LEFT = 30f
        const val MARGIN_RIGHT = 565f // PAGE_WIDTH - 30
        const val MARGIN_TOP = 30f
        const val MARGIN_BOTTOM = 812f // PAGE_HEIGHT - 30
        const val CONTENT_WIDTH = 535f // MARGIN_RIGHT - MARGIN_LEFT

        // Colors
        val COLOR_BORDER = Color.parseColor("#486581")
        val COLOR_HEADER_BG = Color.parseColor("#F0F4F8")
        val COLOR_TEXT_PRIMARY = Color.parseColor("#102A43")
        val COLOR_TEXT_MUTED = Color.parseColor("#627D98")
    }

    private var currentPage: PdfDocument.Page? = null
    var canvas: Canvas = Canvas()
        private set

    var pageNumber = 0
        private set
    var currentY = MARGIN_TOP

    val paintLine = Paint().apply {
        color = COLOR_BORDER
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    val paintFillHeader = Paint().apply {
        color = COLOR_HEADER_BG
        style = Paint.Style.FILL
    }

    val textPaintRegular = TextPaint().apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 8f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    val textPaintBold = TextPaint().apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 8f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val textPaintTitle = TextPaint().apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val textPaintHeaderLabel = TextPaint().apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 9f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val textPaintSmall = TextPaint().apply {
        color = COLOR_TEXT_MUTED
        textSize = 7f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    init {
        startNewPage()
    }

    fun startNewPage() {
        currentPage?.let {
            pdfDocument.finishPage(it)
        }
        pageNumber++
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        currentPage = page
        canvas = page.canvas
        currentY = MARGIN_TOP

        // Draw outer page border frame
        canvas.drawRect(MARGIN_LEFT, MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, paintLine)
    }

    fun ensureSpace(requiredHeight: Float, onNewPageHeader: (() -> Unit)? = null) {
        if (currentY + requiredHeight > MARGIN_BOTTOM - 20f) { // 20pt buffer for footer
            startNewPage()
            onNewPageHeader?.invoke()
        }
    }

    fun finish() {
        currentPage?.let {
            pdfDocument.finishPage(it)
            currentPage = null
        }
    }

    // ── Safe Text Helper Functions ──────────────────────────────────────────────

    fun drawText(text: String, x: Float, y: Float, paint: TextPaint) {
        if (text.isBlank()) return
        canvas.drawText(text, x, y, paint)
    }

    fun drawTextRightAligned(text: String, rightX: Float, y: Float, paint: TextPaint) {
        if (text.isBlank()) return
        val width = paint.measureText(text)
        canvas.drawText(text, rightX - width, y, paint)
    }

    fun drawTextCentered(text: String, centerX: Float, y: Float, paint: TextPaint) {
        if (text.isBlank()) return
        val width = paint.measureText(text)
        canvas.drawText(text, centerX - (width / 2f), y, paint)
    }

    fun measureWrappedTextHeight(text: String, paint: TextPaint, width: Int): Int {
        if (text.isBlank()) return 0
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
        val layout = builder.build()
        return layout.height
    }

    fun drawWrappedText(text: String, x: Float, y: Float, paint: TextPaint, width: Int): Int {
        if (text.isBlank()) return 0
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
        val layout = builder.build()

        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height
    }
}
