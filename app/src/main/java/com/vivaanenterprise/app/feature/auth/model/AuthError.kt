package com.vivaanenterprise.app.feature.auth.model

import com.vivaanenterprise.app.R

sealed interface AuthError {
    data object InvalidCredentials : AuthError
    data object NetworkUnavailable : AuthError
    data object TooManyRequests : AuthError
    data object UserDisabled : AuthError
    data class Unknown(val message: String? = null) : AuthError

    fun toLocalizedStringRes(): Int = when (this) {
        InvalidCredentials -> R.string.auth_error_invalid_credentials
        NetworkUnavailable -> R.string.auth_error_network
        TooManyRequests -> R.string.auth_error_too_many_requests
        UserDisabled -> R.string.auth_error_user_disabled
        is Unknown -> R.string.auth_error_generic
    }
}
