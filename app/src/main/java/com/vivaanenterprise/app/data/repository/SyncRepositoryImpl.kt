package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.common.TimeProvider
import com.vivaanenterprise.app.core.database.VivaanEnterpriseDatabase
import com.vivaanenterprise.app.core.datastore.SyncPreferencesDataStore
import com.vivaanenterprise.app.data.remote.datasource.FirestoreSyncDataSource
import com.vivaanenterprise.app.data.remote.mapper.toDto
import com.vivaanenterprise.app.data.remote.mapper.toEntity
import com.vivaanenterprise.app.domain.repository.AuthRepository
import com.vivaanenterprise.app.domain.repository.SyncMergePolicy
import com.vivaanenterprise.app.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val db: VivaanEnterpriseDatabase,
    private val firestoreSyncDataSource: FirestoreSyncDataSource,
    private val authRepository: AuthRepository,
    private val syncPreferencesDataStore: SyncPreferencesDataStore,
    private val timeProvider: TimeProvider
) : SyncRepository {

    override suspend fun synchronize(): Result<Unit> {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            return Result.success(Unit)
        }

        val syncStartTime = timeProvider.currentTimeMillis()
        val lastSyncTime = syncPreferencesDataStore.getLastSyncTimestamp()

        return try {
            // ==========================================
            // STEP 1: PUSH LOCAL PENDING CHANGES
            // ==========================================

            // 1. Business Profile
            val profile = db.businessProfileDao().getProfile()
            if (profile != null && profile.syncStatus == SyncStatus.PENDING) {
                try {
                    firestoreSyncDataSource.pushBusinessProfile(profile.toDto())
                    db.businessProfileDao().upsert(profile.copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = syncStartTime, syncError = null))
                } catch (e: Exception) {
                    db.businessProfileDao().upsert(profile.copy(syncStatus = SyncStatus.FAILED, syncError = e.message?.take(100)))
                }
            }

            // 2. Clients
            val pendingClients = db.clientDao().getBySyncStatus(SyncStatus.PENDING)
            for (client in pendingClients) {
                try {
                    firestoreSyncDataSource.pushClient(client.toDto())
                    db.clientDao().upsert(client.copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = syncStartTime, syncError = null))
                } catch (e: Exception) {
                    db.clientDao().upsert(client.copy(syncStatus = SyncStatus.FAILED, syncError = e.message?.take(100)))
                }
            }

            // 3. Products
            val pendingProducts = db.productDao().getBySyncStatus(SyncStatus.PENDING)
            for (product in pendingProducts) {
                try {
                    firestoreSyncDataSource.pushProduct(product.toDto())
                    db.productDao().upsert(product.copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = syncStartTime, syncError = null))
                } catch (e: Exception) {
                    db.productDao().upsert(product.copy(syncStatus = SyncStatus.FAILED, syncError = e.message?.take(100)))
                }
            }

            // 4. Business Documents & Line Items
            val pendingDocuments = db.businessDocumentDao().getBySyncStatus(SyncStatus.PENDING)
            for (doc in pendingDocuments) {
                try {
                    firestoreSyncDataSource.pushBusinessDocument(doc.toDto())
                    val lineItems = db.documentLineItemDao().getByDocumentId(doc.id)
                    for (item in lineItems) {
                        firestoreSyncDataSource.pushDocumentLineItem(item.toDto())
                    }
                    db.businessDocumentDao().upsert(doc.copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = syncStartTime, syncError = null))
                } catch (e: Exception) {
                    db.businessDocumentDao().upsert(doc.copy(syncStatus = SyncStatus.FAILED, syncError = e.message?.take(100)))
                }
            }

            // 5. Client Account Entries
            val pendingAccountEntries = db.clientAccountEntryDao().getBySyncStatus(SyncStatus.PENDING)
            for (entry in pendingAccountEntries) {
                try {
                    firestoreSyncDataSource.pushClientAccountEntry(entry.toDto())
                    db.clientAccountEntryDao().upsert(entry.copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = syncStartTime, syncError = null))
                } catch (e: Exception) {
                    db.clientAccountEntryDao().upsert(entry.copy(syncStatus = SyncStatus.FAILED, syncError = e.message?.take(100)))
                }
            }

            // ==========================================
            // STEP 2: PULL REMOTE CHANGES IN STRICT PARENT->CHILD ORDER
            // ==========================================

            // Order 1: Business Profile
            val remoteProfiles = firestoreSyncDataSource.pullBusinessProfilesSince(lastSyncTime)
            for (remoteDto in remoteProfiles) {
                db.businessProfileDao().upsert(remoteDto.toEntity(syncedAt = syncStartTime))
            }

            // Order 2: Clients
            val remoteClients = firestoreSyncDataSource.pullClientsSince(lastSyncTime)
            for (remoteDto in remoteClients) {
                val localClient = db.clientDao().getById(remoteDto.id)
                when (SyncMergePolicy.evaluateClientMerge(localClient, remoteDto.isDeleted, remoteDto.updatedAt)) {
                    SyncMergePolicy.MergeResult.ACCEPT_REMOTE -> {
                        db.clientDao().upsert(remoteDto.toEntity(syncedAt = syncStartTime))
                    }
                    SyncMergePolicy.MergeResult.REJECT_REMOTE_PROTECT_LOCAL,
                    SyncMergePolicy.MergeResult.REJECT_FINALIZED_SNAPSHOT_MUTATION -> { /* Protect local state */ }
                }
            }

            // Order 3: Products
            val remoteProducts = firestoreSyncDataSource.pullProductsSince(lastSyncTime)
            for (remoteDto in remoteProducts) {
                val localProduct = db.productDao().getById(remoteDto.id)
                when (SyncMergePolicy.evaluateProductMerge(localProduct, remoteDto.isDeleted, remoteDto.updatedAt)) {
                    SyncMergePolicy.MergeResult.ACCEPT_REMOTE -> {
                        db.productDao().upsert(remoteDto.toEntity(syncedAt = syncStartTime))
                    }
                    SyncMergePolicy.MergeResult.REJECT_REMOTE_PROTECT_LOCAL,
                    SyncMergePolicy.MergeResult.REJECT_FINALIZED_SNAPSHOT_MUTATION -> { /* Protect local state */ }
                }
            }

            // Order 4: Business Documents
            val remoteDocuments = firestoreSyncDataSource.pullBusinessDocumentsSince(lastSyncTime)
            for (remoteDto in remoteDocuments) {
                val localDoc = db.businessDocumentDao().getById(remoteDto.id)
                when (SyncMergePolicy.evaluateDocumentMerge(localDoc, remoteDto.updatedAt)) {
                    SyncMergePolicy.MergeResult.ACCEPT_REMOTE -> {
                        db.businessDocumentDao().upsert(remoteDto.toEntity(syncedAt = syncStartTime))
                    }
                    SyncMergePolicy.MergeResult.REJECT_REMOTE_PROTECT_LOCAL,
                    SyncMergePolicy.MergeResult.REJECT_FINALIZED_SNAPSHOT_MUTATION -> { /* Protect local document / finalized snapshot */ }
                }
            }

            // Order 5: Document Line Items (Foreign Key Parent: BusinessDocument)
            val remoteItems = firestoreSyncDataSource.pullDocumentLineItemsSince(lastSyncTime)
            for (remoteDto in remoteItems) {
                val parentDoc = db.businessDocumentDao().getById(remoteDto.documentId)
                if (parentDoc != null) {
                    when (SyncMergePolicy.evaluateLineItemMerge(parentDoc)) {
                        SyncMergePolicy.MergeResult.ACCEPT_REMOTE -> {
                            db.documentLineItemDao().upsert(remoteDto.toEntity())
                        }
                        else -> { /* Protect finalized document line items */ }
                    }
                }
            }

            // Order 6: Client Account Entries (Foreign Key Parents: Client, BusinessDocument)
            val remoteEntries = firestoreSyncDataSource.pullClientAccountEntriesSince(lastSyncTime)
            for (remoteDto in remoteEntries) {
                val localEntry = db.clientAccountEntryDao().getBySyncStatus(SyncStatus.SYNCED).firstOrNull { it.id == remoteDto.id }
                val parentClient = db.clientDao().getById(remoteDto.clientId)
                if (parentClient != null) {
                    when (SyncMergePolicy.evaluateAccountEntryMerge(localEntry, remoteDto.updatedAt)) {
                        SyncMergePolicy.MergeResult.ACCEPT_REMOTE -> {
                            db.clientAccountEntryDao().upsert(remoteDto.toEntity(syncedAt = syncStartTime))
                        }
                        else -> { /* Protect local state */ }
                    }
                }
            }

            // Order 7: Document Sequences
            val remoteSequences = firestoreSyncDataSource.pullDocumentSequencesSince(lastSyncTime)
            for (remoteDto in remoteSequences) {
                db.documentSequenceDao().upsert(remoteDto.toEntity())
            }

            // Advance Cursor ONLY after complete pull succeeds
            syncPreferencesDataStore.setLastSyncTimestamp(syncStartTime)

            Result.success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

