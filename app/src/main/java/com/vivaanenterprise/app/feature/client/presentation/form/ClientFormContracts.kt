package com.vivaanenterprise.app.feature.client.presentation.form

enum class ClientFormFieldValidationError {
    COMPANY_NAME_REQUIRED,
    GSTIN_INVALID,
    PAN_INVALID,
    STATE_CODE_INVALID,
    EMAIL_INVALID,
    PHONE_INVALID
}

data class ClientFormUiState(
    val clientId: String? = null,
    val companyName: String = "",
    val address: String = "",
    val gstin: String = "",
    val state: String = "",
    val stateCode: String = "",
    val email: String = "",
    val phone: String = "",
    val pan: String = "",
    val iec: String = "",
    val otherDetails: String = "",
    val isLoadingClient: Boolean = false,
    val isSaving: Boolean = false,
    val companyNameError: ClientFormFieldValidationError? = null,
    val gstinError: ClientFormFieldValidationError? = null,
    val panError: ClientFormFieldValidationError? = null,
    val stateCodeError: ClientFormFieldValidationError? = null,
    val emailError: ClientFormFieldValidationError? = null,
    val phoneError: ClientFormFieldValidationError? = null
) {
    val isEditMode: Boolean get() = clientId != null
}

sealed interface ClientFormUiIntent {
    data class CompanyNameChanged(val value: String) : ClientFormUiIntent
    data class AddressChanged(val value: String) : ClientFormUiIntent
    data class GstinChanged(val value: String) : ClientFormUiIntent
    data class StateChanged(val value: String) : ClientFormUiIntent
    data class StateCodeChanged(val value: String) : ClientFormUiIntent
    data class EmailChanged(val value: String) : ClientFormUiIntent
    data class PhoneChanged(val value: String) : ClientFormUiIntent
    data class PanChanged(val value: String) : ClientFormUiIntent
    data class IecChanged(val value: String) : ClientFormUiIntent
    data class OtherDetailsChanged(val value: String) : ClientFormUiIntent
    data object SaveClicked : ClientFormUiIntent
}

sealed interface ClientFormUiEffect {
    data object SaveSuccess : ClientFormUiEffect
    data class ShowError(val messageRes: Int) : ClientFormUiEffect
}
