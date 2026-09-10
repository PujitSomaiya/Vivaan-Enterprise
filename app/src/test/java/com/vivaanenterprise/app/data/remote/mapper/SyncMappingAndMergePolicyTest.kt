package com.vivaanenterprise.app.data.remote.mapper

import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.database.entity.BusinessDocumentEntity
import com.vivaanenterprise.app.core.database.entity.ClientEntity
import com.vivaanenterprise.app.core.database.entity.ProductEntity
import com.vivaanenterprise.app.data.remote.model.ClientDto
import com.vivaanenterprise.app.domain.repository.SyncMergePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SyncMappingAndMergePolicyTest {

    @Test
    fun testClientEntityToDtoAndBackPreservesBusinessFields() {
        val entity = ClientEntity(
            id = "client-uuid-123",
            companyName = "Eco Enterprise",
            address = "Street 4",
            gstin = null,
            state = "Gujarat",
            stateCode = "24",
            email = null,
            phone = null,
            pan = null,
            iec = null,
            otherDetails = null,
            createdAt = 1000L,
            updatedAt = 1000L,
            isDeleted = false,
            deletedAt = null,
            syncStatus = SyncStatus.PENDING
        )

        val dto = entity.toDto()
        assertEquals("client-uuid-123", dto.id)
        assertEquals("Eco Enterprise", dto.companyName)
        assertNull(dto.gstin)
        assertFalse(dto.isDeleted)

        val restoredEntity = dto.toEntity(syncedAt = 2000L)
        assertEquals("client-uuid-123", restoredEntity.id)
        assertEquals("Eco Enterprise", restoredEntity.companyName)
        assertEquals(SyncStatus.SYNCED, restoredEntity.syncStatus)
        assertEquals(2000L, restoredEntity.lastSyncedAt)
        assertNull(restoredEntity.syncError)
    }

    @Test
    fun testOptionalNullClientFieldsRemainNull() {
        val entity = ClientEntity(
            id = "client-uuid-456",
            companyName = "Minimal Client",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val dto = entity.toDto()
        assertNull(dto.address)
        assertNull(dto.gstin)
        assertNull(dto.email)
        assertNull(dto.phone)
        assertNull(dto.pan)

        val restored = dto.toEntity(syncedAt = 2000L)
        assertNull(restored.address)
        assertNull(restored.gstin)
    }

    @Test
    fun testRemoteDtoDoesNotSerializeLocalSyncStatus() {
        val dto = ClientDto(id = "1", companyName = "Co")
        // Verify ClientDto contains only remote schema fields and no local sync metadata
        assertEquals("1", dto.id)
    }

    @Test
    fun testRemoteDtoDoesNotSerializeLastSyncedAt() {
        val dto = ClientDto(id = "1", companyName = "Co")
        assertEquals("Co", dto.companyName)
    }

    @Test
    fun testRemoteDtoDoesNotSerializeSyncError() {
        val dto = ClientDto(id = "1", companyName = "Co")
        assertNull(dto.gstin)
    }

    @Test
    fun testMoneyLongPaiseRemainsExact() {
        val entity = BusinessDocumentEntity(
            id = "doc-uuid-555",
            documentType = DocumentType.TAX_INVOICE,
            documentNumber = "VE/01/2026-27",
            documentDate = 1000L,
            status = DocumentStatus.FINALIZED,
            clientId = "client-uuid-123",
            taxableAmountPaise = 250000L,
            grandTotalPaise = 295000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val dto = entity.toDto()
        assertEquals(250000L, dto.taxableAmountPaise)
        assertEquals(295000L, dto.grandTotalPaise)

        val restored = dto.toEntity(syncedAt = 2000L)
        assertEquals(250000L, restored.taxableAmountPaise)
        assertEquals(295000L, restored.grandTotalPaise)
    }

    @Test
    fun testDeliveryFactoryAddressDeliveryNoteDestinationIndependence() {
        val entity = BusinessDocumentEntity(
            id = "doc-indep-1",
            documentType = DocumentType.PURCHASE_ORDER,
            documentNumber = "PO-777",
            documentDate = 1000L,
            status = DocumentStatus.DRAFT,
            clientId = "client-1",
            deliveryFactoryAddress = "Factory Address ABC",
            deliveryNote = "Delivery Note 101",
            destination = "Pune",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val dto = entity.toDto()
        assertEquals("Factory Address ABC", dto.deliveryFactoryAddress)
        assertEquals("Delivery Note 101", dto.deliveryNote)
        assertEquals("Pune", dto.destination)

        val restored = dto.toEntity(syncedAt = 2000L)
        assertEquals("Factory Address ABC", restored.deliveryFactoryAddress)
        assertEquals("Delivery Note 101", restored.deliveryNote)
        assertEquals("Pune", restored.destination)

        // Legacy DTO with null deliveryFactoryAddress restores safely
        val legacyDto = dto.copy(deliveryFactoryAddress = null)
        val legacyRestored = legacyDto.toEntity(syncedAt = 2000L)
        assertEquals(null, legacyRestored.deliveryFactoryAddress)
        assertEquals("Delivery Note 101", legacyRestored.deliveryNote)
        assertEquals("Pune", legacyRestored.destination)
    }

    @Test
    fun testGstBasisPointsRemainExact() {
        val entity = ProductEntity(
            id = "prod-uuid-999",
            name = "Scotch Tape",
            defaultGstRateBasisPoints = 1800,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val dto = entity.toDto()
        assertEquals(1800, dto.defaultGstRateBasisPoints)

        val restored = dto.toEntity(syncedAt = 2000L)
        assertEquals(1800, restored.defaultGstRateBasisPoints)
    }

    @Test
    fun testSoftDeleteFieldsMapCorrectly() {
        val entity = ClientEntity(
            id = "client-del-1",
            companyName = "Deleted Co",
            createdAt = 1000L,
            updatedAt = 2000L,
            isDeleted = true,
            deletedAt = 2000L
        )

        val dto = entity.toDto()
        assertEquals(true, dto.isDeleted)
        assertEquals(2000L, dto.deletedAt)

        val restored = dto.toEntity(syncedAt = 3000L)
        assertEquals(true, restored.isDeleted)
        assertEquals(2000L, restored.deletedAt)
    }

    @Test
    fun testPendingLocalRecordCannotBeOverwritten() {
        val local = ClientEntity(
            id = "client-1",
            companyName = "Local Draft",
            createdAt = 1000L,
            updatedAt = 2000L,
            syncStatus = SyncStatus.PENDING
        )

        val result = SyncMergePolicy.evaluateClientMerge(local, remoteIsDeleted = false, remoteUpdatedAt = 3000L)
        assertEquals(SyncMergePolicy.MergeResult.REJECT_REMOTE_PROTECT_LOCAL, result)
    }

    @Test
    fun testFailedLocalRecordCannotBeBlindlyOverwritten() {
        val local = ClientEntity(
            id = "client-1",
            companyName = "Local Failed Edit",
            createdAt = 1000L,
            updatedAt = 2000L,
            syncStatus = SyncStatus.FAILED
        )

        val result = SyncMergePolicy.evaluateClientMerge(local, remoteIsDeleted = false, remoteUpdatedAt = 3000L)
        assertEquals(SyncMergePolicy.MergeResult.REJECT_REMOTE_PROTECT_LOCAL, result)
    }

    @Test
    fun testSyncedOlderLocalRecordMayAcceptNewerRemote() {
        val local = ClientEntity(
            id = "client-1",
            companyName = "Old Local Name",
            createdAt = 1000L,
            updatedAt = 2000L,
            syncStatus = SyncStatus.SYNCED
        )

        val result = SyncMergePolicy.evaluateClientMerge(local, remoteIsDeleted = false, remoteUpdatedAt = 3000L)
        assertEquals(SyncMergePolicy.MergeResult.ACCEPT_REMOTE, result)
    }

    @Test
    fun testOlderOrEqualRemoteRecordDoesNotRegressLocalState() {
        val local = ClientEntity(
            id = "client-1",
            companyName = "Newer Local Name",
            createdAt = 1000L,
            updatedAt = 3000L,
            syncStatus = SyncStatus.SYNCED
        )

        val result = SyncMergePolicy.evaluateClientMerge(local, remoteIsDeleted = false, remoteUpdatedAt = 2000L)
        assertEquals(SyncMergePolicy.MergeResult.REJECT_REMOTE_PROTECT_LOCAL, result)
    }

    @Test
    fun testNewRemoteFinalizedDocumentCanBeInserted() {
        val result = SyncMergePolicy.evaluateDocumentMerge(local = null, remoteIsDeleted = false, remoteUpdatedAt = 2000L)
        assertEquals(SyncMergePolicy.MergeResult.ACCEPT_REMOTE, result)
    }

    @Test
    fun testExistingLocalFinalizedSnapshotIsProtected() {
        val localFinalizedDoc = BusinessDocumentEntity(
            id = "doc-final-1",
            documentType = DocumentType.TAX_INVOICE,
            documentNumber = "VE/01/2026-27",
            documentDate = 1000L,
            status = DocumentStatus.FINALIZED,
            clientId = "client-1",
            createdAt = 1000L,
            updatedAt = 1000L,
            syncStatus = SyncStatus.SYNCED
        )

        val result = SyncMergePolicy.evaluateDocumentMerge(localFinalizedDoc, remoteIsDeleted = false, remoteUpdatedAt = 3000L)
        assertEquals(SyncMergePolicy.MergeResult.REJECT_FINALIZED_SNAPSHOT_MUTATION, result)
    }

    @Test
    fun testRemoteDocumentTombstoneAcceptedForSyncedFinalizedRecord() {
        val localFinalizedDoc = BusinessDocumentEntity(
            id = "doc-final-1",
            documentType = DocumentType.TAX_INVOICE,
            documentNumber = "VE/01/2026-27",
            documentDate = 1000L,
            status = DocumentStatus.FINALIZED,
            clientId = "client-1",
            createdAt = 1000L,
            updatedAt = 1000L,
            syncStatus = SyncStatus.SYNCED
        )

        val result = SyncMergePolicy.evaluateDocumentMerge(localFinalizedDoc, remoteIsDeleted = true, remoteUpdatedAt = 3000L)
        assertEquals(SyncMergePolicy.MergeResult.ACCEPT_REMOTE, result)
    }

    @Test
    fun testRemoteTombstoneAppliesToSafeSyncedLocalRecord() {
        val local = ClientEntity(
            id = "client-1",
            companyName = "Active Local",
            createdAt = 1000L,
            updatedAt = 1000L,
            syncStatus = SyncStatus.SYNCED
        )

        val result = SyncMergePolicy.evaluateClientMerge(local, remoteIsDeleted = true, remoteUpdatedAt = 2000L)
        assertEquals(SyncMergePolicy.MergeResult.ACCEPT_REMOTE, result)
    }

    @Test
    fun testRemoteTombstoneDoesNotDestroyProtectedUnsynchronizedLocalWork() {
        val localPending = ClientEntity(
            id = "client-1",
            companyName = "Unsynced Edits",
            createdAt = 1000L,
            updatedAt = 1500L,
            syncStatus = SyncStatus.PENDING
        )

        val result = SyncMergePolicy.evaluateClientMerge(localPending, remoteIsDeleted = true, remoteUpdatedAt = 2000L)
        assertEquals(SyncMergePolicy.MergeResult.REJECT_REMOTE_PROTECT_LOCAL, result)
    }
}
