package com.vivaanenterprise.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    fullWidth: Boolean = true
) {
    val buttonModifier = if (fullWidth) {
        modifier.fillMaxWidth().height(AppTheme.sizing.buttonHeight)
    } else {
        modifier.height(AppTheme.sizing.buttonHeight)
    }

    Button(
        onClick = onClick,
        modifier = buttonModifier,
        enabled = enabled && !isLoading,
        shape = AppTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colorScheme.primary,
            contentColor = AppTheme.colorScheme.onPrimary,
            disabledContainerColor = AppTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor = AppTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AppTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = AppTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    fullWidth: Boolean = true
) {
    val buttonModifier = if (fullWidth) {
        modifier.fillMaxWidth().height(AppTheme.sizing.buttonHeight)
    } else {
        modifier.height(AppTheme.sizing.buttonHeight)
    }

    OutlinedButton(
        onClick = onClick,
        modifier = buttonModifier,
        enabled = enabled && !isLoading,
        shape = AppTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppTheme.colorScheme.secondary,
            disabledContentColor = AppTheme.colorScheme.secondary.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AppTheme.colorScheme.secondary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = AppTheme.typography.labelLarge
            )
        }
    }
}
