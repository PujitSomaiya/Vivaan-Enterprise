package com.vivaanenterprise.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.database.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Upsert
    suspend fun upsert(product: ProductEntity)

    @Query("SELECT * FROM products WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getByIdIncludingDeleted(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id AND isDeleted = 0")
    fun observeById(id: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND isActive = 1 ORDER BY name ASC")
    fun observeActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<ProductEntity>

    @Query("UPDATE products SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus = SyncStatus.PENDING)
}
