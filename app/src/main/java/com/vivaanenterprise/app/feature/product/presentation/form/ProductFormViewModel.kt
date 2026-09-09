package com.vivaanenterprise.app.feature.product.presentation.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.domain.model.Product
import com.vivaanenterprise.app.domain.repository.ProductRepository
import com.vivaanenterprise.app.feature.product.model.ProductGstUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: String? = savedStateHandle["productId"]

    private val _uiState = MutableStateFlow(ProductFormUiState(productId = productId))
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<ProductFormUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        if (productId != null && productId.isNotBlank()) {
            loadExistingProduct(productId)
        }
    }

    private fun loadExistingProduct(id: String) {
        _uiState.update { it.copy(isLoadingProduct = true) }
        viewModelScope.launch {
            val product = productRepository.getProductById(id)
            if (product != null) {
                _uiState.update { current ->
                    current.copy(
                        name = product.name,
                        hsnSac = product.hsnSac ?: "",
                        gstRateInput = ProductGstUtils.formatBasisPointsToPercentage(product.defaultGstRateBasisPoints),
                        isActive = product.isActive,
                        isLoadingProduct = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingProduct = false) }
                _uiEffect.send(ProductFormUiEffect.ShowError(R.string.error_product_not_found))
            }
        }
    }

    fun onIntent(intent: ProductFormUiIntent) {
        when (intent) {
            is ProductFormUiIntent.NameChanged -> _uiState.update { it.copy(name = intent.value, nameError = null) }
            is ProductFormUiIntent.HsnSacChanged -> _uiState.update { it.copy(hsnSac = intent.value, hsnSacError = null) }
            is ProductFormUiIntent.GstRateChanged -> _uiState.update { it.copy(gstRateInput = intent.value, gstRateError = null) }
            is ProductFormUiIntent.IsActiveChanged -> _uiState.update { it.copy(isActive = intent.value) }
            ProductFormUiIntent.SaveClicked -> saveProduct()
        }
    }

    private fun saveProduct() {
        val currentState = _uiState.value
        if (currentState.isSaving || currentState.isLoadingProduct) return

        val nameError = if (currentState.name.trim().isEmpty()) {
            ProductFormFieldValidationError.NAME_REQUIRED
        } else null

        val hsnSacError = if (currentState.hsnSac.trim().isEmpty()) {
            ProductFormFieldValidationError.HSN_SAC_REQUIRED
        } else null

        val parsedGstBasisPoints = ProductGstUtils.parseGstPercentageToBasisPoints(currentState.gstRateInput)
        val gstRateError = if (parsedGstBasisPoints == null) {
            ProductFormFieldValidationError.GST_RATE_INVALID
        } else null

        if (nameError != null || hsnSacError != null || gstRateError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    hsnSacError = hsnSacError,
                    gstRateError = gstRateError
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val productToSave = Product(
                    id = currentState.productId ?: "",
                    name = currentState.name,
                    hsnSac = currentState.hsnSac,
                    defaultGstRateBasisPoints = parsedGstBasisPoints!!,
                    isActive = currentState.isActive,
                    createdAt = 0L,
                    updatedAt = 0L
                )

                val result = if (currentState.isEditMode) {
                    productRepository.updateProduct(productToSave)
                } else {
                    productRepository.createProduct(productToSave)
                }

                if (result.isSuccess) {
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.send(ProductFormUiEffect.SaveSuccess)
                } else {
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.send(ProductFormUiEffect.ShowError(R.string.error_saving_product))
                }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isSaving = false) }
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _uiEffect.send(ProductFormUiEffect.ShowError(R.string.error_saving_product))
            }
        }
    }
}
