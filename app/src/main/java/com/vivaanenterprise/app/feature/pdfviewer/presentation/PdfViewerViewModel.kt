package com.vivaanenterprise.app.feature.pdfviewer.presentation

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.domain.model.BusinessDocument
import com.vivaanenterprise.app.domain.pdf.PdfGenerationResult
import com.vivaanenterprise.app.domain.usecase.GenerateBusinessDocumentPdfUseCase
import com.vivaanenterprise.app.feature.pdfviewer.service.DefaultPdfRenderSession
import com.vivaanenterprise.app.feature.pdfviewer.service.ExportResult
import com.vivaanenterprise.app.feature.pdfviewer.service.PdfCacheManager
import com.vivaanenterprise.app.feature.pdfviewer.service.PdfExportManager
import com.vivaanenterprise.app.feature.pdfviewer.service.PdfFilenameSanitizer
import com.vivaanenterprise.app.feature.pdfviewer.service.PdfRenderSession
import com.vivaanenterprise.app.feature.pdfviewer.service.PdfShareManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
open class PdfViewerViewModel @Inject constructor(
    private val generatePdfUseCase: GenerateBusinessDocumentPdfUseCase,
    private val cacheManager: PdfCacheManager,
    private val shareManager: PdfShareManager,
    private val exportManager: PdfExportManager,
    private val filenameSanitizer: PdfFilenameSanitizer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val documentId: String = checkNotNull(savedStateHandle["documentId"]) { "documentId is required" }

    private val _uiState = MutableStateFlow(PdfViewerUiState(documentId = documentId))
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PdfViewerUiEffect>()
    val uiEffect: SharedFlow<PdfViewerUiEffect> = _uiEffect.asSharedFlow()

    private var currentDocument: BusinessDocument? = null
    private var renderSession: PdfRenderSession? = null
    private var cachedPdfFile: File? = null

    init {
        loadPdf()
    }

    fun onIntent(intent: PdfViewerUiIntent) {
        when (intent) {
            is PdfViewerUiIntent.OnLoadPage -> loadPage(intent.pageIndex, intent.targetWidthPx)
            PdfViewerUiIntent.OnRetry -> loadPdf()
            PdfViewerUiIntent.OnShareClick -> handleShare()
            PdfViewerUiIntent.OnSaveClick -> handleSave()
            is PdfViewerUiIntent.OnSaveDestinationSelected -> handleSaveDestinationSelected(intent.destinationUri)
        }
    }

    private fun loadPdf() {
        viewModelScope.launch {
            closeSession()
            _uiState.update {
                it.copy(
                    isInitialLoading = true,
                    errorMessage = null,
                    pages = emptyMap()
                )
            }

            try {
                val doc = generatePdfUseCase.getDocument(documentId)
                if (doc == null) {
                    _uiState.update { it.copy(isInitialLoading = false, errorMessage = "Document not found") }
                    return@launch
                }
                currentDocument = doc

                val genResult = generatePdfUseCase(documentId)

                when (genResult) {
                    is PdfGenerationResult.Success -> {
                        val file = cacheManager.writePdfToCache(documentId, genResult.pdfBytes)
                        cachedPdfFile = file

                        val session = openRenderSession(file)
                        renderSession = session

                        val initialPages = (0 until session.pageCount).associateWith { idx ->
                            PageRenderState(pageIndex = idx)
                        }

                        _uiState.update {
                            it.copy(
                                documentNumber = doc.documentNumber,
                                documentType = doc.documentType,
                                pageCount = session.pageCount,
                                pages = initialPages,
                                isInitialLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                    PdfGenerationResult.Failure.DocumentNotFinalized -> {
                        _uiState.update { it.copy(isInitialLoading = false, errorMessage = "Document is not finalized") }
                    }
                    PdfGenerationResult.Failure.MissingSellerSnapshot,
                    PdfGenerationResult.Failure.MissingClientSnapshot -> {
                        _uiState.update { it.copy(isInitialLoading = false, errorMessage = "Missing required snapshot data") }
                    }
                    PdfGenerationResult.Failure.NoLineItems -> {
                        _uiState.update { it.copy(isInitialLoading = false, errorMessage = "Document has no line items") }
                    }
                    PdfGenerationResult.Failure.MissingHistoricalTaxTreatment -> {
                        _uiState.update { it.copy(isInitialLoading = false, errorMessage = "Historical tax treatment unavailable") }
                    }
                    is PdfGenerationResult.Failure.Error -> {
                        _uiState.update { it.copy(isInitialLoading = false, errorMessage = "Unable to generate PDF: ${genResult.exception.message}") }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update { it.copy(isInitialLoading = false, errorMessage = "Failed to load PDF viewer: ${e.message}") }
            }
        }
    }

    protected open fun openRenderSession(file: File): PdfRenderSession {
        return DefaultPdfRenderSession.open(file)
    }

    private val renderAccessOrder = mutableListOf<Int>()

    private fun loadPage(pageIndex: Int, targetWidthPx: Int) {
        val session = renderSession ?: return
        if (pageIndex < 0 || pageIndex >= session.pageCount) return

        // Clamp width safety
        val safeWidthPx = targetWidthPx.coerceIn(100, MAX_RENDER_WIDTH_PX)

        val currentPageState = _uiState.value.pages[pageIndex]
        val hasBitmap = currentPageState?.bitmap != null
        val sameWidth = currentPageState?.renderedWidthPx == safeWidthPx

        if (currentPageState?.isLoading == true || (hasBitmap && sameWidth)) {
            return
        }

        _uiState.update { current ->
            val updatedPages = current.pages.toMutableMap()
            updatedPages[pageIndex] = PageRenderState(pageIndex = pageIndex, isLoading = true, renderedWidthPx = safeWidthPx)
            current.copy(pages = updatedPages)
        }

        viewModelScope.launch {
            try {
                val bitmap = session.renderPage(pageIndex, safeWidthPx)
                _uiState.update { current ->
                    val updatedPages = current.pages.toMutableMap()
                    updatedPages[pageIndex] = PageRenderState(
                        pageIndex = pageIndex,
                        bitmap = bitmap,
                        isLoading = false,
                        isError = bitmap == null,
                        renderedWidthPx = safeWidthPx
                    )

                    renderAccessOrder.remove(pageIndex)
                    renderAccessOrder.add(pageIndex)

                    // Bounded memory retention: Keep only MAX_RENDERED_PAGES rendered Bitmaps
                    while (renderAccessOrder.size > MAX_RENDERED_PAGES) {
                        val evictedIndex = renderAccessOrder.removeAt(0)
                        val evictedState = updatedPages[evictedIndex]
                        if (evictedState != null) {
                            updatedPages[evictedIndex] = evictedState.copy(bitmap = null)
                        }
                    }

                    current.copy(pages = updatedPages)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiState.update { current ->
                    val updatedPages = current.pages.toMutableMap()
                    updatedPages[pageIndex] = PageRenderState(pageIndex = pageIndex, isLoading = false, isError = true)
                    current.copy(pages = updatedPages)
                }
            }
        }
    }

    companion object {
        private const val MAX_RENDERED_PAGES = 5
        private const val MAX_RENDER_WIDTH_PX = 2048
    }

    private fun handleShare() {
        if (_uiState.value.isSharing || _uiState.value.isSaving) return
        val file = cachedPdfFile ?: return
        val doc = currentDocument ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSharing = true) }
            try {
                val intent = shareManager.createShareIntent(file, doc)
                _uiEffect.emit(PdfViewerUiEffect.LaunchShareChooser(intent))
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiEffect.emit(PdfViewerUiEffect.ShowSnackbar("Unable to prepare PDF for sharing"))
            } finally {
                _uiState.update { it.copy(isSharing = false) }
            }
        }
    }

    private fun handleSave() {
        if (_uiState.value.isSaving || _uiState.value.isSharing) return
        val file = cachedPdfFile ?: return
        val doc = currentDocument ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val result = exportManager.saveToDownloadsApi29(file, doc)
                    when (result) {
                        is ExportResult.Success -> {
                            _uiEffect.emit(PdfViewerUiEffect.ShowSnackbar(result.message))
                        }
                        is ExportResult.Failure -> {
                            _uiEffect.emit(PdfViewerUiEffect.ShowSnackbar("Failed to save PDF to Downloads"))
                        }
                    }
                    _uiState.update { it.copy(isSaving = false) }
                } else {
                    val filename = filenameSanitizer.formatUserFacingFilename(doc)
                    _uiEffect.emit(PdfViewerUiEffect.LaunchCreateDocumentPicker(filename))
                    // isSaving cleared upon picker completion callback OnSaveDestinationSelected
                }
            } catch (ce: CancellationException) {
                _uiState.update { it.copy(isSaving = false) }
                throw ce
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _uiEffect.emit(PdfViewerUiEffect.ShowSnackbar("Failed to save PDF"))
            }
        }
    }

    private fun handleSaveDestinationSelected(destinationUri: android.net.Uri) {
        val file = cachedPdfFile ?: run {
            _uiState.update { it.copy(isSaving = false) }
            return
        }

        viewModelScope.launch {
            try {
                val result = exportManager.copyToUri(file, destinationUri)
                when (result) {
                    is ExportResult.Success -> {
                        _uiEffect.emit(PdfViewerUiEffect.ShowSnackbar(result.message))
                    }
                    is ExportResult.Failure -> {
                        _uiEffect.emit(PdfViewerUiEffect.ShowSnackbar("Failed to save PDF to selected location"))
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _uiEffect.emit(PdfViewerUiEffect.ShowSnackbar("Failed to save PDF"))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun closeSession() {
        try {
            renderSession?.close()
        } catch (_: Exception) {}
        renderSession = null
    }

    override fun onCleared() {
        super.onCleared()
        closeSession()
    }
}
