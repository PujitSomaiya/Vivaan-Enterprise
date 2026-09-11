package com.vivaanenterprise.app.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.designsystem.theme.VivaanEnterpriseTheme

/**
 * Modern, professional VE monogram logo mark for Vivaan Enterprise.
 * Renders a clean "VE" industrial/business mark within a rounded badge.
 */
@Composable
fun VeLogo(
    modifier: Modifier = Modifier,
    size: Dp = AppTheme.sizing.logoMedium,
    primaryColor: Color = AppTheme.colorScheme.primary,
    accentColor: Color = AppTheme.colorScheme.secondary,
    contentDescription: String? = null
) {
    val semanticsModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier.clearAndSetSemantics { }
    }

    Box(
        modifier = semanticsModifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // 1. Soft Rounded Badge Background
            val badgeSize = w * 0.96f
            val badgeOffset = (w - badgeSize) / 2f
            drawRoundRect(
                color = primaryColor.copy(alpha = 0.08f),
                topLeft = Offset(badgeOffset, badgeOffset),
                size = Size(badgeSize, badgeSize),
                cornerRadius = CornerRadius(badgeSize * 0.22f, badgeSize * 0.22f)
            )

            // 2. Outer Geometric Badge Border
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(badgeOffset, badgeOffset),
                size = Size(badgeSize, badgeSize),
                cornerRadius = CornerRadius(badgeSize * 0.22f, badgeSize * 0.22f),
                style = Stroke(width = w * 0.045f)
            )

            val strokeW = w * 0.085f
            val cap = StrokeCap.Round
            val join = StrokeJoin.Round

            // 3. Clean 'V' Monogram (Left side)
            val vPath = Path().apply {
                moveTo(w * 0.18f, h * 0.30f)
                lineTo(w * 0.35f, h * 0.70f)
                lineTo(w * 0.52f, h * 0.30f)
            }
            drawPath(
                path = vPath,
                color = primaryColor,
                style = Stroke(width = strokeW, cap = cap, join = join)
            )

            // 4. Clean 'E' Monogram (Right side)
            // Vertical Spine of E
            drawLine(
                color = primaryColor,
                start = Offset(w * 0.58f, h * 0.30f),
                end = Offset(w * 0.58f, h * 0.70f),
                strokeWidth = strokeW,
                cap = cap
            )
            // Top Bar of E
            drawLine(
                color = primaryColor,
                start = Offset(w * 0.58f, h * 0.30f),
                end = Offset(w * 0.82f, h * 0.30f),
                strokeWidth = strokeW,
                cap = cap
            )
            // Middle Bar of E (Accent color)
            drawLine(
                color = accentColor,
                start = Offset(w * 0.58f, h * 0.50f),
                end = Offset(w * 0.78f, h * 0.50f),
                strokeWidth = strokeW * 0.9f,
                cap = cap
            )
            // Bottom Bar of E
            drawLine(
                color = primaryColor,
                start = Offset(w * 0.58f, h * 0.70f),
                end = Offset(w * 0.82f, h * 0.70f),
                strokeWidth = strokeW,
                cap = cap
            )
        }
    }
}

/**
 * Full Vivaan Enterprise brand component displaying the VE logo mark and company title.
 */
@Composable
fun VivaanBrandLogo(
    modifier: Modifier = Modifier,
    logoSize: Dp = AppTheme.sizing.logoMedium,
    isHorizontal: Boolean = false
) {
    if (isHorizontal) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            VeLogo(size = logoSize)
            Spacer(modifier = Modifier.width(AppTheme.spacing.md))
            Column {
                Text(
                    text = "VE",
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = AppTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colorScheme.onSurface
                )
            }
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VeLogo(size = logoSize)
            Spacer(modifier = Modifier.height(AppTheme.spacing.md))
            Text(
                text = stringResource(R.string.app_name),
                style = AppTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.primary
            )
        }
    }
}

@Preview(name = "VeLogo Light Preview", showBackground = true)
@Composable
private fun VeLogoPreview() {
    VivaanEnterpriseTheme {
        Column(
            modifier = Modifier.size(200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VeLogo(size = 96.dp)
        }
    }
}

@Preview(name = "VivaanBrandLogo Preview", showBackground = true)
@Composable
private fun VivaanBrandLogoPreview() {
    VivaanEnterpriseTheme {
        VivaanBrandLogo(logoSize = 80.dp)
    }
}
