package com.vivaanenterprise.app.core.database.entity

import androidx.room.Entity
import com.vivaanenterprise.app.core.common.DocumentType

@Entity(
    tableName = "document_sequences",
    primaryKeys = ["documentType", "financialYear"]
)
data class DocumentSequenceEntity(
    val documentType: DocumentType,
    val financialYear: String,
    val lastSequenceNumber: Int,
    val updatedAt: Long
)
