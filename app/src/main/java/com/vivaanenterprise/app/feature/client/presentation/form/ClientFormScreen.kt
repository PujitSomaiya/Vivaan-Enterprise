package com.vivaanenterprise.app.feature.client.presentation.form

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vivaanenterprise.app.domain.model.IndianState
import com.vivaanenterprise.app.feature.document.presentation.components.IndianStatePickerBottomSheet
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
fun ClientFormRoute(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClientFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ClientFormUiEffect.SaveSuccess -> onSaveSuccess()
                is ClientFormUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                }
            }
        }
    }

    ClientFormScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormScreen(
    uiState: ClientFormUiState,
    onIntent: (ClientFormUiIntent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var showStatePicker by remember { mutableStateOf(false) }

    val titleRes = if (uiState.isEditMode) R.string.edit_client_title else R.string.add_client_title

    if (showStatePicker) {
        IndianStatePickerBottomSheet(
            selectedStateCode = uiState.stateCode,
            onSelectState = { onIntent(ClientFormUiIntent.StateSelected(it.code)) },
            onDismiss = { showStatePicker = false }
        )
    }

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
        if (uiState.isLoadingClient) {
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
                            value = uiState.companyName,
                            onValueChange = { onIntent(ClientFormUiIntent.CompanyNameChanged(it)) },
                            label = stringResource(R.string.company_name_label),
                            errorText = uiState.companyNameError?.let { stringResource(R.string.error_company_name_required) },
                            enabled = !uiState.isSaving,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        AppTextField(
                            value = uiState.address,
                            onValueChange = { onIntent(ClientFormUiIntent.AddressChanged(it)) },
                            label = stringResource(R.string.address_label),
                            enabled = !uiState.isSaving,
                            singleLine = false,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        AppTextField(
                            value = uiState.gstin,
                            onValueChange = { onIntent(ClientFormUiIntent.GstinChanged(it)) },
                            label = stringResource(R.string.gstin_label),
                            errorText = uiState.gstinError?.let { stringResource(R.string.error_gstin_invalid) },
                            enabled = !uiState.isSaving,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        val selectedIndianState = IndianState.findByCode(uiState.stateCode)
                        val stateDisplayValue = selectedIndianState?.displayName ?: if (uiState.state.isNotBlank()) "${uiState.state} (${uiState.stateCode})" else ""

                        AppTextField(
                            value = stateDisplayValue,
                            onValueChange = {},
                            label = stringResource(R.string.state_picker_label),
                            errorText = uiState.stateCodeError?.let { stringResource(R.string.error_state_code_invalid) },
                            enabled = !uiState.isSaving,
                            onClick = { showStatePicker = true },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(R.string.select_state_title)
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        AppTextField(
                            value = uiState.email,
                            onValueChange = { onIntent(ClientFormUiIntent.EmailChanged(it)) },
                            label = stringResource(R.string.email_label),
                            errorText = uiState.emailError?.let { stringResource(R.string.error_email_invalid) },
                            enabled = !uiState.isSaving,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        AppTextField(
                            value = uiState.phone,
                            onValueChange = { onIntent(ClientFormUiIntent.PhoneChanged(it)) },
                            label = stringResource(R.string.phone_label),
                            errorText = uiState.phoneError?.let { stringResource(R.string.error_phone_invalid) },
                            enabled = !uiState.isSaving,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        AppTextField(
                            value = uiState.pan,
                            onValueChange = { onIntent(ClientFormUiIntent.PanChanged(it)) },
                            label = stringResource(R.string.pan_label),
                            errorText = uiState.panError?.let { stringResource(R.string.error_pan_invalid) },
                            enabled = !uiState.isSaving,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        AppTextField(
                            value = uiState.iec,
                            onValueChange = { onIntent(ClientFormUiIntent.IecChanged(it)) },
                            label = stringResource(R.string.iec_label),
                            enabled = !uiState.isSaving,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        AppTextField(
                            value = uiState.otherDetails,
                            onValueChange = { onIntent(ClientFormUiIntent.OtherDetailsChanged(it)) },
                            label = stringResource(R.string.other_details_label),
                            enabled = !uiState.isSaving,
                            singleLine = false,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    onIntent(ClientFormUiIntent.SaveClicked)
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                        AppPrimaryButton(
                            text = stringResource(R.string.save_client_action),
                            onClick = {
                                keyboardController?.hide()
                                onIntent(ClientFormUiIntent.SaveClicked)
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

@Preview(name = "Client Form Add Preview")
@Composable
private fun ClientFormAddPreview() {
    VivaanEnterpriseTheme {
        ClientFormScreen(
            uiState = ClientFormUiState(),
            onIntent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "Client Form Edit Preview")
@Composable
private fun ClientFormEditPreview() {
    VivaanEnterpriseTheme {
        ClientFormScreen(
            uiState = ClientFormUiState(
                clientId = "123",
                companyName = "Eco Enterprise",
                gstin = "24CHWPG0910J1ZB",
                state = "Gujarat",
                stateCode = "24"
            ),
            onIntent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
