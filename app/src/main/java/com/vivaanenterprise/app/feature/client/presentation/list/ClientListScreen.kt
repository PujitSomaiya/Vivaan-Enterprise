package com.vivaanenterprise.app.feature.client.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.component.AppCard
import com.vivaanenterprise.app.core.designsystem.component.AppEmptyState
import com.vivaanenterprise.app.core.designsystem.component.AppLoadingState
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.AppSearchField
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.designsystem.theme.VivaanEnterpriseTheme
import com.vivaanenterprise.app.domain.model.Client

@Composable
fun ClientListRoute(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddClient: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClientListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ClientListUiEffect.NavigateToDetail -> onNavigateToDetail(effect.clientId)
                is ClientListUiEffect.NavigateToAddClient -> onNavigateToAddClient()
            }
        }
    }

    ClientListScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    uiState: ClientListUiState,
    onIntent: (ClientListUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.clients_title)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onIntent(ClientListUiIntent.AddClientClicked) },
                containerColor = AppTheme.colorScheme.primary,
                contentColor = AppTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_client_title)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AppSearchField(
                query = uiState.searchQuery,
                onQueryChange = { onIntent(ClientListUiIntent.SearchQueryChanged(it)) },
                placeholder = stringResource(R.string.search_clients_placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.spacing.md)
            )

            when {
                uiState.isLoading -> {
                    AppLoadingState(modifier = Modifier.fillMaxSize())
                }
                uiState.clients.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    AppEmptyState(
                        title = stringResource(R.string.empty_search_title),
                        message = stringResource(R.string.empty_search_description),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.clients.isEmpty() -> {
                    AppEmptyState(
                        title = stringResource(R.string.empty_clients_title),
                        message = stringResource(R.string.empty_clients_description),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = AppTheme.spacing.md,
                            end = AppTheme.spacing.md,
                            bottom = AppTheme.spacing.xxl
                        ),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                    ) {
                        items(
                            items = uiState.clients,
                            key = { client -> client.id }
                        ) { client ->
                            ClientItemRow(
                                client = client,
                                onClick = { onIntent(ClientListUiIntent.ClientClicked(client.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientItemRow(
    client: Client,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.md)
        ) {
            Text(
                text = client.companyName,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colorScheme.onSurface
            )

            if (!client.gstin.isNullOrBlank() || !client.state.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    client.gstin?.let { gstin ->
                        Text(
                            text = "GSTIN: $gstin",
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    client.state?.let { state ->
                        Text(
                            text = state,
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Client List Content Preview")
@Composable
private fun ClientListContentPreview() {
    VivaanEnterpriseTheme {
        ClientListScreen(
            uiState = ClientListUiState(
                clients = listOf(
                    Client(
                        id = "1",
                        companyName = "Eco Enterprise",
                        gstin = "24CHWPG0910J1ZB",
                        state = "Gujarat",
                        createdAt = 1000L,
                        updatedAt = 1000L
                    ),
                    Client(
                        id = "2",
                        companyName = "Mahalaxmi Traders",
                        gstin = null,
                        state = "Maharashtra",
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "Client List Empty Preview")
@Composable
private fun ClientListEmptyPreview() {
    VivaanEnterpriseTheme {
        ClientListScreen(
            uiState = ClientListUiState(clients = emptyList()),
            onIntent = {}
        )
    }
}
