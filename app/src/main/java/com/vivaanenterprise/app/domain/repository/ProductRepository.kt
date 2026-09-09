package com.vivaanenterprise.app.domain.repository

import com.vivaanenterprise.app.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(): Flow<List<Product>>
    fun observeActiveProducts(): Flow<List<Product>>
    fun observeProductById(id: String): Flow<Product?>
    suspend fun getProductById(id: String): Product?
    suspend fun createProduct(product: Product): Result<Unit>
    suspend fun updateProduct(product: Product): Result<Unit>
    suspend fun setProductActive(id: String, isActive: Boolean): Result<Unit>
    suspend fun deleteProduct(id: String): Result<Unit>
}
