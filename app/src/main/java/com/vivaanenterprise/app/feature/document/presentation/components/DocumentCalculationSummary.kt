package com.vivaanenterprise.app.feature.document.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vivaanenterprise.app.core.designsystem.component.AppCard
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.pdf.PdfFormattingUtils
import com.vivaanenterprise.app.domain.model.DocumentCalculationResult
import com.vivaanenterprise.app.domain.model.TaxTreatment

@Composable
fun DocumentCalculationSummary(
    preview: DocumentCalculationResult?,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.md)
        ) {
            Text(
                text = "Calculation Summary",
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

            if (preview == null) {
                Text(
                    text = "Fill in valid quantity and rate to calculate preview.",
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val taxableStr = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(preview.taxableAmountPaise)}"
                val totalTaxStr = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(preview.totalTaxAmountPaise)}"
                val grandTotalStr = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(preview.grandTotalPaise)}"

                SummaryRow("Taxable Amount:", taxableStr)

                when (preview.taxTreatment) {
                    TaxTreatment.INTER_STATE -> {
                        val igstStr = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(preview.igstAmountPaise)}"
                        SummaryRow("IGST:", igstStr)
                    }
                    TaxTreatment.INTRA_STATE -> {
                        val cgstStr = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(preview.cgstAmountPaise)}"
                        val sgstStr = "₹ ${PdfFormattingUtils.formatPaiseToCurrency(preview.sgstAmountPaise)}"
                        SummaryRow("CGST:", cgstStr)
                        SummaryRow("SGST:", sgstStr)
                    }
                }

                SummaryRow("Total Tax:", totalTaxStr)

                Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Grand Total:",
                        style = AppTheme.typography.titleLarge,
                        color = AppTheme.colorScheme.primary
                    )
                    Text(
                        text = grandTotalStr,
                        style = AppTheme.typography.titleLarge,
                        color = AppTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                preview.amountInWords?.let { words ->
                    Text(
                        text = words,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = AppTheme.typography.bodyMedium, color = AppTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = AppTheme.typography.bodyMedium, color = AppTheme.colorScheme.onSurface)
    }
}
