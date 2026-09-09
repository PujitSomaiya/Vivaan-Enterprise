package com.vivaanenterprise.app.domain.model

data class Product(
    val id: String,
    val name: String,
    val hsnSac: String,
    val defaultGstRateBasisPoints: Int,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)
