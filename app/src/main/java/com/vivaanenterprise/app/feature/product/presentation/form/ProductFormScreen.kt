package com.vivaanenterprise.app.feature.product.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.component.AppCard
import com.vivaanenterprise.app.core.designsystem.component.AppLoadingState
import com.vivaanenterprise.app.core.designsystem.component.AppPrimaryButton
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.AppTextField
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.designsystem.theme.VivaanEnterpriseTheme

@Composable
fun ProductFormRoute(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ProductFormUiEffect.SaveSuccess -> onSaveSuccess()
                is ProductFormUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                }
            }
        }
    }

    ProductFormScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    uiState: ProductFormUiState,
    onIntent: (ProductFormUiIntent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val titleRes = if (uiState.isEditMode) R.string.edit_product_title else R.string.add_product_title

    AppScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back_desc)
                        )
                    }
                }
            )
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        if (uiState.isLoadingProduct) {
            AppLoadingState(modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(AppTheme.spacing.md)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.spacing.md)
                    ) {
                        AppTextField(
                            value = uiState.name,
                            onValueChange = { onIntent(ProductFormUiIntent.NameChanged(it)) },
                            label = stringResource(R.string.product_name_label),
                            errorText = uiState.nameError?.let { stringResource(R.string.error_product_name_required) },
                            enabled = !uiState.isSaving,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        AppTextField(
                            value = uiState.hsnSac,
                            onValueChange = { onIntent(ProductFormUiIntent.HsnSacChanged(it)) },
                            label = stringResource(R.string.hsn_sac_label),
                            errorText = uiState.hsnSacError?.let { stringResource(R.string.error_hsn_sac_required) },
                            enabled = !uiState.isSaving,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        AppTextField(
                            value = uiState.gstRateInput,
                            onValueChange = { onIntent(ProductFormUiIntent.GstRateChanged(it)) },
                            label = stringResource(R.string.gst_rate_label),
                            errorText = uiState.gstRateError?.let { stringResource(R.string.error_gst_rate_invalid) },
                            enabled = !uiState.isSaving,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    onIntent(ProductFormUiIntent.SaveClicked)
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.is_active_label),
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = uiState.isActive,
                                onCheckedChange = { onIntent(ProductFormUiIntent.IsActiveChanged(it)) },
                                enabled = !uiState.isSaving
                            )
                        }

                        Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                        AppPrimaryButton(
                            text = stringResource(R.string.save_product_action),
                            onClick = {
                                keyboardController?.hide()
                                onIntent(ProductFormUiIntent.SaveClicked)
                            },
                            enabled = !uiState.isSaving,
                            isLoading = uiState.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Product Form Add Preview")
@Composable
private fun ProductFormAddPreview() {
    VivaanEnterpriseTheme {
        ProductFormScreen(
            uiState = ProductFormUiState(),
            onIntent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "Product Form Edit Preview")
@Composable
private fun ProductFormEditPreview() {
    VivaanEnterpriseTheme {
        ProductFormScreen(
            uiState = ProductFormUiState(
                productId = "p-100",
                name = "3M anti-slip 15mm Scotch Tape",
                hsnSac = "3919",
                gstRateInput = "18",
                isActive = true
            ),
            onIntent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
