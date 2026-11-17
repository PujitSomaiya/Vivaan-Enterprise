# Requirements Specification: Vivaan Enterprise

## Functional Requirements

### 1. Client Management
- **REQ-CLIENT-01**: Create, read, update, and search clients with business name, contact person, phone, email, billing address, and GSTIN.
- **REQ-CLIENT-02**: Client GSTIN validation and state code extraction according to Indian GST standards.
- **REQ-CLIENT-03**: Display client transaction history, total billed, outstanding balance, and account ledgers.

### 2. Product Management
- **REQ-PROD-01**: Create, read, update, and search products with description, HSN code, unit price (in integer paise), unit of measurement (UOM), and GST tax rate slab.
- **REQ-PROD-02**: Maintain product catalog locally in Room with pending sync tracking.

### 3. Document Generation (Tax Invoice & Purchase Order)
- **REQ-DOC-01**: Create Tax Invoices (`TAX_INVOICE`) and Purchase Orders (`PURCHASE_ORDER`) with independent sequential numbering.
- **REQ-DOC-02**: Allow manual override of suggested document numbers while maintaining sequence integrity.
- **REQ-DOC-03**: Deterministic tax calculation engine (`DocumentCalculator`) computing taxable amount, CGST, SGST, IGST, and round-off in integer paise (`Long`).
- **REQ-DOC-04**: Freeze seller, client, and product snapshots into the document upon finalization.

### 4. Dynamic PDF & Viewer
- **REQ-PDF-01**: Dynamic generation of standard single-page A4 PDFs via native `android.graphics.pdf.PdfDocument`.
- **REQ-PDF-02**: In-app PDF viewing using native `android.graphics.pdf.PdfRenderer`.
- **REQ-PDF-03**: Share documents via Android `FileProvider` with temporary read permission content URIs.
- **REQ-PDF-04**: Export PDFs to public storage via Android `MediaStore`.

### 5. Client Account & Ledger
- **REQ-ACC-01**: Maintain debit/credit ledger entries per client.
- **REQ-ACC-02**: Automatically generate debit entries on invoice finalization; record payment credits with payment method and reference note.

### 6. Offline-First & Sync
- **REQ-SYNC-01**: All operations execute against local Room database immediately.
- **REQ-SYNC-02**: Maintain sync metadata (`PENDING`, `SYNCED`, `FAILED`) and sync timestamp on all syncable entities.
- **REQ-SYNC-03**: WorkManager background job synchronizes pending changes to Cloud Firestore with parent-child entity ordering.
- **REQ-SYNC-04**: UI exposes unobtrusive sync status indicator reflecting current sync state.

## Non-Functional & Quality Requirements

- **NFR-ARCH-01**: Strict Unidirectional Data Flow (UDF) and MVI presentation pattern with immutable `UiState`, `UiIntent`, and `UiEffect`.
- **NFR-MONEY-01**: Zero binary floating-point representation (`Double`/`Float`) in monetary calculations.
- **NFR-CONCUR-01**: Structured concurrency; rethrow `CancellationException`; no `GlobalScope`.
- **NFR-UI-01**: Material 3 design system adhering to `DESIGN.md`; touch targets >= 48x48dp; edge-to-edge support; explicit states (`Loading`, `Content`, `Empty`, `Error`, `Offline`).
- **NFR-SEC-01**: `android:allowBackup="false"`, no credentials in source control, read-only content URIs.
- **NFR-REL-01**: Full R8 minification and resource shrinking enabled in release build.
