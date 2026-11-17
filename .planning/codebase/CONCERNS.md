# Codebase Concerns & Architectural Risks

**Analysis Date:** 2026-11-17

This document catalogs technical debt, architectural risks, performance considerations, security posture, and critical edge cases identified across the Vivaan Enterprise codebase. Each item is grounded in direct repository evidence with exact file references and actionable remediation pathways.

---

## 1. Technical Debt

### Stray Duplicate Repository Implementation in Root Source Tree
- **Severity:** Medium
- **Files:**
  - `app/src/data/repository/ClientAccountRepositoryImpl.kt` (Stray file)
  - `app/src/main/java/com/vivaanenterprise/app/data/repository/ClientAccountRepositoryImpl.kt` (Authoritative file)
- **Impact:** An obsolete copy of `ClientAccountRepositoryImpl.kt` exists directly under `app/src/data/repository/` outside `app/src/main/java/`. It is tracked in Git. The stray version performs in-memory filtering of documents (`docs.filter { it.status == DocumentStatus.FINALIZED }`) rather than delegating aggregation to Room's `documentDao.observeDashboardSummary()`. It pollutes the directory tree, creates confusion for maintainers, and could be accidentally reintroduced into compilation paths.
- **Fix approach:** Remove `app/src/data/repository/ClientAccountRepositoryImpl.kt` from Git tracking and delete the stray `app/src/data` directory hierarchy.

### Finalized Document Cancellation Blocked Without Reversal Accounting
- **Severity:** Medium
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/data/repository/DocumentRepositoryImpl.kt` (Lines 506–509)
- **Impact:** In `DocumentRepositoryImpl.cancelDocument()`, cancelling a finalized Tax Invoice is hardcoded to fail:
  ```kotlin
  if (existing.status == DocumentStatus.FINALIZED && existing.documentType == DocumentType.TAX_INVOICE) {
      return Result.failure(IllegalStateException("Finalized Tax Invoice cancellation is unsupported until accounting reversal semantics are implemented"))
  }
  ```
  While intentional to preserve ledger consistency, users who finalize an invoice with an error cannot void or cancel it. Indian GST statutory compliance requires either Credit Notes (under Section 34 of the CGST Act) or reversing journal entries.
- **Fix approach:** Implement a formal Credit Note workflow or an accounting reversal transaction that debits/credits `client_account_entries` atomically before transitioning the invoice status to `CANCELLED`.

### Document Sequence Push Missing in Sync Engine
- **Severity:** High
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/data/repository/SyncRepositoryImpl.kt` (Lines 39–97, 176–180)
  - `app/src/main/java/com/vivaanenterprise/app/data/remote/datasource/FirestoreSyncDataSource.kt` (Line 65)
  - `app/src/main/java/com/vivaanenterprise/app/core/database/entity/DocumentSequenceEntity.kt`
- **Impact:** `FirestoreSyncDataSource` implements `pushDocumentSequence(dto: DocumentSequenceDto)`, but `SyncRepositoryImpl.synchronize()` never invokes it during the push step (Step 1). Only pull is executed during Step 2 (`pullDocumentSequencesSince`). Because local sequence increments are never pushed to Firestore, remote sequence state remains stale or empty, disabling collaborative sequence tracking across devices. Furthermore, `DocumentSequenceEntity` lacks `syncStatus` metadata.
- **Fix approach:** Add `syncStatus` and `lastSyncedAt` fields to `DocumentSequenceEntity`. Update `DocumentSequenceDao` and Step 1 of `SyncRepositoryImpl.synchronize()` to iterate over pending sequence numbers and call `firestoreSyncDataSource.pushDocumentSequence()`.

### In-Memory ViewModel Startup Flag Side-Effects
- **Severity:** Low
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/app/VivaanEnterpriseApp.kt` (Lines 30–55)
- **Impact:** `MainViewModel` uses an in-memory mutable variable `private var hasHandledStartup = false` inside `authRepository.authState.onEach { ... }` combined with `SharingStarted.WhileSubscribed(5_000)`. If the app is in the background long enough for subscription to stop (5 seconds) and the ViewModel is re-instantiated upon process recreation, or if transient auth fluctuations occur, the seeding and initial sync routines can be triggered repeatedly.
- **Fix approach:** Persist initial seeding completion flags into `DataStorePreferences` (e.g., `isSeedingCompleted: Boolean`) rather than relying on transient ViewModel instance state.

---

## 2. Known Issues & Edge Cases

### Missing Tombstones for Deleted Line Items on Draft Updates
- **Severity:** High
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/data/repository/DocumentRepositoryImpl.kt` (Line 212)
  - `app/src/main/java/com/vivaanenterprise/app/core/database/entity/DocumentLineItemEntity.kt`
  - `app/src/main/java/com/vivaanenterprise/app/data/repository/SyncRepositoryImpl.kt` (Lines 148–158)
- **Impact:** When a draft document is edited via `DocumentRepositoryImpl.updateDraft()`, old line items are purged from Room using `lineItemDao.deleteByDocumentId(document.id)`. However, `DocumentLineItemEntity` and `DocumentLineItemDto` do not carry soft-delete fields (`isDeleted`, `deletedAt`). If the original line items were already synced to Firestore, they remain in Firestore. When another device syncs or a fresh install pulls remote line items via `pullDocumentLineItemsSince()`, those purged line items are resurrected and upserted back into the draft in Room, corrupting line item positions and totals.
- **Fix approach:** Either add soft-delete tombstone semantics (`isDeleted: Boolean = false`, `deletedAt: Long? = null`) to `DocumentLineItemEntity` and `DocumentLineItemDto`, or sync line items as a nested array within `BusinessDocumentDto` rather than as a separate top-level collection.

### Sync Merge Policy Overwrites Finalized Document Line Items
- **Severity:** High
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/domain/repository/SyncMergePolicy.kt` (Lines 56–59)
  - `app/src/main/java/com/vivaanenterprise/app/data/repository/SyncRepositoryImpl.kt` (Lines 148–158)
- **Impact:** `SyncMergePolicy.evaluateLineItemMerge(parentDocument)` unconditionally returns `MergeResult.ACCEPT_REMOTE`. While `evaluateDocumentMerge` correctly rejects mutations to local finalized documents (`REJECT_FINALIZED_SNAPSHOT_MUTATION`), inbound remote line items are still upserted into `documentLineItemDao` even if the parent document in Room is `DocumentStatus.FINALIZED`. This violates Rule 13 of `AGENTS.md` regarding historical document immutability.
- **Fix approach:** Modify `evaluateLineItemMerge` to return `REJECT_FINALIZED_SNAPSHOT_MUTATION` if `parentDocument?.status == DocumentStatus.FINALIZED`.

### Unconditional Document Sequence Overwrite Rollback
- **Severity:** High
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/data/repository/SyncRepositoryImpl.kt` (Lines 177–180)
- **Impact:** When remote document sequences are pulled, `SyncRepositoryImpl` executes:
  ```kotlin
  for (remoteDto in remoteSequences) {
      db.documentSequenceDao().upsert(remoteDto.toEntity())
  }
  ```
  There is no comparison against local `lastSequenceNumber`. If the local device has advanced the sequence to `15` while offline, and remote returns `10`, the local sequence is overwritten backward to `10`. Subsequent documents created offline will attempt to use sequence numbers `11`, `12`, colliding with previously finalized documents.
- **Fix approach:** Resolve sequence conflicts deterministically using `maxOf(localSequence.lastSequenceNumber, remoteDto.lastSequenceNumber)` when upserting pulled sequences.

### Manual Bitmap.recycle() Induces RenderThread Race Crashes in PDF Viewer
- **Severity:** High
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/feature/pdfviewer/presentation/PdfViewerViewModel.kt` (Lines 178–184)
  - `app/src/main/java/com/vivaanenterprise/app/feature/pdfviewer/presentation/PdfViewerScreen.kt` (Lines 251–257)
- **Impact:** To enforce bounded memory retention (`MAX_RENDERED_PAGES = 3`), `PdfViewerViewModel` actively calls `updatedPages[evictedIndex]?.bitmap?.recycle()`. In Jetpack Compose, the evicted bitmap may still be bound to an active `ImageBitmap` composable during scroll or layout passes. If the Android graphics hardware pipeline (`RenderThread`) attempts to draw a canvas using a recycled bitmap, the application crashes immediately with `java.lang.RuntimeException: Canvas: trying to use a recycled bitmap`.
- **Fix approach:** Avoid calling `bitmap.recycle()` directly in managed Kotlin code. Nullify the reference in `_uiState` (`updatedPages[evictedIndex] = PageRenderState(...)`) and allow the Android ART Garbage Collector to safely reclaim the bitmap memory once detached from active RenderNodes.

### PDF Footer Boundary Overflow Under Maximum Content Height
- **Severity:** Medium
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/core/pdf/AndroidBusinessDocumentPdfGenerator.kt` (Lines 797–823)
  - `app/src/main/java/com/vivaanenterprise/app/core/pdf/PdfPageContext.kt` (Lines 27, 139–144)
- **Impact:** `drawFooter()` draws the computer-generated disclaimer banner directly at `ctx.currentY` without verifying available page space via `ctx.ensureSpace(bannerHeight)`. If the Bank Details & Declaration block finishes near the page boundary (e.g. `currentY = 805`), the footer draws between `805` and `821`, crossing past `CONTENT_BOTTOM = 812f` and clipping off the bottom margin of the page.
- **Fix approach:** Invoke `ctx.ensureSpace(bannerHeight)` prior to rendering the footer rectangle and text.

---

## 3. Security Considerations

### Android Manifest Hardening
- **Status:** Verified Secure
- **Files:**
  - `app/src/main/AndroidManifest.xml` (Lines 4–43)
- **Findings:**
  - `android:allowBackup="false"` is strictly maintained, preventing unauthorized extraction of the Room SQLite database, DataStore preferences, and cache via `adb backup`.
  - Component exposure is minimal: `MainActivity` is the only exported activity (`android:exported="true"`), protected with launcher intent filter constraints. `FileProvider` and WorkManager startup providers are explicitly configured with `android:exported="false"`.

### FileProvider Scope & Content URI Isolation
- **Status:** Verified Secure
- **Files:**
  - `app/src/main/AndroidManifest.xml` (Lines 23–31)
  - `app/src/main/res/xml/file_paths.xml` (Line 3)
  - `app/src/main/java/com/vivaanenterprise/app/feature/pdfviewer/service/PdfServices.kt`
- **Findings:**
  - Temporary PDF files are saved to `context.cacheDir/pdfs/` and shared via `androidx.core.content.FileProvider`.
  - In `file_paths.xml`, `<cache-path name="shared_pdfs" path="pdfs/" />` restricts access strictly to the dedicated subdirectory.
  - Share intents use `content://` URIs with `Intent.FLAG_GRANT_READ_URI_PERMISSION`. No raw `file://` URIs are exposed, fully mitigating `FileUriExposedException`.

### Credential & Secret Audit
- **Status:** Verified Secure
- **Findings:**
  - No plaintext credentials, private keys, API secrets, or passwords exist in source files or configuration assets.
  - Remote authentication and database access rely entirely on Firebase Auth SDK sessions and standard `google-services.json` client tokens.

### Unversioned Remote Firestore Security Rules
- **Severity:** High
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/core/firebase/FirestoreCollections.kt`
- **Risk:** There is no `firestore.rules` configuration committed to the repository. Remote Firestore rules are maintained out-of-band in the Firebase Console. Without automated rule validation in CI or repository versioning, updates could accidentally expose business documents, client GSTINs, and financial ledgers to unauthenticated or cross-tenant access.
- **Fix approach:** Commit a production-grade `firestore.rules` file to the repository root and integrate Firebase CLI rules deployment into continuous integration.

### Flat Root Collection Multi-Tenant Collision Risk
- **Severity:** Medium
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/core/firebase/FirestoreCollections.kt` (Lines 3–11)
  - `app/src/main/java/com/vivaanenterprise/app/data/remote/datasource/FirestoreSyncDataSource.kt`
- **Risk:** All Firestore collections (`business_profiles`, `clients`, `products`, `documents`, `document_line_items`, `client_account_entries`, `document_sequences`) are top-level root collections. Document IDs are generated via UUID without user UID scoping or workspace tenant prefixing. If multiple business operators connect using separate Google accounts without a multi-tenant security rule or document hierarchy, their documents and master catalogs will intermingle in Firestore.
- **Fix approach:** Scope Firestore document paths under user or organization nodes (e.g. `users/{userId}/documents/{documentId}` or `organizations/{orgId}/documents/{documentId}`).

---

## 4. Performance Considerations

### Compose Recomposition Instability across UI State Models
- **Severity:** Medium
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/feature/invoice/presentation/InvoiceUiState.kt`
  - `app/src/main/java/com/vivaanenterprise/app/feature/purchaseorder/presentation/PurchaseOrderUiState.kt`
  - `app/src/main/java/com/vivaanenterprise/app/feature/document/history/list/DocumentsUiState.kt`
  - `app/src/main/java/com/vivaanenterprise/app/feature/client/presentation/list/ClientListContracts.kt`
  - `app/src/main/java/com/vivaanenterprise/app/feature/pdfviewer/presentation/PdfViewerUiState.kt`
- **Impact:** None of the UI state data classes are marked with Compose's `@Immutable` or `@Stable` annotations. Furthermore, they contain standard Kotlin mutable collection interfaces (`List<DocumentLineUiState>`, `List<DocumentItemUiState>`, `Map<Int, PageRenderState>`). The Compose compiler marks these classes as **unstable**. Consequently, composable functions taking these states (e.g., `InvoiceScreen`, `DocumentsScreen`, `PdfViewerScreen`) cannot leverage smart recomposition skipping, causing full-screen recompositions on minor state updates.
- **Fix approach:**
  1. Add `@Immutable` to all feature `UiState` data classes.
  2. Adopt `kotlinx.collections.immutable` (`ImmutableList`, `PersistentList`) for list/map fields within UI states.

### Debug Build Minification Build-Time Penalty
- **Severity:** Medium
- **Files:**
  - `app/build.gradle.kts` (Lines 24–31)
  - `AGENTS.md` (Rule 19)
- **Impact:** Commit `e100ff5` enabled `isMinifyEnabled = true` and `isShrinkResources = true` on the `debug` build type. This contradicts Rule 19 of `AGENTS.md` ("Debug builds: isMinifyEnabled = false, isShrinkResources = false"). Running R8 code shrinking and resource optimization on every debug build increases incremental compilation time by 30–60+ seconds and strips stack trace line numbers unless mapping files are actively remapped.
- **Fix approach:** Revert `debug { isMinifyEnabled = false; isShrinkResources = false }` in `app/build.gradle.kts` while maintaining strict R8 minification on `release`.

### Full In-Memory Heap Buffering During PDF Generation
- **Severity:** Low
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/core/pdf/AndroidBusinessDocumentPdfGenerator.kt` (Lines 81–83)
- **Impact:** `ByteArrayOutputStream` buffers the entire PDF binary in memory before returning byte arrays. For large multi-page documents (10+ pages), holding the uncompressed PDF binary in RAM simultaneously with rendered `ARGB_8888` page bitmaps in `PdfViewerViewModel` strains the Android 32-bit/64-bit app heap on lower-end devices.
- **Fix approach:** Stream generated PDF bytes directly to temporary files via `FileOutputStream` rather than buffering in heap RAM.

---

## 5. Fragile Areas & Maintenance Hotspots

### PDF Coordinate Engine and Multi-Page Table Flow
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/core/pdf/AndroidBusinessDocumentPdfGenerator.kt` (827 lines)
  - `app/src/main/java/com/vivaanenterprise/app/core/pdf/PdfPageContext.kt` (331 lines)
- **Why Fragile:** PDF rendering uses precise top-down coordinate arithmetic across 7 contiguous blocks (Brand Header, Metadata Grid, Bill To / Delivery, Line Items, Totals & Tax Summary, Bank Details & Declaration, Footer). Any change to font sizes, padding tokens, or line wrapping can push content past page boundaries, triggering continuation page breaks.
- **Safe Modification:**
  - Always run `AndroidBusinessDocumentPdfGeneratorTest` across both single-page (VE/06, VE/05) and multi-page stress scenarios (25+ items) after adjusting layout metrics.
  - Never draw text or lines using hardcoded Y offsets without advancing `ctx.currentY`.

### Sequential Non-Atomic Firestore Sync Operations
- **Files:**
  - `app/src/main/java/com/vivaanenterprise/app/data/repository/SyncRepositoryImpl.kt` (Lines 50–96)
- **Why Fragile:** Step 1 executes iterative network requests (`for (client in pendingClients)`) sequentially. If network connectivity drops midway (e.g. after pushing clients but before pushing invoices), Room records are left in partial `SYNCED` and `FAILED` states.
- **Safe Modification:** Group pending records into atomic Firestore `WriteBatch` operations (up to 500 writes per batch) so local entity sync states update in unified transactions.

---

## 6. Scaling Limits

| Subsystem | Current Boundary | Limit / Failure Mode | Scaling Remediation |
| :--- | :--- | :--- | :--- |
| **Room Document Queries** | `Flow<List<BusinessDocumentEntity>>` | > 2,000 documents will cause memory bloat and UI frame drops in Compose `LazyColumn`. | Implement Android Jetpack Paging 3 (`PagingSource`) with `LazyPagingItems`. |
| **Firestore Sync Pull** | `whereGreaterThanOrEqualTo("updatedAt", timestamp)` | Queries without `.limit()` will time out or exhaust memory if pulling > 5,000 historical records on clean installs. | Add pagination cursor limits (`.limit(200)`) to `FirestoreSyncDataSource`. |
| **PDF Page Viewer Cache** | Bounded to 3 rendered bitmaps | Rapid multi-page scrolling can churn bitmap allocations. | Use a fixed Bitmap pool or lower resolution downsampling for inactive thumbnail pages. |

---

## 7. Dependencies & Platform Risks

- **Compile SDK 37 / Target SDK 37:**
  - `compileSdk = 37` and `targetSdk = 37` in `app/build.gradle.kts` target the latest Android 16 release. Developers must monitor upcoming platform restrictions on edge-to-edge system bars, background work scheduling, and predictive back gestures.
- **Absence of Immutable Collections:**
  - Lack of `kotlinx-collections-immutable` prevents compiler-verified stability in Compose state data classes.

---

## 8. Test Coverage Gaps

| Area | Missing Test Coverage | Impact & Risk | Priority |
| :--- | :--- | :--- | :--- |
| **Compose UI Screens** | Zero UI tests with `createComposeRule()` for `DashboardScreen`, `InvoiceScreen`, `PurchaseOrderScreen`, `PdfViewerScreen`, and `ClientListScreen`. | Visual regressions, state loss on rotation, and touch event breakages cannot be caught automatically. | **High** |
| **FirebaseSyncWorker** | No unit or instrumentation test for `FirebaseSyncWorker.doWork()`. | Worker parameter parsing, Hilt injection in background processes, and `Result.retry()` logic are unverified. | **Medium** |
| **FirestoreSyncDataSource** | No mock tests verifying Firestore query construction or timeout behavior. | Potential query format errors or network retry issues remain undetected until runtime. | **Medium** |
| **SyncPreferencesDataStore** | No isolated unit test for timestamp cursor reads and writes. | Sync cursor corruption or failure to advance timestamp would cause infinite re-sync loops. | **Low** |

---

*Concerns audit: 2026-11-17*
