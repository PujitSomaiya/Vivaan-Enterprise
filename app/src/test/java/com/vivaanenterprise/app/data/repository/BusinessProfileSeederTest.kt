package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.common.TimeProvider
import com.vivaanenterprise.app.core.database.dao.BusinessProfileDao
import com.vivaanenterprise.app.core.database.entity.BusinessProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BusinessProfileSeederTest {

    @Test
    fun testBusinessProfileSeederInsertsDocumentedValuesOnce() = runTest {
        val fakeDao = FakeBusinessProfileDao()
        val fakeTimeProvider = object : TimeProvider {
            override fun currentTimeMillis(): Long = 1000000L
        }

        val seeder = BusinessProfileSeeder(fakeDao, fakeTimeProvider)
        seeder.seedInitialData()

        val profile = fakeDao.getProfile()
        assertNotNull(profile)
        assertEquals(BusinessProfileSeeder.SEED_PROFILE_ID, profile?.id)
        assertEquals("VIVAAN ENTERPRISE", profile?.businessName)
        assertEquals("24CHWPG0910J1ZB", profile?.gstin)
        assertEquals("Gujarat", profile?.state)
        assertEquals("24", profile?.stateCode)
        assertEquals("HDFC BANK", profile?.bankName)

        // Calling a second time does not overwrite
        val updatedProfile = profile!!.copy(businessName = "VIVAAN EDITED")
        fakeDao.upsert(updatedProfile)

        seeder.seedInitialData()
        assertEquals("VIVAAN EDITED", fakeDao.getProfile()?.businessName)
    }
}

class FakeBusinessProfileDao : BusinessProfileDao {
    var profile: BusinessProfileEntity? = null

    override suspend fun upsert(profile: BusinessProfileEntity) {
        this.profile = profile
    }

    override fun observeProfile(): Flow<BusinessProfileEntity?> {
        return flowOf(profile)
    }

    override suspend fun getProfile(): BusinessProfileEntity? {
        return profile
    }
}
