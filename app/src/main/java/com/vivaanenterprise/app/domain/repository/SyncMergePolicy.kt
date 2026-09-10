package com.vivaanenterprise.app.domain.repository

import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.database.entity.BusinessDocumentEntity
import com.vivaanenterprise.app.core.database.entity.ClientAccountEntryEntity
import com.vivaanenterprise.app.core.database.entity.ClientEntity
import com.vivaanenterprise.app.core.database.entity.DocumentLineItemEntity
import com.vivaanenterprise.app.core.database.entity.DocumentSequenceEntity
import com.vivaanenterprise.app.core.database.entity.ProductEntity

object SyncMergePolicy {

    enum class MergeResult {
        ACCEPT_REMOTE,
        REJECT_REMOTE_PROTECT_LOCAL,
        REJECT_FINALIZED_SNAPSHOT_MUTATION
    }

    // Merge decision for Client
    fun evaluateClientMerge(local: ClientEntity?, remoteIsDeleted: Boolean, remoteUpdatedAt: Long): MergeResult {
        if (local == null) return MergeResult.ACCEPT_REMOTE
        if (local.syncStatus == SyncStatus.PENDING || local.syncStatus == SyncStatus.FAILED) {
            return MergeResult.REJECT_REMOTE_PROTECT_LOCAL
        }
        if (remoteUpdatedAt >= local.updatedAt) return MergeResult.ACCEPT_REMOTE
        return MergeResult.REJECT_REMOTE_PROTECT_LOCAL
    }

    // Merge decision for Product
    fun evaluateProductMerge(local: ProductEntity?, remoteIsDeleted: Boolean, remoteUpdatedAt: Long): MergeResult {
        if (local == null) return MergeResult.ACCEPT_REMOTE
        if (local.syncStatus == SyncStatus.PENDING || local.syncStatus == SyncStatus.FAILED) {
            return MergeResult.REJECT_REMOTE_PROTECT_LOCAL
        }
        if (remoteUpdatedAt >= local.updatedAt) return MergeResult.ACCEPT_REMOTE
        return MergeResult.REJECT_REMOTE_PROTECT_LOCAL
    }

    // Merge decision for BusinessDocument
    fun evaluateDocumentMerge(local: BusinessDocumentEntity?, remoteIsDeleted: Boolean, remoteUpdatedAt: Long): MergeResult {
        if (local == null) return MergeResult.ACCEPT_REMOTE
        if (local.syncStatus == SyncStatus.PENDING || local.syncStatus == SyncStatus.FAILED) {
            return MergeResult.REJECT_REMOTE_PROTECT_LOCAL
        }
        if (remoteIsDeleted) {
            return MergeResult.ACCEPT_REMOTE
        }
        if (local.status == DocumentStatus.FINALIZED && !local.isDeleted) {
            return MergeResult.REJECT_FINALIZED_SNAPSHOT_MUTATION
        }
        if (remoteUpdatedAt >= local.updatedAt) return MergeResult.ACCEPT_REMOTE
        return MergeResult.REJECT_REMOTE_PROTECT_LOCAL
    }

    // Merge decision for Line Item
    fun evaluateLineItemMerge(parentDocument: BusinessDocumentEntity?): MergeResult {
        return MergeResult.ACCEPT_REMOTE
    }

    // Merge decision for Account Entry
    fun evaluateAccountEntryMerge(local: ClientAccountEntryEntity?, remoteUpdatedAt: Long): MergeResult {
        if (local == null) return MergeResult.ACCEPT_REMOTE
        if (local.syncStatus == SyncStatus.PENDING || local.syncStatus == SyncStatus.FAILED) {
            return MergeResult.REJECT_REMOTE_PROTECT_LOCAL
        }
        if (remoteUpdatedAt >= local.updatedAt) return MergeResult.ACCEPT_REMOTE
        return MergeResult.REJECT_REMOTE_PROTECT_LOCAL
    }
}
