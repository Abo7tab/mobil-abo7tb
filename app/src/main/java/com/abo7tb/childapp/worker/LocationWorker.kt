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

import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

@HiltWorker
class LocationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiService: ChildApiService,
    private val securePrefsManager: SecurePrefsManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val uuid = securePrefsManager.getUuid() ?: return Result.failure()
            
            val fusedLocationClient = LocationServices
                .getFusedLocationProviderClient(applicationContext)
            
            // Check permissions explicitly to satisfy Lint, though this is a worker
            if (androidx.core.app.ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return Result.failure()
            }

            // Get last location
            val location = fusedLocationClient.lastLocation.await()
            
            if (location != null) {
                apiService.updateLocation(
                    uuid = uuid,
                    request = com.abo7tb.childapp.data.remote.models.LocationRequest(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        recordedAt = System.currentTimeMillis()
                    )
                )
                Timber.d("Location updated: lat=${location.latitude}, lng=${location.longitude}")
                Result.success()
            } else {
                Timber.d("Location was null")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Location tracking failed")
            Result.retry()
        }
    }
}
