package com.vivaanenterprise.app.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface SyncScheduler {
    fun enqueueSync()
}

@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SyncScheduler {

    companion object {
        const val UNIQUE_WORK_NAME = "firebase_sync_work"
    }

    override fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<FirebaseSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest
        )
    }
}
