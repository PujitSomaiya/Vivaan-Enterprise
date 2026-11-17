package com.vivaanenterprise.app.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface SyncScheduler {
    fun enqueueSync(forceImmediate: Boolean = false, forceReconcile: Boolean = false)
    fun schedulePeriodicSync()
    fun cancelSync()
}

@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SyncScheduler {

    companion object {
        const val UNIQUE_WORK_NAME = "firebase_sync_work"
        const val PERIODIC_WORK_NAME = "firebase_periodic_sync_work"
        const val PERIODIC_INTERVAL_MINUTES = 15L
    }

    override fun enqueueSync(forceImmediate: Boolean, forceReconcile: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequestBuilder = OneTimeWorkRequestBuilder<FirebaseSyncWorker>()
            .setConstraints(constraints)

        if (forceReconcile) {
            syncRequestBuilder.setInputData(workDataOf(FirebaseSyncWorker.KEY_FORCE_RECONCILE to true))
        }

        val syncRequest = syncRequestBuilder.build()

        val policy = if (forceImmediate) {
            ExistingWorkPolicy.REPLACE
        } else {
            ExistingWorkPolicy.APPEND_OR_REPLACE
        }

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            policy,
            syncRequest
        )
    }

    override fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<FirebaseSyncWorker>(
            PERIODIC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }

    override fun cancelSync() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}
