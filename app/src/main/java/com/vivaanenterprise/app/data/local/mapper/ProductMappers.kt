package com.vivaanenterprise.app.data.local.mapper

import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.database.entity.ProductEntity
import com.vivaanenterprise.app.domain.model.Product

fun ProductEntity.toDomain(): Product? {
    val nonNullHsn = hsnSac?.trim()?.ifBlank { null }
    val nonNullGst = defaultGstRateBasisPoints
    if (name.isBlank() || nonNullHsn == null || nonNullGst == null) {
        return null
    }
    return Product(
        id = id,
        name = name,
        hsnSac = nonNullHsn,
        defaultGstRateBasisPoints = nonNullGst,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Product.toEntity(
    syncStatus: SyncStatus = SyncStatus.PENDING,
    isDeleted: Boolean = false,
    deletedAt: Long? = null
): ProductEntity = ProductEntity(
    id = id,
    name = name,
    hsnSac = hsnSac,
    defaultGstRateBasisPoints = defaultGstRateBasisPoints,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    syncStatus = syncStatus
)
