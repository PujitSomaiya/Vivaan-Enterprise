package com.vivaanenterprise.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    isError: Boolean = errorText != null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = AppTheme.typography.bodyLarge,
            label = {
                Text(
                    text = label,
                    style = AppTheme.typography.bodyMedium
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            shape = AppTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colorScheme.primary,
                unfocusedBorderColor = AppTheme.colorScheme.outline,
                errorBorderColor = AppTheme.colorScheme.error,
                focusedLabelColor = AppTheme.colorScheme.primary,
                unfocusedLabelColor = AppTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = AppTheme.colorScheme.error,
                focusedContainerColor = AppTheme.colorScheme.surface,
                unfocusedContainerColor = AppTheme.colorScheme.surface
            )
        )
        if (isError && !errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colorScheme.error,
                modifier = Modifier.padding(
                    start = AppTheme.spacing.sm,
                    top = AppTheme.spacing.xxs
                )
            )
        }
    }
}

