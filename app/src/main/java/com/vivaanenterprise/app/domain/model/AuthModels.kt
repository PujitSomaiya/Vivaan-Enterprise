package com.vivaanenterprise.app.domain.model

data class AuthenticatedUser(
    val id: String,
    val email: String?
)

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: AuthenticatedUser) : AuthState
}
