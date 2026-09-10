package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.IdGenerator
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.common.TimeProvider
import com.vivaanenterprise.app.core.database.VivaanEnterpriseDatabase
import com.vivaanenterprise.app.core.database.dao.BusinessDocumentDao
import com.vivaanenterprise.app.core.database.dao.BusinessProfileDao
import com.vivaanenterprise.app.core.database.dao.ClientAccountEntryDao
import com.vivaanenterprise.app.core.database.dao.ClientDao
import com.vivaanenterprise.app.core.database.dao.DocumentLineItemDao
import com.vivaanenterprise.app.core.database.dao.DocumentSequenceDao
import com.vivaanenterprise.app.core.database.dao.ProductDao
import com.vivaanenterprise.app.core.database.entity.BusinessDocumentEntity
import com.vivaanenterprise.app.core.database.entity.BusinessProfileEntity
import com.vivaanenterprise.app.core.database.entity.ClientAccountEntryEntity
import com.vivaanenterprise.app.core.database.entity.ClientEntity
import com.vivaanenterprise.app.core.database.entity.DocumentLineItemEntity
import com.vivaanenterprise.app.core.database.entity.DocumentSequenceEntity
import com.vivaanenterprise.app.core.database.entity.ProductEntity
import com.vivaanenterprise.app.core.sync.SyncScheduler
import com.vivaanenterprise.app.domain.model.DocumentFinalizationResult
import com.vivaanenterprise.app.domain.model.DocumentLineItem
import com.vivaanenterprise.app.domain.model.DocumentValidationError
import com.vivaanenterprise.app.domain.util.DocumentCalculator
import com.vivaanenterprise.app.domain.util.IndianCurrencyFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DocumentRepositoryTest {

    private lateinit var fakeDocDao: FakeBusinessDocumentDao
    private lateinit var fakeLineDao: FakeDocumentLineItemDao
    private lateinit var fakeClientDao: FakeClientDao
    private lateinit var fakeProfileDao: FakeBusinessProfileDao
    private lateinit var fakeSeqDao: FakeDocumentSequenceDao
    private lateinit var fakeAccountDao: FakeClientAccountEntryDao
    private lateinit var fakeProductDao: FakeProductDao

    private lateinit var idGenerator: IdGenerator
    private lateinit var timeProvider: TimeProvider
    private lateinit var syncScheduler: SyncScheduler

    private lateinit var repository: DocumentRepositoryImpl

    @Before
    fun setUp() {
        fakeDocDao      = FakeBusinessDocumentDao()
        fakeLineDao     = FakeDocumentLineItemDao()
        fakeClientDao   = FakeClientDao()
        fakeProfileDao  = FakeBusinessProfileDao()
        fakeSeqDao      = FakeDocumentSequenceDao()
        fakeAccountDao  = FakeClientAccountEntryDao()
        fakeProductDao  = FakeProductDao()

        val fakeDatabase = FakeVivaanEnterpriseDatabase(
            fakeDocDao,
            fakeLineDao,
            fakeClientDao,
            fakeProfileDao,
            fakeSeqDao,
            fakeAccountDao,
            fakeProductDao
        )

        var idCount = 1
        idGenerator   = object : IdGenerator { override fun newId(): String = "test-id-${idCount++}" }
        timeProvider  = object : TimeProvider  { override fun currentTimeMillis(): Long = 1700000000000L }
        syncScheduler = object : SyncScheduler { override fun enqueueSync() {} }

        // Real calculator — tests now verify internally computed financial values
        val calculator = DocumentCalculator(IndianCurrencyFormatter())

        repository = DocumentRepositoryImpl(
            database      = fakeDatabase,
            idGenerator   = idGenerator,
            timeProvider  = timeProvider,
            syncScheduler = syncScheduler,
            calculator    = calculator
        )
    }

    @Test
    fun testSuggestDocumentNumberIndependentByDocumentTypeAndDoesNotConsumeSequence() = runTest {
        val invNumber = repository.suggestDocumentNumber(DocumentType.TAX_INVOICE, 1700000000000L)
        val poNumber  = repository.suggestDocumentNumber(DocumentType.PURCHASE_ORDER, 1700000000000L)

        assertEquals("VE/01/2023-24", invNumber)
        assertEquals("VE/01/2023-24", poNumber)

        // Suggesting does not consume sequence state
        assertEquals(0, fakeSeqDao.sequences.size)
    }

    /**
     * Verifies that the repository:
     * - internally calls DocumentCalculator (intra-state: seller "24", placeOfSupply "24")
     * - persists the correct calculated grand total (qty=10, rate=₹150, GST 18% → ₹1,770.00)
     * - freezes product master description on the line item snapshot
     * - creates the TAX_INVOICE account entry with the exact grand total
     * - advances the document sequence exactly once
     * - rejects a retry finalization attempt
     * - does not re-snapshot after product master is later edited
     */
    @Test
    fun testFinalizationCalculatesInternally_freezesSnapshots_createsAccountEntry() = runTest {
        fakeClientDao.clients["client-1"] = ClientEntity(
            id          = "client-1",
            companyName = "Acme Corp",
            address     = "Ahmedabad",
            gstin       = "24AAAAC1234A1Z1",
            state       = "Gujarat",
            stateCode   = "24",
            createdAt   = 1000L,
            updatedAt   = 1000L
        )

        fakeProfileDao.profile = BusinessProfileEntity(
            id                  = "profile-1",
            businessName        = "VIVAAN ENTERPRISE",
            addressLine1        = "Street 4",
            addressLine2        = "Jorawar Nagar",
            cityStatePincode    = "Surendranagar, Gujarat",
            gstin               = "24CHWPG0910J1ZB",   // seller state code "24"
            mobile              = "9737178061",
            pan                 = "CHWPG0910J",
            bankAccountName     = "SHETH JANVI",
            bankName            = "HDFC BANK",
            bankAccountNumber   = "50100419622062",
            bankIfsc            = "HDFC0000299",
            bankBranch          = "Paldi",
            declaration         = "Declaration text",
            authorisedSignatory = "For VIVAAN",
            createdAt           = 1000L,
            updatedAt           = 1000L
        )

        fakeProductDao.products["prod-1"] = ProductEntity(
            id                      = "prod-1",
            name                    = "Product Master Name",
            hsnSac                  = "3919",
            defaultGstRateBasisPoints = 1800,
            isActive                = true,
            createdAt               = 1000L,
            updatedAt               = 1000L
        )

        val lineItem = DocumentLineItem(
            id                  = "line-1",
            documentId          = "",
            productId           = "prod-1",
            position            = 0,
            descriptionSnapshot = "Initial Draft Desc",
            hsnSacSnapshot      = "3919",
            quantity            = 10L,
            ratePaise           = 15000L,   // ₹150.00
            gstRateBasisPoints  = 1800,      // 18%
            createdAt           = 0L,
            updatedAt           = 0L
        )

        // placeOfSupply "24" == seller "24" → INTRA-STATE → CGST 9% + SGST 9%
        // taxable = 10 × 15000 = 150_000 paise (₹1,500)
        // CGST  9% = 150_000 × 900  / 10_000 = 13_500 paise (₹135)
        // SGST  9% = 150_000 × 900  / 10_000 = 13_500 paise (₹135)
        // grandTotal = 150_000 + 13_500 + 13_500 = 177_000 paise (₹1,770)
        val draft = repository.createDraft(
            type           = DocumentType.TAX_INVOICE,
            clientId       = "client-1",
            documentDate   = 1700000000000L,
            lineItems      = listOf(lineItem),
            placeOfSupply  = "24"
        ).getOrThrow()

        // Draft creation does not consume sequence
        assertEquals(0, fakeSeqDao.sequences.size)

        val finalizeResult = repository.finalizeDocument(draft.id)
        assertTrue(finalizeResult is DocumentFinalizationResult.Success)
        val finalizedDoc = (finalizeResult as DocumentFinalizationResult.Success).document

        assertEquals(DocumentStatus.FINALIZED, finalizedDoc.status)
        assertEquals(com.vivaanenterprise.app.domain.model.TaxTreatment.INTRA_STATE, finalizedDoc.taxTreatment)
        assertEquals(177_000L, finalizedDoc.grandTotalPaise)

        // Product master description was frozen onto the line item
        val finalizedLine = finalizedDoc.lineItems.first()
        assertEquals("Product Master Name", finalizedLine.descriptionSnapshot)
        assertEquals(177_000L, finalizedLine.lineTotalPaise)

        // Account entry was created with exact calculator grand total
        assertEquals(1, fakeAccountDao.entries.size)
        assertEquals(177_000L, fakeAccountDao.entries.values.first().amountPaise)

        // Sequence advanced exactly once
        assertEquals(1, fakeSeqDao.sequences.size)
        assertEquals(1, fakeSeqDao.sequences.values.first().lastSequenceNumber)

        // Retry finalization returns Invalid (document is no longer DRAFT)
        val retryResult = repository.finalizeDocument(draft.id)
        assertTrue(retryResult is DocumentFinalizationResult.Invalid)
        assertEquals(1, fakeSeqDao.sequences.values.first().lastSequenceNumber)

        // Later product master edit does NOT mutate the frozen line item snapshot
        fakeProductDao.products["prod-1"] = fakeProductDao.products["prod-1"]!!.copy(name = "Edited Master Name")
        val fetchedDoc = repository.getDocumentById(draft.id)
        assertEquals("Product Master Name", fetchedDoc?.lineItems?.first()?.descriptionSnapshot)
    }

    @Test
    fun testZeroGstTaxTreatmentFinalization() = runTest {
        fakeClientDao.clients["client-inter"] = ClientEntity(
            id          = "client-inter",
            companyName = "Interstate Client",
            stateCode   = "27",
            createdAt   = 1000L,
            updatedAt   = 1000L
        )

        fakeClientDao.clients["client-intra"] = ClientEntity(
            id          = "client-intra",
            companyName = "Intrastate Client",
            stateCode   = "24",
            createdAt   = 1000L,
            updatedAt   = 1000L
        )

        fakeProfileDao.profile = BusinessProfileEntity(
            id                  = "profile-1",
            businessName        = "VIVAAN ENTERPRISE",
            addressLine1        = "Street 4",
            addressLine2        = "Jorawar Nagar",
            cityStatePincode    = "Surendranagar",
            gstin               = "24CHWPG0910J1ZB",
            mobile              = "9737178061",
            pan                 = "CHWPG0910J",
            bankAccountName     = "SHETH JANVI",
            bankName            = "HDFC BANK",
            bankAccountNumber   = "50100419622062",
            bankIfsc            = "HDFC0000299",
            bankBranch          = "Paldi",
            declaration         = "Declaration text",
            authorisedSignatory = "For VIVAAN",
            createdAt           = 1000L,
            updatedAt           = 1000L
        )

        val zeroGstLine = DocumentLineItem(
            id                  = "line-0",
            documentId          = "",
            productId           = null,
            position            = 0,
            descriptionSnapshot = "Zero GST Product",
            hsnSacSnapshot      = "9999",
            quantity            = 1L,
            ratePaise           = 10000L,
            gstRateBasisPoints  = 0, // 0% GST
            createdAt           = 0L,
            updatedAt           = 0L
        )

        // 1. Zero GST Interstate Finalization
        val interDraft = repository.createDraft(
            type           = DocumentType.TAX_INVOICE,
            clientId       = "client-inter",
            documentDate   = 1700000000000L,
            lineItems      = listOf(zeroGstLine),
            placeOfSupply  = "27"
        ).getOrThrow()

        val interFinal = (repository.finalizeDocument(interDraft.id) as DocumentFinalizationResult.Success).document
        assertEquals(com.vivaanenterprise.app.domain.model.TaxTreatment.INTER_STATE, interFinal.taxTreatment)
        assertEquals(0L, interFinal.igstAmountPaise)
        assertEquals(0L, interFinal.cgstAmountPaise)
        assertEquals(0L, interFinal.sgstAmountPaise)

        // 2. Zero GST Intrastate Finalization
        val intraDraft = repository.createDraft(
            type           = DocumentType.TAX_INVOICE,
            clientId       = "client-intra",
            documentDate   = 1700000000000L,
            lineItems      = listOf(zeroGstLine),
            placeOfSupply  = "24"
        ).getOrThrow()

        val intraFinal = (repository.finalizeDocument(intraDraft.id) as DocumentFinalizationResult.Success).document
        assertEquals(com.vivaanenterprise.app.domain.model.TaxTreatment.INTRA_STATE, intraFinal.taxTreatment)
        assertEquals(0L, intraFinal.igstAmountPaise)
        assertEquals(0L, intraFinal.cgstAmountPaise)
        assertEquals(0L, intraFinal.sgstAmountPaise)
    }

    /**
     * Verifies that finalizing a PURCHASE_ORDER does NOT create a client account entry.
     * Seller "24", placeOfSupply "27" → INTER-STATE → IGST.
     * qty=5, rate=₹10, GST 18% → taxable=5_000, IGST=900, grandTotal=5_900
     */
    @Test
    fun testPurchaseOrderFinalizationDoesNotCreateAccountEntry() = runTest {
        fakeClientDao.clients["client-1"] = ClientEntity(
            id          = "client-1",
            companyName = "Acme Corp",
            stateCode   = "27",
            createdAt   = 1000L,
            updatedAt   = 1000L
        )

        fakeProfileDao.profile = BusinessProfileEntity(
            id                  = "profile-1",
            businessName        = "VIVAAN ENTERPRISE",
            addressLine1        = "Street 4",
            addressLine2        = "Jorawar Nagar",
            cityStatePincode    = "Surendranagar",
            gstin               = "24CHWPG0910J1ZB",
            mobile              = "9737178061",
            pan                 = "CHWPG0910J",
            bankAccountName     = "SHETH JANVI",
            bankName            = "HDFC BANK",
            bankAccountNumber   = "50100419622062",
            bankIfsc            = "HDFC0000299",
            bankBranch          = "Paldi",
            declaration         = "Declaration text",
            authorisedSignatory = "For VIVAAN",
            createdAt           = 1000L,
            updatedAt           = 1000L
        )

        val lineItem = DocumentLineItem(
            id                  = "line-1",
            documentId          = "",
            productId           = null,
            position            = 0,
            descriptionSnapshot = "PO Item",
            quantity            = 5L,
            ratePaise           = 1000L,
            gstRateBasisPoints  = 1800,
            createdAt           = 0L,
            updatedAt           = 0L
        )

        val draft = repository.createDraft(
            type          = DocumentType.PURCHASE_ORDER,
            clientId      = "client-1",
            documentDate  = 1700000000000L,
            lineItems     = listOf(lineItem),
            placeOfSupply = "27"   // inter-state
        ).getOrThrow()

        val finalizeResult = repository.finalizeDocument(draft.id)
        assertTrue(finalizeResult is DocumentFinalizationResult.Success)

        // No account entry for Purchase Order
        assertEquals(0, fakeAccountDao.entries.size)
    }

    /**
     * Verifies that cancelling a finalized TAX_INVOICE is rejected with a specific message.
     */
    @Test
    fun testFinalizedTaxInvoiceCancellationIsUnsupported() = runTest {
        fakeClientDao.clients["client-1"] = ClientEntity(
            id          = "client-1",
            companyName = "Acme Corp",
            stateCode   = "27",
            createdAt   = 1000L,
            updatedAt   = 1000L
        )

        fakeProfileDao.profile = BusinessProfileEntity(
            id                  = "profile-1",
            businessName        = "VIVAAN ENTERPRISE",
            addressLine1        = "Street 4",
            addressLine2        = "Jorawar Nagar",
            cityStatePincode    = "Surendranagar",
            gstin               = "24CHWPG0910J1ZB",
            mobile              = "9737178061",
            pan                 = "CHWPG0910J",
            bankAccountName     = "SHETH JANVI",
            bankName            = "HDFC BANK",
            bankAccountNumber   = "50100419622062",
            bankIfsc            = "HDFC0000299",
            bankBranch          = "Paldi",
            declaration         = "Declaration text",
            authorisedSignatory = "For VIVAAN",
            createdAt           = 1000L,
            updatedAt           = 1000L
        )

        val lineItem = DocumentLineItem(
            id                  = "line-1",
            documentId          = "",
            productId           = null,
            position            = 0,
            descriptionSnapshot = "Item",
            quantity            = 1L,
            ratePaise           = 1000L,
            gstRateBasisPoints  = 1800,
            createdAt           = 0L,
            updatedAt           = 0L
        )

        val draft = repository.createDraft(
            type          = DocumentType.TAX_INVOICE,
            clientId      = "client-1",
            documentDate  = 1700000000000L,
            lineItems     = listOf(lineItem),
            placeOfSupply = "27"   // inter-state
        ).getOrThrow()

        repository.finalizeDocument(draft.id)

        val cancelResult = repository.cancelDocument(draft.id)
        assertTrue(cancelResult.isFailure)
        assertEquals(
            "Finalized Tax Invoice cancellation is unsupported until accounting reversal semantics are implemented",
            cancelResult.exceptionOrNull()?.message
        )
    }

    /**
     * Verifies that finalizing without a place-of-supply returns MissingPlaceOfSupply.
     */
    @Test
    fun testFinalizationWithoutPlaceOfSupplyReturnsValidationError() = runTest {
        fakeClientDao.clients["client-1"] = ClientEntity(
            id = "client-1", companyName = "Test Co", createdAt = 1000L, updatedAt = 1000L
        )
        fakeProfileDao.profile = BusinessProfileEntity(
            id = "p", businessName = "VE", addressLine1 = "", addressLine2 = "",
            cityStatePincode = "", gstin = "24CHWPG0910J1ZB", mobile = "", pan = "",
            bankAccountName = "", bankName = "", bankAccountNumber = "", bankIfsc = "",
            bankBranch = "", declaration = "", authorisedSignatory = "",
            createdAt = 1000L, updatedAt = 1000L
        )
        val lineItem = DocumentLineItem(
            id = "l1", documentId = "", position = 0, descriptionSnapshot = "X",
            quantity = 1L, ratePaise = 1000L, gstRateBasisPoints = 1800,
            createdAt = 0L, updatedAt = 0L
        )
        // No placeOfSupply set
        val draft = repository.createDraft(
            type = DocumentType.TAX_INVOICE, clientId = "client-1",
            documentDate = 1700000000000L, lineItems = listOf(lineItem)
        ).getOrThrow()

        val result = repository.finalizeDocument(draft.id)
        assertTrue(result is DocumentFinalizationResult.Invalid)
        val errors = (result as DocumentFinalizationResult.Invalid).errors
        assertTrue(errors.contains(DocumentValidationError.MissingPlaceOfSupply))
    }
}

private class FakeVivaanEnterpriseDatabase(
    private val docDao: BusinessDocumentDao,
    private val lineDao: DocumentLineItemDao,
    private val clientDao: ClientDao,
    private val profileDao: BusinessProfileDao,
    private val seqDao: DocumentSequenceDao,
    private val accountDao: ClientAccountEntryDao,
    private val productDao: ProductDao
) : VivaanEnterpriseDatabase() {
    override fun businessDocumentDao(): BusinessDocumentDao = docDao
    override fun documentLineItemDao(): DocumentLineItemDao = lineDao
    override fun clientDao(): ClientDao = clientDao
    override fun productDao(): ProductDao = productDao
    override fun businessProfileDao(): BusinessProfileDao = profileDao
    override fun documentSequenceDao(): DocumentSequenceDao = seqDao
    override fun clientAccountEntryDao(): ClientAccountEntryDao = accountDao

    override fun createInvalidationTracker(): androidx.room.InvalidationTracker {
        return androidx.room.InvalidationTracker(this, "business_documents")
    }

    override fun clearAllTables() {}
}

private class FakeBusinessDocumentDao : BusinessDocumentDao {
    val documents = mutableMapOf<String, BusinessDocumentEntity>()

    override suspend fun upsert(document: BusinessDocumentEntity) {
        documents[document.id] = document
    }

    override suspend fun getById(id: String): BusinessDocumentEntity? {
        return documents[id]?.takeIf { !it.isDeleted }
    }

    override fun observeById(id: String): Flow<BusinessDocumentEntity?> {
        return flowOf(runBlocking { getById(id) })
    }

    override fun observeAllDocuments(): Flow<List<BusinessDocumentEntity>> {
        return flowOf(documents.values.filter { !it.isDeleted })
    }

    override fun observeDocumentsByType(type: DocumentType): Flow<List<BusinessDocumentEntity>> {
        return flowOf(documents.values.filter { it.documentType == type && !it.isDeleted })
    }

    override fun observeDocumentsByClient(clientId: String): Flow<List<BusinessDocumentEntity>> {
        return flowOf(documents.values.filter { it.clientId == clientId && !it.isDeleted })
    }

    override suspend fun getBySyncStatus(status: SyncStatus): List<BusinessDocumentEntity> {
        return documents.values.filter { it.syncStatus == status }
    }

    override suspend fun findByDocumentTypeAndNumber(
        documentType: DocumentType,
        documentNumber: String
    ): BusinessDocumentEntity? {
        return documents.values.find { it.documentType == documentType && it.documentNumber == documentNumber && !it.isDeleted }
    }

    override suspend fun softDelete(
        id: String,
        deletedAt: Long,
        updatedAt: Long,
        syncStatus: SyncStatus
    ) {
        documents[id]?.let {
            documents[id] = it.copy(isDeleted = true, deletedAt = deletedAt, updatedAt = updatedAt, syncStatus = syncStatus)
        }
    }
}

private class FakeDocumentLineItemDao : DocumentLineItemDao {
    val items = mutableMapOf<String, DocumentLineItemEntity>()

    override suspend fun upsertAll(itemsList: List<DocumentLineItemEntity>) {
        itemsList.forEach { items[it.id] = it }
    }

    override suspend fun upsert(item: DocumentLineItemEntity) {
        items[item.id] = item
    }

    override fun observeByDocumentId(documentId: String): Flow<List<DocumentLineItemEntity>> {
        return flowOf(runBlocking { getByDocumentId(documentId) })
    }

    override suspend fun getByDocumentId(documentId: String): List<DocumentLineItemEntity> {
        return items.values.filter { it.documentId == documentId }.sortedBy { it.position }
    }

    override suspend fun deleteByDocumentId(documentId: String) {
        items.entries.removeIf { it.value.documentId == documentId }
    }
}

private class FakeClientDao : ClientDao {
    val clients = mutableMapOf<String, ClientEntity>()

    override suspend fun upsert(client: ClientEntity) { clients[client.id] = client }
    override suspend fun getById(id: String): ClientEntity? = clients[id]?.takeIf { !it.isDeleted }
    override fun observeById(id: String): Flow<ClientEntity?> = flowOf(runBlocking { getById(id) })
    override fun observeActiveClients(): Flow<List<ClientEntity>> = flowOf(clients.values.filter { !it.isDeleted })
    override suspend fun getBySyncStatus(status: SyncStatus): List<ClientEntity> = emptyList()
    override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {}
}

private class FakeProductDao : ProductDao {
    val products = mutableMapOf<String, ProductEntity>()

    override suspend fun upsert(product: ProductEntity) { products[product.id] = product }
    override suspend fun getById(id: String): ProductEntity? = products[id]?.takeIf { !it.isDeleted && it.isActive }
    override suspend fun getByIdIncludingDeleted(id: String): ProductEntity? = products[id]
    override fun observeById(id: String): Flow<ProductEntity?> = flowOf(runBlocking { getById(id) })
    override fun observeAllProducts(): Flow<List<ProductEntity>> = flowOf(products.values.filter { !it.isDeleted })
    override fun observeActiveProducts(): Flow<List<ProductEntity>> = flowOf(products.values.filter { !it.isDeleted && it.isActive })
    override suspend fun getBySyncStatus(status: SyncStatus): List<ProductEntity> = emptyList()
    override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncStatus: SyncStatus) {}
}

private class FakeDocumentSequenceDao : DocumentSequenceDao {
    val sequences = mutableMapOf<String, DocumentSequenceEntity>()

    override suspend fun upsert(sequence: DocumentSequenceEntity) {
        val key = "${sequence.documentType}_${sequence.financialYear}"
        sequences[key] = sequence
    }

    override suspend fun getSequence(
        documentType: DocumentType,
        financialYear: String
    ): DocumentSequenceEntity? {
        val key = "${documentType}_${financialYear}"
        return sequences[key]
    }
}

private class FakeClientAccountEntryDao : ClientAccountEntryDao {
    val entries = mutableMapOf<String, ClientAccountEntryEntity>()

    override suspend fun upsert(entry: ClientAccountEntryEntity) {
        entries[entry.id] = entry
    }

    override fun observeByClientId(clientId: String): Flow<List<ClientAccountEntryEntity>> {
        return flowOf(entries.values.filter { it.clientId == clientId && !it.isDeleted })
    }

    override suspend fun getBySyncStatus(status: SyncStatus): List<ClientAccountEntryEntity> {
        return entries.values.filter { it.syncStatus == status }
    }

    override suspend fun findByDocumentId(documentId: String): ClientAccountEntryEntity? {
        return entries.values.find { it.documentId == documentId && !it.isDeleted }
    }

    override suspend fun softDelete(
        id: String,
        deletedAt: Long,
        updatedAt: Long,
        syncStatus: SyncStatus
    ) {
        entries[id]?.let {
            entries[id] = it.copy(isDeleted = true, deletedAt = deletedAt, updatedAt = updatedAt, syncStatus = syncStatus)
        }
    }
}
