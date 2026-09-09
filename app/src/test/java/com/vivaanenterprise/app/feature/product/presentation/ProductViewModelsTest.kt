package com.vivaanenterprise.app.feature.product.presentation

import androidx.lifecycle.SavedStateHandle
import com.vivaanenterprise.app.domain.model.Product
import com.vivaanenterprise.app.domain.repository.ProductRepository
import com.vivaanenterprise.app.feature.product.presentation.detail.ProductDetailUiEffect
import com.vivaanenterprise.app.feature.product.presentation.detail.ProductDetailUiIntent
import com.vivaanenterprise.app.feature.product.presentation.detail.ProductDetailViewModel
import com.vivaanenterprise.app.feature.product.presentation.form.ProductFormFieldValidationError
import com.vivaanenterprise.app.feature.product.presentation.form.ProductFormUiEffect
import com.vivaanenterprise.app.feature.product.presentation.form.ProductFormUiIntent
import com.vivaanenterprise.app.feature.product.presentation.form.ProductFormViewModel
import com.vivaanenterprise.app.feature.product.presentation.list.ProductFilter
import com.vivaanenterprise.app.feature.product.presentation.list.ProductListUiEffect
import com.vivaanenterprise.app.feature.product.presentation.list.ProductListUiIntent
import com.vivaanenterprise.app.feature.product.presentation.list.ProductListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeProductRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeProductRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testProductListViewModelObservesFiltersAndSearches() = runTest {
        fakeRepository.productsFlow.value = listOf(
            Product("p-1", "3M Scotch Tape", hsnSac = "3919", defaultGstRateBasisPoints = 1800, isActive = true, createdAt = 0, updatedAt = 0),
            Product("p-2", "Packaging Box", hsnSac = "4819", defaultGstRateBasisPoints = 1200, isActive = false, createdAt = 0, updatedAt = 0)
        )

        val viewModel = ProductListViewModel(fakeRepository)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.products.size)

        // Search by name
        viewModel.onIntent(ProductListUiIntent.SearchQueryChanged("scotch"))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.products.size)
        assertEquals("3M Scotch Tape", viewModel.uiState.value.products.first().name)

        // Search by HSN
        viewModel.onIntent(ProductListUiIntent.SearchQueryChanged("4819"))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.products.size)
        assertEquals("Packaging Box", viewModel.uiState.value.products.first().name)

        // Reset search and test ACTIVE filter
        viewModel.onIntent(ProductListUiIntent.SearchQueryChanged(""))
        viewModel.onIntent(ProductListUiIntent.FilterChanged(ProductFilter.ACTIVE))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.products.size)
        assertTrue(viewModel.uiState.value.products.first().isActive)
    }

    @Test
    fun testProductListViewModelEmitsNavigationEffects() = runTest {
        val viewModel = ProductListViewModel(fakeRepository)

        viewModel.onIntent(ProductListUiIntent.ProductClicked("p-10"))
        assertEquals(ProductListUiEffect.NavigateToDetail("p-10"), viewModel.uiEffect.first())

        viewModel.onIntent(ProductListUiIntent.AddProductClicked)
        assertEquals(ProductListUiEffect.NavigateToAddProduct, viewModel.uiEffect.first())
    }

    @Test
    fun testProductFormViewModelValidationPreventsSave() = runTest {
        val viewModel = ProductFormViewModel(fakeRepository, SavedStateHandle())

        viewModel.onIntent(ProductFormUiIntent.NameChanged("   "))
        viewModel.onIntent(ProductFormUiIntent.HsnSacChanged(""))
        viewModel.onIntent(ProductFormUiIntent.GstRateChanged("invalid-gst"))
        viewModel.onIntent(ProductFormUiIntent.SaveClicked)

        advanceUntilIdle()

        assertEquals(ProductFormFieldValidationError.NAME_REQUIRED, viewModel.uiState.value.nameError)
        assertEquals(ProductFormFieldValidationError.HSN_SAC_REQUIRED, viewModel.uiState.value.hsnSacError)
        assertEquals(ProductFormFieldValidationError.GST_RATE_INVALID, viewModel.uiState.value.gstRateError)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(0, fakeRepository.createCount)
    }

    @Test
    fun testProductFormViewModelSuccessfulSaveEmitsEffect() = runTest {
        val viewModel = ProductFormViewModel(fakeRepository, SavedStateHandle())

        viewModel.onIntent(ProductFormUiIntent.NameChanged("New Tape"))
        viewModel.onIntent(ProductFormUiIntent.HsnSacChanged("3919"))
        viewModel.onIntent(ProductFormUiIntent.GstRateChanged("18"))
        viewModel.onIntent(ProductFormUiIntent.SaveClicked)

        advanceUntilIdle()

        assertEquals(1, fakeRepository.createCount)
        assertEquals(ProductFormUiEffect.SaveSuccess, viewModel.uiEffect.first())
    }

    @Test
    fun testProductFormViewModelEditModeLoadsProduct() = runTest {
        fakeRepository.productMap["p-99"] = Product(
            id = "p-99",
            name = "Existing Tape",
            hsnSac = "3919",
            defaultGstRateBasisPoints = 1800,
            isActive = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val viewModel = ProductFormViewModel(fakeRepository, SavedStateHandle(mapOf("productId" to "p-99")))
        advanceUntilIdle()

        assertEquals("Existing Tape", viewModel.uiState.value.name)
        assertEquals("3919", viewModel.uiState.value.hsnSac)
        assertEquals("18", viewModel.uiState.value.gstRateInput)
        assertTrue(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun testProductDetailViewModelObservesProductAndDeletes() = runTest {
        fakeRepository.productsFlow.value = listOf(
            Product("p-50", "Detail Item", hsnSac = "1234", defaultGstRateBasisPoints = 1800, createdAt = 0, updatedAt = 0)
        )
        fakeRepository.productMap["p-50"] = Product("p-50", "Detail Item", hsnSac = "1234", defaultGstRateBasisPoints = 1800, createdAt = 0, updatedAt = 0)

        val viewModel = ProductDetailViewModel(fakeRepository, SavedStateHandle(mapOf("productId" to "p-50")))
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("Detail Item", viewModel.uiState.value.product?.name)

        viewModel.onIntent(ProductDetailUiIntent.ConfirmDeleteClicked)
        advanceUntilIdle()

        assertEquals(1, fakeRepository.deleteCount)
        assertEquals(ProductDetailUiEffect.DeleteSuccess, viewModel.uiEffect.first())
    }
}

private class FakeProductRepository : ProductRepository {
    val productsFlow = MutableStateFlow<List<Product>>(emptyList())
    val productMap = mutableMapOf<String, Product>()
    var createCount = 0
    var updateCount = 0
    var deleteCount = 0

    override fun observeProducts(): Flow<List<Product>> = productsFlow

    override fun observeActiveProducts(): Flow<List<Product>> = productsFlow.map { list -> list.filter { it.isActive } }

    override fun observeProductById(id: String): Flow<Product?> = productsFlow.map { list -> list.find { it.id == id } }

    override suspend fun getProductById(id: String): Product? = productMap[id]

    override suspend fun createProduct(product: Product): Result<Unit> {
        createCount++
        productMap[product.id] = product
        productsFlow.update { it + product }
        return Result.success(Unit)
    }

    override suspend fun updateProduct(product: Product): Result<Unit> {
        updateCount++
        productMap[product.id] = product
        productsFlow.update { list -> list.map { if (it.id == product.id) product else it } }
        return Result.success(Unit)
    }

    override suspend fun setProductActive(id: String, isActive: Boolean): Result<Unit> {
        productMap[id]?.let { existing ->
            val updated = existing.copy(isActive = isActive)
            productMap[id] = updated
            productsFlow.update { list -> list.map { if (it.id == id) updated else it } }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteProduct(id: String): Result<Unit> {
        deleteCount++
        productMap.remove(id)
        productsFlow.update { list -> list.filter { it.id != id } }
        return Result.success(Unit)
    }
}
