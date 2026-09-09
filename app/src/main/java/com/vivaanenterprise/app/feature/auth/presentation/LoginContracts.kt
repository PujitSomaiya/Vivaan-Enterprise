package com.vivaanenterprise.app.feature.auth.presentation

import com.vivaanenterprise.app.feature.auth.model.AuthError

enum class LoginFieldValidationError {
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    PASSWORD_REQUIRED
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: LoginFieldValidationError? = null,
    val passwordError: LoginFieldValidationError? = null
)

sealed interface LoginUiIntent {
    data class EmailChanged(val email: String) : LoginUiIntent
    data class PasswordChanged(val password: String) : LoginUiIntent
    data object PasswordVisibilityToggleClicked : LoginUiIntent
    data object LoginClicked : LoginUiIntent
}

sealed interface LoginUiEffect {
    data class ShowError(val error: AuthError) : LoginUiEffect
}
