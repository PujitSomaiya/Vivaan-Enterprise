package com.vivaanenterprise.app.feature.product.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: String = checkNotNull(savedStateHandle["productId"])

    private val _isDeleting = MutableStateFlow(false)
    private val _showDeleteDialog = MutableStateFlow(false)

    private val _uiEffect = Channel<ProductDetailUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    val uiState: StateFlow<ProductDetailUiState> = combine(
        productRepository.observeProductById(productId),
        _isDeleting,
        _showDeleteDialog
    ) { product, isDeleting, showDeleteDialog ->
        ProductDetailUiState(
            productId = productId,
            product = product,
            isLoading = false,
            isDeleting = isDeleting,
            showDeleteDialog = showDeleteDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProductDetailUiState(productId = productId, isLoading = true)
    )

    fun onIntent(intent: ProductDetailUiIntent) {
        when (intent) {
            ProductDetailUiIntent.EditClicked -> {
                viewModelScope.launch {
                    _uiEffect.send(ProductDetailUiEffect.NavigateToEdit(productId))
                }
            }
            is ProductDetailUiIntent.ToggleActiveClicked -> toggleActive(intent.isActive)
            ProductDetailUiIntent.DeleteClicked -> _showDeleteDialog.value = true
            ProductDetailUiIntent.DismissDeleteDialog -> _showDeleteDialog.value = false
            ProductDetailUiIntent.ConfirmDeleteClicked -> deleteProduct()
        }
    }

    private fun toggleActive(isActive: Boolean) {
        viewModelScope.launch {
            val result = productRepository.setProductActive(productId, isActive)
            if (result.isFailure) {
                _uiEffect.send(ProductDetailUiEffect.ShowError(R.string.error_saving_product))
            }
        }
    }

    private fun deleteProduct() {
        _showDeleteDialog.value = false
        _isDeleting.value = true
        viewModelScope.launch {
            try {
                val result = productRepository.deleteProduct(productId)
                if (result.isSuccess) {
                    _isDeleting.value = false
                    _uiEffect.send(ProductDetailUiEffect.DeleteSuccess)
                } else {
                    _isDeleting.value = false
                    _uiEffect.send(ProductDetailUiEffect.ShowError(R.string.error_deleting_product))
                }
            } catch (e: CancellationException) {
                _isDeleting.value = false
                throw e
            } catch (e: Exception) {
                _isDeleting.value = false
                _uiEffect.send(ProductDetailUiEffect.ShowError(R.string.error_deleting_product))
            }
        }
    }
}
