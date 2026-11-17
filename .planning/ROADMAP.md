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
- **Phase 20: Bidirectional Multi-Device Sync Engine (Inbound/Outbound Consistency & Data Reconciliation)**
  - **Goal**: Guarantee full two-way data consistency across multiple live devices so invoices, clients, products, and ledger entries created on one device reliably reflect on all other devices upon synchronization.
  - **Scope**:
    1. *Lookback & Full Reconciliation Strategy*: Replace brittle exact-millisecond `whereGreaterThanOrEqualTo("updatedAt", lastSyncTime)` filtering with a robust lookback reconciliation window and full collection reconcile option on manual sync to prevent clock skew or delayed syncs from dropping remote records.
    2. *Foreign Key Dependency Ordering & Transaction Safety*: Guarantee atomic, dependency-ordered ingestion into Room (BusinessProfile -> Clients -> Products -> BusinessDocuments -> LineItems -> ClientAccountEntries) with missing parent fallbacks so foreign key constraints never fail on remote pull.
    3. *Document Sequence Multi-Device Sync*: Implement outbound push and inbound pull for `DocumentSequenceEntity`, preventing duplicate invoice numbering (`VE/XX/YYYY-YY`) when multiple devices generate documents offline.
    4. *Bidirectional Manual & Periodic Sync Verification*: Verify multi-device scenario in unit and integration tests where Device A creates an invoice and Device B syncs and immediately renders that invoice, its line items, and ledger account entries in Room.
  - **Status**: Planned

- **Phase 21: Compose UI Common Component Refactoring & Code De-duplication**
  - **Goal**: Eliminate redundant, deeply nested boilerplate across all Jetpack Compose UI screens by extracting reusable domain and layout components into clean, small, shared functions.
  - **Scope**:
    1. *Form Architecture & Section Extraction*: Extract shared form sections (Party Info cards, Line Item list editor, Financial Summary & GST breakdown cards, Delivery Details card) shared identically between `InvoiceScreen.kt` and `PurchaseOrderScreen.kt`.
    2. *List & Search Screen Harmonization*: Extract common search bar + filter chips + empty state + swipeable/selectable entity cards shared across `ClientListScreen.kt`, `ProductListScreen.kt`, and `DocumentsScreen.kt`.
    3. *Detail & Action Screen Harmonization*: Extract reusable metadata rows, financial stat banners, and dialog action bars shared across `ClientDetailScreen.kt`, `ProductDetailScreen.kt`, and `DocumentDetailScreen.kt`.
    4. *Strict Theme & Design System Adherence*: Ensure all extracted common components adhere strictly to `DESIGN.md` tokens, touch target minimums (>= 48dp), and system font scaling.
  - **Status**: Planned

- **Phase 22: Performance & Recomposition Audit**
  - Goal: Verify `@Stable` and `@Immutable` contracts across all UiState classes, verify LazyColumn key stability.
  - Status: Planned

- **Phase 23: Multi-Page PDF Handling & Export Enhancements**
  - Goal: Extended layout for high line item counts (>10 items) across multiple A4 pages with page budget management.
  - Status: Planned
