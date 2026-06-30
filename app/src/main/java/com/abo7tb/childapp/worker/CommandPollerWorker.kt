package com.abo7tb.childapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.data.remote.ChildApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class CommandPollerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiService: ChildApiService,
    private val securePrefsManager: SecurePrefsManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val uuid = securePrefsManager.getUuid() ?: return Result.failure()
        return try {
            val response = apiService.getPendingCommands(uuid)
            if (response.isSuccessful) {
                Timber.d("Commands fetched successfully")
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.retry()
        }
    }
}
