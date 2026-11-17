package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.common.TimeProvider
import com.vivaanenterprise.app.core.database.VivaanEnterpriseDatabase
import com.vivaanenterprise.app.core.database.dao.BusinessDocumentDao
import com.vivaanenterprise.app.core.database.dao.BusinessProfileDao
import com.vivaanenterprise.app.core.database.dao.ClientAccountEntryDao
import com.vivaanenterprise.app.core.database.dao.ClientDao
import com.vivaanenterprise.app.core.database.dao.DashboardSummaryProjection
import com.vivaanenterprise.app.core.database.dao.DocumentLineItemDao
import com.vivaanenterprise.app.core.database.dao.DocumentSequenceDao
import com.vivaanenterprise.app.core.database.dao.ProductDao
import com.vivaanenterprise.app.core.database.entity.BusinessDocumentEntity
import com.vivaanenterprise.app.core.database.entity.BusinessProfileEntity
import com.vivaanenterprise.app.core.database.entity.ClientAccountEntryEntity
import com.vivaanenterprise.app.core.database.entity.ClientEntity
import com.vivaanenterprise.app.core.database.entity.DocumentLineItemEntity
import com.vivaanenterprise.app.core.database.entity.DocumentSequenceEntity
import com.vivaanenterprise.app.core.database.entity.ProductEntity
import com.vivaanenterprise.app.core.datastore.SyncMetadata
import com.vivaanenterprise.app.core.datastore.SyncPreferencesDataStore
import com.vivaanenterprise.app.data.remote.datasource.FirestoreSyncDataSource
import com.vivaanenterprise.app.data.remote.model.BusinessDocumentDto
import com.vivaanenterprise.app.data.remote.model.BusinessProfileDto
import com.vivaanenterprise.app.data.remote.model.ClientAccountEntryDto
import com.vivaanenterprise.app.data.remote.model.ClientDto
import com.vivaanenterprise.app.data.remote.model.DocumentLineItemDto
import com.vivaanenterprise.app.data.remote.model.DocumentSequenceDto
import com.vivaanenterprise.app.data.remote.model.ProductDto
import com.vivaanenterprise.app.domain.model.AuthState
import com.vivaanenterprise.app.domain.model.AuthenticatedUser
import com.vivaanenterprise.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncRepositoryRecoveryTest {

    private lateinit var fakeClientDao: FakeRecoveryClientDao
    private lateinit var fakeSyncDataSource: FakeRecoveryFirestoreDataSource
    private lateinit var fakeDataStore: FakeRecoverySyncDataStore
    private lateinit var fakeAuthRepo: FakeRecoveryAuthRepository
    private lateinit var syncRepository: SyncRepositoryImpl

    private val fixedTime = 1700000000000L

    @Before
    fun setUp() {
        fakeClientDao = FakeRecoveryClientDao()
        fakeSyncDataSource = FakeRecoveryFirestoreDataSource()
        fakeDataStore = FakeRecoverySyncDataStore()
        fakeAuthRepo = FakeRecoveryAuthRepository()

        val fakeDatabase = FakeRecoveryDatabase(fakeClientDao)

        val timeProvider = object : TimeProvider {
            override fun currentTimeMillis(): Long = fixedTime
        }

        syncRepository = SyncRepositoryImpl(
            db = fakeDatabase,
            firestoreSyncDataSource = fakeSyncDataSource,
            authRepository = fakeAuthRepo,
            syncPreferencesDataStore = fakeDataStore,
            timeProvider = timeProvider
        )
    }

    @Test
    fun testFailedEntityFromRuleExpirationIsRetriedAndTransitionsToSynced() = runTest {
        // Arrange: A client entity previously failed due to expired Firebase security rules
        val failedClient = ClientEntity(
            id = "client-outage-01",
            companyName = "Eco Enterprise Ltd",
            createdAt = fixedTime,
            updatedAt = fixedTime,
            syncStatus = SyncStatus.FAILED,
            syncError = "PERMISSION_DENIED: Missing or insufficient permissions."
        )
        fakeClientDao.clients[failedClient.id] = failedClient

        // Act: Sync runs now that Firebase rules are fixed
        val result = syncRepository.synchronize()

        // Assert:
        assertTrue(result.isSuccess)
        assertEquals(1, fakeSyncDataSource.pushedClients.size)
        assertEquals("client-outage-01", fakeSyncDataSource.pushedClients[0].id)

        val updatedClient = fakeClientDao.clients["client-outage-01"]
        assertNotNull(updatedClient)
        assertEquals(SyncStatus.SYNCED, updatedClient?.syncStatus)
        assertEquals(fixedTime, updatedClient?.lastSyncedAt)
        assertNull(updatedClient?.syncError)

        assertEquals("SUCCESS", fakeDataStore.lastStatus)
        assertEquals(fixedTime, fakeDataStore.lastSuccessTimestamp)
    }

    @Test
    fun testHardErrorDuringPushMarksEntityFailedAndRecordsFailure() = runTest {
        // Arrange: Firestore throws PERMISSION_DENIED
        fakeSyncDataSource.shouldFailWithPermissionDenied = true
        val pendingClient = ClientEntity(
            id = "client-pending-02",
            companyName = "New Venture",
            createdAt = fixedTime,
            updatedAt = fixedTime,
            syncStatus = SyncStatus.PENDING
        )
        fakeClientDao.clients[pendingClient.id] = pendingClient

        // Act
        val result = syncRepository.synchronize()

        // Assert: Returns failure
        assertTrue(result.isFailure)
        val updatedClient = fakeClientDao.clients["client-pending-02"]
        assertEquals(SyncStatus.FAILED, updatedClient?.syncStatus)
        assertTrue(updatedClient?.syncError?.contains("PERMISSION_DENIED") == true)

        assertEquals("FAILED", fakeDataStore.lastStatus)
        assertTrue(fakeDataStore.lastError?.contains("PERMISSION_DENIED") == true)
    }

    private class FakeRecoveryDatabase(
        private val clientDao: FakeRecoveryClientDao
    ) : VivaanEnterpriseDatabase() {
        override fun clientDao(): ClientDao = clientDao
        override fun businessProfileDao(): BusinessProfileDao = FakeRecoveryBusinessProfileDao()
        override fun productDao(): ProductDao = FakeRecoveryProductDao()
        override fun businessDocumentDao(): BusinessDocumentDao = FakeRecoveryBusinessDocumentDao()
        override fun documentLineItemDao(): DocumentLineItemDao = FakeRecoveryDocumentLineItemDao()
        override fun clientAccountEntryDao(): ClientAccountEntryDao = FakeRecoveryAccountEntryDao()
        override fun documentSequenceDao(): DocumentSequenceDao = FakeRecoverySequenceDao()
        override fun clearAllTables() {}
        override fun createInvalidationTracker(): androidx.room.InvalidationTracker =
            androidx.room.InvalidationTracker(this, "clients")
    }

    private class FakeRecoveryBusinessProfileDao : BusinessProfileDao {
        override suspend fun upsert(profile: BusinessProfileEntity) {}
        override fun observeProfile(): Flow<BusinessProfileEntity?> = flowOf(null)
        override suspend fun getProfile(): BusinessProfileEntity? = null
        override suspend fun getUnsyncedProfile(): BusinessProfileEntity? = null
        override fun observeUnsyncedCount(): Flow<Int> = flowOf(0)
    }

    private class FakeRecoveryProductDao : ProductDao {
        override suspend fun upsert(product: ProductEntity) {}
        override suspend fun getById(id: String): ProductEntity? = null
        override suspend fun getByIdIncludingDeleted(id: String): ProductEntity? = null
        override fun observeById(id: String): Flow<ProductEntity?> = flowOf(null)
        override fun observeAllProducts(): Flow<List<ProductEntity>> = flowOf(emptyList())
        override fun observeActiveProducts(): Flow<List<ProductEntity>> = flowOf(emptyList())
        override suspend fun getBySyncStatus(status: SyncStatus): List<ProductEntity> = emptyList()
        override suspend fun getBySyncStatuses(statuses: List<SyncStatus>): List<ProductEntity> = emptyList()
        override suspend fun getUnsynced(): List<ProductEntity> = emptyList()
        override fun observeUnsyncedCount(): Flow<Int> = flowOf(0)
        override fun observeFailedCount(): Flow<Int> = flowOf(0)
        override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {}
    }

    private class FakeRecoveryBusinessDocumentDao : BusinessDocumentDao {
        override suspend fun upsert(document: BusinessDocumentEntity) {}
        override suspend fun getById(id: String): BusinessDocumentEntity? = null
        override suspend fun getByIdIncludingDeleted(id: String): BusinessDocumentEntity? = null
        override fun observeById(id: String): Flow<BusinessDocumentEntity?> = flowOf(null)
        override fun observeAllDocuments(): Flow<List<BusinessDocumentEntity>> = flowOf(emptyList())
        override fun observeDocumentsByType(type: DocumentType): Flow<List<BusinessDocumentEntity>> = flowOf(emptyList())
        override fun observeDocumentsByClient(clientId: String): Flow<List<BusinessDocumentEntity>> = flowOf(emptyList())
        override suspend fun getBySyncStatus(status: SyncStatus): List<BusinessDocumentEntity> = emptyList()
        override suspend fun getBySyncStatuses(statuses: List<SyncStatus>): List<BusinessDocumentEntity> = emptyList()
        override suspend fun getUnsynced(): List<BusinessDocumentEntity> = emptyList()
        override fun observeUnsyncedCount(): Flow<Int> = flowOf(0)
        override fun observeFailedCount(): Flow<Int> = flowOf(0)
        override suspend fun findByDocumentTypeAndNumber(documentType: DocumentType, documentNumber: String): BusinessDocumentEntity? = null
        override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {}
        override fun observeDashboardSummary(): Flow<DashboardSummaryProjection> = flowOf(DashboardSummaryProjection(0, 0L))
    }

    private class FakeRecoveryDocumentLineItemDao : DocumentLineItemDao {
        override suspend fun upsertAll(items: List<DocumentLineItemEntity>) {}
        override suspend fun upsert(item: DocumentLineItemEntity) {}
        override fun observeByDocumentId(documentId: String): Flow<List<DocumentLineItemEntity>> = flowOf(emptyList())
        override suspend fun getByDocumentId(documentId: String): List<DocumentLineItemEntity> = emptyList()
        override suspend fun deleteByDocumentId(documentId: String) {}
    }

    private class FakeRecoveryAccountEntryDao : ClientAccountEntryDao {
        override suspend fun upsert(entry: ClientAccountEntryEntity) {}
        override fun observeByClientId(clientId: String): Flow<List<ClientAccountEntryEntity>> = flowOf(emptyList())
        override suspend fun getBySyncStatus(status: SyncStatus): List<ClientAccountEntryEntity> = emptyList()
        override suspend fun getBySyncStatuses(statuses: List<SyncStatus>): List<ClientAccountEntryEntity> = emptyList()
        override suspend fun getUnsynced(): List<ClientAccountEntryEntity> = emptyList()
        override fun observeUnsyncedCount(): Flow<Int> = flowOf(0)
        override fun observeFailedCount(): Flow<Int> = flowOf(0)
        override suspend fun findByDocumentId(documentId: String): ClientAccountEntryEntity? = null
        override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {}
    }

    private class FakeRecoverySequenceDao : DocumentSequenceDao {
        override suspend fun upsert(sequence: DocumentSequenceEntity) {}
        override suspend fun getSequence(documentType: DocumentType, financialYear: String): DocumentSequenceEntity? = null
    }

    private class FakeRecoveryClientDao : ClientDao {
        val clients = mutableMapOf<String, ClientEntity>()

        override suspend fun upsert(client: ClientEntity) {
            clients[client.id] = client
        }

        override suspend fun getById(id: String): ClientEntity? = clients[id]
        override fun observeById(id: String): Flow<ClientEntity?> = flowOf(clients[id])
        override fun observeActiveClients(): Flow<List<ClientEntity>> = flowOf(clients.values.toList())
        override suspend fun getBySyncStatus(status: SyncStatus): List<ClientEntity> =
            clients.values.filter { it.syncStatus == status }
        override suspend fun getBySyncStatuses(statuses: List<SyncStatus>): List<ClientEntity> =
            clients.values.filter { it.syncStatus in statuses }
        override suspend fun getUnsynced(): List<ClientEntity> =
            clients.values.filter { it.syncStatus == SyncStatus.PENDING || it.syncStatus == SyncStatus.FAILED }
        override fun observeUnsyncedCount(): Flow<Int> =
            flowOf(clients.values.count { it.syncStatus == SyncStatus.PENDING || it.syncStatus == SyncStatus.FAILED })
        override fun observeFailedCount(): Flow<Int> =
            flowOf(clients.values.count { it.syncStatus == SyncStatus.FAILED })
        override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {}
    }

    private class FakeRecoveryFirestoreDataSource : FirestoreSyncDataSource() {
        val pushedClients = mutableListOf<ClientDto>()
        var shouldFailWithPermissionDenied = false

        override suspend fun pushClient(dto: ClientDto) {
            if (shouldFailWithPermissionDenied) {
                throw RuntimeException("PERMISSION_DENIED: Missing or insufficient permissions.")
            }
            pushedClients.add(dto)
        }

        override suspend fun pullBusinessProfilesSince(sinceTimestamp: Long): List<BusinessProfileDto> = emptyList()
        override suspend fun pullClientsSince(sinceTimestamp: Long): List<ClientDto> = emptyList()
        override suspend fun pullProductsSince(sinceTimestamp: Long): List<ProductDto> = emptyList()
        override suspend fun pullBusinessDocumentsSince(sinceTimestamp: Long): List<BusinessDocumentDto> = emptyList()
        override suspend fun pullDocumentLineItemsSince(sinceTimestamp: Long): List<DocumentLineItemDto> = emptyList()
        override suspend fun pullClientAccountEntriesSince(sinceTimestamp: Long): List<ClientAccountEntryDto> = emptyList()
        override suspend fun pullDocumentSequencesSince(sinceTimestamp: Long): List<DocumentSequenceDto> = emptyList()
    }

    private class FakeRecoverySyncDataStore : SyncPreferencesDataStore() {
        var lastSuccessTimestamp: Long = 0L
        var lastStatus: String = "IDLE"
        var lastError: String? = null

        override suspend fun getLastSyncTimestamp(): Long = lastSuccessTimestamp
        override suspend fun recordSyncSuccess(timestamp: Long) {
            lastSuccessTimestamp = timestamp
            lastStatus = "SUCCESS"
            lastError = null
        }
        override suspend fun recordSyncFailure(attemptTimestamp: Long, errorMessage: String) {
            lastStatus = "FAILED"
            lastError = errorMessage
        }
        override fun observeSyncMetadata(): Flow<SyncMetadata> =
            flowOf(SyncMetadata(lastSyncTimestamp = lastSuccessTimestamp, lastSyncStatus = lastStatus, lastSyncError = lastError))
    }

    private class FakeRecoveryAuthRepository : AuthRepository {
        val testUser = AuthenticatedUser(id = "user-recovery-01", email = "operator@vivaanenterprise.com")
        override val authState: Flow<AuthState> = flowOf(AuthState.SignedIn(testUser))
        override fun getCurrentUser(): AuthenticatedUser = testUser
        override suspend fun signInWithEmail(email: String, password: String) = Result.success(testUser)
        override suspend fun signOut() = Result.success(Unit)
    }
}
