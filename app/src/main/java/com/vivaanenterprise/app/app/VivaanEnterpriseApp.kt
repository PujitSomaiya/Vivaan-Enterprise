package com.vivaanenterprise.app.app

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.app.navigation.AppNavHost
import com.vivaanenterprise.app.core.designsystem.theme.VivaanEnterpriseTheme
import com.vivaanenterprise.app.core.sync.SyncScheduler
import com.vivaanenterprise.app.domain.model.AuthState
import com.vivaanenterprise.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.vivaanenterprise.app.data.repository.ProductSeeder

@HiltViewModel
class MainViewModel @Inject constructor(
    val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
    private val productSeeder: ProductSeeder,
    private val businessProfileSeeder: com.vivaanenterprise.app.data.repository.BusinessProfileSeeder
) : ViewModel() {

    private var hasHandledStartup = false

    val authState: StateFlow<AuthState> = authRepository.authState
        .onEach { state ->
            if (state is AuthState.SignedIn && !hasHandledStartup) {
                hasHandledStartup = true
                viewModelScope.launch {
                    try {
                        businessProfileSeeder.seedInitialData()
                    } catch (e: Exception) {
                        // Ignore seeder error
                    }
                    try {
                        productSeeder.seedInitialData()
                    } catch (e: Exception) {
                        // Ignore seeder error
                    }
                    try {
                        syncScheduler.enqueueSync()
                    } catch (e: Exception) {
                        // Ignore sync enqueue error
                    }
                }
            } else if (state is AuthState.SignedOut) {
                hasHandledStartup = false
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthState.Loading
        )
}

@Composable
fun VivaanEnterpriseApp(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    VivaanEnterpriseTheme {
        AppNavHost(
            authStateFlow = mainViewModel.authState,
            authRepository = mainViewModel.authRepository
        )
    }
}
