package com.vivaanenterprise.app.feature.auth.model

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import java.util.Locale

object AuthErrorMapper {
    fun mapThrowableToAuthError(throwable: Throwable): AuthError {
        return when (throwable) {
            is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials
            is FirebaseAuthInvalidUserException -> {
                val errorCode = throwable.errorCode.lowercase(Locale.ROOT)
                if (errorCode.contains("disabled")) {
                    AuthError.UserDisabled
                } else {
                    AuthError.InvalidCredentials
                }
            }
            is FirebaseNetworkException -> AuthError.NetworkUnavailable
            else -> {
                val message = throwable.message?.lowercase(Locale.ROOT) ?: ""
                when {
                    message.contains("network") || message.contains("connection") -> AuthError.NetworkUnavailable
                    message.contains("too many") || message.contains("blocked") -> AuthError.TooManyRequests
                    message.contains("invalid") || message.contains("credential") || message.contains("password") -> AuthError.InvalidCredentials
                    message.contains("disabled") -> AuthError.UserDisabled
                    else -> AuthError.Unknown(throwable.message)
                }
            }
        }
    }
}
