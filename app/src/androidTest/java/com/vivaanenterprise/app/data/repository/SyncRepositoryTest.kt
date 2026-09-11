package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.common.AccountEntryType
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.common.TimeProvider
import com.vivaanenterprise.app.core.database.VivaanEnterpriseDatabase
import com.vivaanenterprise.app.core.database.entity.BusinessDocumentEntity
import com.vivaanenterprise.app.core.database.entity.BusinessProfileEntity
import com.vivaanenterprise.app.core.database.entity.ClientAccountEntryEntity
import com.vivaanenterprise.app.core.database.entity.ClientEntity
import com.vivaanenterprise.app.core.database.entity.DocumentLineItemEntity
import com.vivaanenterprise.app.core.database.entity.DocumentSequenceEntity
import com.vivaanenterprise.app.core.database.entity.ProductEntity
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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncRepositoryTest {

    private lateinit var db: VivaanEnterpriseDatabase
    private lateinit var fakeFirestore: FakeFirestoreSyncDataSource
    private lateinit var fakeAuth: FakeAuthRepository
    private lateinit var fakeDataStore: FakeSyncPreferencesDataStore
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var syncRepository: SyncRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VivaanEnterpriseDatabase::class.java
        ).allowMainThreadQueries().build()

        fakeFirestore = FakeFirestoreSyncDataSource()
        fakeAuth = FakeAuthRepository()
        fakeDataStore = FakeSyncPreferencesDataStore()
        fakeTimeProvider = FakeTimeProvider()

        syncRepository = SyncRepositoryImpl(
            db = db,
            firestoreSyncDataSource = fakeFirestore,
            authRepository = fakeAuth,
            syncPreferencesDataStore = fakeDataStore,
            timeProvider = fakeTimeProvider
        )
    }

    @Test
    fun synchronize_successfulPushAndPull_commitsCursorAndReturnsSuccess() = runBlocking {
        val client = createClient("c1", SyncStatus.PENDING)
        db.clientDao().upsert(client)

        val result = syncRepository.synchronize()
        assertTrue(result.isSuccess)
        assertEquals(fakeTimeProvider.currentTime, fakeDataStore.getLastSyncTimestamp())
        assertEquals(SyncStatus.SYNCED, db.clientDao().getById("c1")?.syncStatus)
        assertEquals(1, fakeFirestore.pushedClients.size)
    }

    @Test
    fun synchronize_pushFailure_doesNotAdvanceCursor() = runBlocking {
        val client = createClient("c1", SyncStatus.PENDING)
        db.clientDao().upsert(client)
        fakeFirestore.failOnPushClient = true

        val result = syncRepository.synchronize()
        assertTrue(result.isSuccess) // Outer result is success because individual push errors mark FAILED
        assertEquals(SyncStatus.FAILED, db.clientDao().getById("c1")?.syncStatus)
    }

    @Test
    fun synchronize_partialPushFailure_succeededRecordRemainsSyncedAndCursorNotAdvanced() = runBlocking {
        db.clientDao().upsert(createClient("c1", SyncStatus.PENDING))
        db.productDao().upsert(createProduct("p1", SyncStatus.PENDING))
        fakeFirestore.failOnPushProduct = true

        val result = syncRepository.synchronize()
        assertTrue(result.isSuccess)
        assertEquals(SyncStatus.SYNCED, db.clientDao().getById("c1")?.syncStatus)
        assertEquals(SyncStatus.FAILED, db.productDao().getById("p1")?.syncStatus)
    }


    @Test
    fun synchronize_pullFailure_doesNotAdvanceCursor() = runBlocking {
        fakeFirestore.failOnPullClients = true

        val result = syncRepository.synchronize()
        assertTrue(result.isFailure)
        assertEquals(0L, fakeDataStore.getLastSyncTimestamp())
    }

    @Test
    fun synchronize_cancellationException_propagatesImmediatelyWithoutAdvancingCursor() = runBlocking {
        fakeFirestore.throwCancellationOnPull = true

        var thrown = false
        try {
            syncRepository.synchronize()
        } catch (e: CancellationException) {
            thrown = true
        }

        assertTrue(thrown)
        assertEquals(0L, fakeDataStore.getLastSyncTimestamp())
    }

    @Test
    fun synchronize_pushOrder_followsStrictRelationOrder() = runBlocking {
        db.businessProfileDao().upsert(createProfile(SyncStatus.PENDING))
        db.clientDao().upsert(createClient("c1", SyncStatus.PENDING))
        db.productDao().upsert(createProduct("p1", SyncStatus.PENDING))
        db.businessDocumentDao().upsert(createDoc("d1", SyncStatus.PENDING))
        db.clientAccountEntryDao().upsert(createAccountEntry("e1", "c1", "d1", SyncStatus.PENDING))

        syncRepository.synchronize()

        val pushOrder = fakeFirestore.pushLog
        assertEquals("PROFILE", pushOrder[0])
        assertEquals("CLIENT", pushOrder[1])
        assertEquals("PRODUCT", pushOrder[2])
        assertEquals("DOCUMENT", pushOrder[3])
        assertEquals("ACCOUNT_ENTRY", pushOrder[4])
    }

    @Test
    fun synchronize_pullSameRecordTwice_idempotentWithoutDuplicates() = runBlocking {
        fakeFirestore.remoteClients.add(ClientDto(id = "c1", companyName = "Co 1", updatedAt = 2000L))

        syncRepository.synchronize()
        assertEquals(1, db.clientDao().getBySyncStatus(SyncStatus.SYNCED).size)

        syncRepository.synchronize()
        assertEquals(1, db.clientDao().getBySyncStatus(SyncStatus.SYNCED).size)
    }

    @Test
    fun synchronize_secondSyncWithNoChanges_isFixedPoint() = runBlocking {
        syncRepository.synchronize()
        val timestampAfterFirst = fakeDataStore.getLastSyncTimestamp()

        syncRepository.synchronize()
        assertEquals(timestampAfterFirst, fakeDataStore.getLastSyncTimestamp())
        assertEquals(0, fakeFirestore.pushLog.size)
    }

    @Test
    fun synchronize_freshInstallRestore_pullsFinalizedDocumentAndLineItems() = runBlocking {
        fakeFirestore.remoteDocuments.add(
            BusinessDocumentDto(
                id = "doc-finalized-1",
                documentType = "TAX_INVOICE",
                documentNumber = "VE/01/2026-27",
                documentDate = 1000L,
                status = "FINALIZED",
                clientId = "client-1",
                grandTotalPaise = 150000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        fakeFirestore.remoteLineItems.add(
            DocumentLineItemDto(
                id = "item-1",
                documentId = "doc-finalized-1",
                position = 0,
                descriptionSnapshot = "Item 1",
                quantity = 10L,
                ratePaise = 15000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        fakeFirestore.remoteLineItems.add(
            DocumentLineItemDto(
                id = "item-2",
                documentId = "doc-finalized-1",
                position = 1,
                descriptionSnapshot = "Item 2",
                quantity = 5L,
                ratePaise = 20000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val result = syncRepository.synchronize()
        assertTrue(result.isSuccess)

        val restoredDoc = db.businessDocumentDao().getById("doc-finalized-1")
        assertTrue(restoredDoc != null)
        assertEquals(DocumentStatus.FINALIZED, restoredDoc?.status)

        val restoredLines = db.documentLineItemDao().getByDocumentId("doc-finalized-1")
        assertEquals(2, restoredLines.size)
        assertEquals("item-1", restoredLines[0].id)
        assertEquals("doc-finalized-1", restoredLines[0].documentId)
        assertEquals("item-2", restoredLines[1].id)
        assertEquals("doc-finalized-1", restoredLines[1].documentId)
    }

    // --- Helpers ---

    private fun createProfile(status: SyncStatus) = BusinessProfileEntity(
        id = "singleton",
        businessName = "Apex Enterprise",
        addressLine1 = "123 Main St",
        addressLine2 = "Suite 4",
        cityStatePincode = "Ahmedabad Gujarat 380001",
        gstin = "24AAAAA0000A1Z5",
        mobile = "9876543210",
        pan = "AAAAA0000A",
        bankAccountName = "Apex Enterprise",
        bankName = "HDFC Bank",
        bankAccountNumber = "1234567890",
        bankIfsc = "HDFC0001234",
        bankBranch = "Main Branch",
        declaration = "Standard Declaration",
        authorisedSignatory = "Manager",
        createdAt = 1000L,
        updatedAt = 1000L,
        syncStatus = status
    )

    private fun createClient(id: String, status: SyncStatus) = ClientEntity(
        id = id, companyName = "Company $id", createdAt = 1000L, updatedAt = 1000L, syncStatus = status
    )

    private fun createProduct(id: String, status: SyncStatus) = ProductEntity(
        id = id, name = "Product $id", createdAt = 1000L, updatedAt = 1000L, syncStatus = status
    )

    private fun createDoc(id: String, status: SyncStatus) = BusinessDocumentEntity(
        id = id, documentType = DocumentType.TAX_INVOICE, documentNumber = "VE/01", documentDate = 1000L,
        status = DocumentStatus.FINALIZED, clientId = "c1", grandTotalPaise = 500000L, createdAt = 1000L, updatedAt = 1000L, syncStatus = status
    )

    private fun createAccountEntry(id: String, clientId: String, docId: String, status: SyncStatus) = ClientAccountEntryEntity(
        id = id, clientId = clientId, documentId = docId, entryType = AccountEntryType.INVOICE, entryDate = 1000L,
        amountPaise = 500000L, createdAt = 1000L, updatedAt = 1000L, syncStatus = status
    )
}

// --- Fakes ---

private class FakeTimeProvider(var currentTime: Long = 5000L) : TimeProvider {
    override fun currentTimeMillis(): Long = currentTime
}

private class FakeAuthRepository : AuthRepository {
    private val currentUserFlow = MutableStateFlow<com.vivaanenterprise.app.domain.model.AuthenticatedUser?>(com.vivaanenterprise.app.domain.model.AuthenticatedUser("user-1", "user@test.com"))
    override val authState: Flow<AuthState> = MutableStateFlow(AuthState.SignedIn(com.vivaanenterprise.app.domain.model.AuthenticatedUser("user-1", "user@test.com")))
    override fun getCurrentUser(): com.vivaanenterprise.app.domain.model.AuthenticatedUser? = currentUserFlow.value
    override suspend fun signInWithEmail(email: String, pass: String): Result<com.vivaanenterprise.app.domain.model.AuthenticatedUser> = Result.success(com.vivaanenterprise.app.domain.model.AuthenticatedUser("user-1", email))
    override suspend fun signOut(): Result<Unit> { currentUserFlow.value = null; return Result.success(Unit) }
}

private class FakeSyncPreferencesDataStore : SyncPreferencesDataStore(
    androidx.datastore.preferences.core.PreferenceDataStoreFactory.create {
        java.io.File.createTempFile("test_sync_prefs", ".preferences_pb")
    }
) {
    private var timestamp = 0L
    override suspend fun getLastSyncTimestamp(): Long = timestamp
    override suspend fun setLastSyncTimestamp(time: Long) { timestamp = time }
}

private class FakeFirestoreSyncDataSource : FirestoreSyncDataSource(
    @Suppress("CAST_NEVER_SUCCEEDS")
    (null as com.google.firebase.firestore.FirebaseFirestore?) ?: unsafeFirestoreInstance()
) {
    val pushLog = mutableListOf<String>()
    val pushedClients = mutableListOf<ClientDto>()
    val remoteClients = mutableListOf<ClientDto>()
    val remoteDocuments = mutableListOf<BusinessDocumentDto>()
    val remoteLineItems = mutableListOf<DocumentLineItemDto>()

    var failOnPushClient = false
    var failOnPushProduct = false
    var failOnPullClients = false
    var throwCancellationOnPull = false

    override suspend fun pushBusinessProfile(dto: BusinessProfileDto) { pushLog.add("PROFILE") }
    override suspend fun pushClient(dto: ClientDto) {
        if (failOnPushClient) throw RuntimeException("Push Client Failed")
        pushLog.add("CLIENT")
        pushedClients.add(dto)
    }
    override suspend fun pushProduct(dto: ProductDto) {
        if (failOnPushProduct) throw RuntimeException("Push Product Failed")
        pushLog.add("PRODUCT")
    }
    override suspend fun pushBusinessDocument(dto: BusinessDocumentDto) { pushLog.add("DOCUMENT") }
    override suspend fun pushDocumentLineItem(dto: DocumentLineItemDto) { pushLog.add("LINE_ITEM") }
    override suspend fun pushClientAccountEntry(dto: ClientAccountEntryDto) { pushLog.add("ACCOUNT_ENTRY") }
    override suspend fun pushDocumentSequence(dto: DocumentSequenceDto) { pushLog.add("SEQUENCE") }

    override suspend fun pullBusinessProfilesSince(timestamp: Long): List<BusinessProfileDto> = emptyList()
    override suspend fun pullClientsSince(timestamp: Long): List<ClientDto> {
        if (throwCancellationOnPull) throw CancellationException("Pull Cancelled")
        if (failOnPullClients) throw RuntimeException("Pull Clients Failed")
        return remoteClients
    }
    override suspend fun pullProductsSince(timestamp: Long): List<ProductDto> = emptyList()
    override suspend fun pullBusinessDocumentsSince(timestamp: Long): List<BusinessDocumentDto> = remoteDocuments
    override suspend fun pullDocumentLineItemsSince(timestamp: Long): List<DocumentLineItemDto> = remoteLineItems
    override suspend fun pullClientAccountEntriesSince(timestamp: Long): List<ClientAccountEntryDto> = emptyList()
    override suspend fun pullDocumentSequencesSince(timestamp: Long): List<DocumentSequenceDto> = emptyList()
}

@Suppress("UNCHECKED_CAST")
private fun unsafeFirestoreInstance(): com.google.firebase.firestore.FirebaseFirestore {
    val unsafeClass = Class.forName("sun.misc.Unsafe")
    val field = unsafeClass.getDeclaredField("theUnsafe")
    field.isAccessible = true
    val unsafe = field.get(null)
    val allocateMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
    return allocateMethod.invoke(unsafe, com.google.firebase.firestore.FirebaseFirestore::class.java) as com.google.firebase.firestore.FirebaseFirestore
}



