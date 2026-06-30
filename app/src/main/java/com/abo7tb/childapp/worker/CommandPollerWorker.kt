package com.abo7tb.childapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abo7tb.childapp.domain.command.CommandExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class CommandPollerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val commandExecutor: CommandExecutor
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val executed = commandExecutor.pollAndExecuteCommands()
            Timber.d("CommandPollerWorker: executed $executed commands")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "CommandPollerWorker failed")
            Result.retry()
        }
    }
}
