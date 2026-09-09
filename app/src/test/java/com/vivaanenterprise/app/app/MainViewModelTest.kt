package com.vivaanenterprise.app.app

import com.vivaanenterprise.app.core.database.dao.BusinessProfileDao
import com.vivaanenterprise.app.core.database.dao.ProductDao
import com.vivaanenterprise.app.core.sync.SyncScheduler
import com.vivaanenterprise.app.data.repository.BusinessProfileSeeder
import com.vivaanenterprise.app.data.repository.ProductSeeder
import com.vivaanenterprise.app.domain.model.AuthState
import com.vivaanenterprise.app.domain.model.AuthenticatedUser
import com.vivaanenterprise.app.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeSyncScheduler: FakeSyncScheduler
    private lateinit var fakeSeeder: FakeProductSeeder
    private lateinit var fakeProfileSeeder: FakeBusinessProfileSeeder

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        fakeSyncScheduler = FakeSyncScheduler()
        fakeSeeder = FakeProductSeeder()
        fakeProfileSeeder = FakeBusinessProfileSeeder()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAuthenticatedBootstrapTriggersSeedAndSyncExactlyOnce() = runTest {
        val testUser = AuthenticatedUser(id = "user-123", email = "test@example.com")
        fakeAuthRepository.authStateFlow.value = AuthState.SignedIn(testUser)

        val viewModel = MainViewModel(
            authRepository = fakeAuthRepository,
            syncScheduler = fakeSyncScheduler,
            productSeeder = fakeSeeder,
            businessProfileSeeder = fakeProfileSeeder
        )

        backgroundScope.launch(testDispatcher) { viewModel.authState.collect {} }
        advanceUntilIdle()

        assertEquals(1, fakeProfileSeeder.seedCallCount)
        assertEquals(1, fakeSeeder.seedCallCount)
        assertEquals(1, fakeSyncScheduler.enqueueCount)

        // Multiple state emissions or navigation recomposition observations do not re-run seeding
        fakeAuthRepository.authStateFlow.value = AuthState.SignedIn(testUser)
        advanceUntilIdle()

        assertEquals(1, fakeProfileSeeder.seedCallCount)
        assertEquals(1, fakeSeeder.seedCallCount)
        assertEquals(1, fakeSyncScheduler.enqueueCount)
    }
}

private class FakeAuthRepository : AuthRepository {
    val authStateFlow = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: Flow<AuthState> = authStateFlow
    override fun getCurrentUser(): AuthenticatedUser? = null
    override suspend fun signInWithEmail(email: String, password: String) =
        Result.success(AuthenticatedUser(id = "user-123", email = email))
    override suspend fun signOut() = Result.success(Unit)
}

private class FakeSyncScheduler : SyncScheduler {
    var enqueueCount = 0
    override fun enqueueSync() {
        enqueueCount++
    }
}

private class FakeBusinessProfileSeeder : BusinessProfileSeeder(
    businessProfileDao = object : BusinessProfileDao {
        override suspend fun upsert(profile: com.vivaanenterprise.app.core.database.entity.BusinessProfileEntity) {}
        override fun observeProfile() = flowOf(null)
        override suspend fun getProfile() = null
    },
    timeProvider = object : com.vivaanenterprise.app.core.common.TimeProvider {
        override fun currentTimeMillis(): Long = 0L
    }
) {
    var seedCallCount = 0
    override suspend fun seedInitialData() {
        seedCallCount++
    }
}

private class FakeProductSeeder : ProductSeeder(
    productDao = object : ProductDao {
        override suspend fun upsert(product: com.vivaanenterprise.app.core.database.entity.ProductEntity) {}
        override suspend fun getById(id: String) = null
        override suspend fun getByIdIncludingDeleted(id: String) = null
        override fun observeById(id: String) = flowOf(null)
        override fun observeAllProducts() = flowOf(emptyList<com.vivaanenterprise.app.core.database.entity.ProductEntity>())
        override fun observeActiveProducts() = flowOf(emptyList<com.vivaanenterprise.app.core.database.entity.ProductEntity>())
        override suspend fun getBySyncStatus(status: com.vivaanenterprise.app.core.common.SyncStatus) = emptyList<com.vivaanenterprise.app.core.database.entity.ProductEntity>()
        override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: com.vivaanenterprise.app.core.common.SyncStatus) {}
    },
    syncScheduler = FakeSyncScheduler(),
    timeProvider = object : com.vivaanenterprise.app.core.common.TimeProvider {
        override fun currentTimeMillis(): Long = 0L
    }
) {
    var seedCallCount = 0
    override suspend fun seedInitialData() {
        seedCallCount++
    }
}
