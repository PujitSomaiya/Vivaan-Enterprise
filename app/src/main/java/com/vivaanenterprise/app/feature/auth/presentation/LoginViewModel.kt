package com.vivaanenterprise.app.feature.auth.presentation

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.domain.repository.AuthRepository
import com.vivaanenterprise.app.feature.auth.model.AuthErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<LoginUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onIntent(intent: LoginUiIntent) {
        when (intent) {
            is LoginUiIntent.EmailChanged -> {
                _uiState.update { current ->
                    current.copy(
                        email = intent.email,
                        emailError = null
                    )
                }
            }
            is LoginUiIntent.PasswordChanged -> {
                _uiState.update { current ->
                    current.copy(
                        password = intent.password,
                        passwordError = null
                    )
                }
            }
            is LoginUiIntent.PasswordVisibilityToggleClicked -> {
                _uiState.update { current ->
                    current.copy(isPasswordVisible = !current.isPasswordVisible)
                }
            }
            is LoginUiIntent.LoginClicked -> login()
        }
    }

    private fun login() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        val trimmedEmail = currentState.email.trim()
        val emailError = when {
            trimmedEmail.isEmpty() -> LoginFieldValidationError.EMAIL_REQUIRED
            !com.vivaanenterprise.app.feature.auth.model.EmailValidator.isValidEmail(trimmedEmail) -> LoginFieldValidationError.EMAIL_INVALID
            else -> null
        }

        val passwordError = if (currentState.password.isEmpty()) {
            LoginFieldValidationError.PASSWORD_REQUIRED
        } else {
            null
        }

        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, emailError = null, passwordError = null) }

        viewModelScope.launch {
            try {
                val result = authRepository.signInWithEmail(trimmedEmail, currentState.password)
                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    val throwable = result.exceptionOrNull() ?: Exception("Unknown error")
                    if (throwable is CancellationException) {
                        _uiState.update { it.copy(isLoading = false) }
                        throw throwable
                    }
                    val mappedError = AuthErrorMapper.mapThrowableToAuthError(throwable)
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.send(LoginUiEffect.ShowError(mappedError))
                }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw e
            } catch (e: Exception) {
                val mappedError = AuthErrorMapper.mapThrowableToAuthError(e)
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.send(LoginUiEffect.ShowError(mappedError))
            }
        }
    }
}
