package com.vivaanenterprise.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.database.entity.BusinessDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDocumentDao {
    @Upsert
    suspend fun upsert(document: BusinessDocumentEntity)

    @Query("SELECT * FROM business_documents WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): BusinessDocumentEntity?

    @Query("SELECT * FROM business_documents WHERE id = :id LIMIT 1")
    suspend fun getByIdIncludingDeleted(id: String): BusinessDocumentEntity?

    @Query("SELECT * FROM business_documents WHERE id = :id AND isDeleted = 0")
    fun observeById(id: String): Flow<BusinessDocumentEntity?>

    @Query("SELECT * FROM business_documents WHERE isDeleted = 0 ORDER BY documentDate DESC, updatedAt DESC, id DESC")
    fun observeAllDocuments(): Flow<List<BusinessDocumentEntity>>

    @Query("SELECT * FROM business_documents WHERE documentType = :type AND isDeleted = 0 ORDER BY documentDate DESC, createdAt DESC")
    fun observeDocumentsByType(type: DocumentType): Flow<List<BusinessDocumentEntity>>

    @Query("SELECT * FROM business_documents WHERE clientId = :clientId AND isDeleted = 0 ORDER BY documentDate DESC")
    fun observeDocumentsByClient(clientId: String): Flow<List<BusinessDocumentEntity>>

    @Query("SELECT * FROM business_documents WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<BusinessDocumentEntity>

    @Query("SELECT * FROM business_documents WHERE documentType = :documentType AND documentNumber = :documentNumber AND isDeleted = 0 LIMIT 1")
    suspend fun findByDocumentTypeAndNumber(documentType: DocumentType, documentNumber: String): BusinessDocumentEntity?

    @Query("UPDATE business_documents SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus = SyncStatus.PENDING)
    @Query("SELECT COUNT(*) AS count, COALESCE(SUM(grandTotalPaise), 0) AS totalBilledPaise FROM business_documents WHERE documentType = 'TAX_INVOICE' AND status = 'FINALIZED' AND isDeleted = 0")
    fun observeDashboardSummary(): Flow<DashboardSummaryProjection>
}

data class DashboardSummaryProjection(
    val count: Int,
    val totalBilledPaise: Long
)
