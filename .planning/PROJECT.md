# Project Context: Vivaan Enterprise

## What Vivaan Enterprise Is
Vivaan Enterprise is a specialized, offline-first Android business application designed for small-team billing operations, client document management, and ledger accounting. Its primary functions include:
- Client & Product master catalog management
- Tax Invoice generation (`TAX_INVOICE`)
- Purchase Order generation (`PURCHASE_ORDER`)
- Client billing account history and ledger transactions
- Native A4 PDF generation and in-app viewing (`android.graphics.pdf.PdfDocument`, `PdfRenderer`)
- Local Room persistence as single source of truth
- Asynchronous background synchronization to Cloud Firestore via WorkManager

## Target Users
- Small business owners, billing clerks, and internal sales operators (1–5 users) managing invoicing and purchase orders on Android mobile devices with intermittent or no network connectivity.

## Core Architectural Invariants
1. **Room is the Local Source of Truth**: UI components observe local Room data via `Flow`. The UI NEVER observes Firestore directly.
2. **Offline-First Writes**: Writes mutate local Room entities first inside transactions. Local mutations immediately update UI state and mark records with pending sync metadata. Background WorkManager tasks synchronize pending changes with Firestore.
3. **Exact Financial Precision**: Monetary amounts use integer minor units (`Long` paise). Binary floating-point types (`Double`, `Float`) are strictly prohibited for monetary calculations.
4. **Immutable Document Snapshots**: Finalized documents snapshot seller, client, and product data to preserve historical integrity regardless of subsequent catalog edits.
5. **Dynamic PDF Generation**: PDFs are dynamically rendered on-demand from snapshots. Binaries are not stored permanently in the database.
6. **MVI Architecture**: Jetpack Compose screens observe immutable `UiState` and emit `UiIntent` / `UiAction` events to Hilt-injected ViewModels.

## Technology Stack
- **OS / Platform**: Android (`minSdk = 26`, `targetSdk = 37`, `compileSdk = 37`)
- **Language / Runtime**: Kotlin 2.3.10, Java 17
- **UI Framework**: Jetpack Compose with Material 3 design system
- **Build System**: Android Gradle Plugin 9.4.0, Gradle Wrapper 9.6.0
- **Dependency Injection**: Hilt
- **Persistence**: Room Database (with SQL schema export) & DataStore Preferences (for UI preferences only)
- **Background Tasks**: WorkManager
- **Backend / Auth**: Firebase Authentication & Cloud Firestore (Firebase BOM)
- **Code Optimization**: R8 minification and resource shrinking enabled in release

## Key Reference Documentation
- [docs/ARCHITECTURE.md](file:///Users/macm90/OT/Projects/Vivaan-Enterprise/docs/ARCHITECTURE.md)
- [docs/DATA_MODEL.md](file:///Users/macm90/OT/Projects/Vivaan-Enterprise/docs/DATA_MODEL.md)
- [docs/PDF_SPEC.md](file:///Users/macm90/OT/Projects/Vivaan-Enterprise/docs/PDF_SPEC.md)
- [DESIGN.md](file:///Users/macm90/OT/Projects/Vivaan-Enterprise/DESIGN.md)
- [AGENTS.md](file:///Users/macm90/OT/Projects/Vivaan-Enterprise/AGENTS.md)
- [README.md](file:///Users/macm90/OT/Projects/Vivaan-Enterprise/README.md)
