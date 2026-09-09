package com.vivaanenterprise.app.feature.pdfviewer.service

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

interface PdfRenderSession {
    val pageCount: Int
    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): Bitmap?
    fun close()
}

class DefaultPdfRenderSession private constructor(
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer
) : PdfRenderSession {

    private val mutex = Mutex()
    private var isClosed = false

    override val pageCount: Int
        get() = if (!isClosed) renderer.pageCount else 0

    override suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): Bitmap? = withContext(Dispatchers.Default) {
        mutex.withLock {
            if (isClosed || pageIndex < 0 || pageIndex >= renderer.pageCount) {
                return@withContext null
            }
            var page: PdfRenderer.Page? = null
            try {
                page = renderer.openPage(pageIndex)
                val pdfWidth = page.width
                val pdfHeight = page.height

                if (pdfWidth <= 0 || pdfHeight <= 0) return@withContext null

                val scale = targetWidthPx.toFloat() / pdfWidth.toFloat()
                val targetHeightPx = (pdfHeight * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(targetWidthPx.coerceAtLeast(1), targetHeightPx, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                null
            } finally {
                try {
                    page?.close()
                } catch (_: Exception) {}
            }
        }
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        try {
            renderer.close()
        } catch (_: Exception) {}
        try {
            pfd.close()
        } catch (_: Exception) {}
    }

    companion object {
        fun open(pdfFile: File): DefaultPdfRenderSession {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            try {
                val renderer = PdfRenderer(pfd)
                return DefaultPdfRenderSession(pfd, renderer)
            } catch (e: Exception) {
                try { pfd.close() } catch (_: Exception) {}
                throw e
            }
        }
    }
}
