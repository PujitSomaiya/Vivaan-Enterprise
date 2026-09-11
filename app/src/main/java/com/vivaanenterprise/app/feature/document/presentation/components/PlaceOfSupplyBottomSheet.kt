package com.vivaanenterprise.app.feature.document.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.domain.model.IndianState

@Composable
fun PlaceOfSupplyBottomSheet(
    selectedStateCode: String,
    onSelectState: (IndianState) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    IndianStatePickerBottomSheet(
        selectedStateCode = selectedStateCode,
        onSelectState = onSelectState,
        onDismiss = onDismiss,
        title = stringResource(R.string.select_place_of_supply_title),
        modifier = modifier
    )
}
