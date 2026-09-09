package com.vivaanenterprise.app.feature.product

import com.vivaanenterprise.app.core.common.IdGenerator
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.common.TimeProvider
import com.vivaanenterprise.app.core.database.dao.ProductDao
import com.vivaanenterprise.app.core.database.entity.ProductEntity
import com.vivaanenterprise.app.core.sync.SyncScheduler
import com.vivaanenterprise.app.data.local.mapper.toDomain
import com.vivaanenterprise.app.data.repository.ProductRepositoryImpl
import com.vivaanenterprise.app.data.repository.ProductSeeder
import com.vivaanenterprise.app.domain.model.Product
import com.vivaanenterprise.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProductRepositoryAndSeederTest {

    private lateinit var fakeProductDao: FakeProductDao
    private lateinit var fakeSyncScheduler: FakeSyncScheduler
    private lateinit var repository: ProductRepository
    private lateinit var seeder: ProductSeeder

    @Before
    fun setUp() {
        fakeProductDao = FakeProductDao()
        fakeSyncScheduler = FakeSyncScheduler()

        val idGen = object : IdGenerator {
            override fun newId(): String = "gen-prod-uuid-123"
        }

        val timeProv = object : TimeProvider {
            override fun currentTimeMillis(): Long = 7000L
        }

        repository = ProductRepositoryImpl(
            productDao = fakeProductDao,
            syncScheduler = fakeSyncScheduler,
            idGenerator = idGen,
            timeProvider = timeProv
        )

        seeder = ProductSeeder(
            productDao = fakeProductDao,
            syncScheduler = fakeSyncScheduler,
            timeProvider = timeProv
        )
    }

    @Test
    fun testCreateGeneratesIdAndNormalizesFields() = runBlocking {
        val product = Product(
            id = "",
            name = " 3M Scotch Tape ",
            hsnSac = " 3919 ",
            defaultGstRateBasisPoints = 1800,
            isActive = true,
            createdAt = 0L,
            updatedAt = 0L
        )

        val result = repository.createProduct(product)
        assertTrue(result.isSuccess)

        val saved = fakeProductDao.products["gen-prod-uuid-123"]
        assertNotNull(saved)
        assertEquals("gen-prod-uuid-123", saved?.id)
        assertEquals("3M Scotch Tape", saved?.name)
        assertEquals("3919", saved?.hsnSac)
        assertEquals(SyncStatus.PENDING, saved?.syncStatus)
        assertEquals(1, fakeSyncScheduler.enqueueCount)
    }

    @Test
    fun testUpdatePreservesIdAndCreatedAt() = runBlocking {
        fakeProductDao.products["p-1"] = ProductEntity(
            id = "p-1",
            name = "Original Name",
            hsnSac = "1000",
            defaultGstRateBasisPoints = 1200,
            isActive = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val updatedProduct = Product(
            id = "p-1",
            name = "New Product Name",
            hsnSac = "2000",
            defaultGstRateBasisPoints = 1800,
            isActive = false,
            createdAt = 9999L,
            updatedAt = 9999L
        )

        val result = repository.updateProduct(updatedProduct)
        assertTrue(result.isSuccess)

        val saved = fakeProductDao.products["p-1"]
        assertEquals("p-1", saved?.id)
        assertEquals("New Product Name", saved?.name)
        assertEquals(1000L, saved?.createdAt)
        assertEquals(7000L, saved?.updatedAt)
        assertEquals(SyncStatus.PENDING, saved?.syncStatus)
        assertEquals(1, fakeSyncScheduler.enqueueCount)
    }

    @Test
    fun testSetProductActivePersistsLocallyAndSchedulesSync() = runBlocking {
        fakeProductDao.products["p-2"] = ProductEntity(
            id = "p-2",
            name = "Active Item",
            isActive = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val result = repository.setProductActive("p-2", false)
        assertTrue(result.isSuccess)

        val saved = fakeProductDao.products["p-2"]
        assertFalse(saved?.isActive == true)
        assertEquals(7000L, saved?.updatedAt)
        assertEquals(SyncStatus.PENDING, saved?.syncStatus)
        assertEquals(1, fakeSyncScheduler.enqueueCount)
    }

    @Test
    fun testDeleteProductPerformsSoftDelete() = runBlocking {
        fakeProductDao.products["p-3"] = ProductEntity(
            id = "p-3",
            name = "To Delete",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val result = repository.deleteProduct("p-3")
        assertTrue(result.isSuccess)

        val saved = fakeProductDao.products["p-3"]
        assertTrue(saved?.isDeleted == true)
        assertEquals(7000L, saved?.deletedAt)
        assertEquals(SyncStatus.PENDING, saved?.syncStatus)

        val activeList = repository.observeProducts().first()
        assertTrue(activeList.none { it.id == "p-3" })
    }

    @Test
    fun testSyncSchedulerFailureDoesNotRollbackLocalPersistence() = runBlocking {
        fakeSyncScheduler.shouldFail = true
        val product = Product(id = "p-4", name = "Local Safe Product", hsnSac = "3919", defaultGstRateBasisPoints = 1800, createdAt = 0L, updatedAt = 0L)

        val result = repository.createProduct(product)
        assertTrue(result.isSuccess)
        assertNotNull(fakeProductDao.products["p-4"])
    }

    @Test
    fun testSeederFirstInvocationInsertsExactSeedProduct() = runBlocking {
        seeder.seedInitialData()

        val seedSaved = fakeProductDao.products[ProductSeeder.SEED_PRODUCT_ID]
        assertNotNull(seedSaved)
        assertEquals("3M anti-slip 15mm Scotch Tape", seedSaved?.name)
        assertEquals("3919", seedSaved?.hsnSac)
        assertEquals(1800, seedSaved?.defaultGstRateBasisPoints)
        assertTrue(seedSaved?.isActive == true)
        assertEquals(SyncStatus.PENDING, seedSaved?.syncStatus)
        assertEquals(1, fakeSyncScheduler.enqueueCount)
    }

    @Test
    fun testSeederSecondInvocationIsIdempotentAndDoesNotEnqueueSync() = runBlocking {
        seeder.seedInitialData()
        val initialEnqueueCount = fakeSyncScheduler.enqueueCount

        seeder.seedInitialData()
        assertEquals(initialEnqueueCount, fakeSyncScheduler.enqueueCount)
    }

    @Test
    fun testSeederDoesNotResetUserEditedSeedProduct() = runBlocking {
        seeder.seedInitialData()

        // User edits the seed product name
        val editedSeed = Product(
            id = ProductSeeder.SEED_PRODUCT_ID,
            name = "User Edited Tape",
            hsnSac = "3919",
            defaultGstRateBasisPoints = 1800,
            isActive = true,
            createdAt = 7000L,
            updatedAt = 8000L
        )
        repository.updateProduct(editedSeed)

        // Seeder runs again on app startup
        seeder.seedInitialData()

        val saved = fakeProductDao.products[ProductSeeder.SEED_PRODUCT_ID]
        assertEquals("User Edited Tape", saved?.name)
    }

    @Test
    fun testSeederDoesNotRecreateSoftDeletedSeedProduct() = runBlocking {
        seeder.seedInitialData()

        // User soft deletes the seed product
        repository.deleteProduct(ProductSeeder.SEED_PRODUCT_ID)

        // Seeder runs again
        seeder.seedInitialData()

        val saved = fakeProductDao.products[ProductSeeder.SEED_PRODUCT_ID]
        assertTrue(saved?.isDeleted == true)

        val visibleProducts = repository.observeProducts().first()
        assertTrue(visibleProducts.none { it.id == ProductSeeder.SEED_PRODUCT_ID })
    }

    @Test
    fun testSeederDoesNotReactivateDeactivatedSeedProduct() = runBlocking {
        seeder.seedInitialData()

        // User deactivates the seed product
        repository.setProductActive(ProductSeeder.SEED_PRODUCT_ID, false)

        // Seeder runs again
        seeder.seedInitialData()

        val saved = fakeProductDao.products[ProductSeeder.SEED_PRODUCT_ID]
        assertFalse(saved?.isActive == true)
    }

    @Test
    fun testMalformedNullablePersistenceRowHandledSafely() {
        val malformedEntity1 = ProductEntity(
            id = "m-1",
            name = "Corrupt Item",
            hsnSac = null,
            defaultGstRateBasisPoints = 1800,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        org.junit.Assert.assertNull(malformedEntity1.toDomain())

        val malformedEntity2 = ProductEntity(
            id = "m-2",
            name = "Corrupt Item 2",
            hsnSac = "3919",
            defaultGstRateBasisPoints = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        org.junit.Assert.assertNull(malformedEntity2.toDomain())
    }
}

private class FakeProductDao : ProductDao {
    val products = mutableMapOf<String, ProductEntity>()
    private val flow = MutableStateFlow<List<ProductEntity>>(emptyList())

    private fun updateFlow() {
        flow.value = products.values.filter { !it.isDeleted }.sortedBy { it.name }
    }

    override suspend fun upsert(product: ProductEntity) {
        products[product.id] = product
        updateFlow()
    }

    override suspend fun getById(id: String): ProductEntity? {
        val product = products[id]
        return if (product?.isDeleted == false) product else null
    }

    override suspend fun getByIdIncludingDeleted(id: String): ProductEntity? {
        return products[id]
    }

    override fun observeById(id: String): Flow<ProductEntity?> {
        return flow.map { list -> list.find { it.id == id } }
    }

    override fun observeAllProducts(): Flow<List<ProductEntity>> = flow

    override fun observeActiveProducts(): Flow<List<ProductEntity>> {
        return flow.map { list -> list.filter { it.isActive } }
    }

    override suspend fun getBySyncStatus(status: SyncStatus): List<ProductEntity> {
        return products.values.filter { it.syncStatus == status }
    }

    override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {
        products[id]?.let { existing ->
            products[id] = existing.copy(
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
            throw RuntimeException("Sync scheduler error")
        }
    }
}
