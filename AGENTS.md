# Vivaan Enterprise Engineering Rules

## 1. Project Overview
Vivaan Enterprise is a small internal Android business application designed primarily for single-user or small team (1–5 users) document management and billing operations. Its primary business functions include:
- Client & Product master management
- Tax Invoice generation (`TAX_INVOICE`)
- Purchase Order generation (`PURCHASE_ORDER`)
- Document history and client billing account history
- Offline-first operation with local database persistence
- Background synchronization with Cloud Firestore
- Dynamic PDF generation, in-app viewing, and sharing/exporting

The application stores structured business and document data. PDF files are not stored as primary business records; they are generated dynamically from persisted document snapshots on demand.

## 2. Required Preparation Before Changing Code
Before modifying any code, engineers and AI coding assistants MUST perform this mandatory sequence:
1. Read this `AGENTS.md`.
2. Read `DESIGN.md` for UI and styling work.
3. Read relevant specification documents in `docs/` (`ARCHITECTURE.md`, `DATA_MODEL.md`, `PDF_SPEC.md`).
4. Inspect the complete end-to-end user flow being modified.
5. Inspect all callers and downstream consumers before changing shared APIs or models.
6. Preserve existing working behavior and contracts.
7. Make the smallest safe change that fully satisfies the requirements.
8. Run verification commands synchronously and confirm exit code 0.

*Note: Nested `AGENTS.md` files in subdirectories override root guidelines for their specific component if introduced in future modularization.*

## 3. Technology Stack & Configuration
The project targets modern Android engineering standards:
- **Language**: Kotlin (AGP 9 built-in Kotlin support enabled)
- **UI Framework**: Jetpack Compose with Material 3 design system
- **Build System**: Android Gradle Plugin 9.4.0, Gradle Wrapper 9.6.0
- **SDK Targets**: `compileSdk = 37`, `targetSdk = 37`, `minSdk = 26`
- **JDK / Java Target**: Java 17 / JVM 17
- **Annotation Processing**: KSP (Kotlin Symbol Processing)
- **Dependency Injection**: Hilt
- **Local Persistence**: Room (for business data) & DataStore Preferences (for key-value UI settings)
- **Background Tasks**: WorkManager
- **Remote Backend**: Firebase Authentication & Cloud Firestore
- **Asynchronous Execution**: Kotlin Coroutines & Flow (`StateFlow` / `SharedFlow`)
- **Release Optimization**: R8 minification and resource shrinking enabled (`isMinifyEnabled = true`, `isShrinkResources = true`)

*The version catalog at [`gradle/libs.versions.toml`](file:///Users/macm90/AndroidStudioProjects/VivaanEnterprise/gradle/libs.versions.toml) is the single authoritative source of truth for library version numbers.*

## 4. Architecture & Data Flow
The project follows Unidirectional Data Flow (UDF) with a strict MVI presentation pattern:
```
Compose UI  <--->  MVI ViewModel  <--->  Repository  <--->  Room Database (Local Source of Truth)
                                                                 ▲
                                                                 │ (Sync Metadata)
                                                            WorkManager / Sync
                                                                 │
                                                                 ▼
                                                          Cloud Firestore
```
- **Room is the Local Source of Truth**: UI components observe local Room data via `Flow`. The UI NEVER observes Firestore directly.
- **Offline-First Writes**: Writes mutate local Room entities first inside transactions. Local mutations immediately update UI state and mark records with pending sync metadata. Background WorkManager tasks synchronize pending changes with Firestore.
- **Dynamic PDF Generation**: PDFs are dynamically generated on-demand from frozen, persisted document snapshots using native `android.graphics.pdf.PdfDocument`.

## 5. Compose UI Rules
- **Stateless Screen Composables**: Screen composables (`<Feature>Screen`) must be stateless regarding business logic, accepting immutable state objects and emitting event lambdas.
- **Route Boundary**: Scoping and obtaining ViewModels occurs strictly at the route level (`<Feature>Route`). Do not pass ViewModel references into child composables.
- **State Collection**: Collect flows in Compose using `collectAsStateWithLifecycle()` to respect Android lifecycle states.
- **Performance & Keys**: Use stable keys (`key` parameter) in `LazyColumn`/`LazyRow` items based on unique entity IDs (do not use list index as a key for dynamic lists).
- **Design System Tokens**: Do not hardcode dimensions, typography, or colors. Use semantic tokens defined in `DESIGN.md` and `Theme.kt`.
- **Accessibility & Design**: Ensure touch target sizes are at least 48x48dp, support system text scaling, handle safe draw insets/system bars, handle IME focus/keyboard actions natively, and provide localized string resources (`stringResource`).

## 6. MVI Presentation Rules
Each feature module contains:
- `UiState`: Data class or sealed hierarchy representing immutable, exhaustive rendering state.
- `UiIntent` / `UiAction`: Sealed hierarchy or clear named functions representing user interactions.
- `UiEffect`: One-shot event contract (e.g., Snackbar messages, navigation triggers) exposed via a appropriate one-time stream.
- `ViewModel`: Manages business state via `StateFlow` and handles intents without retaining UI view references.

*Transient events such as Toasts, Snackbars, and navigation triggers must NOT be stored permanently inside durable `UiState`.*

## 7. Coroutines & Flow Discipline
- **Structured Concurrency**: All coroutines must be launched within lifecycle-owned scopes (`viewModelScope` or `rememberCoroutineScope` for pure UI effects).
- **No GlobalScope**: `GlobalScope` is strictly prohibited.
- **Cancellation**: `CancellationException` must never be caught and swallowed in broad `try-catch` blocks; always rethrow it.
- **Dispatchers**: Use `Dispatchers.Main` for UI state coordination, `Dispatchers.IO` for disk/database I/O, and `Dispatchers.Default` for heavy calculations.
- **Lifecycle Awareness**: Derive state using `stateIn(..., SharingStarted.WhileSubscribed(5_000), ...)` to avoid background resource leaks.

## 8. Offline-First & Data Persistence Rules
- **ROOM IS THE LOCAL SOURCE OF TRUTH**: All reads originate from Room.
- **DataStore Isolation**: `DataStore` Preferences is reserved strictly for lightweight UI settings (e.g., theme preference, last selected tab). It MUST NEVER be used as a document or business JSON blob store.
- **Local-First Operations**: Normal CRUD operations (creating clients, products, invoices) must complete offline without checking network connectivity first.
- **Sync Metadata**: Entities subject to synchronization must carry synchronization status (`PENDING`, `SYNCED`, `FAILED`) and timestamp metadata.

## 9. Firebase Integration Rules
- **Targeted SDKs**: Use `firebase-auth` for user authentication and `firebase-firestore` for remote data backup/sync via the Firebase BOM.
- **Prohibited SDKs**: Do NOT add Firebase Storage, Realtime Database, Analytics, Crashlytics, or Remote Config without explicit user requirement.
- **No Direct Binding**: ViewModels and Compose screens must never consume Firestore snapshot listeners directly.

## 10. Database & Room Rules
- **Entity Boundaries**: Room entities represent physical database tables.
- **Strict Migrations**: Destructive migrations (`fallbackToDestructiveMigration()`) are strictly forbidden for production business data.
- **Atomic Writes**: Multi-table operations (e.g., finalizing an invoice, updating line items, and recording account entries) must run inside Room `@Transaction` blocks.
- **Timestamps**: All timestamps must be stored as Unix epoch milliseconds (`Long`) or UTC ISO-8601 strings.
- **No Floating-Point Money**: Never store or calculate monetary amounts using `Double` or `Float`.

## 11. Financial Precision & Money Rules
- **No Double/Float**: `Double` and `Float` are strictly prohibited for rates, taxable amounts, GST calculations, line item totals, and grand totals due to binary floating-point rounding errors.
- **Exact Money Representation**: Persisted monetary figures MUST use a single exact representation based on integer minor units: `Long` paise (e.g., ₹ 2,500.00 = `250000L`, ₹ 48,852.00 = `4885200L`).
- **Single Source of Calculation**: Document calculations must be performed by a single deterministic calculation engine (`DocumentCalculator`). The output `DocumentCalculationResult` feeds the UI, database persistence, and PDF renderer identically.

## 12. Document Management & Numbering
- **Supported Documents**: `TAX_INVOICE` and `PURCHASE_ORDER`.
- **Independent Sequences**: Invoice and Purchase Order numbering sequences are separate and independent.
- **Numbering Strategy**: Automatic numbering provides a suggested value (e.g., `VE/06/2026-27`), but automatic numbering is convenience, not authority. The user may manually override the entire suggested document number.
- **Database Strategy Deferred**: Database indices, duplicate handling, and repository validation strategies are deferred to the actual document implementation steps.


## 13. Immutable Document Snapshots
- **Snapshot Freezing**: When a document is **Finalized**, all relevant master data (seller details, client name/address/GSTIN, product description/HSN/tax rate) is snapshot-frozen directly within the document record.
- **Historical Immutability**: Subsequent updates or deletions in the Client or Product master catalogs MUST NOT modify previously finalized historical invoices/purchase orders.
- **PDF Generation Source**: Dynamic PDF generation for historical documents MUST render from the saved document snapshot, NEVER from current master catalog tables.

## 14. PDF Generation & Viewing
- **Native Implementation**: Dynamic PDF generation uses native `android.graphics.pdf.PdfDocument`. In-app rendering uses `android.graphics.pdf.PdfRenderer`. Third-party PDF libraries are prohibited.
- **On-Demand Generation**: Permanent PDF file binaries are NOT stored in Room or Firestore. PDFs are rendered on-demand into temporary cache files.
- **Export & Sharing**: Exporting saves PDFs to user-accessible storage via `MediaStore`. Sharing utilizes Android `FileProvider` with temporary read permission content URIs (`content://`). `file://` URIs are forbidden.

## 15. Design System & Styling
- Refer to `DESIGN.md` for full palette, typography, and shape specifications.
- Hardcoded hex colors, arbitrary DP margins, raw text styles, or duplicate layout dimensions are prohibited in screen composables.

## 16. Complete State Coverage
Every screen implementation must explicitly account for all visual states:
1. `Loading` (progress indicators)
2. `Content` (populated data)
3. `Empty` (helpful zero-state guidance)
4. `Validation / Error` (field-level inline errors or retry banners)
5. `Offline / Sync Status` (unobtrusive sync state indicators)

## 17. Security & Privacy
- `android:allowBackup="false"` must be strictly maintained in `AndroidManifest.xml`.
- No credentials, API keys, or private client financial data in version control or plain-text logcat.
- Minimize exported Android components (`android:exported="false"` unless launcher/explicitly required).
- Content URIs issued via `FileProvider` must enforce strict read-only grant flags.

## 18. Dependency Management
- Do not introduce third-party libraries without explicit project approval.
- Rely on official Android Jetpack, Kotlin, and Firebase libraries.

## 19. ProGuard & R8 Configuration
- Debug builds: `isMinifyEnabled = false`, `isShrinkResources = false`.
- Release builds: `isMinifyEnabled = true`, `isShrinkResources = true`.
- ProGuard rules (`proguard-rules.pro`) must remain minimal, clean, and specifically justified for Room/Hilt/Firebase reflection requirements.

## 20. Verification Commands
Before submitting any task, execute the following commands synchronously and ensure exit code 0:
```bash
./gradlew --version
./gradlew help
./gradlew build --dry-run
./gradlew assembleDebug
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew assembleRelease
```
Never claim a command passed without executing it and verifying clean output.
