package com.vivaanenterprise.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.designsystem.theme.*

@Composable
fun AppLoadingState(
    modifier: Modifier = Modifier,
    label: String = stringResource(id = R.string.loading_default)
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.md),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = AppTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))
            Text(
                text = label,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AppEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = AppTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.md))
            Text(
                text = title,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (message != null) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                Text(
                    text = message,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.md))
                AppPrimaryButton(
                    text = actionText,
                    onClick = onActionClick,
                    fullWidth = false
                )
            }
        }
    }
}

@Composable
fun AppErrorState(
    title: String = stringResource(id = R.string.error_title_default),
    modifier: Modifier = Modifier,
    message: String? = null,
    retryText: String = stringResource(id = R.string.retry_action_default),
    onRetryClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = AppTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.md))
            Text(
                text = title,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            if (message != null) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                Text(
                    text = message,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (onRetryClick != null) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.md))
                AppSecondaryButton(
                    text = retryText,
                    onClick = onRetryClick,
                    fullWidth = false
                )
            }
        }
    }
}

@Composable
fun AppSyncIndicator(
    status: com.vivaanenterprise.app.core.common.SyncStatus,
    modifier: Modifier = Modifier
) {
    val bgColor = when (status) {
        com.vivaanenterprise.app.core.common.SyncStatus.SYNCED -> LightSuccessContainer
        com.vivaanenterprise.app.core.common.SyncStatus.PENDING -> LightWarningContainer
        com.vivaanenterprise.app.core.common.SyncStatus.FAILED -> LightErrorContainer
    }
    val textColor = when (status) {
        com.vivaanenterprise.app.core.common.SyncStatus.SYNCED -> LightOnSuccessContainer
        com.vivaanenterprise.app.core.common.SyncStatus.PENDING -> LightOnWarningContainer
        com.vivaanenterprise.app.core.common.SyncStatus.FAILED -> LightOnErrorContainer
    }
    val text = when (status) {
        com.vivaanenterprise.app.core.common.SyncStatus.SYNCED -> "SYNCED"
        com.vivaanenterprise.app.core.common.SyncStatus.PENDING -> "PENDING"
        com.vivaanenterprise.app.core.common.SyncStatus.FAILED -> "FAILED"
    }

    androidx.compose.material3.Surface(
        color = bgColor,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = text,
                style = AppTheme.typography.labelSmall,
                color = textColor,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}
