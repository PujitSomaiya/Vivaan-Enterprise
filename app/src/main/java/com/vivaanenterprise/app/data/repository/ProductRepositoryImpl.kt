package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.common.IdGenerator
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.common.TimeProvider
import com.vivaanenterprise.app.core.database.dao.ProductDao
import com.vivaanenterprise.app.core.sync.SyncScheduler
import com.vivaanenterprise.app.data.local.mapper.toDomain
import com.vivaanenterprise.app.data.local.mapper.toEntity
import com.vivaanenterprise.app.domain.model.Product
import com.vivaanenterprise.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val syncScheduler: SyncScheduler,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) : ProductRepository {

    override fun observeProducts(): Flow<List<Product>> {
        return productDao.observeAllProducts().map { list ->
            list.mapNotNull { it.toDomain() }
        }
    }

    override fun observeActiveProducts(): Flow<List<Product>> {
        return productDao.observeActiveProducts().map { list ->
            list.mapNotNull { it.toDomain() }
        }
    }

    override fun observeProductById(id: String): Flow<Product?> {
        return productDao.observeById(id).map { it?.toDomain() }
    }

    override suspend fun getProductById(id: String): Product? {
        return productDao.getById(id)?.toDomain()
    }

    override suspend fun createProduct(product: Product): Result<Unit> {
        return try {
            val now = timeProvider.currentTimeMillis()
            val finalId = if (product.id.isBlank()) idGenerator.newId() else product.id
            val normalizedProduct = product.copy(
                id = finalId,
                name = product.name.trim(),
                hsnSac = product.hsnSac.trim(),
                defaultGstRateBasisPoints = product.defaultGstRateBasisPoints,
                isActive = product.isActive,
                createdAt = if (product.createdAt == 0L) now else product.createdAt,
                updatedAt = now
            )

            productDao.upsert(normalizedProduct.toEntity(syncStatus = SyncStatus.PENDING))

            try {
                syncScheduler.enqueueSync()
            } catch (e: Exception) {
                // Background sync enqueue failure must not corrupt local persistence success
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            val existing = productDao.getById(product.id)
                ?: return Result.failure(IllegalStateException("Product with id ${product.id} not found"))

            val now = timeProvider.currentTimeMillis()
            val normalizedProduct = product.copy(
                name = product.name.trim(),
                hsnSac = product.hsnSac.trim(),
                defaultGstRateBasisPoints = product.defaultGstRateBasisPoints,
                isActive = product.isActive,
                createdAt = existing.createdAt,
                updatedAt = now
            )

            productDao.upsert(
                normalizedProduct.toEntity(
                    syncStatus = SyncStatus.PENDING,
                    isDeleted = existing.isDeleted,
                    deletedAt = existing.deletedAt
                )
            )

            try {
                syncScheduler.enqueueSync()
            } catch (e: Exception) {
                // Ignore sync scheduling failure for local safety
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setProductActive(id: String, isActive: Boolean): Result<Unit> {
        return try {
            val existing = productDao.getById(id)
                ?: return Result.failure(IllegalStateException("Product with id $id not found"))

            val now = timeProvider.currentTimeMillis()
            val updated = existing.copy(
                isActive = isActive,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )

            productDao.upsert(updated)

            try {
                syncScheduler.enqueueSync()
            } catch (e: Exception) {
                // Ignore sync scheduling failure for local safety
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(id: String): Result<Unit> {
        return try {
            val now = timeProvider.currentTimeMillis()
            productDao.softDelete(
                id = id,
                deletedAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )

            try {
                syncScheduler.enqueueSync()
            } catch (e: Exception) {
                // Ignore sync scheduling failure for local safety
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
