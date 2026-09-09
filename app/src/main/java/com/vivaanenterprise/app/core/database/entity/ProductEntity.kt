package com.vivaanenterprise.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vivaanenterprise.app.core.common.SyncStatus

@Entity(
    tableName = "products",
    indices = [
        Index("name"),
        Index("isActive"),
        Index("updatedAt"),
        Index("syncStatus"),
        Index("isDeleted")
    ]
)
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val hsnSac: String? = null,
    val defaultGstRateBasisPoints: Int? = null,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncedAt: Long? = null,
    val syncError: String? = null
)
