package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.database.entity.BusinessProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BusinessProfileRepositoryTest {

    @Test
    fun testBusinessProfileMappingDoesNotDeriveStateCodeFromGstin() = runTest {
        val fakeDao = FakeBusinessProfileDao()

        // 1) Explicit stateCode = "24" -> domain stateCode = "24"
        fakeDao.profile = BusinessProfileEntity(
            id = "profile-1",
            businessName = "VIVAAN ENTERPRISE",
            addressLine1 = "Street 4",
            addressLine2 = "Jorawar Nagar",
            cityStatePincode = "Surendranagar",
            gstin = "24CHWPG0910J1ZB",
            mobile = "9737178061",
            pan = "CHWPG0910J",
            bankAccountName = "SHETH JANVI",
            bankName = "HDFC BANK",
            bankAccountNumber = "50100419622062",
            bankIfsc = "HDFC0000299",
            bankBranch = "Paldi",
            declaration = "Declaration",
            authorisedSignatory = "Signatory",
            state = "Gujarat",
            stateCode = "24",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val repo = BusinessProfileRepositoryImpl(fakeDao)
        val domainProfile1 = repo.getProfile()
        assertEquals("24", domainProfile1?.stateCode)
        assertEquals("Gujarat", domainProfile1?.state)

        // 2) Null stateCode with GSTIN "24..." -> domain stateCode MUST be null (not derived as "24")
        fakeDao.profile = fakeDao.profile?.copy(stateCode = null)

        val domainProfile2 = repo.getProfile()
        assertNull("Domain stateCode must be null when entity.stateCode is null", domainProfile2?.stateCode)

        val observedProfile = repo.observeProfile().first()
        assertNull("Observed domain stateCode must be null when entity.stateCode is null", observedProfile?.stateCode)
    }
}
