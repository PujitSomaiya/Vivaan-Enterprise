# Testing Patterns
**Analysis Date:** 2026-11-17

## Test Framework

**Runner:**
- **Pure JVM Unit Tests**: JUnit 4.13.2 via Gradle Test Runner
- **Android Instrumentation Tests**: `androidx.test.runner.AndroidJUnitRunner` via AndroidX Test 1.3.0
- Config: `app/build.gradle.kts` (lines 20, 105-109) and `gradle/libs.versions.toml`

**Assertion Library:**
- Standard JUnit assertions (`org.junit.Assert.assertEquals`, `assertTrue`, `assertFalse`, `assertNotNull`, `assertNull`)
- Kotlin assertions (`kotlin.test.*`)

**Run Commands:**
```bash
./gradlew testDebugUnitTest              # Run all local JVM unit tests
./gradlew connectedAndroidTest           # Run all on-device / emulator instrumentation tests
./gradlew testDebugUnitTest --info       # Run unit tests with detailed console logs
./gradlew testDebugUnitTest --continue   # Run unit tests continuing past failures
```

---

## Test File Organization

**Location:**
- Separate test roots under `app/src/`:
  - `app/src/test/java/com/vivaanenterprise/app/`: Local JVM unit tests (no Android runtime dependency, runs fast).
  - `app/src/androidTest/java/com/vivaanenterprise/app/`: Android instrumentation tests (requires emulator or device for Room SQLite, native PDF rendering, and Android context).
  - `app/schemas/`: Exported Room schema JSONs consumed by `RoomDatabaseMigrationTest` via `sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")`.

**Naming:**
- Unit test files end with `Test.kt` (e.g. `DocumentCalculatorTest.kt`, `InvoiceViewModelTest.kt`, `ClientRepositoryAndValidationTest.kt`).
- Instrumentation test files end with `Test.kt` or `InstrumentationTest.kt` (e.g. `PersistenceTest.kt`, `Step16DaoInstrumentationTest.kt`, `RoomDatabaseMigrationTest.kt`).

**Structure:**
```
app/src/
├── test/java/com/vivaanenterprise/app/
│   ├── app/                                 # App-level routing and root viewmodel tests
│   │   └── MainViewModelTest.kt
│   ├── core/pdf/                            # PDF formatting and coordinate tests
│   │   └── PdfFormattingUtilsTest.kt
│   ├── data/
│   │   ├── remote/mapper/                   # DTO mapping and merge policy tests
│   │   │   └── SyncMappingAndMergePolicyTest.kt
│   │   └── repository/                      # Local repository and database seeder tests
│   │       ├── BusinessProfileRepositoryTest.kt
│   │       ├── BusinessProfileSeederTest.kt
│   │       ├── ClientAccountRepositoryTest.kt
│   │       └── DocumentRepositoryTest.kt
│   ├── domain/util/                         # Domain math, currency parsing, and calculation engine tests
│   │   ├── DocumentCalculatorTest.kt
│   │   ├── ExactCurrencyParserTest.kt
│   │   ├── FinancialYearResolverTest.kt
│   │   └── IndianCurrencyFormatterTest.kt
│   └── feature/                             # MVI ViewModels, form validation, and immutability tests
│       ├── account/ClientAccountViewModelTest.kt
│       ├── auth/presentation/LoginViewModelTest.kt
│       ├── client/
│       │   ├── ClientRepositoryAndValidationTest.kt
│       │   └── presentation/ClientViewModelsTest.kt
│       ├── dashboard/DashboardViewModelTest.kt
│       ├── document/history/
│       │   ├── DocumentDetailViewModelTest.kt
│       │   ├── DocumentsViewModelTest.kt
│       │   └── HistoricalDocumentImmutabilityTest.kt
│       ├── invoice/presentation/InvoiceViewModelTest.kt
│       ├── pdfviewer/
│       │   ├── presentation/PdfViewerViewModelTest.kt
│       │   └── service/BusinessDocumentPdfFileNameFormatterTest.kt
│       ├── product/
│       │   ├── model/ProductGstUtilsTest.kt
│       │   ├── presentation/ProductViewModelsTest.kt
│       │   └── ProductRepositoryAndSeederTest.kt
│       └── purchaseorder/presentation/PurchaseOrderViewModelTest.kt
└── androidTest/java/com/vivaanenterprise/app/
    ├── core/
    │   ├── database/
    │   │   ├── PersistenceTest.kt           # Room entity relationships, Long paise persistence
    │   │   ├── dao/
    │   │   │   └── Step16DaoInstrumentationTest.kt # In-memory Room aggregations & queries
    │   │   └── migration/
    │   │       └── RoomDatabaseMigrationTest.kt    # SQLite migration v1 -> v2 verification
    │   ├── firebase/
    │   │   └── FirebaseInitializationSmokeTest.kt
    │   └── pdf/
    │       └── AndroidBusinessDocumentPdfGeneratorTest.kt # Real PdfDocument rendering & verification
    ├── data/repository/
    │   └── SyncRepositoryTest.kt            # Sync push/pull engine with simulated remote
    └── feature/pdfviewer/service/
        └── PdfServicesTest.kt               # MediaStore export and FileProvider sharing
```

---

## Test Structure

### 1. Pure JVM Unit Test Suite (ViewModel with Coroutines)
```kotlin
// Reference: app/src/test/java/com/vivaanenterprise/app/feature/invoice/presentation/InvoiceViewModelTest.kt
@OptIn(ExperimentalCoroutinesApi::class)
class InvoiceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeClientRepository: FakeClientRepository
    private lateinit var fakeProductRepository: FakeProductRepository
    private lateinit var fakeDocumentRepository: FakeDocumentRepository
    private lateinit var fakeProfileRepository: FakeBusinessProfileRepository
    private lateinit var calculator: DocumentCalculator
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: InvoiceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeClientRepository = FakeClientRepository(listOf(testClient))
        fakeProductRepository = FakeProductRepository(listOf(testProduct))
        fakeDocumentRepository = FakeDocumentRepository()
        fakeProfileRepository = FakeBusinessProfileRepository(testProfile)
        calculator = DocumentCalculator(IndianCurrencyFormatter())
        savedStateHandle = SavedStateHandle()

        viewModel = InvoiceViewModel(
            clientRepository = fakeClientRepository,
            productRepository = fakeProductRepository,
            documentRepository = fakeDocumentRepository,
            profileRepository = fakeProfileRepository,
            calculator = calculator,
            savedStateHandle = savedStateHandle
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateLoadsClientsAndCalculatesDefaults() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.availableClients.size)
        assertEquals(1, state.availableProducts.size)
        assertFalse(state.isInitialLoading)
    }
}
```

### 2. Instrumentation Test Suite (In-Memory Room Database)
```kotlin
// Reference: app/src/androidTest/java/com/vivaanenterprise/app/core/database/PersistenceTest.kt
@RunWith(AndroidJUnit4::class)
class PersistenceTest {

    private lateinit var db: VivaanEnterpriseDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VivaanEnterpriseDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testDocumentAndLineItemRelationshipAndPaiseRounding() = runBlocking {
        val doc = BusinessDocumentEntity(
            id = "doc-uuid-1",
            documentType = DocumentType.TAX_INVOICE,
            documentNumber = "VE/01/2026-27",
            documentDate = 1000L,
            status = DocumentStatus.DRAFT,
            clientId = "client-uuid-1",
            taxableAmountPaise = 250000L,
            grandTotalPaise = 295000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        db.businessDocumentDao().upsert(doc)

        val retrieved = db.businessDocumentDao().getById("doc-uuid-1")
        assertNotNull(retrieved)
        assertEquals(250000L, retrieved?.taxableAmountPaise)
        assertEquals(295000L, retrieved?.grandTotalPaise)
    }
}
```

---

## Mocking

**Framework:**
- **No Mockito or MockK**: The project deliberately does not use reflection-based mocking frameworks.
- Rationale: Hand-rolled Fakes provide superior type-safety, maintainability across refactorings, instant test execution speed, and zero reflection overhead or classloader issues on modern Kotlin/JVM versions.

**Patterns:**
- **Hand-Rolled In-Memory Fakes**: Implement domain interfaces with state backed by `MutableStateFlow` or in-memory `List` collections:
  ```kotlin
  // Reference: app/src/test/java/com/vivaanenterprise/app/feature/document/history/HistoricalDocumentImmutabilityTest.kt:98-123
  private class FakeDocumentRepo(initialDocs: List<BusinessDocument>) : DocumentRepository {
      val flow = MutableStateFlow(initialDocs)
      override fun observeAllDocuments(): Flow<List<BusinessDocument>> = flow
      override fun observeDocumentById(id: String): Flow<BusinessDocument?> = 
          flow.map { docs -> docs.find { it.id == id } }
      override suspend fun getDocumentById(id: String): BusinessDocument? = 
          flow.value.find { it.id == id }
      // ...
  }
  ```

**What to Mock / Fake:**
- Repository interfaces (`ClientRepository`, `ProductRepository`, `DocumentRepository`, `BusinessProfileRepository`) when testing ViewModels or UseCases.
- External systems / remote sync targets (`FirestoreSyncDataSource`).

**What NOT to Mock:**
- **Domain logic & calculators**: `DocumentCalculator`, `IndianCurrencyFormatter`, `ExactCurrencyParser`, and `FinancialYearResolver` MUST be tested with real instances to guarantee arithmetic correctness.
- **Room DAOs**: Use `Room.inMemoryDatabaseBuilder().allowMainThreadQueries().build()` instead of mocking DAOs.
- **Android Parcelable / Snapshots**: Test with real data class instances.

---

## Fixtures and Factories

**Test Data Patterns:**
- Canonical fixtures match real business records (e.g. `VE/06` and `VE/05` tax invoices):
  ```kotlin
  // Reference: app/src/test/java/com/vivaanenterprise/app/domain/util/DocumentCalculatorTest.kt:37-52
  private fun input(
      sellerState: String = "24",
      supplyState: String = "33",
      vararg lines: Triple<Long, Long, Int> // quantity, ratePaise, gstBasisPoints
  ) = DocumentCalculationInput(
      sellerStateCode = sellerState,
      placeOfSupplyStateCode = supplyState,
      lines = lines.mapIndexed { i, (qty, rate, gst) ->
          DocumentCalculationInput.LineInput(
              id = "line-$i",
              quantity = qty,
              ratePaise = rate,
              gstRateBasisPoints = gst
          )
      }
  )
  ```
- Reusable test snapshots for PDF and repository testing:
  ```kotlin
  // Reference: app/src/androidTest/java/com/vivaanenterprise/app/core/pdf/AndroidBusinessDocumentPdfGeneratorTest.kt:32-77
  private val sampleSeller = SellerSnapshot(
      businessName = "VIVAAN ENTERPRISE",
      addressLine1 = "NEAR SHALIBHADRANIVAS, OPP. SIDDHIVINAYAK HOUSE",
      addressLine2 = "STREET NO. 4, MAHATMA GANDHI ROAD, JORAWAR NAGAR",
      cityStatePincode = "SURENDRANAGAR, GUJARAT - 363020",
      gstin = "24CHWPG0910J1ZB",
      mobile = "+91 97371 78061",
      email = "vivaan@enterprise.com",
      pan = "CHWPG0910J",
      stateCode = "24",
      bankAccountName = "SHETH JANVI",
      bankName = "HDFC BANK",
      bankAccountNumber = "50100419622062",
      bankIfsc = "HDFC0000299",
      bankBranch = "PALDI, AHMEDABAD - 380 007",
      declaration = "We declare that this invoice shows the actual price of the goods described.",
      authorisedSignatory = "For VIVAAN ENTERPRISE"
  )
  ```

**Location:**
- Test files maintain localized fixtures or companion factory helpers within their package subtrees.

---

## Coverage

**Requirements:**
- Mandatory 100% verification gate before merge:
  1. `./gradlew build --dry-run`
  2. `./gradlew assembleDebug`
  3. `./gradlew lintDebug`
  4. `./gradlew testDebugUnitTest`
  5. `./gradlew assembleRelease`
- Core calculation engine (`DocumentCalculator`) and currency parser (`ExactCurrencyParser`) require 100% path coverage for rounding half-up, intra/inter-state tax splits, overflow conditions, and basis-point arithmetic.
- Finalized documents require strict immutability verification (`HistoricalDocumentImmutabilityTest`).

**View Coverage / Test Reports:**
- Test execution HTML reports generated at `app/build/reports/tests/testDebugUnitTest/index.html`.
- Lint reports generated at `app/build/reports/lint-results-debug.html`.

---

## Test Types

**Unit Tests:**
- Pure Kotlin JVM tests under `app/src/test/java/`.
- Executes within seconds on CI or local workstations without launching an Android runtime.
- Focus: Domain math, state machines, ViewModel intents/effects, validation, formatting, and DTO mapping.

**Integration Tests:**
- Android instrumentation tests under `app/src/androidTest/java/`.
- Runs on real Android virtual device (AVD) or hardware device.
- Focus:
  - In-memory Room persistence, complex SQL joins, aggregations, and transaction rollbacks (`Step16DaoInstrumentationTest.kt`).
  - Schema migrations using `SupportSQLiteOpenHelper` (`RoomDatabaseMigrationTest.kt`).
  - Dynamic PDF generation with real `android.graphics.pdf.PdfDocument` and verification with `android.graphics.pdf.PdfRenderer` (`AndroidBusinessDocumentPdfGeneratorTest.kt`).
  - MediaStore / FileProvider export and share URIs (`PdfServicesTest.kt`).
  - Background synchronization and conflict resolution (`SyncRepositoryTest.kt`).

**E2E / Smoke Tests:**
- `FirebaseInitializationSmokeTest.kt`: Verifies Firebase App initialization without network dependency.
- In-app PDF render session verification (`PdfServicesTest.kt`).

---

## Common Patterns

### Async Testing with Coroutines
```kotlin
// Reference: app/src/test/java/com/vivaanenterprise/app/feature/document/history/HistoricalDocumentImmutabilityTest.kt:46-96
@Test
fun finalizedDocument_preservesHistoricalClientName_whenClientMasterIsRenamed() = runTest {
    // 1. Arrange test fakes
    val docRepo = FakeDocumentRepo(listOf(finalizedDocument))
    val clientRepo = FakeClientRepo(listOf(renamedClient))

    // 2. Act
    val listViewModel = DocumentsViewModel(docRepo, clientRepo)
    testDispatcher.scheduler.advanceUntilIdle() // Process scheduled coroutines

    // 3. Assert StateFlow output
    val listUiState = listViewModel.uiState.value
    assertEquals("Original Client", listUiState.documents[0].clientDisplayName)
}
```

### CancellationException Propagation Testing
```kotlin
// Reference: app/src/test/java/com/vivaanenterprise/app/feature/auth/presentation/LoginViewModelTest.kt:163-176
@Test
fun testCancellationExceptionIsNotConvertedIntoGenericAuthError() = runTest {
    fakeAuthRepository.signInExceptionToThrow = CancellationException("Coroutines cancelled")

    try {
        viewModel.onIntent(LoginUiIntent.SignInClicked("test@example.com", "password"))
        testDispatcher.scheduler.advanceUntilIdle()
    } catch (e: CancellationException) {
        // Verified: CancellationException was correctly rethrown and NOT swallowed into UI state error
        assertTrue(true)
    }
}
```

### Arithmetic Overflow and Boundary Testing
```kotlin
// Reference: app/src/test/java/com/vivaanenterprise/app/domain/util/DocumentCalculatorTest.kt
@Test
fun testArithmeticOverflowReturnsCalculationError() {
    val input = DocumentCalculationInput(
        sellerStateCode = "24",
        placeOfSupplyStateCode = "24",
        lines = listOf(
            DocumentCalculationInput.LineInput(
                id = "line-overflow",
                quantity = Long.MAX_VALUE / 2,
                ratePaise = 100_000L,
                gstRateBasisPoints = 1800
            )
        )
    )

    val result = calculator.calculate(input)
    assertTrue(result.isFailure)
    val error = (result.exceptionOrNull() as CalculationException).error
    assertEquals(CalculationError.ArithmeticOverflow, error)
}
```

---

*Testing analysis: 2026-11-17*
