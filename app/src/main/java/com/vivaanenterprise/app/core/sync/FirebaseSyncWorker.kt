package com.vivaanenterprise.app.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vivaanenterprise.app.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FirebaseSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_FORCE_RECONCILE = "key_force_reconcile"
    }

    override suspend fun doWork(): Result {
        return try {
            val forceReconcile = inputData.getBoolean(KEY_FORCE_RECONCILE, false)
            val result = syncRepository.synchronize(forceReconcile = forceReconcile)
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

}
