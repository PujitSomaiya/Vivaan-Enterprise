package com.vivaanenterprise.app.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vivaanenterprise.app.core.datastore.AppPreferencesDataStore
import com.vivaanenterprise.app.core.datastore.SyncPreferencesDataStore
import com.vivaanenterprise.app.core.datastore.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideAppPreferencesDataStore(
        dataStore: DataStore<Preferences>
    ): AppPreferencesDataStore {
        return AppPreferencesDataStore(dataStore)
    }

    @Provides
    @Singleton
    fun provideSyncPreferencesDataStore(
        dataStore: DataStore<Preferences>
    ): SyncPreferencesDataStore {
        return SyncPreferencesDataStore(dataStore)
    }
}
