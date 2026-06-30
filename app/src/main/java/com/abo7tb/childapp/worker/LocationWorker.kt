package com.abo7tb.childapp.worker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.data.remote.ChildApiService
import com.abo7tb.childapp.data.remote.models.LocationRequest as ApiLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.w("LocationWorker: fine location permission missing")
                return Result.failure()
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            val location = fetchCurrentLocation(fusedLocationClient)
                ?: run {
                    try {
                        com.google.android.gms.tasks.Tasks.await(
                            fusedLocationClient.lastLocation,
                            10,
                            java.util.concurrent.TimeUnit.SECONDS
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

            if (location != null) {
                val response = apiService.updateLocation(
                    uuid = uuid,
                    request = ApiLocationRequest(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy
                    )
                )
                Timber.d("LocationWorker: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}, HTTP ${response.code()}")
                if (response.isSuccessful) Result.success() else Result.retry()
            } else {
                Timber.w("LocationWorker: location was null")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "LocationWorker: tracking failed")
            Result.retry()
        }
    }

    private suspend fun fetchCurrentLocation(
        fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    ) = suspendCancellableCoroutine { continuation ->
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMaxUpdates(1)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                if (continuation.isActive) {
                    continuation.resume(result.lastLocation)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        ).addOnFailureListener { error ->
            fusedLocationClient.removeLocationUpdates(callback)
            if (continuation.isActive) {
                continuation.resume(null)
            }
            Timber.w(error, "LocationWorker: getCurrentLocation failed")
        }

        continuation.invokeOnCancellation {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}
