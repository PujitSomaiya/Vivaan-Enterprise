package com.vivaanenterprise.app.feature.account

import androidx.lifecycle.SavedStateHandle
import com.vivaanenterprise.app.core.common.AccountEntryType
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.model.ClientAccountEntry
import com.vivaanenterprise.app.domain.model.ClientAccountSummary
import com.vivaanenterprise.app.domain.model.DashboardSummary
import com.vivaanenterprise.app.domain.repository.ClientAccountRepository
import com.vivaanenterprise.app.domain.repository.ClientRepository
import com.vivaanenterprise.app.feature.account.presentation.ClientAccountUiEffect
import com.vivaanenterprise.app.feature.account.presentation.ClientAccountUiIntent
import com.vivaanenterprise.app.feature.account.presentation.ClientAccountViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientAccountViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleClient = Client(
        id = "client-100",
        companyName = "Apex Trading Co",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private val sampleInvoiceEntry1 = ClientAccountEntry(
        id = "entry-1",
        clientId = "client-100",
        documentId = "doc-inv-100",
        entryType = AccountEntryType.INVOICE,
        entryDate = 1700000000000L,
        amountPaise = 2500000L, // ₹ 25,000.00
        narration = "Tax Invoice #VE/01/2026-27",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L,
        syncStatus = SyncStatus.SYNCED
    )

    private val sampleInvoiceEntry2 = ClientAccountEntry(
        id = "entry-2",
        clientId = "client-100",
        documentId = "doc-inv-101",
        entryType = AccountEntryType.INVOICE,
        entryDate = 1705000000000L,
        amountPaise = 1500000L, // ₹ 15,000.00
        narration = "Tax Invoice #VE/02/2026-27",
        createdAt = 1705000000000L,
        updatedAt = 1705000000000L,
        syncStatus = SyncStatus.PENDING
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_validClientAndEntries_loadsContentSuccessfully() = runTest {
        val clientRepo = FakeClientRepo(listOf(sampleClient))
        val accountRepo = FakeClientAccountRepo(
            entries = listOf(sampleInvoiceEntry2, sampleInvoiceEntry1),
            summary = ClientAccountSummary(invoiceCount = 2, totalBilledPaise = 4000000L)
        )

        val savedStateHandle = SavedStateHandle(mapOf("clientId" to "client-100"))
        val viewModel = ClientAccountViewModel(savedStateHandle, clientRepo, accountRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNotNull(state.client)
        assertEquals("Apex Trading Co", state.client?.companyName)
        assertEquals(2, state.invoiceCount)
        assertEquals(4000000L, state.totalBilledPaise)
        assertEquals(2, state.entries.size)
        assertEquals("entry-2", state.entries.first().id) // Newest first
    }

    @Test
    fun load_missingClient_showsErrorState() = runTest {
        val clientRepo = FakeClientRepo(emptyList())
        val accountRepo = FakeClientAccountRepo(emptyList(), ClientAccountSummary(0, 0L))

        val savedStateHandle = SavedStateHandle(mapOf("clientId" to "non-existent"))
        val viewModel = ClientAccountViewModel(savedStateHandle, clientRepo, accountRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.client)
        assertEquals("Client not found", state.errorMessage)
    }

    @Test
    fun load_emptyEntries_showsZeroSummaryAndEmptyList() = runTest {
        val clientRepo = FakeClientRepo(listOf(sampleClient))
        val accountRepo = FakeClientAccountRepo(emptyList(), ClientAccountSummary(0, 0L))

        val savedStateHandle = SavedStateHandle(mapOf("clientId" to "client-100"))
        val viewModel = ClientAccountViewModel(savedStateHandle, clientRepo, accountRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.client)
        assertEquals(0, state.invoiceCount)
        assertEquals(0L, state.totalBilledPaise)
        assertEquals(0, state.entries.size)
    }

    @Test
    fun entryClicked_validDocumentId_emitsOpenDocumentDetail() = runTest {
        val clientRepo = FakeClientRepo(listOf(sampleClient))
        val accountRepo = FakeClientAccountRepo(listOf(sampleInvoiceEntry1), ClientAccountSummary(1, 2500000L))

        val savedStateHandle = SavedStateHandle(mapOf("clientId" to "client-100"))
        val viewModel = ClientAccountViewModel(savedStateHandle, clientRepo, accountRepo)

        val effects = mutableListOf<ClientAccountUiEffect>()
        val job = backgroundScope.launch { viewModel.uiEffect.collect { effects.add(it) } }

        viewModel.onIntent(ClientAccountUiIntent.EntryClicked("doc-inv-100"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(ClientAccountUiEffect.OpenDocumentDetail("doc-inv-100"), effects.first())
        job.cancel()
    }

    @Test
    fun entryClicked_nullDocumentId_emitsShowError() = runTest {
        val clientRepo = FakeClientRepo(listOf(sampleClient))
        val accountRepo = FakeClientAccountRepo(emptyList(), ClientAccountSummary(0, 0L))

        val savedStateHandle = SavedStateHandle(mapOf("clientId" to "client-100"))
        val viewModel = ClientAccountViewModel(savedStateHandle, clientRepo, accountRepo)

        val effects = mutableListOf<ClientAccountUiEffect>()
        val job = backgroundScope.launch { viewModel.uiEffect.collect { effects.add(it) } }

        viewModel.onIntent(ClientAccountUiIntent.EntryClicked(null))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(ClientAccountUiEffect.ShowError("No linked document for this billing entry"), effects.first())
        job.cancel()
    }

    private class FakeClientRepo(initialClients: List<Client>) : ClientRepository {
        val flow = MutableStateFlow(initialClients)
        override fun observeClients(): Flow<List<Client>> = flow
        override fun observeClientById(id: String): Flow<Client?> = MutableStateFlow(flow.value.firstOrNull { it.id == id })
        override suspend fun getClientById(id: String): Client? = flow.value.firstOrNull { it.id == id }
        override suspend fun createClient(client: Client): Result<Unit> = Result.success(Unit)
        override suspend fun updateClient(client: Client): Result<Unit> = Result.success(Unit)
        override suspend fun deleteClient(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeClientAccountRepo(
        private val entries: List<ClientAccountEntry>,
        private val summary: ClientAccountSummary
    ) : ClientAccountRepository {
        override fun observeAccountEntriesForClient(clientId: String): Flow<List<ClientAccountEntry>> = MutableStateFlow(entries)
        override fun observeAccountSummaryForClient(clientId: String): Flow<ClientAccountSummary> = MutableStateFlow(summary)
        override fun observeDashboardSummary(): Flow<DashboardSummary> = MutableStateFlow(DashboardSummary(0, 0L))
    }
}
