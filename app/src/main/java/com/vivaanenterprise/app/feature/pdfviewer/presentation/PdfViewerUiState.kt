package com.vivaanenterprise.app.feature.pdfviewer.presentation

import android.graphics.Bitmap
import android.net.Uri
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.domain.model.BusinessDocument

data class PageRenderState(
    val pageIndex: Int,
    val bitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val renderedWidthPx: Int = 0
)

data class PdfViewerUiState(
    val documentId: String = "",
    val documentNumber: String = "",
    val documentType: DocumentType = DocumentType.TAX_INVOICE,
    val pageCount: Int = 0,
    val pages: Map<Int, PageRenderState> = emptyMap(),
    val isInitialLoading: Boolean = true,
    val isSharing: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isRetryable: Boolean = true
)

sealed interface PdfViewerUiIntent {
    data class OnLoadPage(val pageIndex: Int, val targetWidthPx: Int) : PdfViewerUiIntent
    data object OnRetry : PdfViewerUiIntent
    data object OnShareClick : PdfViewerUiIntent
    data object OnSaveClick : PdfViewerUiIntent
    data class OnSaveDestinationSelected(val destinationUri: Uri) : PdfViewerUiIntent
}

sealed interface PdfViewerUiEffect {
    data class ShowSnackbar(val message: String) : PdfViewerUiEffect
    data class LaunchShareChooser(val shareIntent: android.content.Intent) : PdfViewerUiEffect
    data class LaunchCreateDocumentPicker(val suggestedFilename: String) : PdfViewerUiEffect
}
