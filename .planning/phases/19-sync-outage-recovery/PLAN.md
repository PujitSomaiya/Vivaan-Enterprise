# Phase 19: Firestore Sync Outage Recovery & UI Hardening - Execution Plan

## Phase Overview
- **Goal**: Restore multi-device sync functionality following Firestore security rule expiration, ensure automatic self-healing of `FAILED` local records, add periodic background sync, and provide explicit manual sync triggers with clear diagnostics in the UI.
- **Estimated Complexity**: Medium
- **Prerequisites**: Codebase mapping complete (`.planning/codebase/`). Active branch `bug/sync-ui-issue-17-nov-2026`.

---

## Tasks

### Task 1: DAO Support for Unsynced Entities & Status Counters
- **Objective**: Extend Room DAOs to retrieve all entities requiring sync (`PENDING` or `FAILED`) and expose reactive counts for UI monitoring.
- **Files Modified**:
  - `app/src/main/java/com/vivaanenterprise/app/core/database/dao/ClientDao.kt`
  - `app/src/main/java/com/vivaanenterprise/app/core/database/dao/ProductDao.kt`
  - `app/src/main/java/com/vivaanenterprise/app/core/database/dao/BusinessDocumentDao.kt`
  - `app/src/main/java/com/vivaanenterprise/app/core/database/dao/ClientAccountEntryDao.kt`
- **Changes**:
  - Add `getBySyncStatuses(statuses: List<SyncStatus>): List<Entity>` or `getUnsynced(): List<Entity>` where `syncStatus IN ('PENDING', 'FAILED')`.
  - Add reactive count queries `observeUnsyncedCount(): Flow<Int>` and `observeFailedCount(): Flow<Int>`.
- **Verification**: Run `./gradlew compileDebugKotlin` and verify Room database schema compilation.

---

### Task 2: Sync Engine Auto-Recovery & Error Persistence in `SyncRepositoryImpl`
- **Objective**: Ensure `SyncRepositoryImpl` pushes both `PENDING` and `FAILED` entities, records sync attempts/errors in DataStore, and exposes an observable sync status summary.
- **Files Modified**:
  - `app/src/main/java/com/vivaanenterprise/app/core/datastore/SyncPreferencesDataStore.kt`
  - `app/src/main/java/com/vivaanenterprise/app/domain/repository/SyncRepository.kt`
  - `app/src/main/java/com/vivaanenterprise/app/data/repository/SyncRepositoryImpl.kt`
- **Changes**:
  - Update `SyncPreferencesDataStore` with keys for `lastSyncStatus`, `lastSyncError`, and `lastSyncAttemptTimestamp`.
  - In `SyncRepositoryImpl.synchronize()`, query unsynced entities (`PENDING` + `FAILED`) for:
    - BusinessProfile
    - Clients
    - Products
    - Business Documents & Line Items
    - Client Account Entries
  - On push success: transition to `SyncStatus.SYNCED`, record `lastSyncedAt`, clear `syncError`.
  - On push failure: update `syncStatus = SyncStatus.FAILED`, store trimmed error message, record sync failure in DataStore.
  - Expose `observeSyncStatus(): Flow<SyncStatusSummary>` combining DataStore timestamps/errors with DAO unsynced/failed counts.
- **Verification**: Write unit test in `app/src/test/.../SyncRepositoryRecoveryTest.kt` simulating permission failure followed by recovery.

---

### Task 3: Background Scheduling with Periodic Sync & On-Demand Execution
- **Objective**: Add `PeriodicWorkRequest` for automatic background sync and ensure immediate manual sync uses replacement policy.
- **Files Modified**:
  - `app/src/main/java/com/vivaanenterprise/app/core/sync/SyncScheduler.kt`
  - `app/src/main/java/com/vivaanenterprise/app/app/VivaanEnterpriseApp.kt` (or `MainViewModel.kt`)
- **Changes**:
  - In `WorkManagerSyncScheduler`:
    - Add `schedulePeriodicSync()` (15-minute interval, connected network constraint, `ExistingPeriodicWorkPolicy.KEEP`).
    - Update `enqueueSync(forceImmediate: Boolean = false)` to use `ExistingWorkPolicy.REPLACE` when forced or manual.
  - In `MainViewModel`:
    - Invoke `syncScheduler.schedulePeriodicSync()` on startup when `AuthState.SignedIn`.
- **Verification**: Verify WorkManager request construction and tests pass.

---

### Task 4: Dashboard Sync Indicator, Manual "Sync Now" Button & Diagnostics Dialog
- **Objective**: Give operators real-time visibility into sync status on the Dashboard and TopAppBar, allowing manual sync trigger and detailed error inspection.
- **Files Modified**:
  - `app/src/main/java/com/vivaanenterprise/app/feature/dashboard/presentation/DashboardUiState.kt`
  - `app/src/main/java/com/vivaanenterprise/app/feature/dashboard/presentation/DashboardViewModel.kt`
  - `app/src/main/java/com/vivaanenterprise/app/feature/dashboard/presentation/DashboardScreen.kt`
  - `app/src/main/res/values/strings.xml`
- **Changes**:
  - In `DashboardUiState`, include `syncSummary: SyncStatusSummary` and `isManualSyncing: Boolean`.
  - In `DashboardViewModel`, observe `syncRepository.observeSyncStatus()` and add `onSyncNowClick()`.
  - In `DashboardScreen`:
    - Add a sync action button in `TopAppBar` with animated rotation or badge when syncing/failed.
    - Add an unobtrusive Sync Status card / banner above Quick Actions showing last sync time, pending/failed count, and a "Sync Now" button.
    - Add a dialog/sheet to inspect sync diagnostics when tapping the sync banner (showing broken down counts and last error text).
- **Verification**: Run `./gradlew compileDebugKotlin` and verify Compose preview/rendering.

---

### Task 5: Comprehensive Verification & Regression Testing
- **Objective**: Verify end-to-end correctness of the fix, ensure all existing unit tests and lint checks pass with exit code 0.
- **Commands**:
  ```bash
  ./gradlew testDebugUnitTest
  ./gradlew lintDebug
  ./gradlew assembleDebug
  ```
- **Acceptance Criteria**:
  - [ ] All entities previously stuck in `SyncStatus.FAILED` are automatically included in sync pushes.
  - [ ] Manual "Sync Now" immediately triggers synchronization.
  - [ ] Dashboard displays live sync state, timestamps, and error diagnostics.
  - [ ] WorkManager periodic sync is enqueued on app startup.
  - [ ] All unit tests pass cleanly.
