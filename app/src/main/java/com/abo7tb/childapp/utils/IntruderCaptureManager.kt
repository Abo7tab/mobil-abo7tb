package com.abo7tb.childapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CameraManager
import com.abo7tb.childapp.data.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntruderCaptureManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository
) {
    private val captureScope = CoroutineScope(Dispatchers.IO)
    
    fun captureIntruder(attemptNumber: Int, parentEmail: String?) {
        if (attemptNumber < 3) return
        
        captureScope.launch {
            try {
                captureScreenshot(attemptNumber, parentEmail)
                captureFrontCamera(attemptNumber, parentEmail)
            } catch (e: Exception) {
                Timber.e(e, "Failed to capture intruder")
            }
        }
    }
    
    private suspend fun captureScreenshot(
        attemptNumber: Int, 
        parentEmail: String?
    ) {
        try {
            val screenshot = takeScreenshotInternal()
            
            mediaRepository.uploadScreenshot(
                bitmap = screenshot,
                triggerType = "alert",
                triggerApp = "verify_parent_failed_attempt",
                metadata = mapOf(
                    "reason" to "failed_password_attempt",
                    "attempt_number" to attemptNumber.toString(),
                    "attempted_email" to (parentEmail ?: "unknown"),
                    "timestamp" to System.currentTimeMillis().toString()
                )
            )
            Timber.d("IntruderCapture: Screenshot uploaded")
        } catch (e: Exception) {
            Timber.e(e, "Screenshot capture failed")
        }
    }
    
    private suspend fun captureFrontCamera(
        attemptNumber: Int,
        parentEmail: String?
    ) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val frontCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING) == 
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT
            } ?: return
            
            val photo = silentCapturePhoto(frontCameraId)
            
            mediaRepository.uploadCameraPhoto(
                bitmap = photo,
                captureType = "photo",
                cameraFacing = "front",
                triggerType = "alert",
                metadata = mapOf(
                    "reason" to "failed_password_attempt",
                    "attempt_number" to attemptNumber.toString(),
                    "attempted_email" to (parentEmail ?: "unknown"),
                    "timestamp" to System.currentTimeMillis().toString(),
                    "silent" to "true"
                )
            )
            Timber.d("IntruderCapture: Front camera photo uploaded")
        } catch (e: Exception) {
            Timber.e(e, "Front camera capture failed")
        }
    }
    
    private suspend fun takeScreenshotInternal(): Bitmap {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
    
    private suspend fun silentCapturePhoto(cameraId: String): Bitmap {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
}
