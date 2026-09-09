package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.common.TimeProvider
import com.vivaanenterprise.app.core.database.dao.ProductDao
import com.vivaanenterprise.app.core.database.entity.ProductEntity
import com.vivaanenterprise.app.core.sync.SyncScheduler
import javax.inject.Inject
import javax.inject.Singleton

open class ProductSeeder @Inject constructor(
    private val productDao: ProductDao,
    private val syncScheduler: SyncScheduler,
    private val timeProvider: TimeProvider
) {
    companion object {
        const val SEED_PRODUCT_ID = "seed-product-3m-scotch-tape-15mm"
        const val SEED_PRODUCT_NAME = "3M anti-slip 15mm Scotch Tape"
        const val SEED_PRODUCT_HSN = "3919"
        const val SEED_PRODUCT_GST_BP = 1800
    }

    open suspend fun seedInitialData() {
        val existing = productDao.getByIdIncludingDeleted(SEED_PRODUCT_ID)
        if (existing == null) {
            val now = timeProvider.currentTimeMillis()
            val seedEntity = ProductEntity(
                id = SEED_PRODUCT_ID,
                name = SEED_PRODUCT_NAME,
                hsnSac = SEED_PRODUCT_HSN,
                defaultGstRateBasisPoints = SEED_PRODUCT_GST_BP,
                isActive = true,
                createdAt = now,
                updatedAt = now,
                isDeleted = false,
                deletedAt = null,
                syncStatus = SyncStatus.PENDING
            )
            productDao.upsert(seedEntity)
            try {
                syncScheduler.enqueueSync()
            } catch (e: Exception) {
                // Ignore sync scheduler enqueue failure for local persistence safety
            }
        }
    }
}
