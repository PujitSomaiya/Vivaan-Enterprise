package com.vivaanenterprise.app.domain.repository

import com.vivaanenterprise.app.domain.model.AuthState
import com.vivaanenterprise.app.domain.model.AuthenticatedUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>
    fun getCurrentUser(): AuthenticatedUser?
    suspend fun signInWithEmail(email: String, password: String): Result<AuthenticatedUser>
    suspend fun signOut(): Result<Unit>
}
