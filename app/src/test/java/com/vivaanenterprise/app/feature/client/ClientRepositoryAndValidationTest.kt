package com.vivaanenterprise.app.feature.client

import com.vivaanenterprise.app.core.common.IdGenerator
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.common.TimeProvider
import com.vivaanenterprise.app.core.database.dao.ClientDao
import com.vivaanenterprise.app.core.database.entity.ClientEntity
import com.vivaanenterprise.app.core.sync.SyncScheduler
import com.vivaanenterprise.app.data.repository.ClientRepositoryImpl
import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.repository.ClientRepository
import com.vivaanenterprise.app.feature.client.model.ClientValidationUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClientRepositoryAndValidationTest {

    private lateinit var fakeClientDao: FakeClientDao
    private lateinit var fakeSyncScheduler: FakeSyncScheduler
    private lateinit var repository: ClientRepository

    @Before
    fun setUp() {
        fakeClientDao = FakeClientDao()
        fakeSyncScheduler = FakeSyncScheduler()
        repository = ClientRepositoryImpl(
            clientDao = fakeClientDao,
            syncScheduler = fakeSyncScheduler,
            idGenerator = object : IdGenerator {
                override fun newId(): String = "generated-uuid-123"
            },
            timeProvider = object : TimeProvider {
                override fun currentTimeMillis(): Long = 5000L
            }
        )
    }

    @Test
    fun testCreateUsesGeneratedIdWhenBlank() = runBlocking {
        val client = Client(
            id = "",
            companyName = " Eco Enterprise ",
            address = " Street 4 ",
            createdAt = 0L,
            updatedAt = 0L
        )

        val result = repository.createClient(client)
        assertTrue(result.isSuccess)

        val saved = fakeClientDao.clients["generated-uuid-123"]
        assertNotNull(saved)
        assertEquals("generated-uuid-123", saved?.id)
    }

    @Test
    fun testCreateTrimsNormalizedFields() = runBlocking {
        val client = Client(
            id = "c-1",
            companyName = " Eco Enterprise ",
            address = " Street 4 ",
            gstin = " 24chwpg0910j1zb ",
            pan = " chwpg0910j ",
            stateCode = " 24 ",
            createdAt = 0L,
            updatedAt = 0L
        )

        repository.createClient(client)
        val saved = fakeClientDao.clients["c-1"]
        assertEquals("Eco Enterprise", saved?.companyName)
        assertEquals("Street 4", saved?.address)
        assertEquals("24CHWPG0910J1ZB", saved?.gstin)
        assertEquals("CHWPG0910J", saved?.pan)
        assertEquals("24", saved?.stateCode)
    }

    @Test
    fun testCreateWritesPendingLocallyAndSchedulesSync() = runBlocking {
        val client = Client(id = "c-2", companyName = "Co Name", createdAt = 0L, updatedAt = 0L)
        repository.createClient(client)

        val saved = fakeClientDao.clients["c-2"]
        assertEquals(SyncStatus.PENDING, saved?.syncStatus)
        assertEquals(1, fakeSyncScheduler.enqueueCount)
    }

    @Test
    fun testUpdatePreservesIdAndCreatedAt() = runBlocking {
        fakeClientDao.clients["c-3"] = ClientEntity(
            id = "c-3",
            companyName = "Old Name",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val updatedClient = Client(
            id = "c-3",
            companyName = "New Name",
            createdAt = 9999L,
            updatedAt = 9999L
        )

        repository.updateClient(updatedClient)

        val saved = fakeClientDao.clients["c-3"]
        assertEquals("c-3", saved?.id)
        assertEquals("New Name", saved?.companyName)
        assertEquals(1000L, saved?.createdAt)
        assertEquals(5000L, saved?.updatedAt)
        assertEquals(SyncStatus.PENDING, saved?.syncStatus)
        assertEquals(1, fakeSyncScheduler.enqueueCount)
    }

    @Test
    fun testDeletePerformsSoftDeleteAndEnqueuesSync() = runBlocking {
        fakeClientDao.clients["c-4"] = ClientEntity(
            id = "c-4",
            companyName = "Active Co",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        repository.deleteClient("c-4")

        val saved = fakeClientDao.clients["c-4"]
        assertTrue(saved?.isDeleted == true)
        assertEquals(5000L, saved?.deletedAt)
        assertEquals(SyncStatus.PENDING, saved?.syncStatus)

        val activeList = repository.observeClients().first()
        assertTrue(activeList.none { it.id == "c-4" })
    }

    @Test
    fun testSyncSchedulingFailureDoesNotRollBackPersistedLocalData() = runBlocking {
        fakeSyncScheduler.shouldFail = true
        val client = Client(id = "c-5", companyName = "Local Safe", createdAt = 0L, updatedAt = 0L)

        val result = repository.createClient(client)
        assertTrue(result.isSuccess)
        assertNotNull(fakeClientDao.clients["c-5"])
    }

    @Test
    fun testValidationUtilsRules() {
        assertTrue(ClientValidationUtils.isValidGstin("24CHWPG0910J1ZB"))
        assertFalse(ClientValidationUtils.isValidGstin("INVALID123"))

        assertTrue(ClientValidationUtils.isValidPan("CHWPG0910J"))
        assertFalse(ClientValidationUtils.isValidPan("INVALIDPAN"))

        assertTrue(ClientValidationUtils.isValidStateCode("24"))
        assertFalse(ClientValidationUtils.isValidStateCode("2"))
        assertFalse(ClientValidationUtils.isValidStateCode("ABC"))

        // Email validation tests
        assertTrue(ClientValidationUtils.isValidEmail("test@example.com"))
        assertTrue(ClientValidationUtils.isValidEmail("  info@vivaan.co.in  "))
        assertFalse(ClientValidationUtils.isValidEmail("invalid-email"))
        assertFalse(ClientValidationUtils.isValidEmail("user@com"))

        // Phone validation tests
        assertTrue(ClientValidationUtils.isValidPhone("9737178061"))
        assertTrue(ClientValidationUtils.isValidPhone("+91 97371 78061"))
        assertTrue(ClientValidationUtils.isValidPhone("+919737178061"))
        assertFalse(ClientValidationUtils.isValidPhone("123"))
        assertFalse(ClientValidationUtils.isValidPhone("abc-def-ghij"))
    }
}

private class FakeClientDao : ClientDao {
    val clients = mutableMapOf<String, ClientEntity>()
    private val flow = MutableStateFlow<List<ClientEntity>>(emptyList())

    private fun updateFlow() {
        flow.value = clients.values.filter { !it.isDeleted }.sortedBy { it.companyName }
    }

    override suspend fun upsert(client: ClientEntity) {
        clients[client.id] = client
        updateFlow()
    }

    override suspend fun getById(id: String): ClientEntity? {
        val client = clients[id]
        return if (client?.isDeleted == false) client else null
    }

    override fun observeById(id: String): Flow<ClientEntity?> {
        return flow.map { list -> list.find { it.id == id } }
    }

    override fun observeActiveClients(): Flow<List<ClientEntity>> = flow

    override suspend fun getBySyncStatus(status: SyncStatus): List<ClientEntity> {
        return clients.values.filter { it.syncStatus == status }
    }

    override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {
        clients[id]?.let { existing ->
            clients[id] = existing.copy(
                isDeleted = true,
                deletedAt = deletedAt,
                updatedAt = updatedAt,
                syncStatus = syncStatus
            )
            updateFlow()
        }
    }
}

private class FakeSyncScheduler : SyncScheduler {
    var enqueueCount = 0
    var shouldFail = false

    override fun enqueueSync() {
        enqueueCount++
        if (shouldFail) {
            throw RuntimeException("WorkManager queue failed")
        }
    }
}
