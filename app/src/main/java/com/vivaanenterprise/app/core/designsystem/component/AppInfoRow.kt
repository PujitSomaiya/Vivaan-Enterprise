package com.vivaanenterprise.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme

@Composable
fun AppInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isEmphasized: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))
        Text(
            text = value.ifBlank { "—" },
            style = if (isEmphasized) AppTheme.typography.titleMedium else AppTheme.typography.bodyLarge,
            color = AppTheme.colorScheme.onSurface,
            fontWeight = if (isEmphasized) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun AppHorizontalInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isEmphasized: Boolean = false,
    valueColor: Color = AppTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isEmphasized) AppTheme.typography.titleMedium else AppTheme.typography.bodyLarge,
            color = valueColor,
            fontWeight = if (isEmphasized) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }
}
