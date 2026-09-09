package com.vivaanenterprise.app.domain.repository

import com.vivaanenterprise.app.domain.model.Client
import kotlinx.coroutines.flow.Flow

interface ClientRepository {
    fun observeClients(): Flow<List<Client>>
    fun observeClientById(id: String): Flow<Client?>
    suspend fun getClientById(id: String): Client?
    suspend fun createClient(client: Client): Result<Unit>
    suspend fun updateClient(client: Client): Result<Unit>
    suspend fun deleteClient(id: String): Result<Unit>
}
