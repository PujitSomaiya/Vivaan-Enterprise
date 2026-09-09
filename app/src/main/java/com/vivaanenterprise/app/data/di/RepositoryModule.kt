package com.vivaanenterprise.app.data.di

import com.vivaanenterprise.app.data.repository.AuthRepositoryImpl
import com.vivaanenterprise.app.data.repository.ClientRepositoryImpl
import com.vivaanenterprise.app.data.repository.SyncRepositoryImpl
import com.vivaanenterprise.app.domain.repository.AuthRepository
import com.vivaanenterprise.app.domain.repository.ClientRepository
import com.vivaanenterprise.app.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindClientRepository(impl: ClientRepositoryImpl): ClientRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: com.vivaanenterprise.app.data.repository.ProductRepositoryImpl): com.vivaanenterprise.app.domain.repository.ProductRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(impl: com.vivaanenterprise.app.data.repository.DocumentRepositoryImpl): com.vivaanenterprise.app.domain.repository.DocumentRepository

    @Binds
    @Singleton
    abstract fun bindBusinessProfileRepository(impl: com.vivaanenterprise.app.data.repository.BusinessProfileRepositoryImpl): com.vivaanenterprise.app.domain.repository.BusinessProfileRepository
}
