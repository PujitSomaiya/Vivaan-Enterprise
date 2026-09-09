package com.vivaanenterprise.app.feature.invoice.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vivaanenterprise.app.core.designsystem.component.AppCard
import com.vivaanenterprise.app.core.designsystem.component.AppSearchField
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.domain.model.Client

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientSelectorBottomSheet(
    clients: List<Client>,
    selectedClient: Client?,
    onSelectClient: (Client) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filteredClients = remember(clients, searchQuery) {
        if (searchQuery.isBlank()) clients
        else clients.filter {
            it.companyName.contains(searchQuery, ignoreCase = true) ||
                    (it.gstin?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = AppTheme.spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Client",
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

            AppSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search client by name or GSTIN...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.md))

            if (filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.spacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No clients found",
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = filteredClients,
                        key = { it.id }
                    ) { client ->
                        val isSelected = client.id == selectedClient?.id
                        AppCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppTheme.spacing.xs)
                                .clickable {
                                    onSelectClient(client)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppTheme.spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSelectClient(client)
                                        onDismiss()
                                    }
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = AppTheme.spacing.xs)
                                ) {
                                    Text(
                                        text = client.companyName,
                                        style = AppTheme.typography.titleMedium,
                                        color = AppTheme.colorScheme.onSurface
                                    )
                                    if (!client.gstin.isNullOrBlank()) {
                                        Text(
                                            text = "GSTIN: ${client.gstin}",
                                            style = AppTheme.typography.bodySmall,
                                            color = AppTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val location = listOfNotNull(client.state, client.stateCode).joinToString(" — ")
                                    if (location.isNotBlank()) {
                                        Text(
                                            text = "State: $location",
                                            style = AppTheme.typography.bodySmall,
                                            color = AppTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
