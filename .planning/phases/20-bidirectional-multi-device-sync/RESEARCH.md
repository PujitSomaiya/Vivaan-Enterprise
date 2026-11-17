# Phase 20: Bidirectional Multi-Device Sync Engine - Research

## Context & Incident Diagnosis
The user reported that after fixing Firestore security rules, outbound sync (push) works on the device that created the invoices, but other devices using the application do not receive the uploaded invoices or documents ("inbound and outbound both should work like all devices have to be the same data; 1 device created invoice that should reflect in other as well").

---

## Detailed Code Investigation

### 1. Clock Skew & Brittle Timestamp Filtering
In `FirestoreSyncDataSource.kt`:
```kotlin
open suspend fun pullBusinessDocumentsSince(sinceTimestamp: Long): List<BusinessDocumentDto> {
    return requireFirestore().collection(FirestoreCollections.DOCUMENTS)
        .whereGreaterThanOrEqualTo("updatedAt", sinceTimestamp)
        .get()
        .await()
        .toObjects(BusinessDocumentDto::class.java)
}
```
And in `SyncRepositoryImpl.kt`:
```kotlin
val lastSyncTime = syncPreferencesDataStore.getLastSyncTimestamp()
// ...
val remoteDocuments = firestoreSyncDataSource.pullBusinessDocumentsSince(lastSyncTime)
// ...
syncPreferencesDataStore.recordSyncSuccess(syncStartTime)
```

**The Failure Chain Across Multiple Devices:**
1. Device B syncs or launches at `12:00:00`. `lastSyncTimestamp` in DataStore is set to `12:00:00`.
2. Device A (a second phone) creates an invoice. Device A's system clock is slightly behind (e.g. `11:59:00`), or the invoice was drafted/finalized while Device A was offline. The invoice `updatedAt` is recorded as `11:59:00`.
3. Device A syncs and uploads the invoice to Firestore (`updatedAt = 11:59:00`).
4. Device B syncs at `12:05:00`: Device B executes `whereGreaterThanOrEqualTo("updatedAt", 12:00:00)`.
5. Because `11:59:00 < 12:00:00`, Firestore **excludes** the document from the query result.
6. Device B receives 0 documents and calls `recordSyncSuccess(12:05:00)`.
7. `lastSyncTimestamp` advances to `12:05:00`. Device B will **never** receive Device A's invoice in future incremental queries.

---

### 2. Missing Outbound Push for Document Sequences
In `SyncRepositoryImpl.kt` lines 80–145 (Step 1: Push):
- The sync engine pushes Profile, Clients, Products, Business Documents & Line Items, and Client Account Entries.
- **`pushDocumentSequence` is never called!**
- Even though `pullDocumentSequencesSince` exists in Step 2, `DocumentSequenceEntity` records are never pushed from local Room to Firestore.
- As a consequence, each device generates independent sequences (e.g. both Device A and Device B generate `VE/01/2026-27`), leading to duplicate document numbering collisions across devices.

---

### 3. Foreign Key Dependency Ordering & Transaction Safety
In SQLite/Room:
- `document_line_items` has a foreign key to `business_documents(id)`.
- `client_account_entries` has foreign keys to `clients(id)` and `business_documents(id)`.
- If remote line items or account entries arrive while their parent document is delayed or rejected, SQLite throws `android.database.sqlite.SQLiteConstraintException: FOREIGN KEY constraint failed`.
- `SyncRepositoryImpl.kt` currently runs individual upserts without verifying foreign key prerequisites or wrapping parent-child ingestions safely.

---

## Architectural Remediation Plan

### 1. Robust Pull & Reconciliation Engine
- **Safety Lookback Window**: For routine incremental syncs, query `sinceTimestamp - LOOKBACK_BUFFER_MS` (e.g. 24-hour lookback buffer) so any delayed offline creations or clock skews are never missed.
- **Full Reconciliation on Demand**: When `forceReconcile = true` (such as user clicking "Sync Now") or when `lastSyncTimestamp <= 0L`, pull all active documents from Firestore.
- **Idempotent Merge**: `SyncMergePolicy` already enforces that incoming remote records only overwrite local records if `remote.updatedAt >= local.updatedAt` and local is not currently in un-synced edit state. Re-evaluating existing records is 100% idempotent and safe.

### 2. Complete Sequence Synchronization
- In `DocumentSequenceDao`, add `getAll()` to read all sequence records.
- In `SyncRepositoryImpl.synchronize()` Step 1: Push all local sequences to Firestore via `firestoreSyncDataSource.pushDocumentSequence()`.
- In Step 2 (Pull): When remote sequences are received, merge them using `maxOf(local.lastSequenceNumber, remoteDto.lastSequenceNumber)` to prevent numbering regression or collisions.

### 3. Foreign Key Safe Ingestion
- Ensure parent entities (`BusinessDocument`) are inserted before child entities (`DocumentLineItem`).
- If an account entry references a `documentId` that is not yet present locally, safely handle or defer until document is present.

### 4. Verification
- Multi-device unit tests simulating Device A pushing an invoice, line items, and sequence, followed by Device B pulling and verifying all records exist in Room and render in the UI.
