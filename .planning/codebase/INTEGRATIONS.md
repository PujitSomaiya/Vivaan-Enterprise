# External Integrations
**Analysis Date:** 2026-11-17

## APIs & External Services

**Firebase Cloud Services:**
- Google Cloud Firestore - Remote document database for multi-device backup and bidirectional synchronization.
  - SDK/Client: `com.google.firebase:firebase-firestore` (via Firebase BOM 34.18.0 in `gradle/libs.versions.toml`)
  - Client Injection: Provided via `FirebaseModule` in `app/src/main/java/com/vivaanenterprise/app/core/firebase/di/FirebaseModule.kt`
  - Auth: Authenticated automatically through Firebase Authentication user session
  - Remote Data Source: `app/src/main/java/com/vivaanenterprise/app/data/remote/datasource/FirestoreSyncDataSource.kt`
  - Collections: `business_profiles`, `clients`, `products`, `documents`, `document_line_items`, `client_account_entries`, `document_sequences` (constants defined in `app/src/main/java/com/vivaanenterprise/app/core/firebase/FirestoreCollections.kt`)
  - Integration Pattern: UI components never observe Firestore directly. All writes are offline-first to local Room database entities; background workers push pending mutations and pull remote changes in parent-child hierarchy order (`app/src/main/java/com/vivaanenterprise/app/data/repository/SyncRepositoryImpl.kt`).

**Android Platform & System Services:**
- Android PDF Document Generation - Low-level vector graphics pipeline for generating A4 business documents (Tax Invoices and Purchase Orders).
  - Platform API: Native `android.graphics.pdf.PdfDocument`
  - Implementation: `app/src/main/java/com/vivaanenterprise/app/core/pdf/AndroidBusinessDocumentPdfGenerator.kt`
  - Contract: `app/src/main/java/com/vivaanenterprise/app/domain/pdf/BusinessDocumentPdfGenerator.kt`
  - Data Dependency: Renders strictly from frozen, immutable historical snapshots stored in the document record (`clientSnapshot`, `sellerSnapshot`, `lineItems`), preserving audit integrity.
- Android In-App PDF Rendering - On-device bitmap rasterization for high-fidelity interactive document viewing.
  - Platform API: Native `android.graphics.pdf.PdfRenderer` with `android.os.ParcelFileDescriptor`
  - Implementation: `app/src/main/java/com/vivaanenterprise/app/feature/pdfviewer/service/PdfRenderSession.kt`
  - View Integration: `app/src/main/java/com/vivaanenterprise/app/feature/pdfviewer/presentation/PdfViewerScreen.kt`
- Android Storage Access Framework (SAF) & MediaStore - File system export for generated business PDFs.
  - Platform APIs: `android.provider.MediaStore.Downloads` (Android 10+ / API 29+) and `Intent.ACTION_CREATE_DOCUMENT` via `ActivityResultContracts.CreateDocument`
  - Implementation: `PdfExportManager` in `app/src/main/java/com/vivaanenterprise/app/feature/pdfviewer/service/PdfServices.kt`
  - Target Path: `Environment.DIRECTORY_DOWNLOADS/Vivaan Enterprise/` or user-selected directory URI.
- Android System Share Sheet - Secure inter-app document sharing.
  - Platform API: `android.content.Intent.ACTION_SEND` with `androidx.core.content.FileProvider`
  - Authority: `${applicationId}.fileprovider`
  - Implementation: `PdfShareManager` in `app/src/main/java/com/vivaanenterprise/app/feature/pdfviewer/service/PdfServices.kt`
  - XML Provider Paths: `app/src/main/res/xml/file_paths.xml` mapping `<cache-path name="shared_pdfs" path="pdfs/" />`
  - Manifest Declaration: `app/src/main/AndroidManifest.xml`

**Background Work Scheduling:**
- AndroidX WorkManager - Guaranteed deferrable background sync execution.
  - Platform API: `androidx.work.WorkManager` (2.11.2) with `androidx.hilt:hilt-work` (1.3.0)
  - Worker Class: `FirebaseSyncWorker` in `app/src/main/java/com/vivaanenterprise/app/core/sync/FirebaseSyncWorker.kt`
  - Scheduler: `WorkManagerSyncScheduler` in `app/src/main/java/com/vivaanenterprise/app/core/sync/SyncScheduler.kt`
  - Execution Constraints: Requires active internet connectivity (`NetworkType.CONNECTED`). Uses `ExistingWorkPolicy.APPEND_OR_REPLACE` with unique name `"firebase_sync_work"`.

## Data Storage

**Databases:**
- SQLite via AndroidX Room (Local Source of Truth):
  - Database Name: `vivaan_enterprise.db`
  - Schema Version: 3 (`exportSchema = true` in `app/src/main/java/com/vivaanenterprise/app/core/database/VivaanEnterpriseDatabase.kt`)
  - DI Provider: `app/src/main/java/com/vivaanenterprise/app/core/database/di/DatabaseModule.kt`
  - Persisted Entities:
    - `BusinessProfileEntity` (`business_profiles`) - Company profile, GSTIN, bank details
    - `ClientEntity` (`clients`) - Customer/client master records
    - `ProductEntity` (`products`) - Product master catalog and default tax rates
    - `BusinessDocumentEntity` (`business_documents`) - Header and frozen snapshot records
    - `DocumentLineItemEntity` (`document_line_items`) - Normalized document line items
    - `ClientAccountEntryEntity` (`client_account_entries`) - Ledger records (Debit/Credit/Payment)
    - `DocumentSequenceEntity` (`document_sequences`) - Independent document numbering counters
  - Migrations: Production data preserves zero data loss through sequential Room migrations:
    - `MIGRATION_1_2`: Column additions for notes, terms, and tax treatment
    - `MIGRATION_2_3`: Sync metadata columns (`syncStatus`, `lastSyncedAt`, `syncError`) across all tables
    - Defined in `app/src/main/java/com/vivaanenterprise/app/core/database/migration/DatabaseMigrations.kt`

**File Storage:**
- Local Filesystem Cache:
  - Cache Directory: `context.cacheDir/pdfs/` managed by `PdfCacheManager` in `app/src/main/java/com/vivaanenterprise/app/feature/pdfviewer/service/PdfServices.kt`
  - Strategy: Ephemeral cache files (`pdf_{docId}.pdf`), written using atomic file swap with path containment verification to prevent directory traversal
  - Eviction: Automatic cleanup of files older than 24 hours (`cleanupStaleCache()`)
  - No Raw PDF Blobs in DB: PDFs are generated dynamically on demand; primary records store structured snapshot JSON/fields, never binary PDF data

**Caching:**
- Preferences Caching via AndroidX DataStore:
  - `app_preferences.preferences_pb`: Managed by `AppPreferencesDataStore` in `app/src/main/java/com/vivaanenterprise/app/core/datastore/AppPreferencesDataStore.kt` for UI preferences (theme mode, initial profile setup completed)
  - `sync_preferences.preferences_pb`: Managed by `SyncPreferencesDataStore` in `app/src/main/java/com/vivaanenterprise/app/core/datastore/SyncPreferencesDataStore.kt` for sync watermark timestamp (`last_sync_timestamp`)
- Bitmap Rendering Cache:
  - In-memory cached bitmaps rendered per page on-the-fly inside `PdfRenderSession` (`app/src/main/java/com/vivaanenterprise/app/feature/pdfviewer/service/PdfRenderSession.kt`)

## Authentication & Identity

**Auth Provider:**
- Firebase Authentication:
  - Implementation: `app/src/main/java/com/vivaanenterprise/app/data/repository/AuthRepositoryImpl.kt`
  - Provider Client: `com.google.firebase.auth.FirebaseAuth` injected as singleton from `app/src/main/java/com/vivaanenterprise/app/core/firebase/di/FirebaseModule.kt`
  - Auth Flow: Email/password authentication (`signInWithEmailAndPassword`)
  - State Stream: `AuthStateListener` wrapped in a Kotlin `callbackFlow` emitting `AuthState.SignedIn(AuthenticatedUser)` or `AuthState.SignedOut`
  - Synchronization Gate: If user is not authenticated (`currentUser == null`), remote synchronization safely completes without network activity

## Monitoring & Observability

**Error Tracking:**
- None: External tracking SDKs (Firebase Crashlytics, Sentry, Datadog) are strictly prohibited by project governance (`AGENTS.md` Rule 9)
- Local Error Propagation: Errors are caught, typed (`CalculationError`, `SyncStatus.FAILED`), and exposed to presentation layers via sealed MVI contracts (`UiEffect.ShowSnackbar` and `AppErrorState` in `app/src/main/java/com/vivaanenterprise/app/core/designsystem/component/StateViews.kt`)

**Logs:**
- Android platform logcat (`android.util.Log`) used sparingly
- Security Boundary: Logcat and error streams are prohibited from logging sensitive financial calculations, client details, passwords, or authentication credentials (`AGENTS.md` Rule 17)

## CI/CD & Deployment

**Hosting:**
- Android Application Package (APK) and Android App Bundle (AAB) targets (`applicationId = "com.vivaanenterprise.app"` in `app/build.gradle.kts`)
- Build Types:
  - `debug`: Minification and resource shrinking enabled (`isMinifyEnabled = true`, `isShrinkResources = true`) with debuggable signing
  - `release`: Minification and resource shrinking enabled (`isMinifyEnabled = true`, `isShrinkResources = true`) with ProGuard rules in `app/proguard-rules.pro`

**CI Pipeline:**
- Local Synchronous Verification Suite: Defined in `AGENTS.md` Rule 20:
  - `./gradlew --version`
  - `./gradlew help`
  - `./gradlew build --dry-run`
  - `./gradlew assembleDebug`
  - `./gradlew lintDebug`
  - `./gradlew testDebugUnitTest`
  - `./gradlew assembleRelease`
- Remote CI: No automated GitHub Actions or remote CI configuration in repository; verification is developer/operator driven.

## Environment Configuration

**Required env vars & files:**
- `google-services.json` - Google Services configuration file required inside `app/` directory for Firebase build integration
- `local.properties` - Local Android SDK directory declaration (`sdk.dir`)

**Secrets location:**
- No secrets stored in version control (enforced via `.gitignore` for `local.properties`, `secrets.properties`, `**/google-services.json`, `*.jks`, `*.keystore`)
- Firebase client credentials are baked into `google-services.json` at build time by the Google Services Gradle plugin

## Webhooks & Callbacks

**Incoming:**
- None: No incoming webhook listeners or push notification receivers

**Outgoing:**
- None: No outgoing third-party webhooks; cloud writes are performed via direct client SDK calls inside `FirestoreSyncDataSource` (`app/src/main/java/com/vivaanenterprise/app/data/remote/datasource/FirestoreSyncDataSource.kt`)

---

*Integration audit: 2026-11-17*
