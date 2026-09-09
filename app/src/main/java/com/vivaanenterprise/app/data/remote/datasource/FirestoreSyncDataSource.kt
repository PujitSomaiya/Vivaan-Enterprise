package com.vivaanenterprise.app.data.remote.datasource

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.vivaanenterprise.app.core.firebase.FirestoreCollections
import com.vivaanenterprise.app.data.remote.model.BusinessDocumentDto
import com.vivaanenterprise.app.data.remote.model.BusinessProfileDto
import com.vivaanenterprise.app.data.remote.model.ClientAccountEntryDto
import com.vivaanenterprise.app.data.remote.model.ClientDto
import com.vivaanenterprise.app.data.remote.model.DocumentLineItemDto
import com.vivaanenterprise.app.data.remote.model.DocumentSequenceDto
import com.vivaanenterprise.app.data.remote.model.ProductDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // Push methods
    suspend fun pushBusinessProfile(dto: BusinessProfileDto) {
        firestore.collection(FirestoreCollections.BUSINESS_PROFILES)
            .document(dto.id)
            .set(dto, SetOptions.merge())
            .await()
    }

    suspend fun pushClient(dto: ClientDto) {
        firestore.collection(FirestoreCollections.CLIENTS)
            .document(dto.id)
            .set(dto, SetOptions.merge())
            .await()
    }

    suspend fun pushProduct(dto: ProductDto) {
        firestore.collection(FirestoreCollections.PRODUCTS)
            .document(dto.id)
            .set(dto, SetOptions.merge())
            .await()
    }

    suspend fun pushBusinessDocument(dto: BusinessDocumentDto) {
        firestore.collection(FirestoreCollections.DOCUMENTS)
            .document(dto.id)
            .set(dto, SetOptions.merge())
            .await()
    }

    suspend fun pushDocumentLineItem(dto: DocumentLineItemDto) {
        firestore.collection(FirestoreCollections.DOCUMENT_LINE_ITEMS)
            .document(dto.id)
            .set(dto, SetOptions.merge())
            .await()
    }

    suspend fun pushClientAccountEntry(dto: ClientAccountEntryDto) {
        firestore.collection(FirestoreCollections.CLIENT_ACCOUNT_ENTRIES)
            .document(dto.id)
            .set(dto, SetOptions.merge())
            .await()
    }

    suspend fun pushDocumentSequence(dto: DocumentSequenceDto) {
        val compositeId = "${dto.documentType}_${dto.financialYear}"
        firestore.collection(FirestoreCollections.DOCUMENT_SEQUENCES)
            .document(compositeId)
            .set(dto, SetOptions.merge())
            .await()
    }

    // Pull methods
    suspend fun pullBusinessProfilesSince(sinceTimestamp: Long): List<BusinessProfileDto> {
        return firestore.collection(FirestoreCollections.BUSINESS_PROFILES)
            .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
            .get()
            .await()
            .toObjects(BusinessProfileDto::class.java)
    }

    suspend fun pullClientsSince(sinceTimestamp: Long): List<ClientDto> {
        return firestore.collection(FirestoreCollections.CLIENTS)
            .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
            .get()
            .await()
            .toObjects(ClientDto::class.java)
    }

    suspend fun pullProductsSince(sinceTimestamp: Long): List<ProductDto> {
        return firestore.collection(FirestoreCollections.PRODUCTS)
            .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
            .get()
            .await()
            .toObjects(ProductDto::class.java)
    }

    suspend fun pullBusinessDocumentsSince(sinceTimestamp: Long): List<BusinessDocumentDto> {
        return firestore.collection(FirestoreCollections.DOCUMENTS)
            .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
            .get()
            .await()
            .toObjects(BusinessDocumentDto::class.java)
    }

    suspend fun pullDocumentLineItemsSince(sinceTimestamp: Long): List<DocumentLineItemDto> {
        return firestore.collection(FirestoreCollections.DOCUMENT_LINE_ITEMS)
            .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
            .get()
            .await()
            .toObjects(DocumentLineItemDto::class.java)
    }

    suspend fun pullClientAccountEntriesSince(sinceTimestamp: Long): List<ClientAccountEntryDto> {
        return firestore.collection(FirestoreCollections.CLIENT_ACCOUNT_ENTRIES)
            .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
            .get()
            .await()
            .toObjects(ClientAccountEntryDto::class.java)
    }

    suspend fun pullDocumentSequencesSince(sinceTimestamp: Long): List<DocumentSequenceDto> {
        return firestore.collection(FirestoreCollections.DOCUMENT_SEQUENCES)
            .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
            .get()
            .await()
            .toObjects(DocumentSequenceDto::class.java)
    }
}
