package com.vivaanenterprise.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vivaanenterprise.app.core.common.SyncStatus

@Entity(
    tableName = "clients",
    indices = [
        Index("companyName"),
        Index("updatedAt"),
        Index("syncStatus"),
        Index("isDeleted")
    ]
)
data class ClientEntity(
    @PrimaryKey
    val id: String,
    val companyName: String,
    val address: String? = null,
    val gstin: String? = null,
    val state: String? = null,
    val stateCode: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val pan: String? = null,
    val iec: String? = null,
    val otherDetails: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncedAt: Long? = null,
    val syncError: String? = null
)
