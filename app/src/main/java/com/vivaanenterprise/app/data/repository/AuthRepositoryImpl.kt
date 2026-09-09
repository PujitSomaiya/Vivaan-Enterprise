package com.vivaanenterprise.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.vivaanenterprise.app.domain.model.AuthState
import com.vivaanenterprise.app.domain.model.AuthenticatedUser
import com.vivaanenterprise.app.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val authState: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                trySend(AuthState.SignedIn(AuthenticatedUser(id = user.uid, email = user.email)))
            } else {
                trySend(AuthState.SignedOut)
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    override fun getCurrentUser(): AuthenticatedUser? {
        val user = firebaseAuth.currentUser ?: return null
        return AuthenticatedUser(id = user.uid, email = user.email)
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthenticatedUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Authentication succeeded but user was null"))
            Result.success(AuthenticatedUser(id = user.uid, email = user.email))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
