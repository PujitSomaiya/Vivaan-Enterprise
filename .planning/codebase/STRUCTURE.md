# Vivaan Enterprise Codebase Structure

**Analysis Date:** 2026-11-17

## Directory Layout

```
Vivaan-Enterprise/
├── app/                                        # Main Android application module
│   ├── build.gradle.kts                        # App module build and dependency configuration
│   ├── proguard-rules.pro                      # R8/ProGuard shrinking rules
│   ├── schemas/                                # Exported Room database schema JSON files
│   │   └── com.vivaanenterprise.app.core.database.VivaanEnterpriseDatabase/
│   │       ├── 1.json
│   │       ├── 2.json
│   │       └── 3.json
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml             # Android manifest (no backup, FileProvider, startup)
│       │   ├── java/com/vivaanenterprise/app/  # Primary Kotlin application source root
│       │   │   ├── VivaanEnterpriseApplication.kt # Hilt Application subclass & WorkerFactory
│       │   │   ├── MainActivity.kt             # Single Activity entry point
│       │   │   ├── app/                        # Top-level composition & navigation
│       │   │   │   ├── VivaanEnterpriseApp.kt   # Root composable & MainViewModel
│       │   │   │   └── navigation/             # AppNavHost and Screen route definitions
│       │   │   ├── core/                       # Shared infrastructure & platform utilities
│       │   │   │   ├── common/                 # Result types, enums, time & ID providers
│       │   │   │   ├── database/               # Room Database, entities, DAOs, migrations, converters
│       │   │   │   ├── datastore/              # DataStore Preferences for UI & sync cursor
│       │   │   │   ├── designsystem/           # Material 3 theme tokens, components, previews
│       │   │   │   ├── firebase/               # Firebase Auth & Firestore client wrappers
│       │   │   │   ├── pdf/                    # Native Android PdfDocument generator
│       │   │   │   ├── sync/                   # WorkManager SyncScheduler and Worker
│       │   │   │   └── util/                   # Date & time formatting helpers
│       │   │   ├── data/                       # Data access, mapping & repository implementations
│       │   │   │   ├── di/                     # Hilt repository bindings
│       │   │   │   ├── local/mapper/           # Entity <-> Domain model mappers
│       │   │   │   ├── remote/                 # Firestore datasource, DTOs & mappers
│       │   │   │   └── repository/             # Repository implementations & initial seeders
│       │   │   ├── domain/                     # Pure Kotlin business rules & specifications
│       │   │   │   ├── model/                  # Pure domain entities, validations & error models
│       │   │   │   ├── pdf/                    # PDF generator domain interfaces
│       │   │   │   ├── repository/             # Repository interfaces & merge policies
│       │   │   │   ├── usecase/                # High-level business use cases
│       │   │   │   └── util/                   # Financial calculation engine & formatters
│       │   │   └── feature/                    # MVI feature presentation slices
│       │   │       ├── account/                # Client billing account ledger
│       │   │       ├── auth/                   # Email/password authentication
│       │   │       ├── client/                 # Client master list, detail & form
│       │   │       ├── dashboard/              # Home dashboard & KPI summaries
│       │   │       ├── document/               # Document history & shared components
│       │   │       ├── invoice/                # Tax Invoice builder & editor
│       │   │       ├── pdfviewer/              # On-demand PDF renderer & share sheet
│       │   │       ├── product/                # Product master catalog list, detail & form
│       │   │       └── purchaseorder/          # Purchase Order builder & editor
│       │   └── res/                            # Android app resources
│       │       ├── drawable/                   # Vector logos, icons & splash assets
│       │       ├── mipmap-anydpi-v26/          # Adaptive launcher icons
│       │       ├── values/                     # Strings, themes & splash styles
│       │       └── xml/                        # FileProvider paths configuration
│       ├── test/java/com/vivaanenterprise/app/ # Local JVM unit tests (JUnit 4, Coroutines Test)
│       └── androidTest/java/com/vivaanenterprise/app/ # On-device instrumentation tests (Room, Migration, PDF)
├── docs/                                       # Project specifications & architecture docs
│   ├── ARCHITECTURE.md                         # Architectural rules & design specifications
│   ├── DATA_MODEL.md                           # Entity schemas, fields & relational models
│   └── PDF_SPEC.md                             # Physical PDF layout & rendering rules
├── gradle/                                     # Gradle wrapper & version catalog
│   ├── libs.versions.toml                      # Single source of truth for library versions
│   └── wrapper/                                # Gradle wrapper binaries & properties
├── .planning/                                  # Project management, task tracking & codebase maps
│   └── codebase/                               # Generated codebase intelligence documents
├── build.gradle.kts                            # Root Gradle project configuration
├── settings.gradle.kts                         # Module inclusion & plugin management
├── gradle.properties                           # JVM arguments & AndroidX flags
├── AGENTS.md                                   # Comprehensive developer rules & engineering mandates
├── DESIGN.md                                   # Visual styling tokens, typography & color guidelines
└── README.md                                   # Project summary & getting started guide
```

## Directory Purposes

**`app/src/main/java/com/vivaanenterprise/app/app/`:**
- Purpose: Application-level composition and root navigation wiring.
- Contains: `VivaanEnterpriseApp.kt` (root Composable), `MainViewModel.kt` (startup seeding and auth state collection), and `navigation/` subfolder.
- Key files: `app/src/main/java/com/vivaanenterprise/app/app/navigation/AppNavHost.kt`, `app/src/main/java/com/vivaanenterprise/app/app/navigation/Screen.kt`.

**`app/src/main/java/com/vivaanenterprise/app/core/`:**
- Purpose: Low-level reusable system and platform infrastructure modules.
- Contains:
  - `database/`: Room database setup, entity definitions, DAOs, migrations, and type converters.
  - `datastore/`: Jetpack DataStore Preferences for local UI settings and sync timestamps.
  - `designsystem/`: Material 3 theme definitions (`Theme.kt`, `Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`) and atomic reusable UI components (`AppButton.kt`, `AppCard.kt`, `AppTextField.kt`, `AppScaffold.kt`).
  - `firebase/`: Firebase Authentication and Firestore client wrappers and Hilt modules.
  - `pdf/`: Native Android `PdfDocument` graphics canvas drawing engine and formatting utilities.
  - `sync/`: Background synchronization workers and scheduling infrastructure (`SyncScheduler.kt`, `FirebaseSyncWorker.kt`).
- Key files: `app/src/main/java/com/vivaanenterprise/app/core/database/VivaanEnterpriseDatabase.kt`, `app/src/main/java/com/vivaanenterprise/app/core/pdf/AndroidBusinessDocumentPdfGenerator.kt`.

**`app/src/main/java/com/vivaanenterprise/app/data/`:**
- Purpose: Data layer implementing repository contracts, mediating between local Room storage and Firestore remote synchronization.
- Contains:
  - `local/mapper/`: Bidirectional converters between Room entities and pure Domain models.
  - `remote/`: Remote DTOs (`RemoteDtos.kt`), DTO mappers (`DtoMappers.kt`), and Firestore datasource (`FirestoreSyncDataSource.kt`).
  - `repository/`: Concrete repository implementations (`DocumentRepositoryImpl.kt`, `ClientRepositoryImpl.kt`, `SyncRepositoryImpl.kt`, `AuthRepositoryImpl.kt`) and initial seeders (`ProductSeeder.kt`, `BusinessProfileSeeder.kt`).
- Key files: `app/src/main/java/com/vivaanenterprise/app/data/repository/DocumentRepositoryImpl.kt`, `app/src/main/java/com/vivaanenterprise/app/data/repository/SyncRepositoryImpl.kt`.

**`app/src/main/java/com/vivaanenterprise/app/domain/`:**
- Purpose: Pure Kotlin business logic, models, and contracts independent of the Android SDK.
- Contains:
  - `model/`: Domain data representations (`BusinessDocument`, `Client`, `Product`, `TaxTreatment`, `DocumentFinalizationInput`).
  - `repository/`: Repository interfaces (`DocumentRepository`, `ClientRepository`, `ProductRepository`, `SyncRepository`) and merge policies (`SyncMergePolicy`).
  - `usecase/`: Focused business use cases (`GenerateBusinessDocumentPdfUseCase`).
  - `util/`: Deterministic financial calculation engine (`DocumentCalculator.kt`), currency formatting (`IndianCurrencyFormatter.kt`), and financial year resolver (`FinancialYearResolver.kt`).
- Key files: `app/src/main/java/com/vivaanenterprise/app/domain/util/DocumentCalculator.kt`, `app/src/main/java/com/vivaanenterprise/app/domain/model/BusinessDocumentModels.kt`.

**`app/src/main/java/com/vivaanenterprise/app/feature/`:**
- Purpose: Modular feature slices implementing user flows via MVI.
- Contains:
  - `client/`: Master client directory with list, detail, and add/edit form screens.
  - `product/`: Product catalog management with list, detail, and add/edit form screens.
  - `invoice/`: Tax Invoice generation, line item management, and real-time calculation.
  - `purchaseorder/`: Purchase Order generation with supplier and line item handling.
  - `document/`: Unified document history list, filtering, and detail inspection.
  - `account/`: Client ledger account and balance receivable tracking.
  - `pdfviewer/`: Dynamic in-app PDF rendering and platform file export/share.
  - `dashboard/`: Overview metrics, recent invoices, and shortcut actions.
  - `auth/`: User sign-in screen and authentication state management.
- Key files: `app/src/main/java/com/vivaanenterprise/app/feature/invoice/presentation/InvoiceScreen.kt`, `app/src/main/java/com/vivaanenterprise/app/feature/client/presentation/list/ClientListViewModel.kt`.

**`docs/`:**
- Purpose: Project architectural definitions, database schema contracts, and PDF visual specifications.
- Key files: `docs/ARCHITECTURE.md`, `docs/DATA_MODEL.md`, `docs/PDF_SPEC.md`.

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/vivaanenterprise/app/VivaanEnterpriseApplication.kt`: Application class and WorkManager configuration.
- `app/src/main/java/com/vivaanenterprise/app/MainActivity.kt`: Main launcher activity.
- `app/src/main/java/com/vivaanenterprise/app/app/navigation/AppNavHost.kt`: Top-level Compose navigation graph.

**Configuration:**
- `gradle/libs.versions.toml`: Authoritative version catalog for dependencies and plugins.
- `app/build.gradle.kts`: Android SDK versions, compile options, build features, and dependency blocks.
- `app/proguard-rules.pro`: Optimization and reflection rules for Room, Hilt, and Firebase.
- `AGENTS.md`: Mandatory engineering rules, financial precision mandates, and workflow constraints.
- `DESIGN.md`: Material 3 design system tokens and theme documentation.

**Core Logic:**
- `app/src/main/java/com/vivaanenterprise/app/domain/util/DocumentCalculator.kt`: Single deterministic engine for financial and GST tax calculations.
- `app/src/main/java/com/vivaanenterprise/app/data/repository/DocumentRepositoryImpl.kt`: Atomic document creation, finalization, snapshot freezing, and ledger creation.
- `app/src/main/java/com/vivaanenterprise/app/data/repository/SyncRepositoryImpl.kt`: Offline synchronization engine.
- `app/src/main/java/com/vivaanenterprise/app/core/pdf/AndroidBusinessDocumentPdfGenerator.kt`: Native PDF rendering implementation.

**Testing:**
- Unit tests: `app/src/test/java/com/vivaanenterprise/app/`
- Instrumentation tests: `app/src/androidTest/java/com/vivaanenterprise/app/`

## Naming Conventions

### Files
- **Kotlin Classes / Interfaces**: PascalCase matching the declaration (e.g., `DocumentRepository.kt`, `ClientEntity.kt`).
- **Composable Screens**: Named with feature and `Screen` suffix (e.g., `ClientListScreen.kt`, `InvoiceScreen.kt`).
- **MVI Contracts**: Named with feature and `Contracts` suffix (e.g., `ClientListContracts.kt`, `ProductFormContracts.kt`) or `UiState` suffix (e.g., `InvoiceUiState.kt`).
- **ViewModels**: Named with feature and `ViewModel` suffix (e.g., `ClientListViewModel.kt`, `DashboardViewModel.kt`).
- **Room Entities**: Named with domain concept and `Entity` suffix (e.g., `ClientEntity.kt`, `BusinessDocumentEntity.kt`).
- **Room DAOs**: Named with domain concept and `Dao` suffix (e.g., `ClientDao.kt`, `BusinessDocumentDao.kt`).
- **Mappers**: Named with source/target and `Mappers` suffix (e.g., `ClientMappers.kt`, `DtoMappers.kt`).

### Directories
- Package directories: Lowercase, single-word or concatenated lowercase without underscores (e.g., `designsystem/`, `pdfviewer/`, `purchaseorder/`).
- Feature presentation subpackages:
  - `presentation/list/`: Listing views
  - `presentation/detail/`: Detail inspection views
  - `presentation/form/`: Creation / edit forms

### Components & Routes
- **Route Composables**: Named `<Feature>Route` (e.g., `ClientListRoute`, `InvoiceRoute`). Accepts navigation lambdas, obtains ViewModel via `hiltViewModel()`, and collects state.
- **Screen Composables**: Named `<Feature>Screen` (e.g., `ClientListScreen`, `InvoiceScreen`). Pure stateless composable accepting immutable `UiState` and `(UiIntent) -> Unit`.
- **Reusable Core Components**: Prefixed with `App` (e.g., `AppButton`, `AppCard`, `AppTextField`, `AppScaffold`, `AppTopBar`).

## Where to Add New Code

### Adding a New Feature Flow (e.g., Reports / Analytics)
- **Domain Models & Repositories**:
  - Model: `app/src/main/java/com/vivaanenterprise/app/domain/model/<Name>.kt`
  - Repository interface: `app/src/main/java/com/vivaanenterprise/app/domain/repository/<Name>Repository.kt`
- **Data Implementation**:
  - Repository implementation: `app/src/main/java/com/vivaanenterprise/app/data/repository/<Name>RepositoryImpl.kt`
  - Dependency injection binding: `app/src/main/java/com/vivaanenterprise/app/data/di/RepositoryModule.kt`
- **Presentation (MVI)**:
  - Package: `app/src/main/java/com/vivaanenterprise/app/feature/<feature>/presentation/`
  - Contracts: `<Feature>Contracts.kt` (defining `UiState`, `UiIntent`, and `UiEffect`)
  - ViewModel: `<Feature>ViewModel.kt` annotated with `@HiltViewModel`
  - Screen & Route: `<Feature>Screen.kt` (containing `<Feature>Route` and stateless `<Feature>Screen`)
- **Navigation**:
  - Add route entry to `Screen` sealed class: `app/src/main/java/com/vivaanenterprise/app/app/navigation/Screen.kt`
  - Register composable destination in: `app/src/main/java/com/vivaanenterprise/app/app/navigation/AppNavHost.kt`
- **Unit Tests**:
  - ViewModel tests: `app/src/test/java/com/vivaanenterprise/app/feature/<feature>/presentation/<Feature>ViewModelTest.kt`

### Adding a New Database Entity / Table
1. Define Entity in `app/src/main/java/com/vivaanenterprise/app/core/database/entity/<Name>Entity.kt`.
2. Define DAO in `app/src/main/java/com/vivaanenterprise/app/core/database/dao/<Name>Dao.kt`.
3. Register Entity and DAO in `VivaanEnterpriseDatabase.kt`.
4. Bump database version in `VivaanEnterpriseDatabase.kt` and create migration in `DatabaseMigrations.kt`.
5. Add DTO model and mappers in `data/remote/model/RemoteDtos.kt` and `data/remote/mapper/DtoMappers.kt`.
6. Update `SyncRepositoryImpl.kt` push and pull sequences to respect foreign key dependency ordering.
7. Run `./gradlew testDebugUnitTest` and on-device migration tests.

### Adding a New Reusable UI Component
- Add atomic design system components directly in `app/src/main/java/com/vivaanenterprise/app/core/designsystem/component/`.
- Must adhere strictly to semantic tokens defined in `Theme.kt` and `Spacing.kt`. Never use hardcoded colors or dimensions.

### Adding a Utility or Helper
- Financial / currency calculation: `app/src/main/java/com/vivaanenterprise/app/domain/util/`
- General formatting / UI helpers: `app/src/main/java/com/vivaanenterprise/app/core/util/`

## Special Directories

**`app/schemas/`:**
- Purpose: Contains auto-generated JSON schema files produced by Room's annotation processor via KSP.
- Generated: Yes (by KSP during compilation).
- Committed: **Yes**. Must be committed to version control to enable migration testing against past database schemas via `androidx.room.testing.MigrationTestHelper`.

**`docs/`:**
- Purpose: High-level architectural, data model, and PDF specifications.
- Generated: No (human-authored specifications).
- Committed: **Yes**.

**`.planning/`:**
- Purpose: Project roadmap, milestone specifications, task plans, and codebase intelligence documents.
- Generated: Yes (orchestrated by GSD agent tools).
- Committed: **Yes**.

---

*Structure analysis: 2026-11-17*
