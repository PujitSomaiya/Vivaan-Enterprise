package com.vivaanenterprise.app.domain.repository

import kotlinx.coroutines.flow.Flow

data class SyncStatusSummary(
    val lastSyncTimestamp: Long = 0L,
    val lastSyncAttemptTimestamp: Long = 0L,
    val lastSyncStatus: String = "IDLE", // "SUCCESS", "FAILED", "IDLE"
    val lastSyncError: String? = null,
    val unsyncedCount: Int = 0,
    val failedCount: Int = 0,
    val isSyncing: Boolean = false
)

interface SyncRepository {
    suspend fun synchronize(forceReconcile: Boolean = false): Result<Unit>
    fun observeSyncStatus(): Flow<SyncStatusSummary>
}
