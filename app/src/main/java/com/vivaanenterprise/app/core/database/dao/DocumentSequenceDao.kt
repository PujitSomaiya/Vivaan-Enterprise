package com.vivaanenterprise.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.database.entity.DocumentSequenceEntity

@Dao
interface DocumentSequenceDao {
    @Upsert
    suspend fun upsert(sequence: DocumentSequenceEntity)

    @Query("SELECT * FROM document_sequences WHERE documentType = :documentType AND financialYear = :financialYear LIMIT 1")
    suspend fun getSequence(documentType: DocumentType, financialYear: String): DocumentSequenceEntity?
}
