package com.vivaanenterprise.app.core.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * Reusable drawing, layout, and pagination context wrapper for native Android [PdfDocument].
 * Uses standard A4 dimensions: 595 x 842 points (72 DPI).
 */
class PdfPageContext(
    private val pdfDocument: PdfDocument
) {
    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val CONTENT_LEFT = 30f
        const val CONTENT_RIGHT = 565f // PAGE_WIDTH - 30
        const val CONTENT_TOP = 30f
        const val CONTENT_BOTTOM = 812f // PAGE_HEIGHT - 30
        const val CONTENT_WIDTH = 535f // CONTENT_RIGHT - CONTENT_LEFT

        // Legacy coordinate aliases
        const val MARGIN_LEFT = CONTENT_LEFT
        const val MARGIN_RIGHT = CONTENT_RIGHT
        const val MARGIN_TOP = CONTENT_TOP
        const val MARGIN_BOTTOM = CONTENT_BOTTOM

        // Layout Spacing Tokens (Points)
        const val TOKEN_SECTION_GAP = 8f
        const val TOKEN_METADATA_GAP = 4f
        const val TOKEN_PADDING_SMALL = 4f
        const val TOKEN_PADDING_MEDIUM = 8f
        const val TOKEN_SIGNATURE_HEIGHT = 45f

        // Professional PDF Palette (Printer-friendly slate & dark navy tones)
        val COLOR_BORDER = Color.parseColor("#334E68")        // Crisp slate border stroke
        val COLOR_HEADER_BG = Color.parseColor("#F0F4F8")     // Muted header fill
        val COLOR_TEXT_PRIMARY = Color.parseColor("#102A43")  // Deep navy body text
        val COLOR_TEXT_MUTED = Color.parseColor("#486581")    // Muted slate secondary text
        val COLOR_BRAND_PRIMARY = Color.parseColor("#0F4C81") // Vivaan Enterprise Brand Blue
        val COLOR_BRAND_ACCENT = Color.parseColor("#D97706")  // Warm Amber Accent
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

    val paintThinLine = Paint().apply {
        color = COLOR_BORDER
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    val paintFillHeader = Paint().apply {
        color = COLOR_HEADER_BG
        style = Paint.Style.FILL
    }

    val textPaintRegular = TextPaint().apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 8.5f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    val textPaintBold = TextPaint().apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 8.5f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val textPaintBrandTitle = TextPaint().apply {
        color = COLOR_BRAND_PRIMARY
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val textPaintDocTitle = TextPaint().apply {
        color = COLOR_BRAND_PRIMARY
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
        textSize = 7.5f
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

        // Outer page border frame (rendered only around top/bottom content margin frame if needed)
    }

    fun ensureSpace(requiredHeight: Float, onNewPageHeader: (() -> Unit)? = null) {
        if (currentY + requiredHeight > CONTENT_BOTTOM - 20f) { // 20pt buffer for footer
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

    // ── Native VE Brand Vector Logo Renderer ────────────────────────────────

    /**
     * Renders the VE monogram logo mark cleanly onto the canvas at exact (x, y) coordinates with specified size.
     * Uses resolution-independent vector paths matching [com.vivaanenterprise.app.core.designsystem.component.VeLogo].
     */
    fun drawVeLogo(x: Float, y: Float, size: Float) {
        val badgeSize = size * 0.96f
        val offset = (size - badgeSize) / 2f
        val rect = RectF(x + offset, y + offset, x + offset + badgeSize, y + offset + badgeSize)
        val cornerRadius = badgeSize * 0.22f

        // 1. Soft Badge Background Fill
        val paintBg = Paint().apply {
            color = COLOR_BRAND_PRIMARY
            alpha = 20 // ~8% opacity
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paintBg)

        // 2. Badge Border Stroke
        val paintBorder = Paint().apply {
            color = COLOR_BRAND_PRIMARY
            strokeWidth = size * 0.045f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paintBorder)

        val strokeW = size * 0.085f
        val paintStrokePrimary = Paint().apply {
            color = COLOR_BRAND_PRIMARY
            strokeWidth = strokeW
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        val paintStrokeAccent = Paint().apply {
            color = COLOR_BRAND_ACCENT
            strokeWidth = strokeW * 0.9f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        // 3. Draw 'V' Path
        val vPath = Path().apply {
            moveTo(x + (size * 0.18f), y + (size * 0.30f))
            lineTo(x + (size * 0.35f), y + (size * 0.70f))
            lineTo(x + (size * 0.52f), y + (size * 0.30f))
        }
        canvas.drawPath(vPath, paintStrokePrimary)

        // 4. Draw 'E' Spine & Bars
        // Spine
        canvas.drawLine(
            x + (size * 0.58f), y + (size * 0.30f),
            x + (size * 0.58f), y + (size * 0.70f),
            paintStrokePrimary
        )
        // Top Bar
        canvas.drawLine(
            x + (size * 0.58f), y + (size * 0.30f),
            x + (size * 0.82f), y + (size * 0.30f),
            paintStrokePrimary
        )
        // Middle Accent Bar
        canvas.drawLine(
            x + (size * 0.58f), y + (size * 0.50f),
            x + (size * 0.78f), y + (size * 0.50f),
            paintStrokeAccent
        )
        // Bottom Bar
        canvas.drawLine(
            x + (size * 0.58f), y + (size * 0.70f),
            x + (size * 0.82f), y + (size * 0.70f),
            paintStrokePrimary
        )
    }

    // ── Unified Top-Coordinate Text Rendering Engine ───────────────────────────

    /**
     * Measures the single-line height based on exact [Paint.FontMetrics].
     */
    fun measureSingleLineHeight(paint: TextPaint): Float {
        val fm = paint.fontMetrics
        return fm.descent - fm.ascent
    }

    /**
     * Measures wrapped multiline text height deterministically using [StaticLayout].
     */
    fun measureWrappedTextHeight(text: String, paint: TextPaint, width: Int): Float {
        if (text.isBlank() || width <= 0) return 0f
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.05f)
            .setIncludePad(false)
        val layout = builder.build()
        return layout.height.toFloat()
    }

    /**
     * Draws single-line text where `top` represents the top bound of the font height.
     * Calculates exact baseline position using `top - fontMetrics.ascent`.
     * Returns the bottom Y coordinate (`top + fontMetricsHeight`).
     */
    fun drawSingleLineFromTop(text: String, x: Float, top: Float, paint: TextPaint): Float {
        if (text.isBlank()) return top
        val fm = paint.fontMetrics
        val fontHeight = fm.descent - fm.ascent
        val baseline = top - fm.ascent
        canvas.drawText(text, x, baseline, paint)
        return top + fontHeight
    }

    /**
     * Draws single-line right-aligned text from top coordinate.
     * Returns the bottom Y coordinate.
     */
    fun drawSingleLineRightAlignedFromTop(text: String, rightX: Float, top: Float, paint: TextPaint): Float {
        if (text.isBlank()) return top
        val width = paint.measureText(text)
        return drawSingleLineFromTop(text, rightX - width, top, paint)
    }

    /**
     * Draws single-line centered text from top coordinate.
     * Returns the bottom Y coordinate.
     */
    fun drawSingleLineCenteredFromTop(text: String, centerX: Float, top: Float, paint: TextPaint): Float {
        if (text.isBlank()) return top
        val width = paint.measureText(text)
        return drawSingleLineFromTop(text, centerX - (width / 2f), top, paint)
    }

    /**
     * Draws wrapped multi-line text starting at `top` coordinate.
     * Returns the bottom Y coordinate (`top + layout.height`).
     */
    fun drawWrappedTextFromTop(text: String, x: Float, top: Float, paint: TextPaint, width: Int): Float {
        if (text.isBlank() || width <= 0) return top
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.05f)
            .setIncludePad(false)
        val layout = builder.build()

        canvas.save()
        canvas.translate(x, top)
        layout.draw(canvas)
        canvas.restore()
        return top + layout.height.toFloat()
    }

    // Deprecated raw baseline helpers kept internal for edge legacy compatibility if needed
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
}


