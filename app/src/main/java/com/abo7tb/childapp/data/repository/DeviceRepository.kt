package com.abo7tb.childapp.data.repository

import com.abo7tb.childapp.data.local.SecurePrefsManager
import com.abo7tb.childapp.data.remote.ChildApiService
import com.abo7tb.childapp.data.remote.models.FcmTokenRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val apiService: ChildApiService,
    private val securePrefsManager: SecurePrefsManager
) {

    suspend fun registerDevice(request: com.abo7tb.childapp.data.remote.models.RegisterRequest): Result<com.abo7tb.childapp.data.remote.models.RegisterResponse> {
        return try {
            // First login to get the parent's Bearer token
            val loginRequest = com.abo7tb.childapp.data.remote.models.LoginRequest(
                email = request.parentEmail,
                password = request.parentPassword
            )
            val loginResponse = apiService.loginParent(loginRequest)
            if (!loginResponse.isSuccessful || loginResponse.body()?.data?.token == null) {
                return Result.failure(Exception("Failed to login parent: ${loginResponse.code()}"))
            }
            // Save the token so AuthInterceptor uses it
            val parentToken = loginResponse.body()!!.data!!.token
            securePrefsManager.saveToken(parentToken)

            fun createPart(value: String): okhttp3.RequestBody {
                return okhttp3.RequestBody.create(null, value)
            }
            
            val response = apiService.registerDevice(
                childName = createPart(request.childName),
                childAge = createPart(request.childAge.toString()),
                deviceName = createPart(request.deviceName),
                deviceModel = createPart(request.deviceModel),
                deviceBrand = createPart(request.deviceBrand),
                androidVersion = createPart(request.androidVersion),
                sdkVersion = createPart(request.sdkVersion.toString()),
                deviceId = createPart(request.deviceId),
                appVersion = createPart(request.appVersion)
            )
            if (response.isSuccessful && response.body() != null) {
                val registerData = response.body()!!.data!!
                securePrefsManager.saveUuid(registerData.deviceUuid)
                Result.success(registerData)
            } else {
                Result.failure(Exception("Failed to register: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(token: String): Result<Unit> {
        return try {
            val deviceUuid = securePrefsManager.getUuid()
                ?: return Result.failure(Exception("Device not registered"))
            
            val response = apiService.updateFcmToken(
                uuid = deviceUuid,
                request = FcmTokenRequest(
                    fcmToken = token,
                    pushEnabled = true
                )
            )
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDeviceSettings(): Map<String, Any> {
        val deviceUuid = securePrefsManager.getUuid() ?: return emptyMap()
        val response = apiService.getDeviceSettings(deviceUuid)
        return if (response.isSuccessful) response.body() ?: emptyMap() else emptyMap()
    }

    suspend fun verifyParent(email: String, password: String): Result<com.abo7tb.childapp.data.remote.models.ParentVerificationResponse> {
        return try {
            val deviceUuid = securePrefsManager.getUuid() ?: return Result.failure(Exception("Not registered"))
            val req = com.abo7tb.childapp.data.remote.models.ParentVerificationRequest(email, password)
            val response = apiService.verifyParent(deviceUuid, req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
