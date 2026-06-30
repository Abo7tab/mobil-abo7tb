package com.abo7tb.childapp.data.repository

import android.graphics.Bitmap
import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.data.remote.ChildApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val apiService: ChildApiService,
    private val securePrefsManager: SecurePrefsManager
) {
    suspend fun uploadScreenshot(bitmap: Bitmap, triggerType: String, triggerApp: String, metadata: Map<String, String>) {
        val uuid = securePrefsManager.getUuid() ?: return
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        val body = MultipartBody.Part.createFormData(
            "file", 
            "screenshot_${System.currentTimeMillis()}.png", 
            byteArray.toRequestBody("image/png".toMediaTypeOrNull())
        )
        apiService.uploadScreenshot(uuid, body, triggerType.toRequestBody(), triggerApp.toRequestBody())
    }

    suspend fun uploadCameraPhoto(bitmap: Bitmap, captureType: String, cameraFacing: String, triggerType: String, metadata: Map<String, String>) {
        val uuid = securePrefsManager.getUuid() ?: return
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val byteArray = stream.toByteArray()
        val body = MultipartBody.Part.createFormData(
            "file", 
            "camera_${System.currentTimeMillis()}.jpg", 
            byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
        )
        apiService.uploadCameraPhoto(uuid, body, triggerType.toRequestBody())
    }
}
