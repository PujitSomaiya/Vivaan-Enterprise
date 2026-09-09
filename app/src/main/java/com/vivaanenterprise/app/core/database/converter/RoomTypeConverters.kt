package com.vivaanenterprise.app.core.database.converter

import androidx.room.TypeConverter
import com.vivaanenterprise.app.core.common.AccountEntryType
import com.vivaanenterprise.app.core.common.DocumentStatus
import com.vivaanenterprise.app.core.common.DocumentType
import com.vivaanenterprise.app.core.common.SyncStatus

class RoomTypeConverters {

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = try {
        SyncStatus.valueOf(value)
    } catch (e: Exception) {
        SyncStatus.PENDING
    }

    @TypeConverter
    fun fromDocumentType(value: DocumentType): String = value.name

    @TypeConverter
    fun toDocumentType(value: String): DocumentType = try {
        DocumentType.valueOf(value)
    } catch (e: Exception) {
        DocumentType.TAX_INVOICE
    }

    @TypeConverter
    fun fromDocumentStatus(value: DocumentStatus): String = value.name

    @TypeConverter
    fun toDocumentStatus(value: String): DocumentStatus = try {
        DocumentStatus.valueOf(value)
    } catch (e: Exception) {
        DocumentStatus.DRAFT
    }

    @TypeConverter
    fun fromAccountEntryType(value: AccountEntryType): String = value.name

    @TypeConverter
    fun toAccountEntryType(value: String): AccountEntryType = try {
        AccountEntryType.valueOf(value)
    } catch (e: Exception) {
        AccountEntryType.INVOICE
    }
}
