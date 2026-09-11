package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.common.AccountEntryType
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.database.entity.BusinessDocumentEntity
import com.vivaanenterprise.app.core.database.entity.ClientAccountEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientAccountRepositoryTest {

    @Test
    fun observeAccountSummaryForClient_filtersOnlyInvoiceType_andSumsPaiseExactly() = runBlocking {
        val entry1 = ClientAccountEntryEntity(
            id = "e1",
            clientId = "c1",
            documentId = "d1",
            entryType = AccountEntryType.INVOICE,
            entryDate = 1000L,
            amountPaise = 250000L, // ₹ 2,500
            narration = "Invoice 1",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val entry2 = ClientAccountEntryEntity(
            id = "e2",
            clientId = "c1",
            documentId = "d2",
            entryType = AccountEntryType.INVOICE,
            entryDate = 2000L,
            amountPaise = 150000L, // ₹ 1,500
            narration = "Invoice 2",
            createdAt = 2000L,
            updatedAt = 2000L
        )

        // Non-INVOICE entries (e.g. PAYMENT or ADJUSTMENT) must NOT be counted in V1 billing totals
        val nonInvoiceEntry = ClientAccountEntryEntity(
            id = "e3",
            clientId = "c1",
            documentId = null,
            entryType = AccountEntryType.PAYMENT,
            entryDate = 3000L,
            amountPaise = 500000L,
            narration = "Payment received",
            createdAt = 3000L,
            updatedAt = 3000L
        )

        val fakeAccountDao = FakeAccountDao(listOf(entry1, entry2, nonInvoiceEntry))
        val fakeDocDao = FakeDocumentDao(emptyList())
        val fakeDb = FakeAccountDatabase(fakeAccountDao, fakeDocDao)

        val repository = ClientAccountRepositoryImpl(fakeDb)

        val summary = repository.observeAccountSummaryForClient("c1").first()

        assertEquals(2, summary.invoiceCount)
        assertEquals(400000L, summary.totalBilledPaise)
    }

    @Test
    fun observeDashboardSummary_filtersOnlyFinalizedTaxInvoices_andSumsPaiseExactly() = runBlocking {
        val finalizedInvoice1 = BusinessDocumentEntity(
            id = "d1",
            documentType = DocumentType.TAX_INVOICE,
            documentNumber = "VE/01",
            documentDate = 1000L,
            status = DocumentStatus.FINALIZED,
            clientId = "c1",
            taxableAmountPaise = 100000L,
            grandTotalPaise = 118000L, // ₹ 1,180
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val finalizedInvoice2 = BusinessDocumentEntity(
            id = "d2",
            documentType = DocumentType.TAX_INVOICE,
            documentNumber = "VE/02",
            documentDate = 2000L,
            status = DocumentStatus.FINALIZED,
            clientId = "c2",
            taxableAmountPaise = 200000L,
            grandTotalPaise = 236000L, // ₹ 2,360
            createdAt = 2000L,
            updatedAt = 2000L
        )

        val draftInvoice = BusinessDocumentEntity(
            id = "d3",
            documentType = DocumentType.TAX_INVOICE,
            documentNumber = "VE/03",
            documentDate = 3000L,
            status = DocumentStatus.DRAFT,
            clientId = "c1",
            grandTotalPaise = 500000L,
            createdAt = 3000L,
            updatedAt = 3000L
        )

        val finalizedPO = BusinessDocumentEntity(
            id = "d4",
            documentType = DocumentType.PURCHASE_ORDER,
            documentNumber = "VE/PO/01",
            documentDate = 4000L,
            status = DocumentStatus.FINALIZED,
            clientId = "c1",
            grandTotalPaise = 990000L,
            createdAt = 4000L,
            updatedAt = 4000L
        )

        val fakeAccountDao = FakeAccountDao(emptyList())
        val fakeDocDao = FakeDocumentDao(listOf(finalizedInvoice1, finalizedInvoice2, draftInvoice, finalizedPO))
        val fakeDb = FakeAccountDatabase(fakeAccountDao, fakeDocDao)

        val repository = ClientAccountRepositoryImpl(fakeDb)

        val summary = repository.observeDashboardSummary().first()

        // Count: 2 finalized tax invoices (draft invoice & PO excluded)
        assertEquals(2, summary.finalizedInvoiceCount)
        // Total Billed: 118000 + 236000 = 354000L
        assertEquals(354000L, summary.totalBilledPaise)
    }
}

private class FakeAccountDatabase(
    private val accountDao: com.vivaanenterprise.app.core.database.dao.ClientAccountEntryDao,
    private val docDao: com.vivaanenterprise.app.core.database.dao.BusinessDocumentDao
) : com.vivaanenterprise.app.core.database.VivaanEnterpriseDatabase() {
    override fun clientAccountEntryDao(): com.vivaanenterprise.app.core.database.dao.ClientAccountEntryDao = accountDao
    override fun businessDocumentDao(): com.vivaanenterprise.app.core.database.dao.BusinessDocumentDao = docDao
    override fun documentLineItemDao(): com.vivaanenterprise.app.core.database.dao.DocumentLineItemDao = TODO()
    override fun clientDao(): com.vivaanenterprise.app.core.database.dao.ClientDao = TODO()
    override fun productDao(): com.vivaanenterprise.app.core.database.dao.ProductDao = TODO()
    override fun businessProfileDao(): com.vivaanenterprise.app.core.database.dao.BusinessProfileDao = TODO()
    override fun documentSequenceDao(): com.vivaanenterprise.app.core.database.dao.DocumentSequenceDao = TODO()
    override fun clearAllTables() {}
    override fun createInvalidationTracker(): androidx.room.InvalidationTracker {
        return androidx.room.InvalidationTracker(this, "client_account_entries", "business_documents")
    }
}

private class FakeAccountDao(
    private val initialEntries: List<ClientAccountEntryEntity>
) : com.vivaanenterprise.app.core.database.dao.ClientAccountEntryDao {
    override suspend fun upsert(entry: ClientAccountEntryEntity) {}
    override fun observeByClientId(clientId: String): kotlinx.coroutines.flow.Flow<List<ClientAccountEntryEntity>> {
        return kotlinx.coroutines.flow.flowOf(initialEntries.filter { it.clientId == clientId && !it.isDeleted })
    }
    override suspend fun getBySyncStatus(status: SyncStatus): List<ClientAccountEntryEntity> = emptyList()
    override suspend fun findByDocumentId(documentId: String): ClientAccountEntryEntity? = initialEntries.find { it.documentId == documentId }
    override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {}
}

private class FakeDocumentDao(
    initialDocs: List<BusinessDocumentEntity>
) : com.vivaanenterprise.app.core.database.dao.BusinessDocumentDao {
    private val docs = initialDocs.associateBy { it.id }

    override suspend fun upsert(document: BusinessDocumentEntity) {}
    override suspend fun getById(id: String): BusinessDocumentEntity? = docs[id]
    override suspend fun getByIdIncludingDeleted(id: String): BusinessDocumentEntity? = docs[id]
    override fun observeById(id: String): Flow<BusinessDocumentEntity?> = flowOf(docs[id])
    override fun observeAllDocuments(): Flow<List<BusinessDocumentEntity>> = flowOf(docs.values.toList())
    override fun observeDocumentsByType(type: DocumentType): Flow<List<BusinessDocumentEntity>> = flowOf(docs.values.filter { it.documentType == type && !it.isDeleted })
    override fun observeDocumentsByClient(clientId: String): Flow<List<BusinessDocumentEntity>> = flowOf(docs.values.filter { it.clientId == clientId && !it.isDeleted })
    override fun observeDashboardSummary(): Flow<com.vivaanenterprise.app.core.database.dao.DashboardSummaryProjection> {
        val finalizedInvoices = docs.values.filter { it.documentType == DocumentType.TAX_INVOICE && it.status == com.vivaanenterprise.app.core.common.DocumentStatus.FINALIZED && !it.isDeleted }
        val count = finalizedInvoices.size
        val total = finalizedInvoices.fold(0L) { acc, d -> acc + d.grandTotalPaise }
        return flowOf(com.vivaanenterprise.app.core.database.dao.DashboardSummaryProjection(count, total))
    }
    override suspend fun getBySyncStatus(status: com.vivaanenterprise.app.core.common.SyncStatus): List<BusinessDocumentEntity> = docs.values.filter { it.syncStatus == status }
    override suspend fun findByDocumentTypeAndNumber(documentType: DocumentType, documentNumber: String): BusinessDocumentEntity? = null
    override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {}
}
