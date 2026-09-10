package com.vivaanenterprise.app.feature.dashboard

import com.vivaanenterprise.app.domain.model.ClientAccountEntry
import com.vivaanenterprise.app.domain.model.ClientAccountSummary
import com.vivaanenterprise.app.domain.model.DashboardSummary
import com.vivaanenterprise.app.domain.repository.ClientAccountRepository
import com.vivaanenterprise.app.feature.dashboard.presentation.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_initialZeroState_emitsZeroSummary() = runTest {
        val repo = FakeClientAccountRepo(DashboardSummary(finalizedInvoiceCount = 0, totalBilledPaise = 0L))
        val viewModel = DashboardViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(0, state.summary.finalizedInvoiceCount)
        assertEquals(0L, state.summary.totalBilledPaise)
    }

    @Test
    fun load_populatedSummary_emitsCorrectTotals() = runTest {
        val repo = FakeClientAccountRepo(DashboardSummary(finalizedInvoiceCount = 5, totalBilledPaise = 12500000L))
        val viewModel = DashboardViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(5, state.summary.finalizedInvoiceCount)
        assertEquals(12500000L, state.summary.totalBilledPaise)
    }

    @Test
    fun reactiveUpdate_updatesDashboardSummaryAutomatically() = runTest {
        val repo = FakeClientAccountRepo(DashboardSummary(finalizedInvoiceCount = 1, totalBilledPaise = 250000L))
        val viewModel = DashboardViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.summary.finalizedInvoiceCount)

        repo.dashboardFlow.value = DashboardSummary(finalizedInvoiceCount = 2, totalBilledPaise = 500000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.summary.finalizedInvoiceCount)
        assertEquals(500000L, viewModel.uiState.value.summary.totalBilledPaise)
    }

    private class FakeClientAccountRepo(initialSummary: DashboardSummary) : ClientAccountRepository {
        val dashboardFlow = MutableStateFlow(initialSummary)
        override fun observeAccountEntriesForClient(clientId: String): Flow<List<ClientAccountEntry>> = MutableStateFlow(emptyList())
        override fun observeAccountSummaryForClient(clientId: String): Flow<ClientAccountSummary> = MutableStateFlow(ClientAccountSummary(0, 0L))
        override fun observeDashboardSummary(): Flow<DashboardSummary> = dashboardFlow
    }
}
