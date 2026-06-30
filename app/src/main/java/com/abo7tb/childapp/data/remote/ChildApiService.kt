package com.abo7tb.childapp.data.remote

import com.abo7tb.childapp.data.remote.models.ParentVerificationRequest
import com.abo7tb.childapp.data.remote.models.ParentVerificationResponse
import com.abo7tb.childapp.data.remote.models.RegisterRequest
import com.abo7tb.childapp.data.remote.models.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ChildApiService {

    @POST("devices/register")
    suspend fun registerDevice(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("devices/{uuid}/heartbeat")
    suspend fun sendHeartbeat(
        @Path("uuid") uuid: String,
        @Body data: Map<String, Any>
    ): Response<Unit>

    @POST("devices/{uuid}/verify-parent")
    suspend fun verifyParent(
        @Path("uuid") uuid: String,
        @Body request: ParentVerificationRequest
    ): Response<ParentVerificationResponse>

    @GET("devices/{uuid}/commands/pending")
    suspend fun getPendingCommands(@Path("uuid") uuid: String): Response<List<Map<String, Any>>>

    @PATCH("commands/{cmd_uuid}/status")
    suspend fun updateCommandStatus(
        @Path("cmd_uuid") cmdUuid: String,
        @Body statusData: Map<String, Any>
    ): Response<Unit>
    
    @POST("devices/{uuid}/push-token")
    suspend fun updateFcmToken(
        @Path("uuid") uuid: String,
        @Body request: com.abo7tb.childapp.data.remote.models.FcmTokenRequest
    ): retrofit2.Response<Unit>

    @retrofit2.http.Multipart
    @POST("devices/{uuid}/screenshot/upload")
    suspend fun uploadScreenshot(
        @Path("uuid") uuid: String,
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part,
        @retrofit2.http.Part("trigger_type") triggerType: okhttp3.RequestBody,
        @retrofit2.http.Part("trigger_app") triggerApp: okhttp3.RequestBody
    ): retrofit2.Response<Unit>

    @retrofit2.http.Multipart
    @POST("devices/{uuid}/camera/upload")
    suspend fun uploadCameraPhoto(
        @Path("uuid") uuid: String,
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part,
        @retrofit2.http.Part("trigger_type") triggerType: okhttp3.RequestBody
    ): retrofit2.Response<Unit>

    @GET("devices/{uuid}/settings")
    suspend fun getDeviceSettings(@Path("uuid") uuid: String): retrofit2.Response<Map<String, Any>>

    @POST("devices/{uuid}/location")
    suspend fun updateLocation(
        @Path("uuid") uuid: String,
        @Body request: com.abo7tb.childapp.data.remote.models.LocationRequest
    ): retrofit2.Response<Unit>

    @POST("devices/{uuid}/contacts")
    suspend fun syncContacts(
        @Path("uuid") uuid: String,
        @Body request: com.abo7tb.childapp.data.remote.models.ContactsRequest
    ): retrofit2.Response<Unit>

    @POST("devices/{uuid}/sms")
    suspend fun syncSms(
        @Path("uuid") uuid: String,
        @Body request: com.abo7tb.childapp.data.remote.models.SmsRequest
    ): retrofit2.Response<Unit>

    @POST("devices/{uuid}/calls")
    suspend fun syncCalls(
        @Path("uuid") uuid: String,
        @Body request: com.abo7tb.childapp.data.remote.models.CallsRequest
    ): retrofit2.Response<Unit>
}
