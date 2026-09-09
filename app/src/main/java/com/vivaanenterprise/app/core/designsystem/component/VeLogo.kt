package com.vivaanenterprise.app.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.designsystem.theme.LightPrimary
import com.vivaanenterprise.app.core.designsystem.theme.LightSecondary
import com.vivaanenterprise.app.core.designsystem.theme.VivaanEnterpriseTheme

@Composable
fun VeLogo(
    modifier: Modifier = Modifier,
    size: Dp = AppTheme.sizing.logoMedium,
    primaryColor: Color = AppTheme.colorScheme.primary,
    accentColor: Color = AppTheme.colorScheme.secondary,
    contentDescription: String = "Vivaan Enterprise Logo"
) {
    Box(
        modifier = modifier
            .size(size)
            .semantics {
                this.contentDescription = contentDescription
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Outer Document Frame
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(w * 0.1f, h * 0.1f),
                size = Size(w * 0.8f, h * 0.8f),
                cornerRadius = CornerRadius(w * 0.08f, h * 0.08f),
                style = Stroke(width = w * 0.07f)
            )

            // Minimal "V" Monogram Stroke
            val vPath = Path().apply {
                moveTo(w * 0.28f, h * 0.32f)
                lineTo(w * 0.50f, h * 0.68f)
                lineTo(w * 0.72f, h * 0.32f)
            }
            drawPath(
                path = vPath,
                color = primaryColor,
                style = Stroke(width = w * 0.08f)
            )

            // Accent Document Line ("E" Bar)
            drawLine(
                color = accentColor,
                start = Offset(w * 0.30f, h * 0.75f),
                end = Offset(w * 0.70f, h * 0.75f),
                strokeWidth = w * 0.06f
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VeLogoPreview() {
    VivaanEnterpriseTheme {
        VeLogo(size = 96.dp)
    }
}
