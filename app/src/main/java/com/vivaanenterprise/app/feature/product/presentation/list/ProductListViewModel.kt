package com.vivaanenterprise.app.feature.product.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filter = MutableStateFlow(ProductFilter.ALL)

    private val _uiEffect = Channel<ProductListUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    val uiState: StateFlow<ProductListUiState> = combine(
        productRepository.observeProducts(),
        _searchQuery,
        _filter
    ) { allProducts, query, filter ->
        val trimmedQuery = query.trim().lowercase()
        val filteredProducts = allProducts.filter { product ->
            val matchesSearch = if (trimmedQuery.isEmpty()) {
                true
            } else {
                product.name.lowercase().contains(trimmedQuery) ||
                        product.hsnSac.lowercase().contains(trimmedQuery)
            }

            val matchesFilter = when (filter) {
                ProductFilter.ALL -> true
                ProductFilter.ACTIVE -> product.isActive
                ProductFilter.INACTIVE -> !product.isActive
            }

            matchesSearch && matchesFilter
        }

        ProductListUiState(
            products = filteredProducts,
            searchQuery = query,
            filter = filter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProductListUiState(isLoading = true)
    )

    fun onIntent(intent: ProductListUiIntent) {
        when (intent) {
            is ProductListUiIntent.SearchQueryChanged -> _searchQuery.value = intent.query
            is ProductListUiIntent.FilterChanged -> _filter.value = intent.filter
            is ProductListUiIntent.ProductClicked -> {
                viewModelScope.launch {
                    _uiEffect.send(ProductListUiEffect.NavigateToDetail(intent.productId))
                }
            }
            ProductListUiIntent.AddProductClicked -> {
                viewModelScope.launch {
                    _uiEffect.send(ProductListUiEffect.NavigateToAddProduct)
                }
            }
            is ProductListUiIntent.ToggleActiveClicked -> toggleProductActive(intent.productId, intent.isActive)
        }
    }

    private fun toggleProductActive(productId: String, isActive: Boolean) {
        viewModelScope.launch {
            val result = productRepository.setProductActive(productId, isActive)
            if (result.isFailure) {
                _uiEffect.send(ProductListUiEffect.ShowError(R.string.error_saving_product))
            }
        }
    }
}
