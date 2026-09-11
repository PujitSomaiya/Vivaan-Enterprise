package com.vivaanenterprise.app.feature.client.presentation.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivaanenterprise.app.R
import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.repository.ClientRepository
import com.vivaanenterprise.app.feature.client.model.ClientValidationUtils
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

import com.vivaanenterprise.app.domain.model.IndianState

@HiltViewModel
class ClientFormViewModel @Inject constructor(
    private val clientRepository: ClientRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val clientId: String? = savedStateHandle["clientId"]

    private val _uiState = MutableStateFlow(ClientFormUiState(clientId = clientId))
    val uiState: StateFlow<ClientFormUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<ClientFormUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        if (clientId != null && clientId.isNotBlank()) {
            loadExistingClient(clientId)
        }
    }

    private fun loadExistingClient(id: String) {
        _uiState.update { it.copy(isLoadingClient = true) }
        viewModelScope.launch {
            val client = clientRepository.getClientById(id)
            if (client != null) {
                val resolvedState = IndianState.findByCode(client.stateCode)
                val canonicalStateName = resolvedState?.name ?: (client.state ?: "")
                val canonicalStateCode = resolvedState?.code ?: (client.stateCode ?: "")

                _uiState.update { current ->
                    current.copy(
                        companyName = client.companyName,
                        address = client.address ?: "",
                        gstin = client.gstin ?: "",
                        state = canonicalStateName,
                        stateCode = canonicalStateCode,
                        email = client.email ?: "",
                        phone = client.phone ?: "",
                        pan = client.pan ?: "",
                        iec = client.iec ?: "",
                        otherDetails = client.otherDetails ?: "",
                        isLoadingClient = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingClient = false) }
                _uiEffect.send(ClientFormUiEffect.ShowError(R.string.error_client_not_found))
            }
        }
    }

    fun onIntent(intent: ClientFormUiIntent) {
        when (intent) {
            is ClientFormUiIntent.CompanyNameChanged -> _uiState.update { it.copy(companyName = intent.value, companyNameError = null) }
            is ClientFormUiIntent.AddressChanged -> _uiState.update { it.copy(address = intent.value) }
            is ClientFormUiIntent.GstinChanged -> _uiState.update { it.copy(gstin = intent.value, gstinError = null) }
            is ClientFormUiIntent.StateSelected -> {
                val indianState = IndianState.findByCode(intent.stateCode)
                if (indianState != null) {
                    _uiState.update {
                        it.copy(
                            state = indianState.name,
                            stateCode = indianState.code,
                            stateCodeError = null
                        )
                    }
                }
            }
            is ClientFormUiIntent.EmailChanged -> _uiState.update { it.copy(email = intent.value, emailError = null) }
            is ClientFormUiIntent.PhoneChanged -> _uiState.update { it.copy(phone = intent.value, phoneError = null) }
            is ClientFormUiIntent.PanChanged -> _uiState.update { it.copy(pan = intent.value, panError = null) }
            is ClientFormUiIntent.IecChanged -> _uiState.update { it.copy(iec = intent.value) }
            is ClientFormUiIntent.OtherDetailsChanged -> _uiState.update { it.copy(otherDetails = intent.value) }
            is ClientFormUiIntent.SaveClicked -> saveClient()
        }
    }

    private fun saveClient() {
        val currentState = _uiState.value
        if (currentState.isSaving || currentState.isLoadingClient) return

        val companyNameError = if (currentState.companyName.trim().isEmpty()) {
            ClientFormFieldValidationError.COMPANY_NAME_REQUIRED
        } else null

        val gstinError = if (currentState.gstin.trim().isNotBlank() && !ClientValidationUtils.isValidGstin(currentState.gstin)) {
            ClientFormFieldValidationError.GSTIN_INVALID
        } else null

        val panError = if (currentState.pan.trim().isNotBlank() && !ClientValidationUtils.isValidPan(currentState.pan)) {
            ClientFormFieldValidationError.PAN_INVALID
        } else null

        val stateCodeError = if (currentState.stateCode.trim().isNotBlank() && !ClientValidationUtils.isValidStateCode(currentState.stateCode)) {
            ClientFormFieldValidationError.STATE_CODE_INVALID
        } else null

        val emailError = if (currentState.email.trim().isNotBlank() && !ClientValidationUtils.isValidEmail(currentState.email)) {
            ClientFormFieldValidationError.EMAIL_INVALID
        } else null

        val phoneError = if (currentState.phone.trim().isNotBlank() && !ClientValidationUtils.isValidPhone(currentState.phone)) {
            ClientFormFieldValidationError.PHONE_INVALID
        } else null

        if (companyNameError != null || gstinError != null || panError != null || stateCodeError != null || emailError != null || phoneError != null) {
            _uiState.update {
                it.copy(
                    companyNameError = companyNameError,
                    gstinError = gstinError,
                    panError = panError,
                    stateCodeError = stateCodeError,
                    emailError = emailError,
                    phoneError = phoneError
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val clientToSave = Client(
                    id = currentState.clientId ?: "",
                    companyName = currentState.companyName,
                    address = currentState.address,
                    gstin = currentState.gstin,
                    state = currentState.state,
                    stateCode = currentState.stateCode,
                    email = currentState.email,
                    phone = currentState.phone,
                    pan = currentState.pan,
                    iec = currentState.iec,
                    otherDetails = currentState.otherDetails,
                    createdAt = 0L,
                    updatedAt = 0L
                )

                val result = if (currentState.isEditMode) {
                    clientRepository.updateClient(clientToSave)
                } else {
                    clientRepository.createClient(clientToSave)
                }

                if (result.isSuccess) {
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.send(ClientFormUiEffect.SaveSuccess)
                } else {
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.send(ClientFormUiEffect.ShowError(R.string.error_saving_client))
                }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isSaving = false) }
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _uiEffect.send(ClientFormUiEffect.ShowError(R.string.error_saving_client))
            }
        }
    }
}
