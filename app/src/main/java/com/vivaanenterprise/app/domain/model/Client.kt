package com.vivaanenterprise.app.domain.model

data class Client(
    val id: String,
    val companyName: String,
    val address: String? = null,
    val gstin: String? = null,
    val state: String? = null,
    val stateCode: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val pan: String? = null,
    val iec: String? = null,
    val otherDetails: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
