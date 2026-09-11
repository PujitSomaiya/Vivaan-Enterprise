package com.vivaanenterprise.app.core.common.util

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vivaanenterprise.app.R
import kotlinx.coroutines.launch

private const val DOUBLE_BACK_TIMEOUT_MS = 2_000L

/**
 * Safely finds the host [ComponentActivity] from a Compose [Context].
 */
fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Reusable Compose BackHandler for double-back exit gesture on root screens.
 * Displays a non-blocking Snackbar on first press and finishes the Activity on second press within 2 seconds.
 */
@Composable
fun DoubleBackToExitHandler(
    snackbarHostState: SnackbarHostState,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = enabled) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < DOUBLE_BACK_TIMEOUT_MS) {
            val activity = context.findActivity()
            activity?.finish()
        } else {
            lastBackPressTime = currentTime
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = resources.getString(R.string.press_back_again_to_exit)
                )
            }
        }
    }
}
