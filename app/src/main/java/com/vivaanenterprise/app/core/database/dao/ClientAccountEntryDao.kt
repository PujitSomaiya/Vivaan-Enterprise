package com.vivaanenterprise.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.database.entity.ClientAccountEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientAccountEntryDao {
    @Upsert
    suspend fun upsert(entry: ClientAccountEntryEntity)

    @Query("SELECT * FROM client_account_entries WHERE clientId = :clientId AND isDeleted = 0 ORDER BY entryDate DESC, createdAt DESC")
    fun observeByClientId(clientId: String): Flow<List<ClientAccountEntryEntity>>

    @Query("SELECT * FROM client_account_entries WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<ClientAccountEntryEntity>

    @Query("SELECT * FROM client_account_entries WHERE documentId = :documentId AND isDeleted = 0 LIMIT 1")
    suspend fun findByDocumentId(documentId: String): ClientAccountEntryEntity?

    @Query("UPDATE client_account_entries SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus = SyncStatus.PENDING)
}
