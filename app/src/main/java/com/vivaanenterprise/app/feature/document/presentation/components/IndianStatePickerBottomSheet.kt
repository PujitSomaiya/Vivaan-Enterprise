package com.vivaanenterprise.app.feature.document.presentation.components

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
import androidx.compose.ui.res.stringResource
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.component.AppCard
import com.vivaanenterprise.app.core.designsystem.component.AppSearchField
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.domain.model.IndianState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndianStatePickerBottomSheet(
    selectedStateCode: String,
    onSelectState: (IndianState) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.select_state_title),
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val states = remember { IndianState.ALL_STATES }
    val filteredStates = remember(states, searchQuery) {
        if (searchQuery.isBlank()) states
        else states.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery)
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
                    text = title,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_desc)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

            AppSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = stringResource(R.string.search_state_placeholder),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.md))

            if (filteredStates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.spacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_state_found),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = filteredStates,
                        key = { it.code }
                    ) { stateItem ->
                        val isSelected = stateItem.code == selectedStateCode
                        AppCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppTheme.spacing.xs)
                                .clickable {
                                    onSelectState(stateItem)
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
                                    onClick = null
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = AppTheme.spacing.xs)
                                ) {
                                    Text(
                                        text = stateItem.name,
                                        style = AppTheme.typography.titleMedium,
                                        color = AppTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.gst_state_code_label, stateItem.code),
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
