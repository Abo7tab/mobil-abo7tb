package com.abo7tb.childapp.utils

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

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

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        ).addOnFailureListener { error ->
            fusedLocationClient.removeLocationUpdates(callback)
            if (continuation.isActive) {
                continuation.resume(null)
            }
            Timber.w(error, "LocationHelper: getCurrentLocation failed")
        }

        continuation.invokeOnCancellation {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}
