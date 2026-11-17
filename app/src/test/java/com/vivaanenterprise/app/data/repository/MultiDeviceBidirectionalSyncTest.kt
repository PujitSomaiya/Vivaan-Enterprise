package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.common.DocumentStatus
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end multi-device bidirectional sync test verifying:
 * 1. Device A creates invoices and master entities -> pushes to remote.
 * 2. Device B syncs -> receives and populates all invoices, line items, and sequences in local Room.
 * 3. Device B creates products -> Device A syncs and receives them.
 * 4. Document sequences stay synchronized across devices, preventing number duplication.
 */
class MultiDeviceBidirectionalSyncTest {

    // Shared remote Firestore store simulating Cloud Firestore in the cloud
    private lateinit var sharedCloudRemote: SharedCloudFirestoreFake

    // Device A components (Phone 1)
    private lateinit var deviceADatabase: InMemoryDeviceDatabase
    private lateinit var deviceADataStore: FakeSyncDataStore
    private lateinit var deviceASyncRepo: SyncRepositoryImpl

    // Device B components (Phone 2)
    private lateinit var deviceBDatabase: InMemoryDeviceDatabase
    private lateinit var deviceBDataStore: FakeSyncDataStore
    private lateinit var deviceBSyncRepo: SyncRepositoryImpl

    @Before
    fun setup() {
        sharedCloudRemote = SharedCloudFirestoreFake()

        // Device A setup
        deviceADatabase = InMemoryDeviceDatabase()
        deviceADataStore = FakeSyncDataStore()
        val authRepoA = FakeAuthRepo(AuthenticatedUser("user-1", "user@vivaan.com"))
        val timeA = object : TimeProvider { override fun currentTimeMillis(): Long = 1000000L }
        deviceASyncRepo = SyncRepositoryImpl(
            db = deviceADatabase.toDatabaseProxy(),
            firestoreSyncDataSource = sharedCloudRemote,
            authRepository = authRepoA,
            syncPreferencesDataStore = deviceADataStore,
            timeProvider = timeA
        )

        // Device B setup
        deviceBDatabase = InMemoryDeviceDatabase()
        deviceBDataStore = FakeSyncDataStore()
        val authRepoB = FakeAuthRepo(AuthenticatedUser("user-1", "user@vivaan.com"))
        val timeB = object : TimeProvider { override fun currentTimeMillis(): Long = 1000500L } // slight clock skew
        deviceBSyncRepo = SyncRepositoryImpl(
            db = deviceBDatabase.toDatabaseProxy(),
            firestoreSyncDataSource = sharedCloudRemote,
            authRepository = authRepoB,
            syncPreferencesDataStore = deviceBDataStore,
            timeProvider = timeB
        )
    }

    @Test
    fun device_A_creates_and_pushes_invoice_then_device_B_syncs_and_receives_invoice() = runTest {
        // 1. Device A creates a client locally
        val client = ClientEntity(
            id = "client-101",
            companyName = "Tata Consultancy Services",
            gstin = "24AAAAT1234A1Z5",
            state = "Gujarat",
            stateCode = "24",
            createdAt = 1000000L,
            updatedAt = 1000000L,
            syncStatus = SyncStatus.PENDING
        )
        deviceADatabase.clientDao.upsert(client)

        // 2. Device A creates a finalized invoice with line item and sequence
        val invoice = BusinessDocumentEntity(
            id = "doc-inv-1",
            documentType = DocumentType.TAX_INVOICE,
            documentNumber = "VE/01/2026-27",
            documentDate = 1000000L,
            status = DocumentStatus.FINALIZED,
            clientId = "client-101",
            grandTotalPaise = 5900000L,
            taxableAmountPaise = 5000000L,
            totalTaxAmountPaise = 900000L,
            createdAt = 1000000L,
            updatedAt = 1000000L,
            syncStatus = SyncStatus.PENDING
        )
        val lineItem = DocumentLineItemEntity(
            id = "item-1",
            documentId = "doc-inv-1",
            position = 0,
            descriptionSnapshot = "Custom Industrial Packaging Tape",
            quantity = 100L,
            ratePaise = 50000L,
            gstRateBasisPoints = 1800,
            taxableAmountPaise = 5000000L,
            lineTotalPaise = 5900000L,
            createdAt = 1000000L,
            updatedAt = 1000000L
        )
        val sequence = DocumentSequenceEntity(
            documentType = DocumentType.TAX_INVOICE,
            financialYear = "2026-27",
            lastSequenceNumber = 1,
            updatedAt = 1000000L
        )

        deviceADatabase.documentDao.upsert(invoice)
        deviceADatabase.lineItemDao.upsert(lineItem)
        deviceADatabase.sequenceDao.upsert(sequence)

        // 3. Device A syncs -> pushes client, invoice, line items, and sequence to shared Cloud
        val syncResultA = deviceASyncRepo.synchronize()
        assertTrue("Device A push must succeed", syncResultA.isSuccess)
        assertEquals(SyncStatus.SYNCED, deviceADatabase.clientDao.getById("client-101")?.syncStatus)
        assertEquals(SyncStatus.SYNCED, deviceADatabase.documentDao.getById("doc-inv-1")?.syncStatus)

        // 4. Device B syncs (Device B previously had an older sync timestamp, simulating live usage)
        deviceBDataStore.setLastSyncTimestamp(900000L)
        val syncResultB = deviceBSyncRepo.synchronize()
        assertTrue("Device B pull must succeed", syncResultB.isSuccess)

        // 5. Assert: Device B's Room database received the Client, Invoice, Line Item, and Sequence
        val clientOnB = deviceBDatabase.clientDao.getById("client-101")
        assertNotNull("Client must be received on Device B", clientOnB)
        assertEquals("Tata Consultancy Services", clientOnB?.companyName)

        val docOnB = deviceBDatabase.documentDao.getById("doc-inv-1")
        assertNotNull("Invoice must be received on Device B", docOnB)
        assertEquals("VE/01/2026-27", docOnB?.documentNumber)
        assertEquals(DocumentStatus.FINALIZED, docOnB?.status)
        assertEquals(5900000L, docOnB?.grandTotalPaise)

        val itemsOnB = deviceBDatabase.lineItemDao.getByDocumentId("doc-inv-1")
        assertEquals(1, itemsOnB.size)
        assertEquals("Custom Industrial Packaging Tape", itemsOnB[0].descriptionSnapshot)
        assertEquals(5900000L, itemsOnB[0].lineTotalPaise)

        val seqOnB = deviceBDatabase.sequenceDao.getSequence(DocumentType.TAX_INVOICE, "2026-27")
        assertNotNull("Sequence must be updated on Device B", seqOnB)
        assertEquals(1, seqOnB?.lastSequenceNumber)
    }

    @Test
    fun device_B_creates_product_then_device_A_syncs_and_receives_product() = runTest {
        // Device B adds a product locally
        val product = ProductEntity(
            id = "prod-99",
            name = "Bopp Self Adhesive Tape",
            hsnSac = "3919",
            defaultGstRateBasisPoints = 1800,
            isActive = true,
            createdAt = 1000500L,
            updatedAt = 1000500L,
            syncStatus = SyncStatus.PENDING
        )
        deviceBDatabase.productDao.upsert(product)

        // Device B syncs -> uploads product
        val resultB = deviceBSyncRepo.synchronize()
        assertTrue(resultB.isSuccess)

        // Device A syncs -> pulls product
        val resultA = deviceASyncRepo.synchronize()
        assertTrue(resultA.isSuccess)

        // Assert: Device A has product-99
        val productOnA = deviceADatabase.productDao.getById("prod-99")
        assertNotNull("Product must replicate to Device A", productOnA)
        assertEquals("Bopp Self Adhesive Tape", productOnA?.name)
    }

    @Test
    fun manual_sync_forces_full_reconciliation_regardless_of_past_timestamp() = runTest {
        // Shared cloud already contains an invoice created previously
        sharedCloudRemote.pushClient(
            ClientDto(id = "client-reconcile", companyName = "Reconcile Co", updatedAt = 500000L)
        )
        sharedCloudRemote.pushBusinessDocument(
            BusinessDocumentDto(
                id = "doc-reconcile",
                documentType = "TAX_INVOICE",
                documentNumber = "VE/05/2026-27",
                clientId = "client-reconcile",
                updatedAt = 500000L
            )
        )

        // Device B has an advanced lastSyncTimestamp = 9999999L (in future / clock drift)
        deviceBDataStore.setLastSyncTimestamp(9999999L)

        // Running routine sync with strict filter would skip it, but forceReconcile = true pulls everything!
        val result = deviceBSyncRepo.synchronize(forceReconcile = true)
        assertTrue(result.isSuccess)

        assertNotNull(deviceBDatabase.clientDao.getById("client-reconcile"))
        assertNotNull(deviceBDatabase.documentDao.getById("doc-reconcile"))
    }
}

// =========================================================================
// IN-MEMORY FAKES FOR MULTI-DEVICE SIMULATION
// =========================================================================

private class SharedCloudFirestoreFake : FirestoreSyncDataSource() {
    val profiles = mutableMapOf<String, BusinessProfileDto>()
    val clients = mutableMapOf<String, ClientDto>()
    val products = mutableMapOf<String, ProductDto>()
    val documents = mutableMapOf<String, BusinessDocumentDto>()
    val lineItems = mutableMapOf<String, DocumentLineItemDto>()
    val accountEntries = mutableMapOf<String, ClientAccountEntryDto>()
    val sequences = mutableMapOf<String, DocumentSequenceDto>()

    override suspend fun pushBusinessProfile(dto: BusinessProfileDto) { profiles[dto.id] = dto }
    override suspend fun pushClient(dto: ClientDto) { clients[dto.id] = dto }
    override suspend fun pushProduct(dto: ProductDto) { products[dto.id] = dto }
    override suspend fun pushBusinessDocument(dto: BusinessDocumentDto) { documents[dto.id] = dto }
    override suspend fun pushDocumentLineItem(dto: DocumentLineItemDto) { lineItems[dto.id] = dto }
    override suspend fun pushClientAccountEntry(dto: ClientAccountEntryDto) { accountEntries[dto.id] = dto }
    override suspend fun pushDocumentSequence(dto: DocumentSequenceDto) {
        val key = "${dto.documentType}_${dto.financialYear}"
        sequences[key] = dto
    }

    override suspend fun pullBusinessProfilesSince(sinceTimestamp: Long): List<BusinessProfileDto> =
        profiles.values.filter { it.updatedAt >= sinceTimestamp }
    override suspend fun pullClientsSince(sinceTimestamp: Long): List<ClientDto> =
        clients.values.filter { it.updatedAt >= sinceTimestamp }
    override suspend fun pullProductsSince(sinceTimestamp: Long): List<ProductDto> =
        products.values.filter { it.updatedAt >= sinceTimestamp }
    override suspend fun pullBusinessDocumentsSince(sinceTimestamp: Long): List<BusinessDocumentDto> =
        documents.values.filter { it.updatedAt >= sinceTimestamp }
    override suspend fun pullDocumentLineItemsSince(sinceTimestamp: Long): List<DocumentLineItemDto> =
        lineItems.values.filter { it.updatedAt >= sinceTimestamp }
    override suspend fun pullClientAccountEntriesSince(sinceTimestamp: Long): List<ClientAccountEntryDto> =
        accountEntries.values.filter { it.updatedAt >= sinceTimestamp }
    override suspend fun pullDocumentSequencesSince(sinceTimestamp: Long): List<DocumentSequenceDto> =
        sequences.values.filter { it.updatedAt >= sinceTimestamp }
}

private class InMemoryDeviceDatabase {
    val clientDao = FakeMemClientDao()
    val productDao = FakeMemProductDao()
    val documentDao = FakeMemDocumentDao()
    val lineItemDao = FakeMemLineItemDao()
    val profileDao = FakeMemProfileDao()
    val sequenceDao = FakeMemSequenceDao()
    val accountDao = FakeMemAccountDao()

    fun toDatabaseProxy(): VivaanEnterpriseDatabase {
        return object : VivaanEnterpriseDatabase() {
            override fun clientDao(): ClientDao = this@InMemoryDeviceDatabase.clientDao
            override fun productDao(): ProductDao = this@InMemoryDeviceDatabase.productDao
            override fun businessDocumentDao(): BusinessDocumentDao = this@InMemoryDeviceDatabase.documentDao
            override fun documentLineItemDao(): DocumentLineItemDao = this@InMemoryDeviceDatabase.lineItemDao
            override fun businessProfileDao(): BusinessProfileDao = this@InMemoryDeviceDatabase.profileDao
            override fun documentSequenceDao(): DocumentSequenceDao = this@InMemoryDeviceDatabase.sequenceDao
            override fun clientAccountEntryDao(): ClientAccountEntryDao = this@InMemoryDeviceDatabase.accountDao
            override fun clearAllTables() {}
            override fun createInvalidationTracker(): androidx.room.InvalidationTracker =
                throw UnsupportedOperationException()
            override fun createOpenHelper(config: androidx.room.DatabaseConfiguration): androidx.sqlite.db.SupportSQLiteOpenHelper =
                throw UnsupportedOperationException()
        }
    }
}

private class FakeMemClientDao : ClientDao {
    val data = mutableMapOf<String, ClientEntity>()
    override suspend fun upsert(client: ClientEntity) { data[client.id] = client }
    override suspend fun getById(id: String): ClientEntity? = data[id]?.takeIf { !it.isDeleted }
    override suspend fun getByIdIncludingDeleted(id: String): ClientEntity? = data[id]
    override fun observeById(id: String): Flow<ClientEntity?> = flowOf(data[id])
    override fun observeActiveClients(): Flow<List<ClientEntity>> = flowOf(data.values.filter { !it.isDeleted })
    override suspend fun getBySyncStatus(status: SyncStatus): List<ClientEntity> = data.values.filter { it.syncStatus == status }
    override suspend fun getBySyncStatuses(statuses: List<SyncStatus>): List<ClientEntity> = data.values.filter { it.syncStatus in statuses }
    override suspend fun getUnsynced(): List<ClientEntity> = data.values.filter { it.syncStatus in listOf(SyncStatus.PENDING, SyncStatus.FAILED) }
    override fun observeUnsyncedCount(): Flow<Int> = flowOf(data.values.count { it.syncStatus in listOf(SyncStatus.PENDING, SyncStatus.FAILED) })
    override fun observeFailedCount(): Flow<Int> = flowOf(data.values.count { it.syncStatus == SyncStatus.FAILED })
    override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {}
}

private class FakeMemProductDao : ProductDao {
    val data = mutableMapOf<String, ProductEntity>()
    override suspend fun upsert(product: ProductEntity) { data[product.id] = product }
    override suspend fun getById(id: String): ProductEntity? = data[id]?.takeIf { !it.isDeleted && it.isActive }
    override suspend fun getByIdIncludingDeleted(id: String): ProductEntity? = data[id]
    override fun observeById(id: String): Flow<ProductEntity?> = flowOf(data[id])
    override fun observeAllProducts(): Flow<List<ProductEntity>> = flowOf(data.values.filter { !it.isDeleted })
    override fun observeActiveProducts(): Flow<List<ProductEntity>> = flowOf(data.values.filter { !it.isDeleted && it.isActive })
    override suspend fun getBySyncStatus(status: SyncStatus): List<ProductEntity> = data.values.filter { it.syncStatus == status }
    override suspend fun getBySyncStatuses(statuses: List<SyncStatus>): List<ProductEntity> = data.values.filter { it.syncStatus in statuses }
    override suspend fun getUnsynced(): List<ProductEntity> = data.values.filter { it.syncStatus in listOf(SyncStatus.PENDING, SyncStatus.FAILED) }
    override fun observeUnsyncedCount(): Flow<Int> = flowOf(data.values.count { it.syncStatus in listOf(SyncStatus.PENDING, SyncStatus.FAILED) })
    override fun observeFailedCount(): Flow<Int> = flowOf(data.values.count { it.syncStatus == SyncStatus.FAILED })
    override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {}
}

private class FakeMemDocumentDao : BusinessDocumentDao {
    val data = mutableMapOf<String, BusinessDocumentEntity>()
    override suspend fun upsert(document: BusinessDocumentEntity) { data[document.id] = document }
    override suspend fun getById(id: String): BusinessDocumentEntity? = data[id]?.takeIf { !it.isDeleted }
    override suspend fun getByIdIncludingDeleted(id: String): BusinessDocumentEntity? = data[id]
    override fun observeById(id: String): Flow<BusinessDocumentEntity?> = flowOf(data[id])
    override fun observeAllDocuments(): Flow<List<BusinessDocumentEntity>> = flowOf(data.values.filter { !it.isDeleted })
    override fun observeDocumentsByType(type: DocumentType): Flow<List<BusinessDocumentEntity>> = flowOf(data.values.filter { it.documentType == type && !it.isDeleted })
    override fun observeDocumentsByClient(clientId: String): Flow<List<BusinessDocumentEntity>> = flowOf(data.values.filter { it.clientId == clientId && !it.isDeleted })
    override suspend fun getBySyncStatus(status: SyncStatus): List<BusinessDocumentEntity> = data.values.filter { it.syncStatus == status }
    override suspend fun getBySyncStatuses(statuses: List<SyncStatus>): List<BusinessDocumentEntity> = data.values.filter { it.syncStatus in statuses }
    override suspend fun getUnsynced(): List<BusinessDocumentEntity> = data.values.filter { it.syncStatus in listOf(SyncStatus.PENDING, SyncStatus.FAILED) }
    override fun observeUnsyncedCount(): Flow<Int> = flowOf(data.values.count { it.syncStatus in listOf(SyncStatus.PENDING, SyncStatus.FAILED) })
    override fun observeFailedCount(): Flow<Int> = flowOf(data.values.count { it.syncStatus == SyncStatus.FAILED })
    override suspend fun findByDocumentTypeAndNumber(documentType: DocumentType, documentNumber: String): BusinessDocumentEntity? =
        data.values.find { it.documentType == documentType && it.documentNumber == documentNumber && !it.isDeleted }
    override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {
        data[id]?.let { data[id] = it.copy(isDeleted = true, deletedAt = deletedAt, updatedAt = updatedAt, syncStatus = syncStatus) }
    }
    override fun observeDashboardSummary(): Flow<DashboardSummaryProjection> = flowOf(DashboardSummaryProjection(0, 0L))
}

private class FakeMemLineItemDao : DocumentLineItemDao {
    val data = mutableMapOf<String, DocumentLineItemEntity>()
    override suspend fun upsert(item: DocumentLineItemEntity) { data[item.id] = item }
    override suspend fun upsertAll(items: List<DocumentLineItemEntity>) { items.forEach { data[it.id] = it } }
    override suspend fun getByDocumentId(documentId: String): List<DocumentLineItemEntity> =
        data.values.filter { it.documentId == documentId }.sortedBy { it.position }
    override fun observeByDocumentId(documentId: String): Flow<List<DocumentLineItemEntity>> =
        flowOf(data.values.filter { it.documentId == documentId }.sortedBy { it.position })
    override suspend fun deleteByDocumentId(documentId: String) {
        data.entries.removeIf { it.value.documentId == documentId }
    }
}

private class FakeMemProfileDao : BusinessProfileDao {
    var profile: BusinessProfileEntity? = null
    override suspend fun upsert(profile: BusinessProfileEntity) { this.profile = profile }
    override fun observeProfile(): Flow<BusinessProfileEntity?> = flowOf(profile)
    override suspend fun getProfile(): BusinessProfileEntity? = profile
    override suspend fun getUnsyncedProfile(): BusinessProfileEntity? = profile?.takeIf { it.syncStatus in listOf(SyncStatus.PENDING, SyncStatus.FAILED) }
    override fun observeUnsyncedCount(): Flow<Int> = flowOf(if (profile?.syncStatus in listOf(SyncStatus.PENDING, SyncStatus.FAILED)) 1 else 0)
}

private class FakeMemSequenceDao : DocumentSequenceDao {
    val data = mutableMapOf<String, DocumentSequenceEntity>()
    override suspend fun upsert(sequence: DocumentSequenceEntity) {
        val key = "${sequence.documentType}_${sequence.financialYear}"
        data[key] = sequence
    }
    override suspend fun getSequence(documentType: DocumentType, financialYear: String): DocumentSequenceEntity? =
        data["${documentType}_${financialYear}"]
    override suspend fun getAll(): List<DocumentSequenceEntity> = data.values.toList()
}

private class FakeMemAccountDao : ClientAccountEntryDao {
    val data = mutableMapOf<String, ClientAccountEntryEntity>()
    override suspend fun upsert(entry: ClientAccountEntryEntity) { data[entry.id] = entry }
    override suspend fun getBySyncStatus(status: SyncStatus): List<ClientAccountEntryEntity> =
        data.values.filter { it.syncStatus == status }
    override suspend fun getBySyncStatuses(statuses: List<SyncStatus>): List<ClientAccountEntryEntity> =
        data.values.filter { it.syncStatus in statuses }
    override suspend fun getUnsynced(): List<ClientAccountEntryEntity> =
        data.values.filter { it.syncStatus in listOf(SyncStatus.PENDING, SyncStatus.FAILED) }
    override fun observeUnsyncedCount(): Flow<Int> =
        flowOf(data.values.count { it.syncStatus in listOf(SyncStatus.PENDING, SyncStatus.FAILED) })
    override fun observeFailedCount(): Flow<Int> =
        flowOf(data.values.count { it.syncStatus == SyncStatus.FAILED })
    override fun observeByClientId(clientId: String): Flow<List<ClientAccountEntryEntity>> =
        flowOf(data.values.filter { it.clientId == clientId && !it.isDeleted })
    override suspend fun findByDocumentId(documentId: String): ClientAccountEntryEntity? =
        data.values.find { it.documentId == documentId && !it.isDeleted }
    override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {
        data[id]?.let { data[id] = it.copy(isDeleted = true, deletedAt = deletedAt, updatedAt = updatedAt, syncStatus = syncStatus) }
    }
}

private class FakeSyncDataStore : SyncPreferencesDataStore() {
    private var timestamp: Long = 0L
    private var status: String = "IDLE"
    private var error: String? = null

    override suspend fun getLastSyncTimestamp(): Long = timestamp
    override suspend fun setLastSyncTimestamp(timestamp: Long) { this.timestamp = timestamp }
    override suspend fun recordSyncSuccess(timestamp: Long) {
        this.timestamp = timestamp
        this.status = "SUCCESS"
        this.error = null
    }
    override suspend fun recordSyncFailure(attemptTimestamp: Long, error: String) {
        this.status = "FAILED"
        this.error = error
    }
    override fun observeSyncMetadata(): Flow<SyncMetadata> = flowOf(
        SyncMetadata(lastSyncTimestamp = timestamp, lastSyncStatus = status, lastSyncError = error)
    )
}

private class FakeAuthRepo(private val user: AuthenticatedUser) : AuthRepository {
    override val authState: Flow<AuthState> = flowOf(AuthState.SignedIn(user))
    override fun getCurrentUser(): AuthenticatedUser = user
    override suspend fun signInWithEmail(email: String, password: String) = Result.success(user)
    override suspend fun signOut() = Result.success(Unit)
}
