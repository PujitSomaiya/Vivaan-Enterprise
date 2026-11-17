package com.vivaanenterprise.app.feature.document.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vivaanenterprise.app.core.designsystem.component.AppPrimaryButton
import com.vivaanenterprise.app.core.designsystem.component.AppSecondaryButton
import com.vivaanenterprise.app.core.designsystem.component.AppSectionHeader
import com.vivaanenterprise.app.core.designsystem.component.AppTextField
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.domain.model.IndianState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reusable header information form section for Invoices and Purchase Orders.
 */
@Composable
fun DocumentHeaderSection(
    documentNumber: String,
    onDocumentNumberChange: (String) -> Unit,
    documentNumberLabel: String,
    documentNumberError: String?,
    documentDate: Long,
    onDateClick: () -> Unit,
    dateLabel: String,
    selectedClientName: String,
    onClientClick: () -> Unit,
    clientLabel: String,
    clientError: String?,
    deliveryFactoryAddress: String,
    onDeliveryFactoryAddressChange: (String) -> Unit,
    deliveryFactoryAddressError: String?,
    placeOfSupplyStateCode: String,
    onPlaceOfSupplyClick: () -> Unit,
    placeOfSupplyError: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
        AppSectionHeader(title = "Header Information")
        Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

        // Document Number
        AppTextField(
            value = documentNumber,
            onValueChange = onDocumentNumberChange,
            label = documentNumberLabel,
            errorText = documentNumberError,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

        // Document Date
        val formattedDate = remember(documentDate) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(documentDate))
        }
        AppTextField(
            value = formattedDate,
            onValueChange = {},
            label = dateLabel,
            onClick = onDateClick,
            trailingIcon = {
                Text(
                    text = "Change",
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = AppTheme.spacing.xs)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

        // Client / Supplier Picker
        AppTextField(
            value = selectedClientName,
            onValueChange = {},
            label = clientLabel,
            onClick = onClientClick,
            errorText = clientError,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select $clientLabel"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

        // Delivery / Factory Address
        AppTextField(
            value = deliveryFactoryAddress,
            onValueChange = onDeliveryFactoryAddressChange,
            label = "Delivery / Factory Address",
            errorText = deliveryFactoryAddressError,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

        // Place of Supply Picker
        val posState = remember(placeOfSupplyStateCode) {
            IndianState.findByCode(placeOfSupplyStateCode)
        }
        val posDisplayText = posState?.displayName ?: "State Code: $placeOfSupplyStateCode"

        AppTextField(
            value = posDisplayText,
            onValueChange = {},
            label = "Place of Supply *",
            onClick = onPlaceOfSupplyClick,
            errorText = placeOfSupplyError,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select Place of Supply"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Reusable Line Items section header with an Add Item action button.
 */
@Composable
fun DocumentLineItemsHeader(
    onAddLineItem: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(modifier = Modifier.weight(1f)) {
            AppSectionHeader(title = "Line Items")
        }
        TextButton(onClick = onAddLineItem) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.padding(horizontal = AppTheme.spacing.xxs))
            Text(text = "Add Item")
        }
    }
}

/**
 * Reusable bottom action buttons (Save Draft & Finalize) for document forms.
 */
@Composable
fun DocumentFormActionButtons(
    primaryButtonText: String,
    onRequestFinalize: () -> Unit,
    onSaveDraft: () -> Unit,
    isFinalizing: Boolean,
    isSavingDraft: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
    ) {
        AppPrimaryButton(
            text = primaryButtonText,
            onClick = onRequestFinalize,
            isLoading = isFinalizing,
            enabled = !isSavingDraft && !isFinalizing,
            modifier = Modifier.fillMaxWidth()
        )

        AppSecondaryButton(
            text = "Save Draft",
            onClick = onSaveDraft,
            isLoading = isSavingDraft,
            enabled = !isSavingDraft && !isFinalizing,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.lg))
    }
}
