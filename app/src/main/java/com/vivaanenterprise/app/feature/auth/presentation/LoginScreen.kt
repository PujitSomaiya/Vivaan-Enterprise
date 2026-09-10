package com.vivaanenterprise.app.feature.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.core.designsystem.component.AppCard
import com.vivaanenterprise.app.core.designsystem.component.AppPrimaryButton
import com.vivaanenterprise.app.core.designsystem.component.AppScaffold
import com.vivaanenterprise.app.core.designsystem.component.AppTextField
import com.vivaanenterprise.app.core.designsystem.component.VeLogo
import com.vivaanenterprise.app.core.designsystem.component.VivaanBrandLogo
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.core.designsystem.theme.VivaanEnterpriseTheme

@Composable
fun LoginRoute(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = androidx.compose.ui.platform.LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is LoginUiEffect.ShowError -> {
                    val message = resources.getString(effect.error.toLocalizedStringRes())
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    LoginScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onIntent: (LoginUiIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val emailErrorMessage = uiState.emailError?.let { error ->
        when (error) {
            LoginFieldValidationError.EMAIL_REQUIRED -> stringResource(R.string.error_email_required)
            LoginFieldValidationError.EMAIL_INVALID -> stringResource(R.string.error_email_invalid)
            else -> null
        }
    }

    val passwordErrorMessage = uiState.passwordError?.let { error ->
        when (error) {
            LoginFieldValidationError.PASSWORD_REQUIRED -> stringResource(R.string.error_password_required)
            else -> null
        }
    }

    AppScaffold(
        modifier = modifier,
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(AppTheme.spacing.md)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VivaanBrandLogo(logoSize = AppTheme.sizing.logoMedium)

                    Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                    Text(
                        text = stringResource(R.string.login_title),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                    AppTextField(
                        value = uiState.email,
                        onValueChange = { onIntent(LoginUiIntent.EmailChanged(it)) },
                        label = stringResource(R.string.email_label),
                        errorText = emailErrorMessage,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                    AppTextField(
                        value = uiState.password,
                        onValueChange = { onIntent(LoginUiIntent.PasswordChanged(it)) },
                        label = stringResource(R.string.password_label),
                        errorText = passwordErrorMessage,
                        enabled = !uiState.isLoading,
                        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = { onIntent(LoginUiIntent.PasswordVisibilityToggleClicked) },
                                enabled = !uiState.isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = stringResource(R.string.password_visibility_toggle)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                onIntent(LoginUiIntent.LoginClicked)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                    AppPrimaryButton(
                        text = stringResource(R.string.sign_in_action),
                        onClick = {
                            keyboardController?.hide()
                            onIntent(LoginUiIntent.LoginClicked)
                        },
                        enabled = !uiState.isLoading,
                        isLoading = uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Preview(name = "Login Screen Light")
@Composable
private fun LoginScreenLightPreview() {
    VivaanEnterpriseTheme(darkTheme = false) {
        LoginScreen(
            uiState = LoginUiState(),
            onIntent = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "Login Screen Dark")
@Composable
private fun LoginScreenDarkPreview() {
    VivaanEnterpriseTheme(darkTheme = true) {
        LoginScreen(
            uiState = LoginUiState(email = "user@example.com", isLoading = false),
            onIntent = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
