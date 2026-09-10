package com.vivaanenterprise.app.data.local.mapper

import com.vivaanenterprise.app.core.database.entity.ClientAccountEntryEntity
import com.vivaanenterprise.app.domain.model.ClientAccountEntry

fun ClientAccountEntryEntity.toDomain(): ClientAccountEntry = ClientAccountEntry(
    id = id,
    clientId = clientId,
    documentId = documentId,
    entryType = entryType,
    entryDate = entryDate,
    amountPaise = amountPaise,
    narration = narration,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

fun ClientAccountEntry.toEntity(isDeleted: Boolean = false, deletedAt: Long? = null): ClientAccountEntryEntity = ClientAccountEntryEntity(
    id = id,
    clientId = clientId,
    documentId = documentId,
    entryType = entryType,
    entryDate = entryDate,
    amountPaise = amountPaise,
    narration = narration,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    syncStatus = syncStatus
)
