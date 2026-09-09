package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.common.TimeProvider
import com.vivaanenterprise.app.core.database.dao.BusinessProfileDao
import com.vivaanenterprise.app.core.database.entity.BusinessProfileEntity
import javax.inject.Inject
import javax.inject.Singleton

open class BusinessProfileSeeder @Inject constructor(
    private val businessProfileDao: BusinessProfileDao,
    private val timeProvider: TimeProvider
) {
    companion object {
        const val SEED_PROFILE_ID = "vivaan-enterprise-business-profile"
        const val BUSINESS_NAME = "VIVAAN ENTERPRISE"
        const val ADDRESS_LINE1 = "NEAR SHALIBHADRANIVAS, OPP. SIDDHIVINAYAK HOUSE"
        const val ADDRESS_LINE2 = "STREET NO. 4, MAHATMA GANDHI ROAD, JORAWAR NAGAR"
        const val CITY_STATE_PINCODE = "SURENDRANAGAR, GUJARAT - 363020"
        const val GSTIN = "24CHWPG0910J1ZB"
        const val MOBILE = "+91 97371 78061"
        const val PAN = "CHWPG0910J"
        const val BANK_ACCOUNT_NAME = "SHETH JANVI"
        const val BANK_NAME = "HDFC BANK"
        const val BANK_ACCOUNT_NUMBER = "50100419622062"
        const val BANK_IFSC = "HDFC0000299"
        const val BANK_BRANCH = "SHAIVAL COMPLEX, OPP. CHANDANBALA TOWERS, PALDI, AHMEDABAD - 380 007"
        const val DECLARATION = "We declare that this invoice shows the actual price of the goods described and that all particulars are true and correct."
        const val AUTHORISED_SIGNATORY = "For VIVAAN ENTERPRISE"
        const val STATE = "Gujarat"
        const val STATE_CODE = "24"
    }

    open suspend fun seedInitialData() {
        val existing = businessProfileDao.getProfile()
        if (existing == null) {
            val now = timeProvider.currentTimeMillis()
            val seedEntity = BusinessProfileEntity(
                id = SEED_PROFILE_ID,
                businessName = BUSINESS_NAME,
                addressLine1 = ADDRESS_LINE1,
                addressLine2 = ADDRESS_LINE2,
                cityStatePincode = CITY_STATE_PINCODE,
                gstin = GSTIN,
                mobile = MOBILE,
                email = null,
                pan = PAN,
                bankAccountName = BANK_ACCOUNT_NAME,
                bankName = BANK_NAME,
                bankAccountNumber = BANK_ACCOUNT_NUMBER,
                bankIfsc = BANK_IFSC,
                bankBranch = BANK_BRANCH,
                declaration = DECLARATION,
                authorisedSignatory = AUTHORISED_SIGNATORY,
                state = STATE,
                stateCode = STATE_CODE,
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
            businessProfileDao.upsert(seedEntity)
        }
    }
}
