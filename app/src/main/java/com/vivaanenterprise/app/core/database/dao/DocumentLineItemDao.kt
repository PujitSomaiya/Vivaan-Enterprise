package com.vivaanenterprise.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vivaanenterprise.app.core.database.entity.DocumentLineItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentLineItemDao {
    @Upsert
    suspend fun upsertAll(items: List<DocumentLineItemEntity>)

    @Upsert
    suspend fun upsert(item: DocumentLineItemEntity)

    @Query("SELECT * FROM document_line_items WHERE documentId = :documentId ORDER BY position ASC")
    fun observeByDocumentId(documentId: String): Flow<List<DocumentLineItemEntity>>

    @Query("SELECT * FROM document_line_items WHERE documentId = :documentId ORDER BY position ASC")
    suspend fun getByDocumentId(documentId: String): List<DocumentLineItemEntity>

    @Query("DELETE FROM document_line_items WHERE documentId = :documentId")
    suspend fun deleteByDocumentId(documentId: String)
}
