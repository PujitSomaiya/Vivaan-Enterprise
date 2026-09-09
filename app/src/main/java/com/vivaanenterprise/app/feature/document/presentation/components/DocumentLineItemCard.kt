package com.vivaanenterprise.app.feature.document.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.vivaanenterprise.app.core.designsystem.component.AppCard
import com.vivaanenterprise.app.core.designsystem.component.AppTextField
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.domain.model.DocumentLineCalculation
import com.vivaanenterprise.app.domain.model.Product
import com.vivaanenterprise.app.feature.document.presentation.model.DocumentLineUiState

@Composable
fun DocumentLineItemCard(
    position: Int,
    lineState: DocumentLineUiState,
    lineCalc: DocumentLineCalculation?,
    availableProducts: List<Product>,
    onSelectProduct: (Product) -> Unit,
    onQuantityChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onRemoveLine: () -> Unit,
    canRemove: Boolean,
    modifier: Modifier = Modifier
) {
    var showProductPicker by remember { mutableStateOf(false) }

    if (showProductPicker) {
        ProductSelectorBottomSheet(
            products = availableProducts,
            selectedProduct = lineState.selectedProduct,
            onSelectProduct = onSelectProduct,
            onDismiss = { showProductPicker = false }
        )
    }

    AppCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Item #$position",
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                if (canRemove) {
                    IconButton(onClick = onRemoveLine) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove line item",
                            tint = AppTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

            // Product Selection trigger
            AppTextField(
                value = lineState.selectedProduct?.name ?: "",
                onValueChange = {},
                label = "Product *",
                readOnly = true,
                errorText = lineState.productError,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select product",
                        modifier = Modifier.clickable { showProductPicker = true }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProductPicker = true }
            )

            val prod = lineState.selectedProduct
            if (prod != null) {
                val gstPercent = com.vivaanenterprise.app.core.pdf.PdfFormattingUtils.formatGstRateBasisPoints(prod.defaultGstRateBasisPoints)
                Text(
                    text = "HSN/SAC: ${prod.hsnSac ?: "N/A"}  |  GST Rate: $gstPercent",
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = AppTheme.spacing.xs)
                )
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
            ) {
                AppTextField(
                    value = lineState.quantityInput,
                    onValueChange = onQuantityChange,
                    label = "Quantity *",
                    errorText = lineState.quantityError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                AppTextField(
                    value = lineState.rateInput,
                    onValueChange = onRateChange,
                    label = "Rate (₹) *",
                    errorText = lineState.rateError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            // Calculation result for line item
            if (lineCalc != null) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val taxable = "₹ ${com.vivaanenterprise.app.core.pdf.PdfFormattingUtils.formatPaiseToCurrency(lineCalc.taxableAmountPaise)}"
                    val tax = "₹ ${com.vivaanenterprise.app.core.pdf.PdfFormattingUtils.formatPaiseToCurrency(lineCalc.totalTaxPaise)}"
                    val total = "₹ ${com.vivaanenterprise.app.core.pdf.PdfFormattingUtils.formatPaiseToCurrency(lineCalc.lineTotalPaise)}"

                    Text(text = "Taxable: $taxable", style = AppTheme.typography.bodySmall)
                    Text(text = "Tax: $tax", style = AppTheme.typography.bodySmall)
                    Text(
                        text = "Total: $total",
                        style = AppTheme.typography.titleMedium,
                        color = AppTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
