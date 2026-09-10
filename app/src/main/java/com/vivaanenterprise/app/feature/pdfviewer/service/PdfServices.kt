package com.vivaanenterprise.app.feature.pdfviewer.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.domain.model.BusinessDocument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class PdfFilenameSanitizer @Inject constructor() {
    fun formatUserFacingFilename(document: BusinessDocument): String {
        val prefix = if (document.documentType == DocumentType.TAX_INVOICE) "Tax-Invoice" else "Purchase-Order"
        val rawNum = document.documentNumber.trim()
        val safeNum = sanitizeFilename(rawNum).ifBlank { document.id.take(8) }
        return "${prefix}_${safeNum}.pdf"
    }

    fun sanitizeFilename(input: String): String {
        if (input.isBlank()) return ""
        // Replace slashes, backslashes, colons, spaces, dots, and OS reserved characters with hyphen
        var clean = input.replace(Regex("[\\\\/:*?\"<>|\\s.]"), "-")
        // Collapse multiple consecutive hyphens
        clean = clean.replace(Regex("-+"), "-").trim('-')
        // Prevent path traversal
        clean = clean.replace("..", "")
        // Cap maximum length
        return clean.take(64)
    }
}

@Singleton
open class PdfCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sanitizer: PdfFilenameSanitizer
) {
    private val cacheDir: File
        get() = File(context.cacheDir, "pdfs").apply { if (!exists()) mkdirs() }

    open suspend fun writePdfToCache(documentId: String, pdfBytes: ByteArray): File = withContext(Dispatchers.IO) {
        cleanupStaleCache()
        val safeDocId = sanitizer.sanitizeFilename(documentId).ifBlank { "doc" }
        val finalFile = File(cacheDir, "pdf_${safeDocId}.pdf")
        val tmpFile = File(cacheDir, "pdf_${safeDocId}_${System.currentTimeMillis()}.tmp")

        // Path-based containment check
        val basePath = cacheDir.canonicalFile.toPath()
        val candidatePath = finalFile.canonicalFile.toPath()
        if (!candidatePath.startsWith(basePath) || candidatePath == basePath) {
            throw IllegalArgumentException("Invalid documentId: Path traversal detected")
        }

        try {
            FileOutputStream(tmpFile).use { out ->
                out.write(pdfBytes)
                out.flush()
            }

            val tmpPath = tmpFile.toPath()
            val finalPath = finalFile.toPath()

            try {
                java.nio.file.Files.move(
                    tmpPath,
                    finalPath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE
                )
            } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
                java.nio.file.Files.move(
                    tmpPath,
                    finalPath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
            }
            finalFile
        } catch (ce: CancellationException) {
            if (tmpFile.exists()) tmpFile.delete()
            throw ce
        } catch (e: Exception) {
            if (tmpFile.exists()) tmpFile.delete()
            throw e
        }
    }

    open fun getCachedPdf(documentId: String): File? {
        val safeDocId = sanitizer.sanitizeFilename(documentId).ifBlank { "doc" }
        val file = File(cacheDir, "pdf_${safeDocId}.pdf")
        val basePath = cacheDir.canonicalFile.toPath()
        val candidatePath = file.canonicalFile.toPath()
        if (!candidatePath.startsWith(basePath) || candidatePath == basePath) {
            return null
        }
        return if (file.exists() && file.length() > 0) file else null
    }

    open fun cleanupStaleCache(retentionMillis: Long = 24 * 60 * 60 * 1000L) {
        try {
            val now = System.currentTimeMillis()
            val files = cacheDir.listFiles() ?: return
            for (file in files) {
                if (now - file.lastModified() > retentionMillis) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}

@Singleton
open class PdfShareManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sanitizer: PdfFilenameSanitizer
) {
    open fun createShareIntent(cachedFile: File, document: BusinessDocument): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cachedFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri("PDF", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val title = "Share ${if (document.documentType == DocumentType.TAX_INVOICE) "Tax Invoice" else "Purchase Order"}"
        val chooser = Intent.createChooser(intent, title)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return chooser
    }
}

sealed interface ExportResult {
    data class Success(val message: String, val uri: Uri? = null) : ExportResult
    data class Failure(val error: Throwable) : ExportResult
}

@Singleton
open class PdfExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sanitizer: PdfFilenameSanitizer
) {
    open suspend fun saveToDownloadsApi29(cachedFile: File, document: BusinessDocument): ExportResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext ExportResult.Failure(IllegalStateException("Requires Android 10+ (API 29+)"))
        }

        val filename = sanitizer.formatUserFacingFilename(document)
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Vivaan Enterprise")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        var insertedUri: Uri? = null
        try {
            insertedUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext ExportResult.Failure(IllegalStateException("Failed to create MediaStore download record"))

            resolver.openOutputStream(insertedUri).use { out ->
                if (out == null) throw IllegalStateException("Failed to open output stream for MediaStore URI")
                FileInputStream(cachedFile).use { input ->
                    input.copyTo(out)
                }
                out.flush()
            }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(insertedUri, contentValues, null, null)

            ExportResult.Success("PDF saved to Downloads/Vivaan Enterprise", insertedUri)
        } catch (ce: CancellationException) {
            insertedUri?.let { uri ->
                try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            }
            throw ce
        } catch (e: Exception) {
            insertedUri?.let { uri ->
                try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            }
            ExportResult.Failure(e)
        }
    }

    open suspend fun copyToUri(cachedFile: File, destinationUri: Uri): ExportResult = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(destinationUri).use { out ->
                if (out == null) throw IllegalStateException("Failed to open output stream for destination URI")
                FileInputStream(cachedFile).use { input ->
                    input.copyTo(out)
                }
                out.flush()
            }
            ExportResult.Success("PDF saved successfully", destinationUri)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            ExportResult.Failure(e)
        }
    }
}
