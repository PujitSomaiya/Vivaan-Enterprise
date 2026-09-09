package com.vivaanenterprise.app.data.local.mapper

import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.database.entity.ClientEntity
import com.vivaanenterprise.app.domain.model.Client

fun ClientEntity.toDomain(): Client {
    return Client(
        id = id,
        companyName = companyName,
        address = address,
        gstin = gstin,
        state = state,
        stateCode = stateCode,
        email = email,
        phone = phone,
        pan = pan,
        iec = iec,
        otherDetails = otherDetails,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Client.toEntity(
    syncStatus: SyncStatus = SyncStatus.PENDING,
    isDeleted: Boolean = false,
    deletedAt: Long? = null,
    lastSyncedAt: Long? = null,
    syncError: String? = null
): ClientEntity {
    return ClientEntity(
        id = id,
        companyName = companyName,
        address = address,
        gstin = gstin,
        state = state,
        stateCode = stateCode,
        email = email,
        phone = phone,
        pan = pan,
        iec = iec,
        otherDetails = otherDetails,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        syncStatus = syncStatus,
        lastSyncedAt = lastSyncedAt,
        syncError = syncError
    )
}
