# Project Roadmap: Vivaan Enterprise

## Completed Milestones

### Milestone 1: v1.0.0 Production Release (Completed)
- **Phase 1: Project Setup & Architecture Foundations** (Steps 1–3)
  - Core build setup, Gradle AGP 9.4, Kotlin 2.3.10, Hilt, Room database setup, MVI base infrastructure.
- **Phase 2: Master Data Management** (Steps 4–6)
  - Client Master CRUD, Product Master CRUD, Room DAOs, repositories, MVI screens.
- **Phase 3: Financial Engine & Document Invariants** (Steps 7–9)
  - Integer paise `Long` money engine, `DocumentCalculator`, document numbering sequence.
- **Phase 4: Document Creation & Snapshot Freezing** (Steps 10–12)
  - Tax Invoice & Purchase Order forms, line item editor, finalized snapshot freeze.
- **Phase 5: Dynamic PDF Generation & Viewer** (Steps 13–15)
  - Native A4 `PdfDocument` rendering, in-app `PdfRenderer`, FileProvider share, MediaStore export.
- **Phase 6: Ledger Accounting & Firestore Sync** (Steps 16–18)
  - Client accounts, ledger entries, WorkManager offline synchronization to Cloud Firestore, R8 minification, release auditing.

---

## Active Milestone

### Milestone 2: Production Hardening & Sync Engine Recovery (In Progress)
- **Phase 19: Firestore Sync Outage Recovery & UI Hardening**
  - **Goal**: Resolve live multi-device sync breakdown caused by expired Firebase security rules; ensure auto-recovery for `FAILED` entities, periodic background retries, and explicit user-driven manual sync with diagnostics.
  - **Scope**:
    1. *Sync Engine Auto-Recovery*: Fix `SyncRepositoryImpl` to query and re-attempt push for both `PENDING` and `FAILED` entities (or self-heal `FAILED` status back to `PENDING` on new attempts).
    2. *WorkManager Scheduling*: Add periodic background sync worker (`PeriodicWorkRequest`) with network constraints in addition to one-time startup/write triggers.
    3. *Manual Sync & Diagnostics UI*: Add a "Sync Now" / "Retry" action and sync status banner/indicator on the Dashboard/Top Bar with feedback on last sync time and error state.
    4. *Diagnostics & Verification*: Provide count/status visibility of pending & failed entities (Clients, Products, Documents, Ledgers); verify end-to-end recovery using unit tests.
  - **Status**: Completed (Verified via `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew assembleDebug`, `./gradlew assembleRelease`)
- **Phase 20: Performance & Recomposition Audit**
  - Goal: Verify `@Stable` and `@Immutable` contracts across all UiState classes, verify LazyColumn key stability.
  - Status: Planned
- **Phase 21: Multi-Page PDF Handling & Export Enhancements**
  - Goal: Extended layout for high line item counts (>10 items) across multiple A4 pages with page budget management.
  - Status: Planned
