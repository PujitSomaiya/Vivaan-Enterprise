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
open class FirestoreSyncDataSource @Inject constructor(
    private val firestore: FirebaseFirestore?
) {
    constructor() : this(null)

    private fun requireFirestore(): FirebaseFirestore =
        firestore ?: throw IllegalStateException("Firestore not initialized")

    // Push methods
    open suspend fun pushBusinessProfile(dto: BusinessProfileDto) {
        requireFirestore().collection(FirestoreCollections.BUSINESS_PROFILES)
            .document(dto.id)
            .set(dto, SetOptions.merge())
            .await()
    }

    open suspend fun pushClient(dto: ClientDto) {
        requireFirestore().collection(FirestoreCollections.CLIENTS)
            .document(dto.id)
            .set(dto, SetOptions.merge())
            .await()
    }

    open suspend fun pushProduct(dto: ProductDto) {
        requireFirestore().collection(FirestoreCollections.PRODUCTS)
            .document(dto.id)
            .set(dto, SetOptions.merge())
            .await()
    }

    open suspend fun pushBusinessDocument(dto: BusinessDocumentDto) {
        requireFirestore().collection(FirestoreCollections.DOCUMENTS)
            .document(dto.id)
            .set(dto, SetOptions.merge())
            .await()
    }

    open suspend fun pushDocumentLineItem(dto: DocumentLineItemDto) {
        requireFirestore().collection(FirestoreCollections.DOCUMENT_LINE_ITEMS)
            .document(dto.id)
            .set(dto, SetOptions.merge())
            .await()
    }

    open suspend fun pushClientAccountEntry(dto: ClientAccountEntryDto) {
        requireFirestore().collection(FirestoreCollections.CLIENT_ACCOUNT_ENTRIES)
            .document(dto.id)
            .set(dto, SetOptions.merge())
            .await()
    }

    open suspend fun pushDocumentSequence(dto: DocumentSequenceDto) {
        val compositeId = "${dto.documentType}_${dto.financialYear}"
        requireFirestore().collection(FirestoreCollections.DOCUMENT_SEQUENCES)
            .document(compositeId)
            .set(dto, SetOptions.merge())
            .await()
    }

    // Pull methods
    open suspend fun pullBusinessProfilesSince(sinceTimestamp: Long): List<BusinessProfileDto> {
        val query = if (sinceTimestamp > 0L) {
            requireFirestore().collection(FirestoreCollections.BUSINESS_PROFILES)
                .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
        } else {
            requireFirestore().collection(FirestoreCollections.BUSINESS_PROFILES)
        }
        return query.get().await().toObjects(BusinessProfileDto::class.java)
    }

    open suspend fun pullClientsSince(sinceTimestamp: Long): List<ClientDto> {
        val query = if (sinceTimestamp > 0L) {
            requireFirestore().collection(FirestoreCollections.CLIENTS)
                .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
        } else {
            requireFirestore().collection(FirestoreCollections.CLIENTS)
        }
        return query.get().await().toObjects(ClientDto::class.java)
    }

    open suspend fun pullProductsSince(sinceTimestamp: Long): List<ProductDto> {
        val query = if (sinceTimestamp > 0L) {
            requireFirestore().collection(FirestoreCollections.PRODUCTS)
                .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
        } else {
            requireFirestore().collection(FirestoreCollections.PRODUCTS)
        }
        return query.get().await().toObjects(ProductDto::class.java)
    }

    open suspend fun pullBusinessDocumentsSince(sinceTimestamp: Long): List<BusinessDocumentDto> {
        val query = if (sinceTimestamp > 0L) {
            requireFirestore().collection(FirestoreCollections.DOCUMENTS)
                .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
        } else {
            requireFirestore().collection(FirestoreCollections.DOCUMENTS)
        }
        return query.get().await().toObjects(BusinessDocumentDto::class.java)
    }

    open suspend fun pullDocumentLineItemsSince(sinceTimestamp: Long): List<DocumentLineItemDto> {
        val query = if (sinceTimestamp > 0L) {
            requireFirestore().collection(FirestoreCollections.DOCUMENT_LINE_ITEMS)
                .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
        } else {
            requireFirestore().collection(FirestoreCollections.DOCUMENT_LINE_ITEMS)
        }
        return query.get().await().toObjects(DocumentLineItemDto::class.java)
    }

    open suspend fun pullClientAccountEntriesSince(sinceTimestamp: Long): List<ClientAccountEntryDto> {
        val query = if (sinceTimestamp > 0L) {
            requireFirestore().collection(FirestoreCollections.CLIENT_ACCOUNT_ENTRIES)
                .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
        } else {
            requireFirestore().collection(FirestoreCollections.CLIENT_ACCOUNT_ENTRIES)
        }
        return query.get().await().toObjects(ClientAccountEntryDto::class.java)
    }

    open suspend fun pullDocumentSequencesSince(sinceTimestamp: Long): List<DocumentSequenceDto> {
        val query = if (sinceTimestamp > 0L) {
            requireFirestore().collection(FirestoreCollections.DOCUMENT_SEQUENCES)
                .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
        } else {
            requireFirestore().collection(FirestoreCollections.DOCUMENT_SEQUENCES)
        }
        return query.get().await().toObjects(DocumentSequenceDto::class.java)
    }
}
