package com.vivaanenterprise.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.database.entity.ClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Upsert
    suspend fun upsert(client: ClientEntity)

    @Query("SELECT * FROM clients WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): ClientEntity?

    @Query("SELECT * FROM clients WHERE id = :id AND isDeleted = 0")
    fun observeById(id: String): Flow<ClientEntity?>

    @Query("SELECT * FROM clients WHERE isDeleted = 0 ORDER BY companyName ASC")
    fun observeActiveClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<ClientEntity>

    @Query("UPDATE clients SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus = SyncStatus.PENDING)
}
