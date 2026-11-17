# Phase 19: Firestore Sync Outage Recovery & UI Hardening - Research

## Context & Incident Diagnosis
During active usage on 2–3 production devices, Firestore security rules accidentally expired on the backend. This caused Firestore requests to reject with permission denied errors. Even after restoring the rules, the devices remain unsynchronized.

### Code Investigation Findings
1. **Permanent `FAILED` State Trap**:
   - In `SyncRepositoryImpl.kt` (lines 45–95), when Firestore operations threw exceptions during rule expiration, the catch blocks updated entities to `syncStatus = SyncStatus.FAILED`:
     ```kotlin
     db.clientDao().upsert(client.copy(syncStatus = SyncStatus.FAILED, syncError = e.message?.take(100)))
     ```
   - However, during push, `SyncRepositoryImpl` exclusively queries `SyncStatus.PENDING`:
     ```kotlin
     val pendingClients = db.clientDao().getBySyncStatus(SyncStatus.PENDING)
     val pendingProducts = db.productDao().getBySyncStatus(SyncStatus.PENDING)
     val pendingDocuments = db.businessDocumentDao().getBySyncStatus(SyncStatus.PENDING)
     val pendingAccountEntries = db.clientAccountEntryDao().getBySyncStatus(SyncStatus.PENDING)
     ```
   - Because all previously failed records are marked as `FAILED`, they are excluded from the push query forever.
2. **Conflict Resolution Lockout**:
   - `SyncMergePolicy.kt` evaluates `local.syncStatus == SyncStatus.PENDING || local.syncStatus == SyncStatus.FAILED` and returns `REJECT_REMOTE_PROTECT_LOCAL`. Since local failed entities are protected but never pushed, bidirectional sync is completely frozen.
3. **No Background Periodic Sync**:
   - `WorkManagerSyncScheduler.kt` only schedules a `OneTimeWorkRequestBuilder` on startup or upon repository write operations.
   - There is no `PeriodicWorkRequest` configured to periodically re-attempt sync in the background.
4. **Missing UI Triggers and Diagnostics**:
   - `DashboardScreen.kt` and TopAppBar lack a "Sync Now" action.
   - Users and operators have no visual feedback on whether a sync is pending, failed, or completed, nor what error occurred.

## Implementation Architecture

### 1. DAO & Sync Engine Fixes
- Add queries in `ClientDao`, `ProductDao`, `BusinessDocumentDao`, `ClientAccountEntryDao` to retrieve records needing synchronization:
  - `getBySyncStatuses(listOf(SyncStatus.PENDING, SyncStatus.FAILED))` or `getUnsynced()`
  - Provide reactive count flows: `observeUnsyncedCount()` and `observeFailedCount()`.
- In `SyncRepositoryImpl.synchronize()`:
  - Query all unsynced records (`PENDING` and `FAILED`) for push.
  - Reset failed items to `PENDING` during an active sync cycle or push them directly and transition to `SYNCED` upon success.
  - Record the latest sync attempt timestamp, result status (`SUCCESS` / `FAILED`), and last error message in `SyncPreferencesDataStore`.

### 2. WorkManager Scheduling Hardening
- Extend `SyncScheduler` to support `schedulePeriodicSync()` using `PeriodicWorkRequestBuilder<FirebaseSyncWorker>(15, TimeUnit.MINUTES)` with `NetworkType.CONNECTED` constraint and `ExistingPeriodicWorkPolicy.KEEP`.
- Ensure one-time manual sync triggers use `ExistingWorkPolicy.REPLACE` so users can force an immediate sync without waiting for a stalled backoff queue.
- Register `schedulePeriodicSync()` in `VivaanEnterpriseApp.kt` or `MainViewModel` upon user sign-in.

### 3. Sync State Observation & Dashboard UI
- In `SyncPreferencesDataStore`, persist:
  - `lastSyncTimestamp`: Long
  - `lastSyncAttemptTimestamp`: Long
  - `lastSyncStatus`: String (`SUCCESS`, `FAILED`, `IDLE`)
  - `lastSyncErrorMessage`: String?
- Expose an aggregate `SyncStatusSummary` via `SyncRepository.observeSyncStatus()`.
- Add a Sync Status & Manual Action component to the `DashboardScreen`:
  - Shows sync state: Synced (green checkmark / timestamp), Syncing (progress indicator), or Failed/Pending (warning badge with item count).
  - "Sync Now" / Retry icon button in TopAppBar or within a dashboard sync card.
  - Optional sync details dialog showing counts of pending/failed clients, products, invoices, and ledgers with the last error message.

### 4. Verification Plan
- Unit tests for `SyncRepositoryImpl`:
  - Simulate initial push failure (e.g. `SecurityException` / Firestore error) marking entities `FAILED`.
  - Simulate subsequent sync where `FAILED` entities are picked up and successfully pushed to Firestore.
- Unit tests for DAOs verifying `getUnsynced()` returns both `PENDING` and `FAILED` entities.
- Unit tests for `DashboardViewModel` ensuring sync state changes update UI state correctly.
- Verification commands synchronously executed (`./gradlew testDebugUnitTest`, etc.).
