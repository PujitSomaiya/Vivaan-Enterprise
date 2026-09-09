package com.vivaanenterprise.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AppSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp
)

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }

@Immutable
data class AppSizing(
    val minTouchTarget: Dp = 48.dp,
    val buttonHeight: Dp = 48.dp,
    val textFieldHeight: Dp = 56.dp,
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val logoMedium: Dp = 64.dp,
    val logoLarge: Dp = 96.dp
)

val LocalAppSizing = staticCompositionLocalOf { AppSizing() }
