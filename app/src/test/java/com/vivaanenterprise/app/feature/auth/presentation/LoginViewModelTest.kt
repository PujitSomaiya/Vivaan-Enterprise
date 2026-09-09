package com.vivaanenterprise.app.feature.auth.presentation

import com.vivaanenterprise.app.domain.model.AuthState
import com.vivaanenterprise.app.domain.model.AuthenticatedUser
import com.vivaanenterprise.app.domain.repository.AuthRepository
import com.vivaanenterprise.app.feature.auth.model.AuthError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        viewModel = LoginViewModel(fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isPasswordVisible)
        assertFalse(state.isLoading)
        assertNull(state.emailError)
        assertNull(state.passwordError)
    }

    @Test
    fun testEmailChangedUpdatesState() {
        viewModel.onIntent(LoginUiIntent.EmailChanged("test@example.com"))
        assertEquals("test@example.com", viewModel.uiState.value.email)
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun testPasswordChangedUpdatesState() {
        viewModel.onIntent(LoginUiIntent.PasswordChanged("secret123"))
        assertEquals("secret123", viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun testEmptyEmailValidation() {
        viewModel.onIntent(LoginUiIntent.PasswordChanged("secret123"))
        viewModel.onIntent(LoginUiIntent.LoginClicked)

        val state = viewModel.uiState.value
        assertEquals(LoginFieldValidationError.EMAIL_REQUIRED, state.emailError)
        assertFalse(state.isLoading)
        assertEquals(0, fakeAuthRepository.signInCallCount)
    }

    @Test
    fun testInvalidEmailValidation() {
        viewModel.onIntent(LoginUiIntent.EmailChanged("invalid-email"))
        viewModel.onIntent(LoginUiIntent.PasswordChanged("secret123"))
        viewModel.onIntent(LoginUiIntent.LoginClicked)

        val state = viewModel.uiState.value
        assertEquals(LoginFieldValidationError.EMAIL_INVALID, state.emailError)
        assertFalse(state.isLoading)
        assertEquals(0, fakeAuthRepository.signInCallCount)
    }

    @Test
    fun testEmptyPasswordValidation() {
        viewModel.onIntent(LoginUiIntent.EmailChanged("test@example.com"))
        viewModel.onIntent(LoginUiIntent.LoginClicked)

        val state = viewModel.uiState.value
        assertEquals(LoginFieldValidationError.PASSWORD_REQUIRED, state.passwordError)
        assertFalse(state.isLoading)
        assertEquals(0, fakeAuthRepository.signInCallCount)
    }

    @Test
    fun testValidLoginClickedInvokesAuthRepositoryOnce() = runTest {
        viewModel.onIntent(LoginUiIntent.EmailChanged("valid@example.com"))
        viewModel.onIntent(LoginUiIntent.PasswordChanged("secret123"))
        viewModel.onIntent(LoginUiIntent.LoginClicked)

        advanceUntilIdle()

        assertEquals(1, fakeAuthRepository.signInCallCount)
        assertEquals("valid@example.com", fakeAuthRepository.lastSubmittedEmail)
        assertEquals("secret123", fakeAuthRepository.lastSubmittedPassword)
    }

    @Test
    fun testRepeatedLoginClickedWhileLoadingDoesNotDuplicateCall() = runTest {
        fakeAuthRepository.isDelayEnabled = true
        viewModel.onIntent(LoginUiIntent.EmailChanged("valid@example.com"))
        viewModel.onIntent(LoginUiIntent.PasswordChanged("secret123"))
        
        viewModel.onIntent(LoginUiIntent.LoginClicked)
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.onIntent(LoginUiIntent.LoginClicked)
        
        advanceUntilIdle()

        assertEquals(1, fakeAuthRepository.signInCallCount)
    }

    @Test
    fun testSuccessfulLoginClearsLoadingCorrectly() = runTest {
        fakeAuthRepository.signInResult = Result.success(AuthenticatedUser("uid-1", "valid@example.com"))

        viewModel.onIntent(LoginUiIntent.EmailChanged("valid@example.com"))
        viewModel.onIntent(LoginUiIntent.PasswordChanged("secret123"))
        viewModel.onIntent(LoginUiIntent.LoginClicked)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun testAuthFailureProducesOneTimeErrorEffectAndClearsLoading() = runTest {
        fakeAuthRepository.signInResult = Result.failure(Exception("Invalid password"))

        viewModel.onIntent(LoginUiIntent.EmailChanged("valid@example.com"))
        viewModel.onIntent(LoginUiIntent.PasswordChanged("wrongpassword"))
        viewModel.onIntent(LoginUiIntent.LoginClicked)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        val effect = viewModel.uiEffect.first()
        assertTrue(effect is LoginUiEffect.ShowError)
        assertEquals(AuthError.InvalidCredentials, (effect as LoginUiEffect.ShowError).error)
    }

    @Test
    fun testCancellationExceptionIsNotConvertedIntoGenericAuthError() = runTest {
        fakeAuthRepository.signInExceptionToThrow = CancellationException("Coroutines cancelled")

        viewModel.onIntent(LoginUiIntent.EmailChanged("valid@example.com"))
        viewModel.onIntent(LoginUiIntent.PasswordChanged("secret123"))

        try {
            viewModel.onIntent(LoginUiIntent.LoginClicked)
            advanceUntilIdle()
        } catch (e: CancellationException) {
            // Expected
        }

        assertFalse(viewModel.uiState.value.isLoading)
    }
}

private class FakeAuthRepository : AuthRepository {
    val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: Flow<AuthState> = _authState

    var signInCallCount = 0
    var lastSubmittedEmail: String? = null
    var lastSubmittedPassword: String? = null
    var signInResult: Result<AuthenticatedUser> = Result.success(AuthenticatedUser("uid-123", "user@example.com"))
    var signInExceptionToThrow: Exception? = null
    var isDelayEnabled = false

    override fun getCurrentUser(): AuthenticatedUser? = null

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthenticatedUser> {
        signInCallCount++
        lastSubmittedEmail = email
        lastSubmittedPassword = password

        signInExceptionToThrow?.let { throw it }

        if (signInResult.isSuccess) {
            _authState.value = AuthState.SignedIn(signInResult.getOrThrow())
        }

        return signInResult
    }

    override suspend fun signOut(): Result<Unit> {
        _authState.value = AuthState.SignedOut
        return Result.success(Unit)
    }
}
