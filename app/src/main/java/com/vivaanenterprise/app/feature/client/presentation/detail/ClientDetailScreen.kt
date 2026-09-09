package com.vivaanenterprise.app.feature.client.presentation.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.component.AppCard
import com.vivaanenterprise.app.core.designsystem.component.AppEmptyState
import com.vivaanenterprise.app.core.designsystem.component.AppLoadingState
import com.vivaanenterprise.app.core.designsystem.component.AppPrimaryButton
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.AppSecondaryButton
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.designsystem.theme.VivaanEnterpriseTheme
import com.vivaanenterprise.app.domain.model.Client

@Composable
fun ClientDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClientDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ClientDetailUiEffect.NavigateToEdit -> onNavigateToEdit(effect.clientId)
                ClientDetailUiEffect.DeleteSuccess -> onNavigateBack()
                is ClientDetailUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                }
            }
        }
    }

    ClientDetailScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    uiState: ClientDetailUiState,
    onIntent: (ClientDetailUiIntent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && uiState.client != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = stringResource(R.string.delete_client_dialog_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.delete_client_dialog_message,
                        uiState.client.companyName
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onIntent(ClientDetailUiIntent.ConfirmDeleteClicked)
                    }
                ) {
                    Text(text = stringResource(R.string.delete_action), color = AppTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(R.string.cancel_action))
                }
            }
        )
    }

    AppScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.client_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back_desc)
                        )
                    }
                },
                actions = {
                    if (uiState.client != null) {
                        IconButton(onClick = { onIntent(ClientDetailUiIntent.EditClicked) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_action)
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_action)
                            )
                        }
                    }
                }
            )
        },
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        when {
            uiState.isLoading -> AppLoadingState(modifier = Modifier.fillMaxSize())
            uiState.client == null -> {
                AppEmptyState(
                    title = stringResource(R.string.error_client_not_found),
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                val client = uiState.client
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
                                .padding(AppTheme.spacing.lg)
                        ) {
                            Text(
                                text = client.companyName,
                                style = AppTheme.typography.headlineSmall,
                                color = AppTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                            client.address?.let { address ->
                                DetailRow(label = stringResource(R.string.address_label), value = address)
                            }

                            client.gstin?.let { gstin ->
                                DetailRow(label = stringResource(R.string.gstin_label), value = gstin)
                            }

                            client.state?.let { state ->
                                DetailRow(label = stringResource(R.string.state_label), value = state)
                            }

                            client.stateCode?.let { stateCode ->
                                DetailRow(label = stringResource(R.string.state_code_label), value = stateCode)
                            }

                            client.email?.let { email ->
                                DetailRow(label = stringResource(R.string.email_label), value = email)
                            }

                            client.phone?.let { phone ->
                                DetailRow(label = stringResource(R.string.phone_label), value = phone)
                            }

                            client.pan?.let { pan ->
                                DetailRow(label = stringResource(R.string.pan_label), value = pan)
                            }

                            client.iec?.let { iec ->
                                DetailRow(label = stringResource(R.string.iec_label), value = iec)
                            }

                            client.otherDetails?.let { otherDetails ->
                                DetailRow(label = stringResource(R.string.other_details_label), value = otherDetails)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        AppPrimaryButton(
                            text = stringResource(R.string.edit_action),
                            onClick = { onIntent(ClientDetailUiIntent.EditClicked) },
                            fullWidth = false,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(AppTheme.spacing.md))
                        AppSecondaryButton(
                            text = stringResource(R.string.delete_action),
                            onClick = { showDeleteDialog = true },
                            fullWidth = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = AppTheme.spacing.xs)) {
        Text(
            text = label,
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colorScheme.onSurface
        )
    }
}

@Preview(name = "Client Detail Preview")
@Composable
private fun ClientDetailPreview() {
    VivaanEnterpriseTheme {
        ClientDetailScreen(
            uiState = ClientDetailUiState(
                client = Client(
                    id = "1",
                    companyName = "Eco Enterprise",
                    address = "Street 4, Mahatma Gandhi Road, Rajkot",
                    gstin = "24CHWPG0910J1ZB",
                    state = "Gujarat",
                    stateCode = "24",
                    email = "contact@ecoenterprise.in",
                    phone = "+91 98765 43210",
                    pan = "CHWPG0910J",
                    createdAt = 1000L,
                    updatedAt = 1000L
                )
            ),
            onIntent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
