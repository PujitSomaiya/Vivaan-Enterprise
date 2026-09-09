package com.vivaanenterprise.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vivaanenterprise.app.core.database.converter.RoomTypeConverters
import com.vivaanenterprise.app.core.database.dao.BusinessDocumentDao
import com.vivaanenterprise.app.core.database.dao.BusinessProfileDao
import com.vivaanenterprise.app.core.database.dao.ClientAccountEntryDao
import com.vivaanenterprise.app.core.database.dao.ClientDao
import com.vivaanenterprise.app.core.database.dao.DocumentLineItemDao
import com.vivaanenterprise.app.core.database.dao.DocumentSequenceDao
import com.vivaanenterprise.app.core.database.dao.ProductDao
import com.vivaanenterprise.app.core.database.entity.BusinessDocumentEntity
import com.vivaanenterprise.app.core.database.entity.BusinessProfileEntity
import com.vivaanenterprise.app.core.database.entity.ClientAccountEntryEntity
import com.vivaanenterprise.app.core.database.entity.ClientEntity
import com.vivaanenterprise.app.core.database.entity.DocumentLineItemEntity
import com.vivaanenterprise.app.core.database.entity.DocumentSequenceEntity
import com.vivaanenterprise.app.core.database.entity.ProductEntity

@Database(
    entities = [
        BusinessProfileEntity::class,
        ClientEntity::class,
        ProductEntity::class,
        BusinessDocumentEntity::class,
        DocumentLineItemEntity::class,
        ClientAccountEntryEntity::class,
        DocumentSequenceEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(RoomTypeConverters::class)
abstract class VivaanEnterpriseDatabase : RoomDatabase() {
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun productDao(): ProductDao
    abstract fun businessDocumentDao(): BusinessDocumentDao
    abstract fun documentLineItemDao(): DocumentLineItemDao
    abstract fun clientAccountEntryDao(): ClientAccountEntryDao
    abstract fun documentSequenceDao(): DocumentSequenceDao

    companion object {
        const val DATABASE_NAME = "vivaan_enterprise.db"
    }
}
