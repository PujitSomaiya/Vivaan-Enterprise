package com.vivaanenterprise.app.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vivaanenterprise.app.core.common.AccountEntryType
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.database.VivaanEnterpriseDatabase
import com.vivaanenterprise.app.core.database.entity.BusinessDocumentEntity
import com.vivaanenterprise.app.core.database.entity.ClientAccountEntryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Step16DaoInstrumentationTest {

    private lateinit var db: VivaanEnterpriseDatabase
    private lateinit var documentDao: BusinessDocumentDao
    private lateinit var accountEntryDao: ClientAccountEntryDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VivaanEnterpriseDatabase::class.java
        ).allowMainThreadQueries().build()
        documentDao = db.businessDocumentDao()
        accountEntryDao = db.clientAccountEntryDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // =========================================================================
    // BUSINESS DOCUMENT DASHBOARD AGGREGATE DAO TESTS
    // =========================================================================

    @Test
    fun observeDashboardSummary_emptyDatabase_returnsZeroCountAndZeroPaise() = runBlocking {
        val summary = documentDao.observeDashboardSummary().first()
        assertEquals(0, summary.count)
        assertEquals(0L, summary.totalBilledPaise)
    }

    @Test
    fun observeDashboardSummary_finalizedTaxInvoice_includedInCountAndSum() = runBlocking {
        val doc = createDoc(id = "d1", type = DocumentType.TAX_INVOICE, status = DocumentStatus.FINALIZED, total = 500000L)
        documentDao.upsert(doc)

        val summary = documentDao.observeDashboardSummary().first()
        assertEquals(1, summary.count)
        assertEquals(500000L, summary.totalBilledPaise)
    }

    @Test
    fun observeDashboardSummary_multipleFinalizedInvoices_summedExactly() = runBlocking {
        documentDao.upsert(createDoc(id = "d1", type = DocumentType.TAX_INVOICE, status = DocumentStatus.FINALIZED, total = 250000L))
        documentDao.upsert(createDoc(id = "d2", type = DocumentType.TAX_INVOICE, status = DocumentStatus.FINALIZED, total = 750000L))

        val summary = documentDao.observeDashboardSummary().first()
        assertEquals(2, summary.count)
        assertEquals(1000000L, summary.totalBilledPaise)
    }

    @Test
    fun observeDashboardSummary_draftTaxInvoice_excluded() = runBlocking {
        documentDao.upsert(createDoc(id = "d1", type = DocumentType.TAX_INVOICE, status = DocumentStatus.DRAFT, total = 500000L))

        val summary = documentDao.observeDashboardSummary().first()
        assertEquals(0, summary.count)
        assertEquals(0L, summary.totalBilledPaise)
    }

    @Test
    fun observeDashboardSummary_finalizedPurchaseOrder_excluded() = runBlocking {
        documentDao.upsert(createDoc(id = "d1", type = DocumentType.PURCHASE_ORDER, status = DocumentStatus.FINALIZED, total = 500000L))

        val summary = documentDao.observeDashboardSummary().first()
        assertEquals(0, summary.count)
        assertEquals(0L, summary.totalBilledPaise)
    }

    @Test
    fun observeDashboardSummary_draftPurchaseOrder_excluded() = runBlocking {
        documentDao.upsert(createDoc(id = "d1", type = DocumentType.PURCHASE_ORDER, status = DocumentStatus.DRAFT, total = 500000L))

        val summary = documentDao.observeDashboardSummary().first()
        assertEquals(0, summary.count)
        assertEquals(0L, summary.totalBilledPaise)
    }

    @Test
    fun observeDashboardSummary_cancelledTaxInvoice_excluded() = runBlocking {
        documentDao.upsert(createDoc(id = "d1", type = DocumentType.TAX_INVOICE, status = DocumentStatus.CANCELLED, total = 500000L))

        val summary = documentDao.observeDashboardSummary().first()
        assertEquals(0, summary.count)
        assertEquals(0L, summary.totalBilledPaise)
    }

    @Test
    fun observeDashboardSummary_softDeletedFinalizedInvoice_excluded() = runBlocking {
        val doc = createDoc(id = "d1", type = DocumentType.TAX_INVOICE, status = DocumentStatus.FINALIZED, total = 500000L, isDeleted = true)
        documentDao.upsert(doc)

        val summary = documentDao.observeDashboardSummary().first()
        assertEquals(0, summary.count)
        assertEquals(0L, summary.totalBilledPaise)
    }

    @Test
    fun observeDashboardSummary_reactiveEmission_updatesWhenQualifyingInvoiceInserted() = runBlocking {
        val initial = documentDao.observeDashboardSummary().first()
        assertEquals(0, initial.count)

        documentDao.upsert(createDoc(id = "d1", type = DocumentType.TAX_INVOICE, status = DocumentStatus.FINALIZED, total = 300000L))

        val updated = documentDao.observeDashboardSummary().first()
        assertEquals(1, updated.count)
        assertEquals(300000L, updated.totalBilledPaise)
    }

    // =========================================================================
    // CLIENT ACCOUNT ENTRY DAO TESTS
    // =========================================================================

    @Test
    fun observeByClientId_clientIsolation_returnsOnlyMatchingClientEntries() = runBlocking {
        accountEntryDao.upsert(createAccountEntry(id = "e1", clientId = "client-A", amount = 1000L))
        accountEntryDao.upsert(createAccountEntry(id = "e2", clientId = "client-B", amount = 2000L))

        val clientAEntries = accountEntryDao.observeByClientId("client-A").first()
        assertEquals(1, clientAEntries.size)
        assertEquals("e1", clientAEntries[0].id)
        assertEquals(1000L, clientAEntries[0].amountPaise)
    }

    @Test
    fun observeByClientId_deterministicOrdering_entryDateDesc_thenCreatedAtDesc_thenIdDesc() = runBlocking {
        val e1 = createAccountEntry(id = "e1", clientId = "c1", entryDate = 1000L, createdAt = 1000L)
        val e2 = createAccountEntry(id = "e2", clientId = "c1", entryDate = 2000L, createdAt = 1000L)
        val e3 = createAccountEntry(id = "e3", clientId = "c1", entryDate = 2000L, createdAt = 2000L)

        accountEntryDao.upsert(e1)
        accountEntryDao.upsert(e2)
        accountEntryDao.upsert(e3)

        val entries = accountEntryDao.observeByClientId("c1").first()
        assertEquals(3, entries.size)
        assertEquals("e3", entries[0].id)
        assertEquals("e2", entries[1].id)
        assertEquals("e1", entries[2].id)
    }

    @Test
    fun observeByClientId_reactiveEmission_updatesOnNewInsertion() = runBlocking {
        val initial = accountEntryDao.observeByClientId("c1").first()
        assertEquals(0, initial.size)

        accountEntryDao.upsert(createAccountEntry(id = "e1", clientId = "c1", amount = 150000L))

        val updated = accountEntryDao.observeByClientId("c1").first()
        assertEquals(1, updated.size)
        assertEquals(150000L, updated[0].amountPaise)
    }

    // --- Helpers ---

    private fun createDoc(
        id: String,
        type: DocumentType,
        status: DocumentStatus,
        total: Long,
        isDeleted: Boolean = false
    ): BusinessDocumentEntity {
        return BusinessDocumentEntity(
            id = id,
            documentType = type,
            documentNumber = "DOC-$id",
            documentDate = 1000L,
            status = status,
            clientId = "c1",
            taxableAmountPaise = total,
            cgstAmountPaise = 0L,
            sgstAmountPaise = 0L,
            igstAmountPaise = 0L,
            totalTaxAmountPaise = 0L,
            grandTotalPaise = total,
            createdAt = 1000L,
            updatedAt = 1000L,
            syncStatus = SyncStatus.SYNCED,
            isDeleted = isDeleted,
            deletedAt = null
        )
    }


    private suspend fun createAccountEntry(
        id: String,
        clientId: String,
        amount: Long = 100000L,
        entryDate: Long = 1000L,
        createdAt: Long = 1000L
    ): ClientAccountEntryEntity {
        // Insert parent Client and Document entities to satisfy foreign key constraints
        val client = com.vivaanenterprise.app.core.database.entity.ClientEntity(
            id = clientId,
            companyName = "Company $clientId",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        db.clientDao().upsert(client)

        val docId = "doc-$id"
        val doc = createDoc(id = docId, type = DocumentType.TAX_INVOICE, status = DocumentStatus.FINALIZED, total = amount)
        documentDao.upsert(doc)

        return ClientAccountEntryEntity(
            id = id,
            clientId = clientId,
            documentId = docId,
            entryType = AccountEntryType.INVOICE,
            entryDate = entryDate,
            amountPaise = amount,
            narration = "Tax Invoice",
            createdAt = createdAt,
            updatedAt = createdAt,
            syncStatus = SyncStatus.SYNCED,
            isDeleted = false,
            deletedAt = null
        )
    }

}
