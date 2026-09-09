package com.vivaanenterprise.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vivaanenterprise.app.core.common.AccountEntryType
import com.vivaanenterprise.app.core.common.SyncStatus

@Entity(
    tableName = "client_account_entries",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = BusinessDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("clientId"),
        Index("entryDate"),
        Index("documentId"),
        Index("syncStatus"),
        Index("isDeleted")
    ]
)
data class ClientAccountEntryEntity(
    @PrimaryKey
    val id: String,
    val clientId: String,
    val documentId: String? = null,
    val entryType: AccountEntryType,
    val entryDate: Long,
    val amountPaise: Long,
    val narration: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncedAt: Long? = null,
    val syncError: String? = null
)
