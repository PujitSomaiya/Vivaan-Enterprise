package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.database.dao.BusinessProfileDao
import com.vivaanenterprise.app.core.database.entity.BusinessProfileEntity
import com.vivaanenterprise.app.domain.model.BusinessProfile
import com.vivaanenterprise.app.domain.repository.BusinessProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessProfileRepositoryImpl @Inject constructor(
    private val profileDao: BusinessProfileDao
) : BusinessProfileRepository {

    override fun observeProfile(): Flow<BusinessProfile?> {
        return profileDao.observeProfile().map { entity -> entity?.toDomain() }
    }

    override suspend fun getProfile(): BusinessProfile? {
        return profileDao.getProfile()?.toDomain()
    }

    private fun BusinessProfileEntity.toDomain(): BusinessProfile = BusinessProfile(
        id = id,
        businessName = businessName,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        cityStatePincode = cityStatePincode,
        gstin = gstin,
        mobile = mobile,
        email = email,
        pan = pan,
        bankAccountName = bankAccountName,
        bankName = bankName,
        bankAccountNumber = bankAccountNumber,
        bankIfsc = bankIfsc,
        bankBranch = bankBranch,
        declaration = declaration,
        authorisedSignatory = authorisedSignatory,
        state = state,
        stateCode = stateCode,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
