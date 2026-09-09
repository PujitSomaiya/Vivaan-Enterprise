package com.vivaanenterprise.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.database.entity.BusinessDocumentEntity
import com.vivaanenterprise.app.core.database.entity.ClientEntity
import com.vivaanenterprise.app.core.database.entity.DocumentLineItemEntity
import com.vivaanenterprise.app.core.database.entity.DocumentSequenceEntity
import com.vivaanenterprise.app.core.database.entity.ProductEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenceTest {

    private lateinit var db: VivaanEnterpriseDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VivaanEnterpriseDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testClientInsertAndNullableFields() = runBlocking {
        val client = ClientEntity(
            id = "client-uuid-1",
            companyName = "Eco Enterprise",
            address = null,
            gstin = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        db.clientDao().upsert(client)

        val retrieved = db.clientDao().getById("client-uuid-1")
        assertNotNull(retrieved)
        assertEquals("Eco Enterprise", retrieved?.companyName)
        assertNull(retrieved?.gstin)
        assertNull(retrieved?.state)
    }

    @Test
    fun testProductExactGstRateBasisPoints() = runBlocking {
        val product = ProductEntity(
            id = "prod-uuid-1",
            name = "Scotch Tape",
            defaultGstRateBasisPoints = 1800,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        db.productDao().upsert(product)

        val retrieved = db.productDao().getById("prod-uuid-1")
        assertNotNull(retrieved)
        assertEquals(1800, retrieved?.defaultGstRateBasisPoints)
    }

    @Test
    fun testDocumentAndLineItemRelationshipAndPaiseRounding() = runBlocking {
        val doc = BusinessDocumentEntity(
            id = "doc-uuid-1",
            documentType = DocumentType.TAX_INVOICE,
            documentNumber = "VE/01/2026-27",
            documentDate = 1000L,
            status = DocumentStatus.DRAFT,
            clientId = "client-uuid-1",
            taxableAmountPaise = 250000L,
            grandTotalPaise = 295000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        db.businessDocumentDao().upsert(doc)

        val lineItem = DocumentLineItemEntity(
            id = "item-uuid-1",
            documentId = "doc-uuid-1",
            position = 1,
            descriptionSnapshot = "Item 1",
            quantity = 2L,
            ratePaise = 125000L,
            gstRateBasisPoints = 1800,
            taxableAmountPaise = 250000L,
            lineTotalPaise = 295000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        db.documentLineItemDao().upsert(lineItem)

        val retrievedDoc = db.businessDocumentDao().getById("doc-uuid-1")
        assertNotNull(retrievedDoc)
        assertEquals(250000L, retrievedDoc?.taxableAmountPaise)
        assertEquals(295000L, retrievedDoc?.grandTotalPaise)

        val items = db.documentLineItemDao().getByDocumentId("doc-uuid-1")
        assertEquals(1, items.size)
        assertEquals(125000L, items[0].ratePaise)
    }

    @Test
    fun testSoftDeletedRecordsExcludedFromActiveQuery() = runBlocking {
        val client = ClientEntity(
            id = "client-uuid-2",
            companyName = "Beta Traders",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        db.clientDao().upsert(client)

        var activeList = db.clientDao().observeActiveClients().first()
        assertEquals(1, activeList.size)

        db.clientDao().softDelete("client-uuid-2", deletedAt = 2000L, updatedAt = 2000L)

        activeList = db.clientDao().observeActiveClients().first()
        assertEquals(0, activeList.size)

        val directGet = db.clientDao().getById("client-uuid-2")
        assertNull(directGet)
    }

    @Test
    fun testPendingSyncQueryReturnsPendingRecords() = runBlocking {
        val client1 = ClientEntity(
            id = "client-uuid-3",
            companyName = "Alpha Corp",
            createdAt = 1000L,
            updatedAt = 1000L,
            syncStatus = SyncStatus.PENDING
        )
        val client2 = ClientEntity(
            id = "client-uuid-4",
            companyName = "Gamma LLC",
            createdAt = 1000L,
            updatedAt = 1000L,
            syncStatus = SyncStatus.SYNCED
        )
        db.clientDao().upsert(client1)
        db.clientDao().upsert(client2)

        val pendingList = db.clientDao().getBySyncStatus(SyncStatus.PENDING)
        assertEquals(1, pendingList.size)
        assertEquals("client-uuid-3", pendingList[0].id)
    }

    @Test
    fun testDocumentSequencesIndependentByDocumentType() = runBlocking {
        val invoiceSeq = DocumentSequenceEntity(
            documentType = DocumentType.TAX_INVOICE,
            financialYear = "2026-27",
            lastSequenceNumber = 5,
            updatedAt = 1000L
        )
        val poSeq = DocumentSequenceEntity(
            documentType = DocumentType.PURCHASE_ORDER,
            financialYear = "2026-27",
            lastSequenceNumber = 12,
            updatedAt = 1000L
        )
        db.documentSequenceDao().upsert(invoiceSeq)
        db.documentSequenceDao().upsert(poSeq)

        val fetchedInvoiceSeq = db.documentSequenceDao().getSequence(DocumentType.TAX_INVOICE, "2026-27")
        val fetchedPoSeq = db.documentSequenceDao().getSequence(DocumentType.PURCHASE_ORDER, "2026-27")

        assertEquals(5, fetchedInvoiceSeq?.lastSequenceNumber)
        assertEquals(12, fetchedPoSeq?.lastSequenceNumber)
    }
}
