package com.vivaanenterprise.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SyncPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferenceKeys {
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    open suspend fun getLastSyncTimestamp(): Long {
        val prefs = dataStore.data.first()
        return prefs[PreferenceKeys.LAST_SYNC_TIMESTAMP] ?: 0L
    }

    open suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }
}

