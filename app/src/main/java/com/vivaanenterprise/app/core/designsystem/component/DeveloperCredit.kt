package com.vivaanenterprise.app.core.designsystem.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme

@Composable
fun DeveloperCredit(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val portfolioUrl = stringResource(R.string.developer_credit_portfolio_url)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.developer_credit_prefix),
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.developer_credit_name),
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(portfolioUrl))
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // Fail-safe handling if no browser application is installed
                }
            }
        )
    }
}
