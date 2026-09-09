package com.vivaanenterprise.app.data.repository

import com.vivaanenterprise.app.core.common.IdGenerator
import com.vivaanenterprise.app.core.common.SyncStatus
import com.vivaanenterprise.app.core.common.TimeProvider
import com.vivaanenterprise.app.core.database.dao.ClientDao
import com.vivaanenterprise.app.core.sync.SyncScheduler
import com.vivaanenterprise.app.data.local.mapper.toDomain
import com.vivaanenterprise.app.data.local.mapper.toEntity
import com.vivaanenterprise.app.domain.model.Client
import com.vivaanenterprise.app.domain.repository.ClientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepositoryImpl @Inject constructor(
    private val clientDao: ClientDao,
    private val syncScheduler: SyncScheduler,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) : ClientRepository {

    override fun observeClients(): Flow<List<Client>> {
        return clientDao.observeActiveClients().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeClientById(id: String): Flow<Client?> {
        return clientDao.observeById(id).map { it?.toDomain() }
    }

    override suspend fun getClientById(id: String): Client? {
        return clientDao.getById(id)?.toDomain()
    }

    override suspend fun createClient(client: Client): Result<Unit> {
        return try {
            val now = timeProvider.currentTimeMillis()
            val finalId = if (client.id.isBlank()) idGenerator.newId() else client.id
            val normalizedClient = client.copy(
                id = finalId,
                companyName = client.companyName.trim(),
                address = client.address?.trim()?.ifBlank { null },
                gstin = client.gstin?.trim()?.uppercase()?.ifBlank { null },
                state = client.state?.trim()?.ifBlank { null },
                stateCode = client.stateCode?.trim()?.ifBlank { null },
                email = client.email?.trim()?.ifBlank { null },
                phone = client.phone?.trim()?.ifBlank { null },
                pan = client.pan?.trim()?.uppercase()?.ifBlank { null },
                iec = client.iec?.trim()?.uppercase()?.ifBlank { null },
                otherDetails = client.otherDetails?.trim()?.ifBlank { null },
                createdAt = if (client.createdAt == 0L) now else client.createdAt,
                updatedAt = now
            )

            clientDao.upsert(normalizedClient.toEntity(syncStatus = SyncStatus.PENDING))

            try {
                syncScheduler.enqueueSync()
            } catch (e: Exception) {
                // Background sync enqueue failure must not corrupt local persistence success
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateClient(client: Client): Result<Unit> {
        return try {
            val existing = clientDao.getById(client.id)
                ?: return Result.failure(IllegalStateException("Client with id ${client.id} not found"))

            val now = timeProvider.currentTimeMillis()
            val normalizedClient = client.copy(
                companyName = client.companyName.trim(),
                address = client.address?.trim()?.ifBlank { null },
                gstin = client.gstin?.trim()?.uppercase()?.ifBlank { null },
                state = client.state?.trim()?.ifBlank { null },
                stateCode = client.stateCode?.trim()?.ifBlank { null },
                email = client.email?.trim()?.ifBlank { null },
                phone = client.phone?.trim()?.ifBlank { null },
                pan = client.pan?.trim()?.uppercase()?.ifBlank { null },
                iec = client.iec?.trim()?.uppercase()?.ifBlank { null },
                otherDetails = client.otherDetails?.trim()?.ifBlank { null },
                createdAt = existing.createdAt,
                updatedAt = now
            )

            clientDao.upsert(
                normalizedClient.toEntity(
                    syncStatus = SyncStatus.PENDING,
                    isDeleted = existing.isDeleted,
                    deletedAt = existing.deletedAt
                )
            )

            try {
                syncScheduler.enqueueSync()
            } catch (e: Exception) {
                // Ignore sync scheduling failure for local safety
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteClient(id: String): Result<Unit> {
        return try {
            val now = timeProvider.currentTimeMillis()
            clientDao.softDelete(
                id = id,
                deletedAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )

            try {
                syncScheduler.enqueueSync()
            } catch (e: Exception) {
                // Ignore sync scheduling failure for local safety
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
