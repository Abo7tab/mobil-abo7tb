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
    
    private suspend fun silentCapturePhoto(cameraId: String): Bitmap = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigurationMap = characteristics.get(android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = streamConfigurationMap?.getOutputSizes(android.graphics.ImageFormat.JPEG)
            val size = sizes?.maxByOrNull { it.width * it.height } ?: android.util.Size(640, 480)

            val imageReader = android.media.ImageReader.newInstance(size.width, size.height, android.graphics.ImageFormat.JPEG, 1)

            if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                continuation.resumeWithException(SecurityException("Camera permission not granted"))
                return@suspendCancellableCoroutine
            }

            cameraManager.openCamera(cameraId, object : android.hardware.camera2.CameraDevice.StateCallback() {
                override fun onOpened(camera: android.hardware.camera2.CameraDevice) {
                    try {
                        imageReader.setOnImageAvailableListener({ reader ->
                            val image = reader.acquireLatestImage()
                            if (image != null) {
                                val buffer = image.planes[0].buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                image.close()
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                camera.close()
                                if (continuation.isActive) continuation.resume(bitmap) {}
                            }
                        }, android.os.Handler(android.os.Looper.getMainLooper()))

                        val targets = listOf(imageReader.surface)
                        camera.createCaptureSession(targets, object : android.hardware.camera2.CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: android.hardware.camera2.CameraCaptureSession) {
                                try {
                                    val captureRequest = camera.createCaptureRequest(android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                        addTarget(imageReader.surface)
                                        set(android.hardware.camera2.CaptureRequest.JPEG_ORIENTATION, 90)
                                    }.build()
                                    session.capture(captureRequest, null, null)
                                } catch (e: Exception) {
                                    camera.close()
                                    if (continuation.isActive) continuation.resumeWithException(e)
                                }
                            }
                            override fun onConfigureFailed(session: android.hardware.camera2.CameraCaptureSession) {
                                camera.close()
                                if (continuation.isActive) continuation.resumeWithException(Exception("Camera configuration failed"))
                            }
                        }, null)
                    } catch (e: Exception) {
                        camera.close()
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }

                override fun onDisconnected(camera: android.hardware.camera2.CameraDevice) {
                    camera.close()
                    if (continuation.isActive) continuation.resumeWithException(Exception("Camera disconnected"))
                }

                override fun onError(camera: android.hardware.camera2.CameraDevice, error: Int) {
                    camera.close()
                    if (continuation.isActive) continuation.resumeWithException(Exception("Camera error: $error"))
                }
            }, null)
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }
}
