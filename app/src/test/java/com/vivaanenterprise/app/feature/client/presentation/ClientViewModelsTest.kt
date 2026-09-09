package com.vivaanenterprise.app.feature.client.presentation

import androidx.lifecycle.SavedStateHandle
import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.repository.ClientRepository
import com.vivaanenterprise.app.feature.client.presentation.detail.ClientDetailUiEffect
import com.vivaanenterprise.app.feature.client.presentation.detail.ClientDetailUiIntent
import com.vivaanenterprise.app.feature.client.presentation.detail.ClientDetailViewModel
import com.vivaanenterprise.app.feature.client.presentation.form.ClientFormFieldValidationError
import com.vivaanenterprise.app.feature.client.presentation.form.ClientFormUiEffect
import com.vivaanenterprise.app.feature.client.presentation.form.ClientFormUiIntent
import com.vivaanenterprise.app.feature.client.presentation.form.ClientFormViewModel
import com.vivaanenterprise.app.feature.client.presentation.list.ClientListUiEffect
import com.vivaanenterprise.app.feature.client.presentation.list.ClientListUiIntent
import com.vivaanenterprise.app.feature.client.presentation.list.ClientListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
class ClientViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeClientRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeClientRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testClientListViewModelObservesAndFiltersClients() = runTest {
        fakeRepository.clientsFlow.value = listOf(
            Client("1", "Eco Enterprise", gstin = "24CHWPG0910J1ZB", createdAt = 0, updatedAt = 0),
            Client("2", "Mahalaxmi Traders", gstin = null, createdAt = 0, updatedAt = 0)
        )

        val viewModel = ClientListViewModel(fakeRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.clients.size)

        viewModel.onIntent(ClientListUiIntent.SearchQueryChanged("eco"))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.clients.size)
        assertEquals("Eco Enterprise", viewModel.uiState.value.clients.first().companyName)
    }

    @Test
    fun testClientListViewModelEmitsNavigationEffects() = runTest {
        val viewModel = ClientListViewModel(fakeRepository)

        viewModel.onIntent(ClientListUiIntent.ClientClicked("c-10"))
        assertEquals(ClientListUiEffect.NavigateToDetail("c-10"), viewModel.uiEffect.first())

        viewModel.onIntent(ClientListUiIntent.AddClientClicked)
        assertEquals(ClientListUiEffect.NavigateToAddClient, viewModel.uiEffect.first())
    }

    @Test
    fun testClientFormViewModelValidationPreventsSave() = runTest {
        val viewModel = ClientFormViewModel(fakeRepository, SavedStateHandle())

        viewModel.onIntent(ClientFormUiIntent.CompanyNameChanged("   "))
        viewModel.onIntent(ClientFormUiIntent.GstinChanged("invalid-gstin"))
        viewModel.onIntent(ClientFormUiIntent.EmailChanged("invalid-email"))
        viewModel.onIntent(ClientFormUiIntent.PhoneChanged("123"))
        viewModel.onIntent(ClientFormUiIntent.SaveClicked)

        advanceUntilIdle()

        assertEquals(ClientFormFieldValidationError.COMPANY_NAME_REQUIRED, viewModel.uiState.value.companyNameError)
        assertEquals(ClientFormFieldValidationError.GSTIN_INVALID, viewModel.uiState.value.gstinError)
        assertEquals(ClientFormFieldValidationError.EMAIL_INVALID, viewModel.uiState.value.emailError)
        assertEquals(ClientFormFieldValidationError.PHONE_INVALID, viewModel.uiState.value.phoneError)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(0, fakeRepository.createCount)
    }

    @Test
    fun testClientFormViewModelOptionalBlankEmailAndPhoneAccepted() = runTest {
        val viewModel = ClientFormViewModel(fakeRepository, SavedStateHandle())

        viewModel.onIntent(ClientFormUiIntent.CompanyNameChanged("Valid Company"))
        viewModel.onIntent(ClientFormUiIntent.EmailChanged(""))
        viewModel.onIntent(ClientFormUiIntent.PhoneChanged(""))
        viewModel.onIntent(ClientFormUiIntent.SaveClicked)

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.phoneError)
        assertEquals(1, fakeRepository.createCount)
    }

    @Test
    fun testClientFormViewModelAddModeSuccessfulSaveEmitsEffect() = runTest {
        val viewModel = ClientFormViewModel(fakeRepository, SavedStateHandle())

        viewModel.onIntent(ClientFormUiIntent.CompanyNameChanged("Valid Company"))
        viewModel.onIntent(ClientFormUiIntent.SaveClicked)

        advanceUntilIdle()

        assertEquals(1, fakeRepository.createCount)
        assertEquals(ClientFormUiEffect.SaveSuccess, viewModel.uiEffect.first())
    }

    @Test
    fun testClientFormViewModelEditModeLoadsClient() = runTest {
        fakeRepository.clientMap["c-99"] = Client(
            id = "c-99",
            companyName = "Existing Co",
            state = "Gujarat",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val viewModel = ClientFormViewModel(fakeRepository, SavedStateHandle(mapOf("clientId" to "c-99")))
        advanceUntilIdle()

        assertEquals("Existing Co", viewModel.uiState.value.companyName)
        assertEquals("Gujarat", viewModel.uiState.value.state)
        assertTrue(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun testClientDetailViewModelObservesClientAndDeletes() = runTest {
        fakeRepository.clientsFlow.value = listOf(
            Client("c-50", "Detail Co", createdAt = 0, updatedAt = 0)
        )
        fakeRepository.clientMap["c-50"] = Client("c-50", "Detail Co", createdAt = 0, updatedAt = 0)

        val viewModel = ClientDetailViewModel(fakeRepository, SavedStateHandle(mapOf("clientId" to "c-50")))
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("Detail Co", viewModel.uiState.value.client?.companyName)

        viewModel.onIntent(ClientDetailUiIntent.ConfirmDeleteClicked)
        advanceUntilIdle()

        assertEquals(1, fakeRepository.deleteCount)
        assertEquals(ClientDetailUiEffect.DeleteSuccess, viewModel.uiEffect.first())
    }
}

private class FakeClientRepository : ClientRepository {
    val clientsFlow = MutableStateFlow<List<Client>>(emptyList())
    val clientMap = mutableMapOf<String, Client>()
    var createCount = 0
    var updateCount = 0
    var deleteCount = 0

    override fun observeClients(): Flow<List<Client>> = clientsFlow

    override fun observeClientById(id: String): Flow<Client?> = clientsFlow.map { list -> list.find { it.id == id } }

    override suspend fun getClientById(id: String): Client? = clientMap[id]

    override suspend fun createClient(client: Client): Result<Unit> {
        createCount++
        clientMap[client.id] = client
        clientsFlow.update { it + client }
        return Result.success(Unit)
    }

    override suspend fun updateClient(client: Client): Result<Unit> {
        updateCount++
        clientMap[client.id] = client
        clientsFlow.update { list -> list.map { if (it.id == client.id) client else it } }
        return Result.success(Unit)
    }

    override suspend fun deleteClient(id: String): Result<Unit> {
        deleteCount++
        clientMap.remove(id)
        clientsFlow.update { list -> list.filter { it.id != id } }
        return Result.success(Unit)
    }
}
