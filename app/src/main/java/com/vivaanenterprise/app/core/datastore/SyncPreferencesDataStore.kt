package com.vivaanenterprise.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class SyncMetadata(
    val lastSyncTimestamp: Long = 0L,
    val lastSyncAttemptTimestamp: Long = 0L,
    val lastSyncStatus: String = "IDLE", // "SUCCESS", "FAILED", "IDLE"
    val lastSyncError: String? = null
)

@Singleton
open class SyncPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>?
) {
    constructor() : this(null)

    private fun requireDataStore(): DataStore<Preferences> =
        dataStore ?: throw IllegalStateException("DataStore not initialized")

    private object PreferenceKeys {
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val LAST_SYNC_ATTEMPT_TIMESTAMP = longPreferencesKey("last_sync_attempt_timestamp")
        val LAST_SYNC_STATUS = stringPreferencesKey("last_sync_status")
        val LAST_SYNC_ERROR = stringPreferencesKey("last_sync_error")
    }

    open suspend fun getLastSyncTimestamp(): Long {
        val prefs = requireDataStore().data.first()
        return prefs[PreferenceKeys.LAST_SYNC_TIMESTAMP] ?: 0L
    }

    open suspend fun setLastSyncTimestamp(timestamp: Long) {
        requireDataStore().edit { prefs ->
            prefs[PreferenceKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    open fun observeLastSyncTimestamp(): Flow<Long> {
        val ds = dataStore ?: return flowOf(0L)
        return ds.data.map { prefs ->
            prefs[PreferenceKeys.LAST_SYNC_TIMESTAMP] ?: 0L
        }
    }

    open suspend fun recordSyncSuccess(timestamp: Long) {
        requireDataStore().edit { prefs ->
            prefs[PreferenceKeys.LAST_SYNC_TIMESTAMP] = timestamp
            prefs[PreferenceKeys.LAST_SYNC_ATTEMPT_TIMESTAMP] = timestamp
            prefs[PreferenceKeys.LAST_SYNC_STATUS] = "SUCCESS"
            prefs[PreferenceKeys.LAST_SYNC_ERROR] = ""
        }
    }

    open suspend fun recordSyncFailure(attemptTimestamp: Long, errorMessage: String) {
        requireDataStore().edit { prefs ->
            prefs[PreferenceKeys.LAST_SYNC_ATTEMPT_TIMESTAMP] = attemptTimestamp
            prefs[PreferenceKeys.LAST_SYNC_STATUS] = "FAILED"
            prefs[PreferenceKeys.LAST_SYNC_ERROR] = errorMessage.take(150)
        }
    }

    open fun observeSyncMetadata(): Flow<SyncMetadata> {
        val ds = dataStore ?: return flowOf(SyncMetadata())
        return ds.data.map { prefs ->
            SyncMetadata(
                lastSyncTimestamp = prefs[PreferenceKeys.LAST_SYNC_TIMESTAMP] ?: 0L,
                lastSyncAttemptTimestamp = prefs[PreferenceKeys.LAST_SYNC_ATTEMPT_TIMESTAMP] ?: 0L,
                lastSyncStatus = prefs[PreferenceKeys.LAST_SYNC_STATUS] ?: "IDLE",
                lastSyncError = prefs[PreferenceKeys.LAST_SYNC_ERROR]?.ifEmpty { null }
            )
        }
    }
}
