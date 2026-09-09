package com.vivaanenterprise.app.core.database.di

import android.content.Context
import androidx.room.Room
import com.vivaanenterprise.app.core.common.DefaultIdGenerator
import com.vivaanenterprise.app.core.common.IdGenerator
import com.vivaanenterprise.app.core.common.SystemTimeProvider
import com.vivaanenterprise.app.core.common.TimeProvider
import com.vivaanenterprise.app.core.database.VivaanEnterpriseDatabase
import com.vivaanenterprise.app.core.database.dao.BusinessDocumentDao
import com.vivaanenterprise.app.core.database.dao.BusinessProfileDao
import com.vivaanenterprise.app.core.database.dao.ClientAccountEntryDao
import com.vivaanenterprise.app.core.database.dao.ClientDao
import com.vivaanenterprise.app.core.database.dao.DocumentLineItemDao
import com.vivaanenterprise.app.core.database.dao.DocumentSequenceDao
import com.vivaanenterprise.app.core.database.dao.ProductDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVivaanEnterpriseDatabase(
        @ApplicationContext context: Context
    ): VivaanEnterpriseDatabase {
        return Room.databaseBuilder(
            context,
            VivaanEnterpriseDatabase::class.java,
            VivaanEnterpriseDatabase.DATABASE_NAME
        ).addMigrations(
            com.vivaanenterprise.app.core.database.migration.MIGRATION_1_2,
            com.vivaanenterprise.app.core.database.migration.MIGRATION_2_3
        ).build()
    }

    @Provides
    fun provideBusinessProfileDao(db: VivaanEnterpriseDatabase): BusinessProfileDao = db.businessProfileDao()

    @Provides
    fun provideClientDao(db: VivaanEnterpriseDatabase): ClientDao = db.clientDao()

    @Provides
    fun provideProductDao(db: VivaanEnterpriseDatabase): ProductDao = db.productDao()

    @Provides
    fun provideBusinessDocumentDao(db: VivaanEnterpriseDatabase): BusinessDocumentDao = db.businessDocumentDao()

    @Provides
    fun provideDocumentLineItemDao(db: VivaanEnterpriseDatabase): DocumentLineItemDao = db.documentLineItemDao()

    @Provides
    fun provideClientAccountEntryDao(db: VivaanEnterpriseDatabase): ClientAccountEntryDao = db.clientAccountEntryDao()

    @Provides
    fun provideDocumentSequenceDao(db: VivaanEnterpriseDatabase): DocumentSequenceDao = db.documentSequenceDao()

    @Provides
    @Singleton
    fun provideIdGenerator(): IdGenerator = DefaultIdGenerator()

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider()
}
