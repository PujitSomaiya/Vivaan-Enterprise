package com.vivaanenterprise.app.feature.pdfviewer.presentation

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.designsystem.component.AppErrorState
import com.vivaanenterprise.app.core.designsystem.component.AppLoadingState
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.AppTopBar
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme

@Composable
fun PdfViewerRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            viewModel.onIntent(PdfViewerUiIntent.OnSaveDestinationSelected(uri))
        } else {
            viewModel.onIntent(PdfViewerUiIntent.OnSaveDestinationSelected(android.net.Uri.EMPTY))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is PdfViewerUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is PdfViewerUiEffect.LaunchShareChooser -> {
                    context.startActivity(effect.shareIntent)
                }
                is PdfViewerUiEffect.LaunchCreateDocumentPicker -> {
                    createDocumentLauncher.launch(effect.suggestedFilename)
                }
            }
        }
    }

    PdfViewerScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@Composable
fun PdfViewerScreen(
    uiState: PdfViewerUiState,
    onIntent: (PdfViewerUiIntent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val title = if (uiState.documentNumber.isNotBlank()) {
        val typeLabel = if (uiState.documentType == DocumentType.TAX_INVOICE) "Tax Invoice" else "Purchase Order"
        "$typeLabel - ${uiState.documentNumber}"
    } else {
        "PDF Viewer"
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val targetWidthPx = (screenWidthPx - with(density) { 32.dp.roundToPx() }).coerceAtLeast(300)

    AppScaffold(
        topBar = {
            AppTopBar(
                title = title,
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { onIntent(PdfViewerUiIntent.OnShareClick) },
                        enabled = !uiState.isInitialLoading && uiState.errorMessage == null && !uiState.isSharing
                    ) {
                        if (uiState.isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AppTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share PDF",
                                tint = AppTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(
                        onClick = { onIntent(PdfViewerUiIntent.OnSaveClick) },
                        enabled = !uiState.isInitialLoading && uiState.errorMessage == null && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AppTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Save PDF",
                                tint = AppTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF202B36)) // Neutral dark backdrop for document contrast
        ) {
            when {
                uiState.isInitialLoading -> {
                    AppLoadingState(
                        modifier = Modifier.fillMaxSize(),
                        label = "Generating PDF..."
                    )
                }
                uiState.errorMessage != null -> {
                    AppErrorState(
                        title = "PDF Viewer Error",
                        message = uiState.errorMessage,
                        onRetryClick = if (uiState.isRetryable) { { onIntent(PdfViewerUiIntent.OnRetry) } } else null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = (0 until uiState.pageCount).toList(),
                            key = { index, _ -> index }
                        ) { index, _ ->
                            val pageState = uiState.pages[index]

                            LaunchedEffect(index, targetWidthPx) {
                                onIntent(PdfViewerUiIntent.OnLoadPage(index, targetWidthPx))
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                var scale by remember { mutableStateOf(1f) }
                                var offsetX by remember { mutableStateOf(0f) }
                                var offsetY by remember { mutableStateOf(0f) }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(595f / 842f)
                                        .semantics {
                                            contentDescription = "Page ${index + 1} of ${uiState.pageCount}"
                                        }
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                scale = (scale * zoom).coerceIn(1f, 4f)
                                                if (scale > 1f) {
                                                    offsetX += pan.x
                                                    offsetY += pan.y
                                                } else {
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                }
                                            }
                                        }
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offsetX,
                                            translationY = offsetY
                                        ),
                                    shape = RoundedCornerShape(2.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            pageState?.bitmap != null -> {
                                                Image(
                                                    bitmap = pageState.bitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                            pageState?.isLoading == true -> {
                                                CircularProgressIndicator(
                                                    color = AppTheme.colorScheme.primary,
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                            pageState?.isError == true -> {
                                                Text(
                                                    text = "Failed to render page ${index + 1}",
                                                    style = AppTheme.typography.bodySmall,
                                                    color = AppTheme.colorScheme.error
                                                )
                                            }
                                            else -> {
                                                CircularProgressIndicator(
                                                    color = AppTheme.colorScheme.primary,
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Page ${index + 1} of ${uiState.pageCount}",
                                    style = AppTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
