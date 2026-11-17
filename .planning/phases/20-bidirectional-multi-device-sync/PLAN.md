# Phase 20: Bidirectional Multi-Device Sync Engine - Execution Plan

## Phase Overview
- **Goal**: Guarantee reliable two-way data replication across multiple active devices so invoices, line items, clients, products, and sequences created on one device immediately reflect on other devices upon sync.
- **Estimated Complexity**: Medium
- **Prerequisites**: Phase 19 completed. Active branch `bug/sync-inbound-issue-17-nov-2026`.

---

## Tasks

### Task 1: Robust Remote Pull with Safety Lookback Window & Full Reconcile
- **Objective**: Prevent clock skew and offline delayed uploads from causing remote documents to be missed during pull queries.
- **Files Modified**:
  - `app/src/main/java/com/vivaanenterprise/app/data/remote/datasource/FirestoreSyncDataSource.kt`
  - `app/src/main/java/com/vivaanenterprise/app/data/repository/SyncRepositoryImpl.kt`
  - `app/src/main/java/com/vivaanenterprise/app/domain/repository/SyncRepository.kt`
- **Changes**:
  - In `FirestoreSyncDataSource`:
    - Add methods to pull all records without timestamp filter (for full reconciliation): `pullAllClients()`, `pullAllProducts()`, `pullAllBusinessDocuments()`, `pullAllDocumentLineItems()`, `pullAllClientAccountEntries()`, `pullAllDocumentSequences()`.
  - In `SyncRepository`:
    - Update `synchronize(forceReconcile: Boolean = false): Result<Unit>`.
  - In `SyncRepositoryImpl`:
    - For routine syncs, apply a safety lookback buffer (`LOOKBACK_WINDOW_MS = 24 * 60 * 60 * 1000L` / 24 hours): `val effectiveSince = maxOf(0L, lastSyncTime - LOOKBACK_WINDOW_MS)`.
    - If `forceReconcile == true` (e.g. manual "Sync Now") or `lastSyncTime <= 0L`, perform full collection pull so any missing historical records are guaranteed to be reconciled into Room.
- **Verification**: Run `./gradlew testDebugUnitTest` to verify repository compilation and tests.

---

### Task 2: Document Sequence Outbound Push & Sequence Merging
- **Objective**: Keep document numbering sequences (`VE/XX/YYYY-YY`) synchronized across all devices to prevent number duplication.
- **Files Modified**:
  - `app/src/main/java/com/vivaanenterprise/app/core/database/dao/DocumentSequenceDao.kt`
  - `app/src/main/java/com/vivaanenterprise/app/data/repository/SyncRepositoryImpl.kt`
- **Changes**:
  - In `DocumentSequenceDao`, add `getAll(): List<DocumentSequenceEntity>` and `getByTypeAndFy(type: DocumentType, fy: String): DocumentSequenceEntity?`.
  - In `SyncRepositoryImpl.synchronize()` Step 1 (Push):
    - Query all local document sequences and push them to Firestore via `firestoreSyncDataSource.pushDocumentSequence(seq.toDto())`.
  - In `SyncRepositoryImpl.synchronize()` Step 2 (Pull):
    - When remote sequences arrive, merge with local state:
      `val finalSeq = maxOf(localSeq?.lastSequenceNumber ?: 0, remoteDto.lastSequenceNumber)`
      and upsert the merged sequence to Room.
- **Verification**: Verify sequence DAO queries and test compilation.

---

### Task 3: Foreign Key Safe Parent-to-Child Ingestion in Room
- **Objective**: Prevent SQLite foreign key constraint violations when child items (line items, account entries) are pulled from Firestore.
- **Files Modified**:
  - `app/src/main/java/com/vivaanenterprise/app/data/repository/SyncRepositoryImpl.kt`
- **Changes**:
  - In `SyncRepositoryImpl.synchronize()` Step 2 (Pull):
    - Run pull processing in strict parent-to-child order:
      1. Business Profile
      2. Clients
      3. Products
      4. Business Documents
      5. Document Line Items (verify parent document exists in Room; if missing, query Firestore for the parent document or log cleanly)
      6. Client Account Entries (verify parent client and document exist in Room)
      7. Document Sequences
    - Wrap multi-table ingestion in Room transactions so data is never partially committed in an inconsistent state.
- **Verification**: Write unit test verifying foreign key relations remain intact during ingestion.

---

### Task 4: Multi-Device Bidirectional End-to-End Unit Testing
- **Objective**: Programmatically verify the exact scenario reported by the user across two simulated devices (Device A and Device B).
- **Files Created**:
  - `app/src/test/java/com/vivaanenterprise/app/data/repository/MultiDeviceBidirectionalSyncTest.kt`
- **Tests Implemented**:
  1. `device_A_creates_and_pushes_invoice_then_device_B_syncs_and_receives_invoice()`:
     - Device A creates client, finalized invoice, line items, and ledger entry.
     - Device A runs `synchronize()` -> uploads all records to fake Firestore.
     - Device B (which previously had synced or has different clock timestamp) runs `synchronize()`.
     - Assert that Device B's Room database contains the exact invoice, line items, and client account entry.
  2. `device_B_creates_product_then_device_A_syncs_and_receives_product()`:
     - Verify full bidirectional flow (changes made on Device B replicate back to Device A).
  3. `document_sequence_stays_synchronized_across_devices()`:
     - Verify sequence advances correctly without collisions across devices.
- **Verification**: Run `./gradlew testDebugUnitTest` and confirm all tests pass with exit code 0.

---

### Task 5: Build & Release Verification
- **Objective**: Ensure R8 minification, lint, and all release checks pass without regressions.
- **Commands**:
  ```bash
  ./gradlew testDebugUnitTest
  ./gradlew lintDebug
  ./gradlew assembleDebug
  ./gradlew assembleRelease
  ```
- **Acceptance Criteria**:
  - [ ] Device B receives invoices and line items created on Device A upon sync.
  - [ ] Manual "Sync Now" forces full reconciliation so no record is ever left behind.
  - [ ] Document sequences sync to Firestore, preventing invoice numbering conflicts.
  - [ ] All unit and release tests pass with exit code 0.
