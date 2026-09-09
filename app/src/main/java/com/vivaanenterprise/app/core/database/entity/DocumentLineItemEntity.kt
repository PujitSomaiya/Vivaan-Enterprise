package com.vivaanenterprise.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "document_line_items",
    foreignKeys = [
        ForeignKey(
            entity = BusinessDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("documentId"),
        Index(value = ["documentId", "position"])
    ]
)
data class DocumentLineItemEntity(
    @PrimaryKey
    val id: String,
    val documentId: String,
    val productId: String? = null,
    val position: Int,
    val descriptionSnapshot: String,
    val hsnSacSnapshot: String? = null,
    val quantity: Long,
    val ratePaise: Long,
    val gstRateBasisPoints: Int,
    val taxableAmountPaise: Long = 0L,
    val cgstAmountPaise: Long = 0L,
    val sgstAmountPaise: Long = 0L,
    val igstAmountPaise: Long = 0L,
    val totalTaxPaise: Long = 0L,
    val lineTotalPaise: Long = 0L,
    val createdAt: Long,
    val updatedAt: Long
)
