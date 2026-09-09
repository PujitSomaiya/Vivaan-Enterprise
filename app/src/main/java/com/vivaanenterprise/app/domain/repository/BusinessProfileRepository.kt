package com.vivaanenterprise.app.domain.repository

import com.vivaanenterprise.app.domain.model.BusinessProfile
import kotlinx.coroutines.flow.Flow

interface BusinessProfileRepository {
    fun observeProfile(): Flow<BusinessProfile?>
    suspend fun getProfile(): BusinessProfile?
}
