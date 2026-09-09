package com.vivaanenterprise.app.domain.model

data class BusinessProfile(
    val id: String,
    val businessName: String,
    val addressLine1: String,
    val addressLine2: String,
    val cityStatePincode: String,
    val gstin: String,
    val mobile: String,
    val email: String? = null,
    val pan: String,
    val bankAccountName: String,
    val bankName: String,
    val bankAccountNumber: String,
    val bankIfsc: String,
    val bankBranch: String,
    val declaration: String,
    val authorisedSignatory: String,
    val state: String? = "Gujarat",
    val stateCode: String? = "24",
    val createdAt: Long,
    val updatedAt: Long
)
