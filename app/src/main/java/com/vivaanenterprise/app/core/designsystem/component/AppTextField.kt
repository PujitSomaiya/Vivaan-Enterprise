package com.vivaanenterprise.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onClick: (() -> Unit)? = null
) {
    val isPicker = onClick != null
    val interactionSource = remember { MutableInteractionSource() }

    if (isPicker && enabled && onClick != null) {
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    onClick()
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                readOnly = if (isPicker) true else readOnly,
                interactionSource = interactionSource,
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
                shape = AppTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colorScheme.primary,
                    unfocusedBorderColor = AppTheme.colorScheme.outline,
                    disabledBorderColor = AppTheme.colorScheme.outline.copy(alpha = 0.38f),
                    disabledLabelColor = AppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    disabledTextColor = AppTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    errorBorderColor = AppTheme.colorScheme.error,
                    focusedLabelColor = AppTheme.colorScheme.primary,
                    unfocusedLabelColor = AppTheme.colorScheme.onSurfaceVariant,
                    errorLabelColor = AppTheme.colorScheme.error,
                    focusedContainerColor = AppTheme.colorScheme.surface,
                    unfocusedContainerColor = AppTheme.colorScheme.surface,
                    disabledContainerColor = AppTheme.colorScheme.surface
                )
            )
            if (isPicker && enabled && onClick != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            role = Role.Button,
                            onClick = onClick
                        )
                )
            }
        }
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

