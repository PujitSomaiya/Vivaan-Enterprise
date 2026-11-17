# Coding Conventions
**Analysis Date:** 2026-11-17

## Naming Patterns

**Files:**
- Kotlin source files: UpperCamelCase matching the primary declaration (e.g. `ClientListScreen.kt`, `InvoiceViewModel.kt`, `ExactCurrencyParser.kt`).
- Screen & Route composables: `<Feature>Screen.kt` for the stateless UI component and `<Feature>Route` for the stateful navigation boundary (e.g. `app/src/main/java/com/vivaanenterprise/app/feature/client/presentation/list/ClientListScreen.kt`).
- MVI Contracts: `<Feature>Contracts.kt` or `<Feature>UiState.kt` (e.g. `ClientFormContracts.kt`, `InvoiceUiState.kt`).
- Database entities: `<EntityName>Entity.kt` (e.g. `app/src/main/java/com/vivaanenterprise/app/core/database/entity/BusinessDocumentEntity.kt`).
- DAOs: `<EntityName>Dao.kt` (e.g. `app/src/main/java/com/vivaanenterprise/app/core/database/dao/BusinessDocumentDao.kt`).
- Repositories: Interface `<Name>Repository.kt` in `domain/repository/` and implementation `<Name>RepositoryImpl.kt` in `data/repository/` (e.g. `app/src/main/java/com/vivaanenterprise/app/domain/repository/DocumentRepository.kt` and `app/src/main/java/com/vivaanenterprise/app/data/repository/DocumentRepositoryImpl.kt`).
- Unit tests: `<TargetClass>Test.kt` in `app/src/test/` (e.g. `app/src/test/java/com/vivaanenterprise/app/domain/util/DocumentCalculatorTest.kt`).
- Instrumentation tests: `<TargetClass>Test.kt` or `<Component>InstrumentationTest.kt` in `app/src/androidTest/` (e.g. `app/src/androidTest/java/com/vivaanenterprise/app/core/database/dao/Step16DaoInstrumentationTest.kt`).

**Functions:**
- Standard functions and methods: `camelCase` starting with a verb (e.g. `calculate()`, `observeClients()`, `suggestDocumentNumber()`, `parseToPaise()`).
- Jetpack Compose components: `PascalCase` nouns or noun phrases returning `Unit` (e.g. `ClientListRoute`, `ClientListScreen`, `AppScaffold`, `AppCard`).
- Intent handler functions in ViewModels: `onIntent(intent: UiIntent)` accepting the sealed intent hierarchy.
- Type converters / mappers: Prefix with `toDomain()` or `toEntity()` extension functions (e.g. `app/src/main/java/com/vivaanenterprise/app/data/local/mapper/DocumentMappers.kt`).

**Variables:**
- Read-only variables & properties: `camelCase` (e.g. `grandTotalPaise`, `taxableAmountPaise`, `sellerStateCode`).
- Mutable backing properties for StateFlow / Channel: Underscore prefix in `camelCase` (e.g. `_uiState`, `_searchQuery`, `_uiEffect`).
- Constants / Enum entries / Basis point multipliers: `SCREAMING_SNAKE_CASE` (e.g. `TaxTreatment.INTRA_STATE`, `DocumentStatus.FINALIZED`, `DocumentType.TAX_INVOICE`).
- Financial amounts: MUST always end in `Paise` (e.g. `ratePaise`, `taxableAmountPaise`, `cgstAmountPaise`, `grandTotalPaise`) to unambiguously identify integer minor units.
- Tax rates: MUST always end in `BasisPoints` (e.g. `defaultGstRateBasisPoints`, `igstRateBasisPoints`) representing 1/100th of 1% (1800 = 18.00%).

**Types:**
- Classes, interfaces, sealed interfaces, and enums: `PascalCase` (e.g. `DocumentCalculator`, `ExactCurrencyParser`, `CalculationError`, `TaxTreatment`).
- MVI State classes: `<Feature>UiState` immutable data class (e.g. `ClientListUiState`, `InvoiceUiState`).
- MVI Intent contracts: `<Feature>UiIntent` sealed interface with `data class` or `data object` actions (e.g. `ClientListUiIntent.SearchQueryChanged`, `ClientListUiIntent.AddClientClicked`).
- MVI Effect contracts: `<Feature>UiEffect` sealed interface for one-shot side-effects (e.g. `ClientListUiEffect.NavigateToDetail`, `InvoiceUiEffect.ShowSnackbar`).

---

## Code Style

**Formatting:**
- Official Kotlin Coding Conventions and Android Kotlin Style Guide.
- Indentation: 4 spaces (no tabs).
- Maximum line length: Soft wrap at 120 characters.
- File encoding: UTF-8.
- Kotlin version: 2.3.10 with JVM target 17 (`compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }` in `app/build.gradle.kts`).

**Linting:**
- Android Gradle Plugin Built-in Linter: Configured via AGP 9.4.0 (`./gradlew lintDebug`).
- Mandatory Verification Gate: Verification script requires `./gradlew lintDebug` to exit with code 0 before code completion.
- Compiler warnings treated as errors during release verification: Room schema export enforced via `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` in `app/build.gradle.kts`.

---

## Import Organization

**Order:**
1. Android & AndroidX frameworks (`android.graphics.*`, `androidx.compose.*`, `androidx.lifecycle.*`, `androidx.room.*`)
2. Vivaan Enterprise Core modules (`com.vivaanenterprise.app.core.*`)
3. Vivaan Enterprise Domain & Data layers (`com.vivaanenterprise.app.domain.*`, `com.vivaanenterprise.app.data.*`)
4. Vivaan Enterprise Feature layers (`com.vivaanenterprise.app.feature.*`)
5. Dependency Injection / Dagger Hilt (`dagger.hilt.android.*`, `javax.inject.*`)
6. Kotlin Standard Library & Coroutines (`kotlinx.coroutines.*`, `kotlin.*`)
7. Java standard library (`java.io.*`, `java.util.*`)

**Path Aliases:**
- Native Gradle module pathing: No custom Kotlin compiler path plugins. Standard package structures under `com.vivaanenterprise.app.*`.
- Version catalog aliases: Managed in `gradle/libs.versions.toml` (e.g. `libs.androidx.compose.material3`, `libs.androidx.room.ktx`).

---

## Error Handling

**Patterns:**
- **Typed Sealed Interfaces for Domain Errors**: Never throw generic runtime exceptions for anticipated business failures. Domain operations return typed sealed interfaces or Kotlin's `Result<T>` wrapping typed exceptions:
  ```kotlin
  // Reference: app/src/main/java/com/vivaanenterprise/app/domain/model/CalculationError.kt
  sealed interface CalculationError {
      data class InvalidStateCode(val field: String) : CalculationError
      data object InvalidQuantity : CalculationError
      data object InvalidRate : CalculationError
      data object InvalidGstRate : CalculationError
      data object ArithmeticOverflow : CalculationError
      data object NoLineItems : CalculationError
  }
  
  class CalculationException(val error: CalculationError) : Exception(error.toString())
  ```
- **Document Finalization Results**:
  ```kotlin
  // Reference: app/src/main/java/com/vivaanenterprise/app/domain/model/DocumentValidation.kt
  sealed interface DocumentFinalizationResult {
      data class Success(val document: BusinessDocument) : DocumentFinalizationResult
      data class Invalid(val errors: List<DocumentValidationError>) : DocumentFinalizationResult
      data class Failure(val throwable: Throwable) : DocumentFinalizationResult
  }
  ```
- **Arithmetic Overflow Protection**: Never allow silent integer overflow or wrap-around. Use `Math.multiplyExact` and `Math.addExact`, catching `ArithmeticException` and returning domain error `CalculationError.ArithmeticOverflow`:
  ```kotlin
  // Reference: app/src/main/java/com/vivaanenterprise/app/domain/util/DocumentCalculator.kt:133-143
  val taxableAmountPaise = try {
      Math.multiplyExact(line.quantity, line.ratePaise)
  } catch (e: ArithmeticException) {
      return null
  }
  ```
- **Currency String Parsing Safety**:
  ```kotlin
  // Reference: app/src/main/java/com/vivaanenterprise/app/domain/util/ExactCurrencyParser.kt:23-58
  val rupeePaise = Math.multiplyExact(rupees, 100L)
  Math.addExact(rupeePaise, paiseInt.toLong())
  ```

---

## Logging

**Framework:**
- Native `android.util.Log` or Timber: **STRICTLY RESTRICTED / NOT USED**.

**Patterns:**
- Per project rule in `AGENTS.md` §17: "No credentials, API keys, or private client financial data in version control or plain-text logcat."
- Production classes contain zero arbitrary `Log.d` or `println` calls.
- Failures and diagnostics are captured through structured `UiEffect` emissions, typed `Result.failure`, WorkManager `Result.retry()`/`Result.failure()`, or test assertions.

---

## Comments

**When to Comment:**
- Mandatory KDoc on domain utilities, calculation engines, and synchronization policies explaining mathematical rules, legal GST rounding, and boundary conditions.
- Document calculation verification formulas in class headers (e.g. `taxable = quantity × ratePaise`, `half-up rounding = (taxable × rate + 5_000) / 10_000`).
- No commented-out dead code.

**JSDoc/TSDoc / KDoc:**
- Use standard KDoc (`/** ... */`) with `@param`, `@return`, and `@see` tags on domain classes, use cases, and repositories.

---

## Function Design

**Size:**
- Single responsibility principle: Small, composable functions.
- Complex screen composables broken into distinct modular sub-composables (e.g. `ClientItemRow`, `InvoiceLineItemCard`, `SummaryCard`).

**Parameters:**
- Screen Composables: MUST take immutable `uiState: UiState`, event lambda `onIntent: (UiIntent) -> Unit`, and navigation lambdas. NEVER pass ViewModels down into composables.
- Route Composables: Act as DI and navigation anchors, retrieving the ViewModel with `hiltViewModel()` and collecting state.

**Return Values:**
- Domain queries: Return `Flow<T>` for observable database state or `suspend fun` returning models or `Result<T>`.
- Composables: Always return `Unit`.

---

## Module Design

**Exports:**
- Feature presentation packages encapsulate:
  - `<Feature>Route` & `<Feature>Screen` (UI)
  - `<Feature>ViewModel` (State holder)
  - `<Feature>Contracts` (`UiState`, `UiIntent`, `UiEffect`)
- Domain layer is pure Kotlin (independent of Android UI and Room annotations).
- Data layer implements domain repositories and provides local Room DAOs and remote data sources.

**Barrel Files:**
- Kotlin does not use JavaScript-style barrel files. Explicit imports are used throughout.

---

## Architectural & Domain Conventions

### 1. Financial Precision Discipline
Floating-point arithmetic is strictly prohibited for monetary values:
- **`Double` and `Float` are strictly banned** for rates, taxable amounts, GST calculations, line item totals, and grand totals due to IEEE 754 binary representation errors.
- **Exact Minor Unit (`Long` paise)**: All monetary figures are stored and processed as exact integer minor units (`Long` paise where ₹ 1.00 = `100L`, ₹ 2,500.00 = `250000L`).
- **GST Rates in Basis Points**: GST rates are stored as exact integers representing basis points (`Int` where 10,000 bp = 100%, 1800 bp = 18.00%, 500 bp = 5.00%).
- **Deterministic Single Authority (`DocumentCalculator`)**:
  - Located at `app/src/main/java/com/vivaanenterprise/app/domain/util/DocumentCalculator.kt`.
  - Half-up rounding to the nearest paise using integer arithmetic:
    `roundedGstPaise = (taxableAmountPaise * rateBasisPoints + 5_000) / 10_000`
  - Intra-state tax split guarantee:
    `cgstRate = totalGstRate / 2`
    `sgstRate = totalGstRate - cgstRate` (guarantees `cgstRate + sgstRate == totalGstRate` exactly).
  - UI, persistence, and PDF generator all consume `DocumentCalculationResult` identically.
- **Safe Parsing (`ExactCurrencyParser`)**:
  - Located at `app/src/main/java/com/vivaanenterprise/app/domain/util/ExactCurrencyParser.kt`.
  - Converts user decimal text inputs into `Long` paise directly without floating-point conversion.

### 2. Jetpack Compose UI Rules
- **Stateless Screens**: Screen composables (`<Feature>Screen`) are strictly stateless, accepting immutable `UiState` and emitting `UiIntent` lambdas.
- **Route Boundaries**: ViewModel scoping happens exclusively at `<Feature>Route` (e.g. `ClientListRoute` at `app/src/main/java/com/vivaanenterprise/app/feature/client/presentation/list/ClientListScreen.kt:44`).
- **State Collection**: Collect flows in Compose using `collectAsStateWithLifecycle()`:
  ```kotlin
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  ```
- **Stable Keys in Lazy Layouts**: Dynamic lists in `LazyColumn`/`LazyRow` MUST use unique entity IDs as stable keys, NEVER the list index:
  ```kotlin
  // Reference: app/src/main/java/com/vivaanenterprise/app/feature/client/presentation/list/ClientListScreen.kt:148-151
  items(
      items = uiState.clients,
      key = { client -> client.id }
  ) { client ->
      ClientItemRow(client = client, ...)
  }
  ```
- **Design System Tokens Only**: Hardcoded hex colors and arbitrary layout dimensions are forbidden. All components use semantic tokens defined in:
  - Colors: `AppTheme.colorScheme` / `Theme.kt` (`app/src/main/java/com/vivaanenterprise/app/core/designsystem/theme/Color.kt`)
  - Spacing: `AppTheme.spacing` (`app/src/main/java/com/vivaanenterprise/app/core/designsystem/theme/Spacing.kt`)
  - Typography: `AppTheme.typography` (`app/src/main/java/com/vivaanenterprise/app/core/designsystem/theme/Type.kt`)
  - Shapes: `AppTheme.shapes` (`app/src/main/java/com/vivaanenterprise/app/core/designsystem/theme/Shape.kt`)
- **Complete Visual State Coverage**: Every screen must explicitly handle all 5 states:
  1. `Loading` (`AppLoadingState`)
  2. `Content` (Populated lazy lists/forms)
  3. `Empty` (`AppEmptyState` with descriptive copy)
  4. `Validation / Error` (Field-level errors and error dialogs)
  5. `Offline / Sync Status` (`SyncStatus` badges: `SYNCED`, `PENDING`, `FAILED`)

### 3. Coroutines & Flow Discipline
- **Structured Concurrency**: All coroutines must be launched within lifecycle-owned scopes (`viewModelScope` in ViewModels). `GlobalScope` is strictly prohibited.
- **Rethrowing `CancellationException`**: Broad `try-catch` blocks must NEVER swallow `CancellationException`. It must be explicitly caught and rethrown to prevent coroutine hierarchy breakage:
  ```kotlin
  // Reference: app/src/main/java/com/vivaanenterprise/app/feature/invoice/presentation/InvoiceViewModel.kt:573-575
  try {
      // suspend work
  } catch (ce: CancellationException) {
      throw ce
  } catch (e: Exception) {
      // handle application error
  }
  ```
- **Lifecycle-Safe Subscription**: Derive state using `stateIn` with `SharingStarted.WhileSubscribed(5_000)`:
  ```kotlin
  // Reference: app/src/main/java/com/vivaanenterprise/app/feature/client/presentation/list/ClientListViewModel.kt:44-48
  val uiState: StateFlow<ClientListUiState> = combine(...) { ... }
      .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000),
          initialValue = ClientListUiState(isLoading = true)
      )
  ```
- **Dispatchers**:
  - `Dispatchers.Main` for UI state coordination.
  - `Dispatchers.IO` for disk, database, and background synchronization tasks.
  - `Dispatchers.Default` for CPU-intensive calculation and PDF layout generation.

### 4. Historical Immutability & Snapshots
- When an invoice or purchase order transitions to `DocumentStatus.FINALIZED`, master data is frozen into `SellerSnapshot` and `ClientSnapshot` embedded entities (`app/src/main/java/com/vivaanenterprise/app/domain/model/ClientSnapshot.kt`).
- Subsequent edits, renaming, or deletions in the Client or Product catalog NEVER mutate finalized documents.
- PDF generation renders directly from the saved document snapshot, never from active master tables.

---

*Convention analysis: 2026-11-17*
