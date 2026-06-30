package com.abo7tb.childapp.data.remote

import com.abo7tb.childapp.data.remote.models.ApiResponse
import com.abo7tb.childapp.data.remote.models.CallsRequest
import com.abo7tb.childapp.data.remote.models.CommandStatusRequest
import com.abo7tb.childapp.data.remote.models.ConsentAcceptRequest
import com.abo7tb.childapp.data.remote.models.ContactsRequest
import com.abo7tb.childapp.data.remote.models.FcmTokenRequest
import com.abo7tb.childapp.data.remote.models.LocationRequest
import com.abo7tb.childapp.data.remote.models.LoginRequest
import com.abo7tb.childapp.data.remote.models.LoginResponse
import com.abo7tb.childapp.data.remote.models.ParentVerificationRequest
import com.abo7tb.childapp.data.remote.models.ParentVerificationResponse
import com.abo7tb.childapp.data.remote.models.PendingCommandsResponse
import com.abo7tb.childapp.data.remote.models.RegisterResponse
import com.abo7tb.childapp.data.remote.models.SmsRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ChildApiService {

    @POST("auth/login")
    suspend fun loginParent(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponse>>

    @retrofit2.http.Multipart
    @POST("devices/register")
    suspend fun registerDevice(
        @retrofit2.http.Part("child_name") childName: okhttp3.RequestBody,
        @retrofit2.http.Part("child_age") childAge: okhttp3.RequestBody,
        @retrofit2.http.Part("device_name") deviceName: okhttp3.RequestBody,
        @retrofit2.http.Part("device_model") deviceModel: okhttp3.RequestBody,
        @retrofit2.http.Part("device_brand") deviceBrand: okhttp3.RequestBody,
        @retrofit2.http.Part("android_version") androidVersion: okhttp3.RequestBody,
        @retrofit2.http.Part("sdk_version") sdkVersion: okhttp3.RequestBody,
        @retrofit2.http.Part("device_id") deviceId: okhttp3.RequestBody,
        @retrofit2.http.Part("app_version") appVersion: okhttp3.RequestBody
    ): Response<ApiResponse<RegisterResponse>>

    @POST("devices/{uuid}/heartbeat")
    suspend fun sendHeartbeat(
        @Path("uuid") uuid: String,
        @Body data: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @POST("devices/{uuid}/consent/accept")
    suspend fun acceptConsent(
        @Path("uuid") uuid: String,
        @Body request: ConsentAcceptRequest
    ): Response<Unit>

    @POST("devices/{uuid}/verify-parent")
    suspend fun verifyParent(
        @Path("uuid") uuid: String,
        @Body request: ParentVerificationRequest
    ): Response<ParentVerificationResponse>

    @GET("devices/{uuid}/commands/pending")
    suspend fun getPendingCommands(
        @Path("uuid") uuid: String
    ): Response<ApiResponse<PendingCommandsResponse>>

    @PATCH("commands/{cmd_uuid}/status")
    suspend fun updateCommandStatus(
        @Path("cmd_uuid") cmdUuid: String,
        @Body statusData: CommandStatusRequest
    ): Response<Unit>

    @POST("devices/{uuid}/push-token")
    suspend fun updateFcmToken(
        @Path("uuid") uuid: String,
        @Body request: FcmTokenRequest
    ): Response<Unit>

    @retrofit2.http.Multipart
    @POST("devices/{uuid}/screenshot/upload")
    suspend fun uploadScreenshot(
        @Path("uuid") uuid: String,
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part,
        @retrofit2.http.Part("trigger_type") triggerType: okhttp3.RequestBody,
        @retrofit2.http.Part("trigger_app") triggerApp: okhttp3.RequestBody
    ): Response<Unit>

    @retrofit2.http.Multipart
    @POST("devices/{uuid}/camera/upload")
    suspend fun uploadCameraPhoto(
        @Path("uuid") uuid: String,
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part,
        @retrofit2.http.Part("trigger_type") triggerType: okhttp3.RequestBody
    ): Response<Unit>

    @GET("devices/{uuid}/settings")
    suspend fun getDeviceSettings(@Path("uuid") uuid: String): Response<Map<String, @JvmSuppressWildcards Any>>

    @POST("devices/{uuid}/location")
    suspend fun updateLocation(
        @Path("uuid") uuid: String,
        @Body request: LocationRequest
    ): Response<Unit>

    @POST("devices/{uuid}/contacts/sync")
    suspend fun syncContacts(
        @Path("uuid") uuid: String,
        @Body request: ContactsRequest
    ): Response<Unit>

    @POST("devices/{uuid}/sms/sync")
    suspend fun syncSms(
        @Path("uuid") uuid: String,
        @Body request: SmsRequest
    ): Response<Unit>

    @POST("devices/{uuid}/calls/sync")
    suspend fun syncCalls(
        @Path("uuid") uuid: String,
        @Body request: CallsRequest
    ): Response<Unit>
}
