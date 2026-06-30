package com.abo7tb.childapp.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkerHelper {

    fun enqueueAllWorkers(context: Context) {
        enqueueDailySyncWorkers(context)
        enqueueLocationWorker(context)
        enqueueCommandPollerWorker(context)
        enqueueSettingsSyncWorker(context)
        enqueueImmediateLocationSync(context)
        enqueueImmediateCommandPoll(context)
    }

    fun enqueueDailySyncWorkers(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_data_sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }

    fun enqueueLocationWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val locationRequest = PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "location_tracking",
            ExistingPeriodicWorkPolicy.UPDATE,
            locationRequest
        )
    }

    fun enqueueCommandPollerWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val pollRequest = PeriodicWorkRequestBuilder<CommandPollerWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "command_poller",
            ExistingPeriodicWorkPolicy.UPDATE,
            pollRequest
        )
    }

    fun enqueueSettingsSyncWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val settingsRequest = PeriodicWorkRequestBuilder<SettingsSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "settings_sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            settingsRequest
        )
    }

    fun enqueueImmediateLocationSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<LocationWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "location_immediate",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun enqueueImmediateCommandPoll(context: Context) {
        val request = OneTimeWorkRequestBuilder<CommandPollerWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "command_poll_immediate",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
