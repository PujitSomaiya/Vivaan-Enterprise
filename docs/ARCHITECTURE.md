# Vivaan Enterprise Architecture Specification

## 1. Architectural Goals
The architecture of Vivaan Enterprise is designed around these core principles:
1. **Offline-First Reliability**: Local business operations (creating clients, products, invoices, purchase orders) must complete instantly without requiring network access or server latency.
2. **Deterministic Financial Precision**: Financial calculations must produce identical results across UI display, database storage, and PDF rendering without binary floating-point errors.
3. **Single Local Source of Truth**: Room database is the sole source of data observed by the UI layer.
4. **Immutable Historical Documents**: Finalized invoices and purchase orders store immutable snapshots of seller, client, and product data to prevent historical records from changing when master catalogs update.
5. **Dynamic PDF Generation**: PDF documents are rendered on-demand from structured document snapshots rather than stored as bulky binary blobs.
6. **Pragmatic Single-Module Structure**: Maintain a single `:app` module while preserving clean package separation until scale or build isolation genuinely justifies modularization.

---

## 2. Layer Responsibilities & Package Boundaries

Package root: `com.vivaanenterprise.app`

```text
com.vivaanenterprise.app
├── app/                  # Application subclass, Hilt modules, root navigation
│   └── navigation/       # Top-level NavHost & route composable mappings
├── core/                 # Shared foundational utilities & infrastructure
│   ├── common/           # Result types, dispatchers, extension functions
│   ├── designsystem/     # Color tokens, typography, shapes, atomic UI components
│   ├── database/         # Room Database instance, DAOs, type converters
│   ├── datastore/        # DataStore Preferences (UI settings only)
│   ├── firebase/         # Firebase Auth & Firestore client wrappers
│   ├── sync/             # WorkManager sync workers & sync state orchestrator
│   ├── pdf/              # Native PdfDocument generator & PdfRenderer helper
│   └── util/             # Date formatting, Indian currency formatters
├── data/                 # Data orchestration layer
│   ├── local/            # Room data sources & DataStore access
│   ├── remote/           # Firestore remote data sources & DTO mappers
│   └── repository/       # Repository implementations (coordinating local + remote)
├── domain/               # Domain business logic layer
│   ├── model/            # Pure Kotlin domain models & financial calculation models
│   ├── repository/       # Repository interfaces
│   └── usecase/          # Focused business use cases (e.g., CalculateInvoiceTotalsUseCase)
└── feature/              # Feature modules (MVI presentation layer)
    ├── splash/           # Launch & session verification
    ├── auth/             # Login & authentication screen
    ├── dashboard/        # Main dashboard & quick metrics
    ├── client/           # Client master list & detail/editor
    ├── product/          # Product master catalog
    ├── document/         # Tax Invoice & Purchase Order creation/history
    ├── account/          # Client billing account ledgers
    ├── pdfviewer/        # Full-screen PDF previewer & sharing
    └── settings/         # App preferences & sync management
```

### Layer Rules
- **UI Layer (`feature/`)**: Jetpack Compose screens and MVI ViewModels. ViewModels consume `domain/` use cases or `domain/repository/` interfaces. UI composables never touch database or network sources directly.
- **Domain Layer (`domain/`)**: Pure Kotlin modules containing business models, calculation rules, and repository interfaces. Free of Android framework dependencies (`android.*`).
- **Data Layer (`data/`, `core/database/`, `core/firebase/`)**: Implements repository interfaces. Manages Room DAOs, Firestore sync, DataStore preferences, and maps data transfer objects (DTOs) / Room entities to domain models.
- **PDF Layer (`core/pdf/`)**: Translates structured domain document snapshots into physical PDF pages using native `android.graphics.pdf.PdfDocument`.

---

## 3. Data Flow Architecture

### Read Data Flow (Observation)
```
Room Database  --->  Room DAO (Flow<List<Entity>>)  --->  Local DataSource
                                                             │
                                                             ▼
Compose UI  <---  UiState  <---  ViewModel  <---  Repository (Flow<List<DomainModel>>)
```
- UI components observe data solely through `Flow` emissions coming from Room via the Repository layer.
- The UI layer NEVER directly observes Firestore collections or network sockets.

### Write Data Flow (Offline-First Mutation)
```
User Action (Click "Finalize Invoice")
    │
    ▼
ViewModel (Emits UiIntent)
    │
    ▼
Repository
    │
    ├── 1. Execute Room Transaction (Insert Document, Line Items, Account Ledger)
    │      └── Mark record with syncStatus = PENDING & updatedTimestamp
    │
    ├── 2. UI Immediately Updates (Observing Room Flow)
    │
    └── 3. Enqueue Background WorkManager Job
           │
           ▼
     WorkManager Worker
           │
           ├── Push Pending Record to Cloud Firestore
           │
           └── On Success: Update Room Entity (syncStatus = SYNCED)
```

---

## 4. Offline-First & Synchronization Philosophy

### Room as Source of Truth
- All user-generated business data (clients, products, invoices, purchase orders, account entries) is written to Room first.
- The app operates fully offline after initial user authentication.

### DataStore Scope
- `DataStore` Preferences is strictly restricted to simple key-value UI configuration settings (e.g., `DARK_MODE_ENABLED`, `LAST_SELECTED_TAB`).
- `DataStore` MUST NEVER be used to store JSON arrays or collections of clients, products, or invoices.

### Sync State Lifecycle
Each syncable Room entity contains local-only device synchronization metadata fields:
- `syncStatus`: `PENDING` (needs upload), `SYNCED` (in sync with Firestore), or `FAILED` (encountered error).
- `lastSyncedAt`: Unix epoch timestamp of last successful remote sync on this device.
- `syncError`: Sanitized short technical failure diagnostic string.
*Note: `syncStatus`, `lastSyncedAt`, and `syncError` are local device state ONLY and are never serialized or uploaded to Firestore.*

### Synchronization Boundary & Provisional Merge Policy (Step 5)
- **WorkManager Policy**: Uses `ExistingWorkPolicy.APPEND_OR_REPLACE` via `WorkManagerSyncScheduler` to ensure rapid local mutations trigger follow-up execution if a sync worker is already active.
- **Replay-Safe Cursor**: Incremental pulls query Firestore using `whereGreaterThanOrEqualTo("updatedAt", lastSyncTimestamp)`. The `lastSyncTimestamp` cursor advances only after the complete parent->child pull sequence succeeds.
- **Strict Parent-Child Dependency Ordering**: Pull imports collections in relational hierarchy order: `BusinessProfile` -> `Clients` / `Products` -> `BusinessDocuments` -> `DocumentLineItems` -> `ClientAccountEntries` -> `DocumentSequences` to prevent Room foreign key violations.
- **Provisional Merge Policy**:
  - `PENDING` & `FAILED` local records are protected from being overwritten by remote data.
  - `SYNCED` local records accept remote updates if `remote.updatedAt >= local.updatedAt`.
  - `FINALIZED` local business documents reject remote snapshot mutations to guarantee historical financial record immutability.
  - Remote tombstones (`isDeleted = true`) soft-delete local `SYNCED` records, while preserving `PENDING` local edits.
*Note: Advanced conflict resolution, retries, and clock drift handling are deferred to Step 17.*

---

## 5. Transaction Safety
Operations that touch multiple database tables must be executed inside atomic `@Transaction` blocks in Room DAOs.

Example Multi-Table Transactions:
1. **Finalizing a Tax Invoice**:
   - Update `BusinessDocumentEntity` status from `DRAFT` to `FINALIZED` and freeze snapshots.
   - Insert `DocumentLineItemEntity` snapshot rows.
   - Insert `ClientAccountEntryEntity` row for client receivable.
   - Increment `DocumentSequenceEntity` for the next suggested invoice number.

If any sub-operation fails, the entire transaction rolls back cleanly, maintaining database integrity.

---

## 6. Financial Calculation Architecture
To guarantee 100% financial consistency across UI preview screens, Room persistence, and PDF rendering:
- **Centralized Engine**: All tax calculations (CGST, SGST, IGST), line item extensions, taxable values, discounts, and grand totals are performed by a single deterministic engine (`DocumentCalculator`).
- **No Duplicate Logic**: UI composables and PDF layout renderers MUST NOT write independent calculation formulas. They consume `DocumentCalculationResult` generated by `DocumentCalculator`.

---

## 7. Dynamic PDF Architecture
- **On-Demand Rendering**: PDF files are generated dynamically from stored document snapshot data when requested by the user.
- **No Binary Database Blobs**: Raw `.pdf` file bytes are never stored inside Room or Firestore.
- **Temporary Cache**: Generated PDFs for in-app viewing are stored in `context.cacheDir` (`/cache/pdf/`).
- **Secure File Sharing**: Sharing PDFs with external applications (WhatsApp, Email) utilizes Android `FileProvider` with temporary read permission `content://` URIs. `file://` URIs are forbidden.
