package com.vivaanenterprise.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vivaanenterprise.app.core.database.entity.BusinessProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProfileDao {
    @Upsert
    suspend fun upsert(profile: BusinessProfileEntity)

    @Query("SELECT * FROM business_profiles LIMIT 1")
    fun observeProfile(): Flow<BusinessProfileEntity?>

    @Query("SELECT * FROM business_profiles LIMIT 1")
    suspend fun getProfile(): BusinessProfileEntity?
}
