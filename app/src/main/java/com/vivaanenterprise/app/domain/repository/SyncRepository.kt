package com.vivaanenterprise.app.domain.repository

interface SyncRepository {
    suspend fun synchronize(): Result<Unit>
}
